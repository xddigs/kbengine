package com.isofarm.entity;

import com.isofarm.data.*;
import com.isofarm.utils.Naming;
import com.isofarm.wrld.GameMaster;

/**
 * Represents an NPC, with their own state and behavior, in contrast to {@link Player},
 * who starts with their own {@link Singleton} instance. Inherits from {@link Character}.
 */
@DataClass
public class NPC extends Character {
    private static final String name = Naming.nm.fullName();
    private final Job job;

    public NPC(Job job) {
        super(name);
        this.job = job;
    }

    public NPC() {
        this(Job.FARMER);
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
     * Retrieves job
     * @return {@link Job} value of job
     */
    public Job getJob() {
        return job;
    }
}
