package com.isofarm.graphics;

import com.isofarm.data.BlockData;
import com.isofarm.data.BlockPos;
import com.isofarm.data.Particle;
import com.isofarm.data.Singleton;
import com.isofarm.service.Service;
import com.isofarm.utils.K;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents the particle engine component of the Isofarm runtime.
 *
 * <p>This type centralizes the state, lifecycle and behavior required by its callers,
 * keeping domain rules together with the data they operate on.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li><b>State:</b> Owns the data needed to represent the component consistently.</li>
 *   <li><b>Lifecycle:</b> Exposes the operations used to create, update and release its state.</li>
 *   <li><b>Integration:</b> Coordinates with the surrounding game, rendering or UI systems through its public API.</li>
 * </ul>
 *
 * <p>Callers should use the documented public operations and allow this type to preserve
 * its invariants rather than modifying implementation details directly.
 */
@Singleton
public class ParticleEngine implements Service<Particle> {
    public static final ParticleEngine peng = new ParticleEngine();
    private static final Random random = new Random();
    private final List<Particle> particles = new ArrayList<>();
    private final Matrix4f modelMatrix = new Matrix4f();

    /**
     * Creates a new {@code ParticleEngine} instance.
     */
    private ParticleEngine() {}

    /**
     * Adds addEnemy.
     * @param particle the {@link Particle} supplied as {@code particle}
     * @return the {@link Particle} representing the addEnemy result
     */
    public Particle add(Particle particle) {
        particles.add(particle);
        return particle;
    }

    /**
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     */
    public void update(float delta) {
        for (Particle p : particles) {
            p.update(delta);
        }
        particles.removeIf(Particle::isDead);
    }

    /**
     * Renders this object in the requested render pass.
     * @param shader the {@link Shader} supplied as {@code shader}
     * @param quadMesh the {@link Mesh} supplied as {@code quadMesh}
     * @param camera the {@link CameraView} supplied as {@code camera}
     */
    public void render(Shader shader, Mesh quadMesh, CameraView camera) {
        if (particles.isEmpty()) return;

        shader.bind();
        shader.setUniform("uProjection", camera.getProjectionMatrix());
        shader.setUniform("uView", camera.getViewMatrix());
        shader.setUniform("uParticleAlpha", 1.0f);
        shader.setUniform("uUseTexture", true);
        shader.setUniform("uUseFaceAtlas", false);

        SpriteSheet currentTexture = null;
        for (Particle p : particles) {
            if (p.getTexture() != null) {
                if (p.getTexture() != currentTexture) {
                    if (currentTexture != null) {
                        currentTexture.unbind();
                    }
                    currentTexture = p.getTexture();
                    currentTexture.bind();
                }
            }

            modelMatrix.identity()
                    .translate(p.getX(), p.getY(), p.getZ())
                    .scale(p.getSize() * p.getAlpha());

            shader.setUniform("uModel", modelMatrix);
            shader.setUniform("uUVBounds", new Vector4f(
                    p.getUvOffset().x,
                    p.getUvOffset().y,
                    p.getUvOffset().x + p.getUvScale().x,
                    p.getUvOffset().y + p.getUvScale().y
            ));

            quadMesh.render();
        }

        if (currentTexture != null) {
            currentTexture.unbind();
        }

        shader.setUniform("uParticleAlpha", 1.0f);
        shader.setUniform("uUseTexture", true);
        shader.setUniform("uUseFaceAtlas", true);
        shader.setUniform("uUVBounds", new Vector4f(0.0f, 0.0f, 1.0f, 1.0f));
        shader.unbind();
    }

    /**
     * Transfers or creates the relevant entity or item for spawn block.
     * @param blockPos the {@link BlockPos} supplied as {@code blockPos}
     * @param blockData the {@link BlockData} supplied as {@code blockData}
     * @return the {@link Particle} representing the spawn block result
     */
    public Particle spawnBlock(BlockPos blockPos, BlockData blockData) {
        if (blockData == null || blockData == BlockData.AIR) return null;

        TextureAtlas.TextureRegion region = blockData.getSideRegion();
        if (region == null) {
            region = blockData.getTopRegion();
        }

        Vector2f baseOffset = region != null ? region.offset() : new Vector2f(0.0f, 0.0f);
        Vector2f baseScale = region != null ? region.scale() : new Vector2f(1.0f, 1.0f);

        int totalParticles = K.World.MAX_PARTICLES;

        float particleUvWidth = baseScale.x / 4.0f;
        float particleUvHeight = baseScale.y / 4.0f;

        for (int i = 0; i < totalParticles; i++) {
            if (blockPos == null) continue;
            float px = blockPos.x() + random.nextFloat();
            float py = blockPos.y() + random.nextFloat();
            float pz = blockPos.z() + random.nextFloat();

            float vx = (random.nextFloat() - 0.5f) * 2.5f;
            float vy = random.nextFloat() * 2.5f + 1.0f;
            float vz = (random.nextFloat() - 0.5f) * 2.5f;

            float randomU = baseOffset.x + random.nextFloat() * (baseScale.x - particleUvWidth);
            float randomV = baseOffset.y + random.nextFloat() * (baseScale.y - particleUvHeight);

            Vector2f particleOffset = new Vector2f(randomU, randomV);
            Vector2f particleScale = new Vector2f(particleUvWidth, particleUvHeight);

            float size = 0.08f + random.nextFloat() * 0.06f;
            float maxLife = 0.3f + random.nextFloat() * 0.3f;
            return add(new Particle(px, py, pz, vx, vy, vz, size, maxLife,
                    particleOffset, particleScale, null));
        }
        return null;
    }

