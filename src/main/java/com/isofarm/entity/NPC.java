package com.isofarm.entity;

import com.isofarm.data.*;
import com.isofarm.graphics.gltf.GLTFModel;
import com.isofarm.utils.Naming;
import com.isofarm.wrld.GameMaster;

/**
 * Represents an NPC, with their own state and behavior, in contrast to {@link Player},
 * who starts with their own {@link Singleton} instance. Inherits from {@link Character}.
 */
@DataClass
@TODO(reason="Each NPC should load their own model")
public class NPC extends Character {
    private static final String name = Naming.nm.fullName();
    private final Job job;
    private final GLTFModel npcModel;

    /** Creates a new {@code NPC} instance. */
    public NPC(Job job, GLTFModel npcModel) {
        super(name);
        this.job = job;
        this.npcModel = npcModel;
    }

    /** Creates a new {@code NPC} instance. Defaults to {@link Job#FARMER}. */
    public NPC(GLTFModel npcModel) {
        this(Job.FARMER, npcModel);
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

    /**
     * Retrieves {@code job}
     * @return {@link Job} value of job
     */
    public Job getJob() {
        return job;
    }

    /**
     * Retrieves {@code npcModel}
     * @return {@link GLTFModel} value of npcModel
     */
    public GLTFModel getNpcModel() {
        return npcModel;
    }
}
