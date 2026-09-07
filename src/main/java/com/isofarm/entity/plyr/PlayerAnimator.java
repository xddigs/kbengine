package com.isofarm.entity.plyr;

import com.isofarm.data.Direction;
import com.isofarm.data.Ray;
import com.isofarm.data.RenderPass;
import com.isofarm.entity.Player;
import com.isofarm.entity.states.SneakingState;
import com.isofarm.graphics.*;
import com.isofarm.graphics.gltf.GLTFModel;
import com.isofarm.graphics.gltf.GLTFNode;
import com.isofarm.input.Mouse;
import com.isofarm.item.*;
import com.isofarm.service.TimeService;
import com.isofarm.utils.Settings;
import com.isofarm.wrld.GameMaster;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static org.joml.Math.lerp;
import static org.lwjgl.opengl.GL13.*;

/**
 * Owns the player model, its rendering, facing and procedural animation.
 */
public final class PlayerAnimator {
    private static final float ZERO = 0.0f, MOVE_THRESHOLD = 0.05f;
    private static final float ROTATION_SPEED = 720.0f, FULL_DEGREES = 360.0f, HALF_DEGREES = 180.0f;
    private static final float MAX_HEAD_YAW = (float) Math.toRadians(65), MAX_HEAD_PITCH = (float) Math.toRadians(35);
    private static final float HEAD_SPEED = 12.0f;
    private static final float DEATH_FALL_DURATION = 0.75f;
    private static final float DEATH_FADE_DURATION = 0.75f;
    private Player player;
    private final Matrix4f modelMatrix = new Matrix4f();
    private GLTFModel model;
    private GLTFNode head, torso, backpack, rightArm, leftArm, rightLeg, leftLeg;
    private Quaternionf baseHeadRotation;
    private Vector3f headPosition, torsoPosition, backpackPosition, rightArmPosition, leftArmPosition, rightLegPosition, leftLegPosition;
    private Direction direction = Direction.S;
    private float modelYaw, targetYaw, idleTime, idleWeight, sneakWeight, walkTime, walkWeight, attackTime;
    private float deathTime, deathWeight, deathAlpha = 1.0f;
    private boolean attacking;

    /**
     * Creates the shared player's animator.
     */
    public PlayerAnimator() {}

    /**
     * Initializes model state after singleton construction completes.
     */
    public void initialize() {
        player = Player.plyr;
        model = ResourceManager.rem.getPlayerModel();
        if (model == null) return;
        head = node("Head");
        torso = node("Body");
        backpack = node("Backpack");
        rightArm = node("Right Arm");
        leftArm = node("Left Arm");
        rightLeg = node("Right Leg");
        leftLeg = node("Left Leg");
        EquipmentController.ec.init(model);

        if (head != null) {
            headPosition = copy(head);
            baseHeadRotation = new Quaternionf(head.getRotation());
        }

        torsoPosition = copy(torso);
        backpackPosition = copy(backpack);
        rightArmPosition = copy(rightArm);
        leftArmPosition = copy(leftArm);
        rightLegPosition = copy(rightLeg);
        leftLegPosition = copy(leftLeg);
        if (backpack != null) backpack.setVisible(false);
    }

    private GLTFNode node(String name) { return model.findNode(name); }
    private static Vector3f copy(GLTFNode node) { return node == null ? null : new Vector3f(node.getTranslation()); }

