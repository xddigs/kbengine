package org.kbeng.graphics;

import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * SpriteSheet provides sprite sheet capabilities within the graphics subsystem.
 *
 * It contributes to OpenGL resource ownership, render-state setup, or frame-pipeline execution.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public class SpriteSheet {
    private final Texture texture;
    private final int totalFrames;
    private final int cols;
    private final int rows;
    private final BufferedImage sourceImage;
    private final Map<Integer, Vector3f> frameColors = new HashMap<>();

    /**
     * Creates a new {@code SpriteSheet} instance.
     * @param path the {@link String} supplied as {@code path}
     * @param cols the {@code int} supplied as {@code cols}
     * @param rows the {@code int} supplied as {@code rows}
     */
    public SpriteSheet(String path, int cols, int rows) {
        if (cols <= 0) {
            throw new IllegalArgumentException("cols must be greater than 0");
        }

        if (rows <= 0) {
            throw new IllegalArgumentException("rows must be greater than 0");
        }

        this.texture = new Texture(path);
        this.cols = cols;
        this.rows = rows;
        this.totalFrames = cols * rows;
        this.sourceImage = loadSourceImage(path);
    }

    private static BufferedImage loadSourceImage(String path) {
        try (InputStream input = SpriteSheet.class.getClassLoader().getResourceAsStream(path)) {
            return input == null ? null : ImageIO.read(input);
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * Returns the texture id.
     * @return {@code int}; the texture id
     */
    public int getTextureId() {
        return texture.getId();
    }

    /**
     * Binds this object to the active runtime context.
     */
    public void bind() {
        texture.bind();
    }

    /**
     * Unbinds this object from the active runtime context.
     */
    public void unbind() {
        texture.unbind();
    }

    /**
     * Returns the cols.
     * @return {@code int}; the cols
     */
    public int getCols() {
        return cols;
    }

    /**
     * Returns the rows.
     * @return {@code int}; the rows
     */
    public int getRows() {
        return rows;
    }

    /**
     * Returns the frames per row.
     * @return {@code int}; the frames per row
     */
    public int getFramesPerRow() {
        return cols;
    }

    /**
     * Returns the width.
     * @return {@code float}; the width
     */
    public float getWidth() {
        return texture.getWidth();
    }

    /**
     * Returns the height.
     * @return {@code float}; the height
     */
    public float getHeight() {
        return texture.getHeight();
    }

    /**
     * Returns the frame width.
     * @return {@code int}; the frame width
     */
    public int getFrameWidth() {
        return texture.getWidth() / cols;
    }

    /**
     * Returns the frame height.
     * @return {@code int}; the frame height
     */
    public int getFrameHeight() {
        return texture.getHeight() / rows;
    }

    /**
     * Returns the total frames.
     * @return {@code int}; the total frames
     */
    public int getTotalFrames() {
        return totalFrames;
    }

    /**
     * Returns the uvbounds.
     * @param frameIndex the {@code int} supplied as {@code frameIndex}
     * @return the {@link Vector4f} representing the uvbounds
     */
    public Vector4f getUVBounds(int frameIndex) {
        frameIndex = Math.clamp(frameIndex, 0, totalFrames - 1);
        int column = frameIndex % cols;
        int row = frameIndex / cols;

        float frameWidth = 1.0f / cols;
        float frameHeight = 1.0f / rows;

        float uMin = column * frameWidth;
        float uMax = uMin + frameWidth;

        float vMax = 1.0f - row * frameHeight;
        float vMin = vMax - frameHeight;
        return new Vector4f(uMin, vMin, uMax, vMax);
    }

    /**
     * Returns a saturated midtone colour from an individual sprite frame.
     * Dark outlines and bright highlights are excluded before averaging.
     */
    public Vector3f getFrameColor(int frameIndex) {
        frameIndex = Math.clamp(frameIndex, 0, totalFrames - 1);
        Vector3f cached = frameColors.get(frameIndex);
        if (cached != null) return new Vector3f(cached);
        if (sourceImage == null) return new Vector3f(1.0f);

        int column = frameIndex % cols;
        int row = frameIndex / cols;
        int frameWidth = sourceImage.getWidth() / cols;
        int frameHeight = sourceImage.getHeight() / rows;
        int startX = column * frameWidth;
        int startY = row * frameHeight;

        float red = 0.0f, green = 0.0f, blue = 0.0f, totalWeight = 0.0f;
        for (int y = startY; y < startY + frameHeight; y++) {
            for (int x = startX; x < startX + frameWidth; x++) {
                int pixel = sourceImage.getRGB(x, y);
                int alpha = (pixel >>> 24) & 0xFF;
                if (alpha < 50) continue;

                float r = ((pixel >>> 16) & 0xFF) / 255.0f;
                float g = ((pixel >>> 8) & 0xFF) / 255.0f;
                float b = (pixel & 0xFF) / 255.0f;

                float max = Math.max(r, Math.max(g, b));
                float min = Math.min(r, Math.min(g, b));
                float delta = max - min;

                float value = max;
                float saturation = (max == 0.0f) ? 0.0f : delta / max;

                if (value < 0.35f || saturation < 0.20f) continue;

                float weight = saturation * value;
                red += r * weight;
                green += g * weight;
                blue += b * weight;
                totalWeight += weight;
            }
        }

        Vector3f color;
        if (totalWeight == 0.0f) {
            color = new Vector3f(1.0f);
        } else {
            color = new Vector3f(red / totalWeight, green / totalWeight, blue / totalWeight);
            float satFactor = 1.25f;
            float valFactor = 1.20f;

            adjustHSV(color, satFactor, valFactor);
        }

        frameColors.put(frameIndex, color);
        return new Vector3f(color);
    }

    /**
     * Adjusts the HSV values
     * @param rgb the {@link Vector3f} supplied as {@code rgb}
     * @param satFactor 1.0f = no change, 0.0f = grayscale
     * @param valFactor  1.0f = no change, 0.0f = black
     */
    private static void adjustHSV(Vector3f rgb, float satFactor, float valFactor) {
        float r = rgb.x, g = rgb.y, b = rgb.z;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        if (max == 0.0f) return;

        float h = 0.0f;
        if (delta != 0.0f) {
            if (max == r) h = (g - b) / delta + (g < b ? 6.0f : 0.0f);
            else if (max == g) h = (b - r) / delta + 2.0f;
            else h = (r - g) / delta + 4.0f;
            h /= 6.0f;
        }

        float s = delta / max;
        float v = max;

        s = Math.clamp(s * satFactor, 0.0f, 1.0f);
        v = Math.clamp(v * valFactor, 0.0f, 1.0f);

        int i = (int) (h * 6.0f);
        float f = h * 6.0f - i;
        float p = v * (1.0f - s);
        float q = v * (1.0f - f * s);
        float t = v * (1.0f - (1.0f - f) * s);

        switch (i % 6) {
            case 0 -> { rgb.x = v; rgb.y = t; rgb.z = p; }
            case 1 -> { rgb.x = q; rgb.y = v; rgb.z = p; }
            case 2 -> { rgb.x = p; rgb.y = v; rgb.z = t; }
            case 3 -> { rgb.x = p; rgb.y = q; rgb.z = v; }
            case 4 -> { rgb.x = t; rgb.y = p; rgb.z = v; }
            case 5 -> { rgb.x = v; rgb.y = p; rgb.z = q; }
        }
    }

    /**
     * Releases the resources associated with this object.
     */
    public void dispose() {
        texture.dispose();
    }
}
