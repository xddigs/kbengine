package org.kbeng.data;

import org.joml.Vector3f;

/**
 * Immutable value object containing ray.
 */
@DataClass
public record Ray(Vector3f origin, Vector3f direction) {}