    /**
     * Updates this object for the current simulation step.
     * @param delta the {@code float} argument; frame time in seconds
     */
    public void update(float delta) {
        if (!player.isAlive()) {
            updateDeath(delta);
            return;
        }
        deathTime = 0.0f;
        deathWeight = 0.0f;
        deathAlpha = 1.0f;
        if (backpack != null) backpack.setVisible(player.getInventory().hasBackpackEquipped());
        Vector3f velocity = player.getVelocity();
        boolean moving = Math.abs(velocity.x) > MOVE_THRESHOLD || Math.abs(velocity.z) > MOVE_THRESHOLD;
        if (moving) {
            updateFacing(velocity);
        } else {
            updateFacingCursor();
        }
        updateRotation(delta);
        boolean sneaking = player.getCurrentState() instanceof SneakingState;
        sneakWeight = lerp(sneakWeight, sneaking ? 1 : ZERO, Math.clamp(delta * 75, ZERO, 1));
        if (moving) {
            walkTime += delta * (sneaking ? 7 : 10);
            walkWeight = lerp(walkWeight, 1, Math.clamp(delta * 10, ZERO, 1));
            idleWeight = lerp(idleWeight, ZERO, Math.clamp(delta * 8, ZERO, 1));
        } else {
            walkWeight = lerp(walkWeight, ZERO, Math.clamp(delta * 10, ZERO, 1));
            idleWeight = lerp(idleWeight, 1, Math.clamp(delta * 5, ZERO, 1));
            idleTime += delta * 2.5f;
        }
        float swing = (float) Math.sin(walkTime) * lerp(.45f, .25f, sneakWeight) * walkWeight;
        float breath = (float) Math.sin(idleTime) * .05f * idleWeight * (1 - sneakWeight * .5f);
        float sway = (float) Math.cos(idleTime * .5f) * .02f * idleWeight;
        float offset = .08f * sneakWeight, lean = (float) Math.toRadians(20) * sneakWeight;
        float armBend = (float) Math.toRadians(15) * sneakWeight;
        float attackX = 0, attackY = 0, attackZ = 0;
        if (attacking) {
            attackTime += delta * 10;
            float progress = Math.min(attackTime / (float) Math.PI, 1);
            float curve = (float) Math.sin(Math.sqrt(progress) * Math.PI);
            attackX = curve * 1.35f; attackY = curve * 1.35f * .25f;
            attackZ = (float) Math.sin(progress * Math.PI) * -1.35f * .4f;
            if (attackTime >= Math.PI) { attacking = false; attackTime = 0; }
        }
        translate(head, headPosition, 0, -offset, 0); translate(torso, torsoPosition, 0, -offset, 0);
        translate(backpack, backpackPosition, 0, offset, 0); translate(rightArm, rightArmPosition, 0, -offset, 0);
        translate(leftArm, leftArmPosition, 0, -offset, 0);
        translate(rightLeg, rightLegPosition, 0, 0, .18f * sneakWeight);
        translate(leftLeg, leftLegPosition, 0, 0, .18f * sneakWeight);
        rotate(torso, new Quaternionf().rotateX(-lean + breath));
        rotate(backpack, new Quaternionf().rotateX(-lean + breath));
        rotate(leftArm, new Quaternionf().rotateX(-swing + breath - armBend).rotateZ(-sway));
        rotate(rightArm, new Quaternionf().rotateX(swing + breath - armBend + attackX).rotateY(attackY).rotateZ(sway + attackZ));
        rotate(rightLeg, new Quaternionf().rotateX(-swing)); rotate(leftLeg, new Quaternionf().rotateX(swing));
        updateHead(delta);
        if (model != null) model.updateTransforms();
        updateEquipment();
    }

    /**
     * Advances a short procedural ragdoll pose while awaiting respawn.
     */
    private void updateDeath(float delta) {
        deathTime += delta;
        float progress = Math.min(deathTime / DEATH_FALL_DURATION, 1.0f);
        deathWeight = 1.0f - (float) Math.pow(1.0f - progress, 3.0f);
        float fadeProgress = Math.clamp(
                (deathTime - DEATH_FALL_DURATION) / DEATH_FADE_DURATION, 0.0f, 1.0f);
        deathAlpha = 1.0f - fadeProgress;
        attacking = false;
        attackTime = 0.0f;

        float loosen = deathWeight;
        translate(head, headPosition, 0.0f, -0.08f * loosen, 0.0f);
        translate(torso, torsoPosition, 0.0f, -0.04f * loosen, 0.0f);
        translate(backpack, backpackPosition, 0.0f, 0.04f * loosen, 0.0f);
        translate(rightArm, rightArmPosition, 0.0f, -0.05f * loosen, 0.0f);
        translate(leftArm, leftArmPosition, 0.0f, -0.05f * loosen, 0.0f);
        translate(rightLeg, rightLegPosition, 0.0f, 0.0f, 0.0f);
        translate(leftLeg, leftLegPosition, 0.0f, 0.0f, 0.0f);

        rotate(torso, new Quaternionf().rotateX((float) Math.toRadians(8.0f) * loosen));
        rotate(backpack, new Quaternionf().rotateX((float) Math.toRadians(8.0f) * loosen));
        if (baseHeadRotation != null) {
            rotate(head, new Quaternionf(baseHeadRotation)
                    .rotateZ((float) Math.toRadians(-18.0f) * loosen));
        }
        rotate(leftArm, new Quaternionf().rotateX((float) Math.toRadians(-35.0f) * loosen)
                .rotateZ((float) Math.toRadians(-28.0f) * loosen));
        rotate(rightArm, new Quaternionf().rotateX((float) Math.toRadians(25.0f) * loosen)
                .rotateZ((float) Math.toRadians(32.0f) * loosen));
        rotate(leftLeg, new Quaternionf().rotateX((float) Math.toRadians(18.0f) * loosen));
        rotate(rightLeg, new Quaternionf().rotateX((float) Math.toRadians(-12.0f) * loosen));

        if (model != null) model.updateTransforms();
        EquipmentController.ec.equip(null, null);
    }

