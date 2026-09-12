package org.kbeng.data;

import org.kbeng.graphics.SpriteSheet;
import org.joml.Vector2f;

/**
 * Particle provides particle capabilities within the data subsystem.
 *
 * It represents strongly typed domain data shared between simulation, rendering, input, and persistence boundaries.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public class Particle {
    private float x, y, z;
    private float vx, vy, vz;
    private float life;
    private final float maxLife;
    private final float size;

    private final Vector2f uvOffset;
    private final Vector2f uvScale;
    private final SpriteSheet texture;

    /**
     * Creates a new {@code Particle} instance.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param z the {@code float} supplied as {@code z}
     * @param vx the {@code float} supplied as {@code vx}
     * @param vy the {@code float} supplied as {@code vy}
     * @param vz the {@code float} supplied as {@code vz}
     * @param size the {@code float} supplied as {@code size}
     * @param maxLife the {@code float} supplied as {@code maxLife}
     * @param uvOffset the {@link Vector2f} supplied as {@code uvOffset}
     * @param uvScale the {@link Vector2f} supplied as {@code uvScale}
     * @param texture the {@link SpriteSheet} supplied as {@code texture}
     */
    public Particle(float x, float y, float z, float vx, float vy, float vz,
                    float size, float maxLife, Vector2f uvOffset, Vector2f uvScale,
                    SpriteSheet texture) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.size = size;
        this.maxLife = maxLife;
        this.uvOffset = uvOffset;
        this.uvScale = uvScale;
        this.texture = texture;
        this.life = 0;
    }

    /**
     * Returns the texture.
     * @return the {@link SpriteSheet} representing the texture
     */
    public SpriteSheet getTexture() {
        return texture;
    }

    /**
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     */
    public void update(float delta) {
        life += delta;

        vy -= 9.81f * delta;
        vx *= 0.98f;
        vz *= 0.98f;

        x += vx * delta;
        y += vy * delta;
        z += vz * delta;
    }

    /**
     * Checks whether the dead condition is met.
     * @return {@code true} if dead; otherwise {@code false}
     */
    public boolean isDead() {
        return life >= maxLife;
    }

    /**
     * Returns the alpha.
     * @return {@code float}; the alpha
     */
    public float getAlpha() {
        return 1.0f - (life / maxLife);
    }

    /**
     * Returns the x.
     * @return {@code float}; the x
     */
    public float getX() {
        return x;
    }

    /**
     * Returns the y.
     * @return {@code float}; the y
     */
    public float getY() {
        return y;
    }

    /**
     * Returns the z.
     * @return {@code float}; the z
     */
    public float getZ() {
        return z;
    }

    /**
     * Returns the vx.
     * @return {@code float}; the vx
     */
    public float getVx() {
        return vx;
    }

    /**
     * Returns the vy.
     * @return {@code float}; the vy
     */
    public float getVy() {
        return vy;
    }

    /**
     * Returns the vz.
     * @return {@code float}; the vz
     */
    public float getVz() {
        return vz;
    }

    /**
     * Returns the life.
     * @return {@code float}; the life
     */
    public float getLife() {
        return life;
    }

    /**
     * Returns the max life.
     * @return {@code float}; the max life
     */
    public float getMaxLife() {
        return maxLife;
    }

    /**
     * Returns the size.
     * @return {@code float}; the size
     */
    public float getSize() {
        return size;
    }

    /**
     * Returns the uv offset.
     * @return the {@link Vector2f} representing the uv offset
     */
    public Vector2f getUvOffset() {
        return uvOffset;
    }

    /**
     * Returns the uv scale.
     * @return the {@link Vector2f} representing the uv scale
     */
    public Vector2f getUvScale() {
        return uvScale;
    }
}
