package org.kbeng.data;

import org.kbeng.item.Item;

/**
 * Immutable value object containing stack.
 */
@DataClass
public record Stack(Item item, int amount) {}