    private static void translate(GLTFNode node, Vector3f base, float x, float y, float z) {
        if (node != null && base != null) node.setTranslation(new Vector3f(base).add(x, y, z));
    }
    private static void rotate(GLTFNode node, Quaternionf rotation) { if (node != null) node.setRotation(rotation); }

    private void updateFacing(Vector3f velocity) {
        float raw = (float) Math.toDegrees(Math.atan2(velocity.x, velocity.z));
        setTargetYaw(raw + 180.0f);
    }

    private void updateFacingCursor() {
        float worldYaw = getCursorWorldYaw();
        if (!Float.isNaN(worldYaw)) {
            setTargetYaw((float) Math.toDegrees(worldYaw));
        }
    }

    private void setTargetYaw(float yaw) {
        targetYaw = yaw % FULL_DEGREES;
        if (targetYaw < 0) targetYaw += FULL_DEGREES;
        int sector = (int) Math.round(targetYaw / 45) % 8;
        direction = switch (sector) { case 1 -> Direction.NE; case 2 -> Direction.E; case 3 -> Direction.SE;
            case 4 -> Direction.S; case 5 -> Direction.SW; case 6 -> Direction.W; case 7 -> Direction.NW; default -> Direction.N; };
    }

    private void updateRotation(float delta) {
        float difference = targetYaw - modelYaw;
        while (difference > HALF_DEGREES) difference -= FULL_DEGREES;
        while (difference < -HALF_DEGREES) difference += FULL_DEGREES;
        float maximum = ROTATION_SPEED * delta;
        modelYaw = Math.abs(difference) <= maximum ? targetYaw : modelYaw + Math.copySign(maximum, difference);
        modelYaw %= FULL_DEGREES; if (modelYaw < 0) modelYaw += FULL_DEGREES;
    }

    private void updateHead(float delta) {
        if (head == null || baseHeadRotation == null) return;
        GameMaster game = GameMaster.game;
        float width = Math.max(game.getWindowWidth(), 1), height = Math.max(game.getWindowHeight(), 1);
        float worldYaw = getCursorWorldYaw();
        if (Float.isNaN(worldYaw)) return;
        float yaw = Math.clamp(wrap(worldYaw - (float) Math.toRadians(modelYaw)), -MAX_HEAD_YAW, MAX_HEAD_YAW);
        float normalizedY = Math.clamp((Mouse.getY() - height * .5f) / (height * .5f), -1, 1);
        Quaternionf target = new Quaternionf(baseHeadRotation).rotateY(yaw).rotateX(-normalizedY * MAX_HEAD_PITCH);
        Quaternionf current = new Quaternionf(head.getRotation());
        current.slerp(target, Math.clamp(1 - (float) Math.exp(-HEAD_SPEED * delta), 0, 1));
        head.setRotation(current);
    }

    private float getCursorWorldYaw() {
        GameMaster game = GameMaster.game;
        if (game == null) return Float.NaN;
        float width = Math.max(game.getWindowWidth(), 1);
        float height = Math.max(game.getWindowHeight(), 1);
        Ray ray = game.getCamera().getMouseRay(Mouse.getX(), Mouse.getY(), width, height);
        Vector3f mouse = new Vector3f(ray.origin());

        if (Math.abs(ray.direction().y) > .0001f) {
            mouse.fma((player.getPosition().y - ray.origin().y) / ray.direction().y, ray.direction());
        }

        Vector3f toward = mouse.sub(player.getPosition());
        if (toward.x * toward.x + toward.z * toward.z < 0.000001f) return Float.NaN;
        return (float) Math.atan2(toward.x, toward.z) + (float) Math.PI;
    }

    /**
     * Returns the direction the player is facing.
     * @param angle the {@code float} argument; in radians
     * @return {@link Float} the direction the player is facing
     */
    private static float wrap(float angle) {
        while (angle > Math.PI) angle -= (float) (Math.PI * 2);
        while (angle < -Math.PI) angle += (float) (Math.PI * 2);
        return angle;
    }

