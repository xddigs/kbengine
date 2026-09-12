package org.kbeng.item;

import org.kbeng.data.DataClass;

/**
 * Undroppable defines the undroppable contract within the item subsystem.
 *
 * Implementations are expected to preserve these behavioral guarantees while choosing their own storage and execution strategy.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public interface Undroppable {}
