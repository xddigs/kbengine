package com.isofarm.entity;

import com.isofarm.data.BlockPos;
import com.isofarm.data.DataClass;
import com.isofarm.data.RenderPass;
import com.isofarm.data.TODO;
import com.isofarm.wrld.GameMaster;

/**
 * Encapsulates the state and operations required by animal within the game runtime.
 */
@DataClass
@TODO(reason="Animals are not implemented yet")
public class Animal extends Entity {

    /**
     * Creates a new {@code Animal} instance.
     * @param name the {@link String} supplied as {@code name}
     */
    public Animal(String name) {
        super(name);
    }

    /**
     * Creates a new {@code Animal} instance.
     */
    public Animal() {
        this(Animal.class.getSimpleName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(BlockPos blockPos, float delta) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(GameMaster gameMaster, RenderPass pass) {

    }
}
