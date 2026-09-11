package org.kbeng.entity.enemies;

import org.kbeng.data.BlockPos;
import org.kbeng.data.RenderPass;
import org.kbeng.entity.CharacterAnimator;
import org.kbeng.entity.Enemy;
import org.kbeng.graphics.ResourceManager;
import org.kbeng.graphics.gltf.GLTFModel;
import org.kbeng.wrld.GameMaster;

/** {@inheritDoc} */
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