    /**
     * Updates equipment based on held tool/weapon
     */
    private void updateEquipment() {
        Item item = Settings.selectedItem;
        if (!(item instanceof Tool tool)) {
            EquipmentController.ec.equip(null, null);
            return;
        }
        String type = switch (item) {
            case Sword ignored -> "sword";
            case Pickaxe ignored -> "pickaxe";
            case Axe ignored -> "axe";
            case Hoe ignored -> "hoe";
            case Shovel ignored -> "shovel";
            default -> null;
        };
        EquipmentController.ec.equip(tool.getTier().getName(), type);
    }

    /**
     * Renders this object using the active graphics state.
     * @param game the {@link GameMaster} argument; active game
     * @param pass the {@link RenderPass} argument; render pass
     */
    public void render(GameMaster game, RenderPass pass) {
        if (model == null) return;
        float scale = Settings.getScaledEntity();
        float deathRoll = (float) Math.toRadians(82.0f) * deathWeight;
        float deathDrop = 0.35f * deathWeight;
        float bob = (float) Math.sin(idleTime) * .025f * idleWeight;
        if (pass == RenderPass.SHADOW) {
            Shader shader = ResourceManager.rem.getShadowMapShader();
            if (shader == null) return;
            shader.bind();
            shader.setUniform("uLightSpaceMatrix", ShadowSystem.sys.getLightSpaceMatrix());
            shader.setUniform("uAlphaTest", true);
            modelMatrix.identity().translate(player.getPosition().x, player.getPosition().y + bob - deathDrop,
                    player.getPosition().z).rotateY((float) Math.toRadians(modelYaw)).rotateZ(deathRoll).scale(scale);
            shader.setUniform("uModel", modelMatrix);
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS);
            glDepthMask(true);
            // Back faces provide a stable depth offset for this closed, layered
            // model and prevent its outer skin from shadowing the body beneath it.
            glCullFace(GL_FRONT);
            model.render(shader, modelMatrix);
            glCullFace(GL_BACK);
            glDepthMask(true);
            glDepthFunc(GL_LESS);
            shader.unbind();
            return;
        }
        Shader shader = ResourceManager.rem.getDefaultShader();
        if (shader == null) return;
        CameraView camera = game.getActiveCamera();
        CelestialLighting light = game.getCelestialLighting();
        shader.bind();
        shader.setUniform("uProjection", camera.getProjectionMatrix());
        shader.setUniform("uView", camera.getViewMatrix());
        shader.setUniform("uLightIntensity", light.getIntensity());
        shader.setUniform("uLightDirection", light.getDirection());
        shader.setUniform("uAmbientIntensity", light.getAmbientIntensity());
        shader.setUniform("uSkyColor", TimeService.getSkyColor());
        shader.setUniform("uBaseColor", new Vector3f(1));
        shader.setUniform("uIsSprite", false);
        shader.setUniform("uUseTexture", true);
        shader.setUniform("uParticleAlpha", deathAlpha);
        shader.setUniform("uIsMaskPass", false);
        shader.setUniform("uEnableShadows", Settings.doEnableShadows());
        shader.setUniform("uLightSpaceMatrix", ShadowSystem.sys.getLightSpaceMatrix());
        shader.setUniform("uIsSubmergedEntity", pass == RenderPass.SUBMERGED);
        modelMatrix.identity().translate(player.getPosition().x, player.getPosition().y + bob - deathDrop,
                player.getPosition().z).rotateY((float) Math.toRadians(modelYaw)).rotateZ(deathRoll).scale(scale);
        shader.setUniform("uModel", modelMatrix);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        if (pass == RenderPass.NORMAL) {
            glDepthFunc(GL_LESS);
            glDepthMask(true);
            shader.setUniform("uIsSubmergedEntity", false);
        } else {
            glDepthFunc(GL_GREATER);
            glDepthMask(false);
            shader.setUniform("uIsSubmergedEntity", true);
        }
        model.render(shader, modelMatrix);
        glDepthFunc(GL_LESS);
        glDepthMask(true);
        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_BLEND);
        shader.unbind();
    }

    /**
     * Starts the attack animation.
     */
    public void interact() { attacking = true; attackTime = 0; }
    /**
     * Determines whether attacking is satisfied by the current state.
     * @return {@code true} if attacking; otherwise {@code false}
     */
    public boolean isAttacking() { return attacking; }
    /**
     * Returns direction according to the current object state.
     * @return the {@link Direction} result; current facing direction
     */
    public Direction getDirection() { return direction; }
}
