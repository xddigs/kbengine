package com.isofarm.data;

/**
 * Stores the center and radius of a generated lake.
 */
@DataClass
public record Lake(int x, int z, float radius) {}