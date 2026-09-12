package org.kbeng.data;

/**
 * Singleton defines the singleton contract within the data subsystem.
 *
 * Implementations are expected to preserve these behavioral guarantees while choosing their own storage and execution strategy.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public @interface Singleton {}
