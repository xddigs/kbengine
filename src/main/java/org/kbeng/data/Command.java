package org.kbeng.data;

import java.util.function.Consumer;

/**
 * Immutable value object containing command.
 */
@DataClass
public record Command(
        String name,
        CommandArgument[] args,
        Consumer<String[]> action) {}