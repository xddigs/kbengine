package org.kbeng.data;

import org.joml.Vector3f;

/**
 * Ray is an immutable carrier for ray state in the data subsystem.
 *
 * Record semantics make instances cheap to pass across systems while preserving value-based equality and snapshot safety.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public record Ray(Vector3f origin, Vector3f direction) {}
