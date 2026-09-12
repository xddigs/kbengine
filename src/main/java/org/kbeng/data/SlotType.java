package org.kbeng.data;

/**
 * SlotType declares the canonical slot type set for the data subsystem.
 *
 * Each constant provides a stable, type-safe branch token for runtime decisions and avoids string-based switches.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public enum SlotType {NONE, INVENTORY, BACKPACK, HOTBAR, SHIELD, ARMOR}