    /**
     * Transfers or creates the relevant entity or item for spawn plant.
     * @param blockPos the {@link BlockPos} supplied as {@code blockPos}
     * @param blockData the {@link BlockData} supplied as {@code blockData}
     * @return the {@link Particle} representing the spawn plant result
     */
    public Particle spawnPlant(BlockPos blockPos, BlockData blockData) {
        if (blockPos == null) return null;
        if (blockData == null || blockData == BlockData.AIR) return null;
        if (!blockData.isPlant()) return null;

        TextureAtlas.TextureRegion region = blockData.getTopRegion();
        if (region == null) return null;

        Vector2f baseOffset = region.offset();
        Vector2f baseScale = region.scale();

        int totalParticles = K.World.MAX_PARTICLES;

        float particleUvWidth = baseScale.x / 4.0f;
        float particleUvHeight = baseScale.y / 4.0f;

        Particle first = null;

        for (int i = 0; i < totalParticles; i++) {
            float px = blockPos.x() + random.nextFloat();
            float py = blockPos.y() + random.nextFloat();
            float pz = blockPos.z() + random.nextFloat();

            float vx = (random.nextFloat() - 0.5f) * 2.5f;
            float vy = random.nextFloat() * 2.5f + 1.0f;
            float vz = (random.nextFloat() - 0.5f) * 2.5f;
            float randomU = baseOffset.x + random.nextFloat() * (baseScale.x - particleUvWidth);
            float randomV = baseOffset.y + random.nextFloat() * (baseScale.y - particleUvHeight);
            Vector2f particleOffset = new Vector2f(randomU, randomV);
            Vector2f particleScale = new Vector2f(particleUvWidth, particleUvHeight);
            float size = 0.08f + random.nextFloat() * 0.06f;
            float maxLife = 0.3f + random.nextFloat() * 0.3f;
            Particle particle = new Particle(px, py, pz, vx, vy, vz, size, maxLife,
                    particleOffset, particleScale, null);
            add(particle);
            if (first == null) {
                first = particle;
            }
        }

        return first;
    }

    /**
     * Transfers or creates the relevant entity or item for spawn crop.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param z the {@code float} supplied as {@code z}
     * @param cropSheet the {@link SpriteSheet} supplied as {@code cropSheet}
     * @param frameIndex the {@code int} supplied as {@code frameIndex}
     * @return the {@link Particle} representing the spawn crop result
     */
    public Particle spawnCrop(float x, float y, float z, SpriteSheet cropSheet,
                          int frameIndex) {
        if (cropSheet == null) return null;

        int totalParticles = K.World.MAX_PARTICLES;
        float frameWidthUV = 1.0f / cropSheet.getTotalFrames();
        float baseUvX = frameIndex * frameWidthUV;

        float particleUvWidth = frameWidthUV / 4.0f;
        float particleUvHeight = 1.0f / 4.0f;

        for (int i = 0; i < totalParticles; i++) {
            float px = x + random.nextFloat();
            float py = y + random.nextFloat();
            float pz = z + random.nextFloat();

            float vx = (random.nextFloat() - 0.5f) * 2.5f;
            float vy = random.nextFloat() * 2.5f + 1.0f;
            float vz = (random.nextFloat() - 0.5f) * 2.5f;

            float randomU = baseUvX + random.nextFloat() * (frameWidthUV - particleUvWidth);
            float randomV = random.nextFloat() * (1.0f - particleUvHeight);

            Vector2f particleOffset = new Vector2f(randomU, randomV);
            Vector2f particleScale = new Vector2f(particleUvWidth, particleUvHeight);
            float size = 0.08f + random.nextFloat() * 0.06f;
            float maxLife = 0.3f + random.nextFloat() * 0.3f;

            return add(new Particle(px, py, pz, vx, vy, vz, size, maxLife,
                    particleOffset, particleScale, cropSheet));
        }
        return null;
    }

    /**
     * Removes clear.
     */
    public void clear() {
        particles.clear();
    }
}