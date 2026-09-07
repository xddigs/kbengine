package com.isofarm.entity;

import com.isofarm.data.*;
import com.isofarm.graphics.gltf.GLTFLoader;
import com.isofarm.graphics.gltf.GLTFModel;
import com.isofarm.graphics.gltf.GLTFNode;
import com.isofarm.graphics.*;
import com.isofarm.service.SoundService;
import com.isofarm.service.TimeService;
import com.isofarm.utils.Naming;
import com.isofarm.utils.Settings;
import com.isofarm.wrld.GameMaster;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

/**
 * Represents an NPC, with their own state and behavior, in contrast to {@link Player},
 * who starts with their own {@link Singleton} instance. Inherits from {@link Character}.
 */
@SuppressWarnings("all")
@DataClass
public class NPC extends Character {
    private static final float MOVE_THRESHOLD = 0.03f;
    private static final float WALK_SPEED = 1.25f;
    private static final float MIN_IDLE_TIME = 2.0f;
    private static final float IDLE_TIME_VARIATION = 4.0f;
    private static final float WANDER_RADIUS = 5.0f;
    private final Matrix4f modelMatrix = new Matrix4f();
    private final NPCGender gender;
    private final Job job;
    private final GLTFModel npcModel;
    private final Vector3f home = new Vector3f();
    private final Vector3f destination = new Vector3f();
    private GLTFNode head;
    private GLTFNode body;
    private GLTFNode rightArm;
    private GLTFNode leftArm;
    private GLTFNode rightLeg;
    private GLTFNode leftLeg;
    private Quaternionf baseHeadRotation;
    private float yaw;
    private float walkTime;
    private float idleTime;
    private float idleTimer;
    private boolean walking;

    /**
     * Creates an NPC and loads the model registered by its job.
     *
     * @param gender the voice and localized gender of the character
     * @param job the job that supplies its model and interaction type
     */
    public NPC(NPCGender gender, Job job) {
        super(Naming.nm.fullName());
        this.gender = gender;
        this.job = job;
        this.npcModel = GLTFLoader.load(job.getModelPath());
        setDimensions(0.6f, 1.8f, 0.6f);
        setMaxHitpoints(20);
        setHitpoints(20);
        setMaxStamina(100);
        setStamina(100);
        setGamemode(Gamemode.SURVIVAL);
        setSpeed(WALK_SPEED);
        bindNodes();
        chooseIdleDuration();
    }

    /**
     * Creates a farmer NPC with the supplied gender.
     *
     * @param gender the voice and localized gender of the character
     */
    public NPC(NPCGender gender) {
        this(gender, Job.FARMER);
    }

