package com.isofarm.entity.enemies;

import com.isofarm.entity.Enemy;
import com.isofarm.graphics.gltf.GLTFLoader;

/** {@inheritDoc} */
public class Goblin extends Enemy {

    public Goblin() {
        super(GLTFLoader.load("assets/models/enemies/goblin.gltf"));
    }

    @Override
    public void update(float delta) {

    }
}
