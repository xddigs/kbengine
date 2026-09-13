package org.kbeng.games.rpg.entity.enemies;

import org.kbeng.games.rpg.data.BlockPos;
import org.kbeng.games.rpg.data.RenderPass;
import org.kbeng.games.rpg.entity.CharacterAnimator;
import org.kbeng.games.rpg.entity.Enemy;
import org.kbeng.games.rpg.graphics.ResourceManager;
import org.kbeng.engine.graphics.gltf.GLTFModel;
import org.kbeng.games.rpg.wrld.GameMaster;

/**
 * Goblin provides goblin capabilities within the entity subsystem.
 * It participates in actor simulation, state transitions, and per-frame world interaction contracts.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends Enemy, inheriting shared behavior while specializing subsystem-specific logic.
 */
public class Goblin extends Enemy {
    private final CharacterAnimator animator = new CharacterAnimator(this);
    private static final GLTFModel model = ResourceManager.rem.getModel(Goblin.class);

    public Goblin() {
        super(model);
    }

    @Override
    public void update(BlockPos blockPos, float delta) {
        super.update(blockPos, delta);
        super.update();
        animator.update(model, delta);
        behave(delta);
    }

    @Override
    public void behave(float delta) {

    }

    @Override
    public void render(GameMaster gameMaster, RenderPass pass) {
        if (!isAlive() || model == null) return;
        animator.render(gameMaster, model, pass);
    }
}
