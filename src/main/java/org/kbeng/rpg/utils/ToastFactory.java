package org.kbeng.rpg.utils;

import org.kbeng.engine.utils.Local;
import org.kbeng.engine.utils.K;
import org.kbeng.engine.utils.Utils;

import org.kbeng.rpg.data.Toast;
import org.kbeng.rpg.data.ToastData;
import org.kbeng.rpg.service.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ToastFactory provides toast factory capabilities within the utils subsystem.
 * It centralizes reusable utilities such as constants, localization, settings, and small cross-cutting helpers.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It implements Service<Toast>, providing a concrete strategy for this subsystem contract.
 */
@SuppressWarnings("all")
@Utils
public class ToastFactory implements Service<Toast> {
    private static final List<Toast> toasts = new ArrayList<>();
    private static float windowWidth = K.Window.DEFAULT_WIDTH;

    /**
     * Returns the toasts.
     * @return the {@link List} representing the toasts
     */
    public static List<Toast> getToasts() {
        return toasts;
    }

    /**
     * Sets the window width.
     * @param windowWidth the {@code float} supplied as {@code windowWidth}
     */
    public static void setWindowWidth(float windowWidth) {
        ToastFactory.windowWidth = windowWidth;
        for (Toast toast : toasts) {
            toast.setWindowWidth(windowWidth);
        }
        rearrange();
    }

    /**
     * Adds addEnemy.
     * @param toast the {@link Toast} supplied as {@code toast}
     */
    public static void add(Toast toast) {
        if (toast == null) {
            return;
        }

        toasts.add(toast);
        rearrange();
    }

    /**
     * Reloads this object from its authoritative source.
     */
    public static void reload() {
        toasts.clear();
        rearrange();
    }

    /**
     * Removes removeEnemy.
     * @param toast the {@link Toast} supplied as {@code toast}
     */
    public void remove(Toast toast) {
        if (toasts.remove(toast)) {
            rearrange();
        }
    }

    /**
     * Returns get.
     * @param index the {@code int} supplied as {@code index}
     * @return the {@link Toast} representing the get result
     */
    public static Toast get(int index) {
        return toasts.get(index);
    }

    /**
     * Removes clear.
     */
    public static void clear() {
        toasts.clear();
    }

    /**
     * Returns the number or extent represented by size.
     * @return {@code int}; the size result
     */
    public static int size() {
        return toasts.size();
    }

    /**
     * Checks whether the empty condition is met.
     * @return {@code true} if empty; otherwise {@code false}
     */
    public static boolean isEmpty() {
        return toasts.isEmpty();
    }

    /**
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     */
    public static void update(float delta) {
        if (toasts.isEmpty()) {
            return;
        }

        Iterator<Toast> iterator = toasts.iterator();
        while (iterator.hasNext()) {
            Toast toast = iterator.next();

            toast.update(delta);

            if (toast.isFinished()) {
                iterator.remove();
            }
        }

        rearrange();
    }

    /**
     * Publishes the notification represented by info.
     * @param message the {@link String} supplied as {@code message}
     */
    public static void info(String message) {
        create(ToastData.INFO, message);
    }

    /**
     * Publishes the notification represented by success.
     * @param message the {@link String} supplied as {@code message}
     */
    public static void success(String message) {
        create(ToastData.SUCCESS, message);
    }

    /**
     * Publishes the notification represented by warning.
     * @param message the {@link String} supplied as {@code message}
     */
    public static void warning(String message) {
        create(ToastData.WARNING, message);
    }

    /**
     * Publishes the notification represented by error.
     * @param message the {@link String} supplied as {@code message}
     */
    public static void error(String message) {
        create(ToastData.ERROR, message);
    }

    /**
     * Processes reward and updates the affected inventory or currency balances.
     * @param message the {@link String} supplied as {@code message}
     */
    public static void reward(String message) {
        create(ToastData.REWARD, message);
    }

    /**
     * Processes purchase and updates the affected inventory or currency balances.
     * @param message the {@link String} supplied as {@code message}
     */
    public static void purchase(String message) {
        create(ToastData.PURCHASE, message);
    }

    /**
     * Processes sell and updates the affected inventory or currency balances.
     * @param message the {@link String} supplied as {@code message}
     */
    public static void sell(String message) {
        create(ToastData.SELL, message);
    }

    /**
     * Returns create.
     * @param type the {@link ToastData} supplied as {@code type}
     * @param message the {@link String} supplied as {@code message}
     */
    private static void create(ToastData type, String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        float startX = windowWidth + 250f;
        float targetX = windowWidth - K.UI.TOAST_WIDTH;
        float targetY = K.UI.TOAST_MARGIN_TOP;

        Toast toast = new Toast(startX, targetY, targetX, targetY,
                type, Local.lang.t(message), K.UI.TOAST_DURATION);

        toast.setWindowWidth(windowWidth);
        add(toast);
    }

    /**
     * Reorganizes inventory state for rearrange.
     */
    private static void rearrange() {
        float targetX = windowWidth - K.UI.TOAST_WIDTH;
        float y = K.UI.TOAST_MARGIN_TOP;

        for (Toast toast : toasts) {
            if (toast.isExiting()) {
                continue;
            }

            toast.setTargetX(targetX);
            toast.setTargetY(y);
            y += K.UI.TOAST_HEIGHT + K.UI.TOAST_SPACING;
        }
    }

    /**
     * Handles resize and updates the affected state.
     * @param width the {@code float} supplied as {@code width}
     */
    public static void onResize(float width) {
        windowWidth = width;
    }
}
