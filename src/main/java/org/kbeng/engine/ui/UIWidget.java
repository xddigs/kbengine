package org.kbeng.engine.ui;

/**
 * UIWidget provides a marker interface for UI components within the ui subsystem.
 * It serves as a common type for UI elements, enabling polymorphic behavior and facilitating interaction with the UI framework.
 * Implementing classes can represent various UI elements, such as buttons, panels, sliders, and other interactive components.
 * The interface does not define any methods, allowing implementing classes to define their own specific behavior.
 */
public interface UIWidget {
    default String getTooltipCornerText() {
        return null;
    }
    default String formatTooltip(String tooltipText) {
        return tooltipText;
    }
}
