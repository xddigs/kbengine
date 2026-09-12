package org.kbeng.data;
/**
 * Task defines the task contract within the data subsystem.
 *
 * Implementations are expected to preserve these behavioral guarantees while choosing their own storage and execution strategy.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */

public @interface Task {
    String reason();
}
