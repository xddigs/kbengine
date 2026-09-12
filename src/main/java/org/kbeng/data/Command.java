package org.kbeng.data;

import java.util.function.Consumer;

/**
 * Command is an immutable carrier for command state in the data subsystem.
 *
 * Record semantics make instances cheap to pass across systems while preserving value-based equality and snapshot safety.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public record Command(
        String name,
        CommandArgument[] args,
        Consumer<String[]> action) {}