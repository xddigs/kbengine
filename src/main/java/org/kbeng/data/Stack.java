package org.kbeng.data;

import org.kbeng.item.Item;

/**
 * Stack is an immutable carrier for stack state in the data subsystem.
 * Record semantics make instances cheap to pass across systems while preserving value-based equality and snapshot safety.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public record Stack(Item item, int amount) {}
