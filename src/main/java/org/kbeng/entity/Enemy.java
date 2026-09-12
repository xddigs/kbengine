package org.kbeng.entity;

import org.kbeng.data.DataClass;
import org.kbeng.data.RenderPass;
import org.kbeng.data.Reputation;
import org.kbeng.graphics.gltf.GLTFModel;
import org.kbeng.utils.RandomLocal;
import org.kbeng.wrld.GameMaster;
import org.joml.Vector3f;

/**
 * Hostile {@link Character} archetype used for combat-focused non-player entities.
 *
 * Enemies are initialized from a {@link GLTFModel}, scale core stats from the current player progression,
 * and default to {@link Reputation#HOSTILE}. The class centralizes enemy-side combat expectations while
 * letting concrete enemy variants customize attack logic, AI movement, and rendering details.
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