    /** Creates a female farmer NPC. */
    public NPC() {
        this(NPCGender.FEMALE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(BlockPos blockPos, float delta) {
        if (!isAlive() || delta <= 0.0f) return;
        updateBehavior(delta);
        animate(delta);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(GameMaster gameMaster, RenderPass pass) {
        if (!isAlive() || npcModel == null) return;
        Shader shader = pass == RenderPass.SHADOW
                ? ResourceManager.rem.getShadowMapShader()
                : ResourceManager.rem.getDefaultShader();
        if (shader == null) return;

        float scale = Settings.getScaledEntity();
        float bob = walking ? (float) Math.abs(Math.sin(walkTime * 2.0f)) * 0.025f : 0.0f;
        modelMatrix.identity().translate(position.x, position.y + bob, position.z)
                .rotateY(yaw).scale(scale);
        shader.bind();
        if (pass == RenderPass.SHADOW) {
            shader.setUniform("uLightSpaceMatrix", ShadowSystem.sys.getLightSpaceMatrix());
            shader.setUniform("uAlphaTest", true);
            glCullFace(GL_FRONT);
            npcModel.render(shader, modelMatrix);
            glCullFace(GL_BACK);
        } else {
            CameraView camera = gameMaster.getActiveCamera();
            CelestialLighting light = gameMaster.getCelestialLighting();
            shader.setUniform("uProjection", camera.getProjectionMatrix());
            shader.setUniform("uView", camera.getViewMatrix());
            shader.setUniform("uLightIntensity", light.getIntensity());
            shader.setUniform("uLightDirection", light.getDirection());
            shader.setUniform("uAmbientIntensity", light.getAmbientIntensity());
            shader.setUniform("uSkyColor", TimeService.getSkyColor());
            shader.setUniform("uBaseColor", new Vector3f(1.0f));
            shader.setUniform("uIsSprite", false);
            shader.setUniform("uUseTexture", true);
            shader.setUniform("uParticleAlpha", 1.0f);
            shader.setUniform("uIsMaskPass", false);
            shader.setUniform("uEnableShadows", Settings.doEnableShadows());
            shader.setUniform("uLightSpaceMatrix", ShadowSystem.sys.getLightSpaceMatrix());
            shader.setUniform("uIsSubmergedEntity", pass == RenderPass.SUBMERGED);
            npcModel.render(shader, modelMatrix);
        }
        shader.unbind();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void damage(float amount) {
        super.damage(amount);
        grunt(gender);
    }

    /**
     * Binds the NPC's model nodes to their respective names
     */
    private void bindNodes() {
        head = npcModel.findNode("Head");
        body = npcModel.findNode("Body");
        rightArm = npcModel.findNode("Right Arm");
        leftArm = npcModel.findNode("Left Arm");
        rightLeg = npcModel.findNode("Right Leg");
        leftLeg = npcModel.findNode("Left Leg");
        hideNode("sword");
        hideNode("pickaxe");
        hideNode("axe");
        hideNode("hoe");
        hideNode("shovel");
        if (head != null) baseHeadRotation = new Quaternionf(head.getRotation());
    }

    /**
     * Hides the node with the supplied name.
     * @param name the name of the node to hide
     */
    private void hideNode(String name) {
        GLTFNode node = npcModel.findNode(name);
        if (node != null) node.setVisible(false);
    }

    /**
     * Updates the NPC's behavior.
     * @param delta the {@code float} argument; frame time in seconds
     */
    private void updateBehavior(float delta) {
        if (walking) {
            Vector3f offset = new Vector3f(destination).sub(position);
            offset.y = 0.0f;
            if (offset.lengthSquared() < 0.16f) {
                walking = false;
                setVelocity(0.0f, getVelocity().y, 0.0f);
                chooseIdleDuration();
            } else {
                offset.normalize(WALK_SPEED);
                setVelocity(offset.x, getVelocity().y, offset.z);
                yaw = (float) Math.atan2(offset.x, offset.z) + (float) Math.PI;
            }
        } else {
            idleTimer -= delta;
            setVelocity(0.0f, getVelocity().y, 0.0f);
            if (idleTimer <= 0.0f) chooseDestination();
        }
        collide(GameMaster.game.getWorld(), new Vector3f(velocity.x, 0.0f, velocity.z), delta);
    }

    /** Wanders randomly around the NPC's home point. */
    private void chooseDestination() {
        float angle = (float) (Math.random() * Math.PI * 2.0);
        float radius = (float) Math.sqrt(Math.random()) * WANDER_RADIUS;
        destination.set(home).add((float) Math.sin(angle) * radius, 0.0f,
                (float) Math.cos(angle) * radius);
        walking = true;
    }

    /**
     * Chooses a random duration for the NPC to idle.
     */
    private void chooseIdleDuration() {
        idleTimer = MIN_IDLE_TIME + (float) Math.random() * IDLE_TIME_VARIATION;
    }

    /**
     * Animates the NPC's model based on its current velocity.
     * @param delta the {@code float} argument; frame time in seconds
     */
    private void animate(float delta) {
        boolean moving = walking && (Math.abs(velocity.x) > MOVE_THRESHOLD
                || Math.abs(velocity.z) > MOVE_THRESHOLD);
        float weight = moving ? 1.0f : 0.0f;
        if (moving) walkTime += delta * 8.0f;
        else idleTime += delta * 2.0f;
        float swing = (float) Math.sin(walkTime) * 0.55f * weight;
        float breath = (float) Math.sin(idleTime) * 0.035f * (1.0f - weight);
        rotate(body, new Quaternionf().rotateX(breath));
        rotate(rightArm, new Quaternionf().rotateX(swing + breath));
        rotate(leftArm, new Quaternionf().rotateX(-swing + breath));
        rotate(rightLeg, new Quaternionf().rotateX(-swing));
        rotate(leftLeg, new Quaternionf().rotateX(swing));
        if (head != null && baseHeadRotation != null) {
            rotate(head, new Quaternionf(baseHeadRotation).rotateZ(breath * 0.35f));
        }
        npcModel.updateTransforms();
    }

    /**
     * Translates the supplied node by the supplied offset.
     * @param node the {@link GLTFNode} to translate
     * @param rotation the {@link Quaternionf} to rotate the node by
     */
    private static void rotate(GLTFNode node, Quaternionf rotation) {
        if (node != null) node.setRotation(rotation);
    }

    /**
     * Sets the center point used by the NPC's wandering behavior.
     * @param position the new world-space home and current position
     */
    public void setHome(Vector3f position) {
        setPosition(position);
        home.set(position);
        destination.set(position);
    }

    /**
     * Returns the distance where a ray enters this NPC's collision box.
     * @param origin the world-space ray origin
     * @param direction the normalized world-space ray direction
     * @return the ray distance, or positive infinity when it misses
     */
    public float rayIntersection(Vector3f origin, Vector3f direction) {
        float minX = position.x - dimensions.x * 0.5f;
        float maxX = position.x + dimensions.x * 0.5f;
        float minY = position.y;
        float maxY = position.y + dimensions.y;
        float minZ = position.z - dimensions.z * 0.5f;
        float maxZ = position.z + dimensions.z * 0.5f;
        float tMin = 0.0f;
        float tMax = Float.POSITIVE_INFINITY;
        float[] origins = {origin.x, origin.y, origin.z};
        float[] directions = {direction.x, direction.y, direction.z};
        float[] mins = {minX, minY, minZ};
        float[] maxes = {maxX, maxY, maxZ};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(directions[axis]) < 0.00001f) {
                if (origins[axis] < mins[axis] || origins[axis] > maxes[axis]) {
                    return Float.POSITIVE_INFINITY;
                }
                continue;
            }
            float first = (mins[axis] - origins[axis]) / directions[axis];
            float second = (maxes[axis] - origins[axis]) / directions[axis];
            if (first > second) {
                float swap = first;
                first = second;
                second = swap;
            }
            tMin = Math.max(tMin, first);
            tMax = Math.min(tMax, second);
            if (tMax < tMin) return Float.POSITIVE_INFINITY;
        }
        return tMin;
    }

    /** Plays this character's normal gender-specific voice response. */
    public void speak() {
        SoundService.fx.playGenderSound(SoundGroup.NPC, gender.getSoundIndex(false, false));
    }

    /** Plays this character's male gender-specific voice response. */
    public void grunt(NPCGender gender) {
        SoundService fx = SoundService.fx;
        if (gender.equals(NPCGender.MALE)) fx.playEntitySound(SoundGroup.ENTITY);
        fx.playGenderSound(SoundGroup.NPC, gender.getSoundIndex(true, false));
    }

    /**
     * Retrieves {@code job}
     * @return {@link Job} value of job
     */
    public Job getJob() {
        return job;
    }

    /**
     * Returns this NPC's gender.
     *
     * @return the NPC gender used for localization and voice selection
     */
    public NPCGender getGender() {
        return gender;
    }

    /**
     * Retrieves {@code npcModel}
     * @return {@link GLTFModel} value of npcModel
     */
    public GLTFModel getNpcModel() {
        return npcModel;
    }
}
