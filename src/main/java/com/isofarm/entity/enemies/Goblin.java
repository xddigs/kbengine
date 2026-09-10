package com.isofarm.entity.enemies;

import com.isofarm.data.BlockPos;
import com.isofarm.data.RenderPass;
import com.isofarm.entity.CharacterAnimator;
import com.isofarm.entity.Enemy;
import com.isofarm.graphics.ResourceManager;
import com.isofarm.graphics.gltf.GLTFModel;
import com.isofarm.wrld.GameMaster;

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
        animator.render(gameMaster, model, pass);
    }
}
