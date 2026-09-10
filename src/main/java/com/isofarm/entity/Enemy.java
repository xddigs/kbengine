package com.isofarm.entity;

import com.isofarm.data.DataClass;
import com.isofarm.data.RenderPass;
import com.isofarm.data.Reputation;
import com.isofarm.utils.RandomLocal;
import com.isofarm.wrld.GameMaster;
import org.joml.Vector3f;

/**
 * States and offers data on {@code Enemy}s, such as health, damage, etc.
 */
@DataClass
public abstract class Enemy extends Character {
    private static final long enemyID = RandomLocal.get().nextLong();
    private static final float WIDTH = 0.25f;
    private static final float HEIGHT = 0.8f;
    private static final float SPEED = Player.plyr.getLevel() + (Player.plyr.getSpeed() * 1.5f);
    private static final int LEVEL = Player.plyr.getLevel() + 2;
    private static final int MAX_HITPOINTS = LEVEL * 10;

    /** Creates a new {@link Enemy} instance. */
    public Enemy() {
        super(Enemy.class.getSimpleName());
        setDimensions(new Vector3f(WIDTH, HEIGHT, WIDTH));
        setSpeed(SPEED);
        setLevel(LEVEL);
        setMaxHitpoints(MAX_HITPOINTS);
        setHitpoints(getMaxHitpoints());
        setReputation(Reputation.HOSTILE);
    }

    @Override
    public void render(GameMaster gameMaster, RenderPass pass) {}

    /**
     * Updates the behavior of the {@link Enemy}
     * @param delta the {@link Float} supplied as {@code delta}
     */
    public abstract void update(float delta);

    /**
     * Returns the {@code enemyID}
     * @return {@link Long} supplied as {@code enemyID}
     */
    public static long getEnemyID() {
        return enemyID;
    }
}
