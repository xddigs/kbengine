package com.isofarm.entity;

import com.isofarm.data.DataClass;
import com.isofarm.data.RenderPass;
import com.isofarm.data.Reputation;
import com.isofarm.graphics.gltf.GLTFModel;
import com.isofarm.utils.RandomLocal;
import com.isofarm.wrld.GameMaster;
import org.joml.Vector3f;

/**
 * Abstract base class for all hostile AI-driven characters in the game.
 *
 * <p>Extends {@link Character} by anchoring entity behavior to a 3D {@link GLTFModel}.
 * Unlike neutral or passive {@link NPC}s, enemies are hardcoded with a {@link Reputation#HOSTILE}
 * alignment and actively engage in combat calculations against the player.
 *
 * <p><b>Key Lifecycle & Characteristics:</b>
 * <ul>
 *   <li><b>Visual Representation:</b> Animated via external 3D {@link GLTFModel} assets.</li>
 *   <li><b>Non-Interactive:</b> Cannot be conversed with or traded with; interaction is strictly combat.</li>
 * </ul>
 */
@DataClass
public abstract class Enemy extends Character {
    private final float width = 0.25f;
    private final float height = 0.8f;
    private final float speed = Player.plyr.getLevel() + (Player.plyr.getSpeed() * 1.5f);
    private final int level = Player.plyr.getLevel() + 2;
    private final long enemyID = RandomLocal.get().nextLong();
    private final GLTFModel model;

    /** Creates a new {@link Enemy} instance. */
    public Enemy(GLTFModel model) {
        super(Enemy.class.getSimpleName());
        this.model = model;
        setDimensions(new Vector3f(width, height, width));
        setSpeed(speed);
        setLevel(level);
        setMaxDefense(Player.plyr.getMaxDefense());
        setMaxHitpoints(getLevel() * 10);
        setHitpoints(getMaxHitpoints());
        setReputation(Reputation.HOSTILE);
    }

    /** {@inheritDoc} */
    @Override
    public void render(GameMaster gameMaster, RenderPass pass) {}

    /**
     * Updates the behavior of the {@link Enemy}
     * @param delta the {@link Float} supplied as {@code delta}
     */
    public abstract void behave(float delta);

    /**
     * Returns the {@code enemyID}
     * @return {@link Long} supplied as {@code enemyID}
     */
    public long getEnemyID() {
        return enemyID;
    }

    /**
     * Returns the {@code model} value
     * @return {@link GLTFModel} value of {@code model}
     */
    public GLTFModel getModel() {
        return model;
    }
}
