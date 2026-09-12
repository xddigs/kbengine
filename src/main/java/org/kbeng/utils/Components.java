package org.kbeng.utils;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;

import java.util.List;

/**
 * Represents the components component of the kbengine runtime.
 *
 * <p>This type centralizes the state, lifecycle and behavior required by its callers,
 * keeping domain rules together with the data they operate on.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li><b>State:</b> Owns the data needed to represent the component consistently.</li>
 *   <li><b>Lifecycle:</b> Exposes the operations used to create, update and release its state.</li>
 *   <li><b>Integration:</b> Coordinates with the surrounding game, rendering or UI systems through its public API.</li>
 * </ul>
 *
 * <p>Callers should use the documented public operations and allow this type to preserve
 * its invariants rather than modifying implementation details directly.
 */
@Utils
public final class Components {
    private static final SystemInfo SYSTEM_INFO = new SystemInfo();
    private static final HardwareAbstractionLayer HARDWARE = SYSTEM_INFO.getHardware();
    private static final CentralProcessor CPU = HARDWARE.getProcessor();
    private static final GlobalMemory MEMORY = HARDWARE.getMemory();
    private static final Sensors SENSORS = HARDWARE.getSensors();

    private static final String CPU_NAME = CPU.getProcessorIdentifier().getName();
    private static final String GPU_NAME = loadGpuName();

    /**
     * Loads the gpu name.
     * @return the {@link String} representing the load gpu name result
     */
    private static String loadGpuName() {
        List<GraphicsCard> graphicsCards = HARDWARE.getGraphicsCards();
        if (graphicsCards.isEmpty()) {
            return "GPU: N/A";
        }
        return graphicsCards.getFirst().getName();
    }

    /**
     * Creates a new {@code Components} instance.
     */
    private Components() {}

    /**
     * Returns the cpu.
     * @return the {@link String} representing the cpu
     */
    public static String getCpu() {
        return CPU_NAME;
    }

    /**
     * Returns the gpu.
     * @return the {@link String} representing the gpu
     */
    public static String getGpu() {
        return GPU_NAME;
    }

    /**
     * Returns the cpu temperature.
     * @return the {@link String} representing the cpu temperature
     */
    public static String getCpuTemperature() {
        double temperature = SENSORS.getCpuTemperature();
        if (temperature <= 0 || Double.isNaN(temperature)) {
            return "N/A";
        }

        return String.format("%.1f °C", temperature);
    }

    /**
     * Returns the physical memory.
     * @return the {@link String} representing the physical memory
     */
    public static String getPhysicalMemory() {
        long total = MEMORY.getTotal();
        long available = MEMORY.getAvailable();

        long used = total - available;

        return String.format(
                "%s / %s (%d%%)",
                formatBytes(used),
                formatBytes(total),
                (int) ((used * 100.0) / total)
        );
    }

    /**
     * Produces the textual or converted representation for format bytes.
     * @param bytes the {@code long} supplied as {@code bytes}
     * @return the {@link String} representing the format bytes result
     */
    private static String formatBytes(long bytes) {
        double gb = bytes / (1024.0 * 1024.0 * 1024.0);

        if (gb >= 1.0) {
            return String.format("%.1f GB", gb);
        }

        double mb = bytes / (1024.0 * 1024.0);
        return String.format("%.0f MB", mb);
    }
}
