package org.kbeng.service;

import org.kbeng.data.Singleton;
import org.kbeng.entity.Enemy;
import org.kbeng.wrld.GameMaster;
import org.joml.Vector3f;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents the enemy service component of the kbengine runtime.
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
public class EnemyService implements Service<Enemy> {
    public static final EnemyService enms = new EnemyService();
    private List<Enemy> enemies = new LinkedList<>();

    /** Creates a new {@link EnemyService} instance. */
    private EnemyService() {}

    /**
     * Initializes a starting pool of enemies
     */
    public void init() {
        clear();
    }

    /**
     * Places all registered enemies on the generated terrain around the player
     * spawn point. Positions are deterministic so enemy placement is stable
     * across repeated world spawns.
     */
    public void spawn() {
        for (int index = 0; index < enemies.size(); index++) {
            float angle = (float) (index * Math.PI * 2.0 / Math.max(1, enemies.size()));
            float radius = 6.0f + index % 3;
            float x = 0.5f + (float) Math.sin(angle) * radius;
            float z = 0.5f + (float) Math.cos(angle) * radius;
            float y = GameMaster.game.getWorld().getHighestY(x, z).y() + 1.0f;
            enemies.get(index).setPosition(new Vector3f(x, y, z));
        }
    }

    /**
     * Returns the {@code enemies} value
     * @return {@link List<Enemy>} value of {@code enemies}
     */
    public List<Enemy> getEnemies() {
        return enemies;
    }

    /**
     * Returns the specified mapped enemy to the {@code index}
     * @param index the {@code int} supplied as {@code index}
     * @return the obtained {@link Enemy}
     */
    public Enemy get(int index) {
        return enemies.get(index);
    }

    /**
     * Adds the parametized {@link Enemy} to the enemies list
     * @param enemy the {@link Enemy} supplied as {@code enemy}
     * @return enemy
     */
    public Enemy add(Enemy enemy) {
        enemies.add(enemy);
        GameMaster.game.addEntity(enemy);
        return enemy;
    }

    /**
     * Removes the parametized {@link Enemy} to the enemies list
     * @param enemy the {@link Enemy} supplied as {@code enemy}
     * @return enemy
     */
    public Enemy remove(Enemy enemy) {
        List<Enemy> copy = List.copyOf(enemies);
        copy.remove(enemy);
        GameMaster.game.removeEntity(enemy);
        enemies = copy;
        return enemy;
    }

    /** Clears the enemies list */
    public void clear() {
        enemies.clear();
    }
}
