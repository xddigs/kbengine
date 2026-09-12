package org.kbeng.rpg.item;

/**
 * Craftable defines the craftable contract within the item subsystem.
 * Implementations are expected to preserve these behavioral guarantees while choosing their own storage and execution strategy.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends Item }, inheriting shared behavior while specializing subsystem-specific logic.
 */
public interface Craftable extends Item {}
