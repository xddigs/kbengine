package org.kbeng.games.rpg.data;

/**
 * Direction declares the canonical direction set for the data subsystem.
 * Each constant provides a stable, type-safe branch token for runtime decisions and avoids string-based switches.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public enum Direction {
    SW, S, SE, E, NE, N, NW, W;
}