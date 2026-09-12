package org.kbeng.db.data;

/**
 * Card is an immutable carrier for card state in the data subsystem.
 * Record semantics make instances cheap to pass across systems while preserving value-based equality and snapshot safety.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public record Card() {}
