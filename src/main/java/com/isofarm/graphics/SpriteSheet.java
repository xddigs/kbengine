package com.isofarm.graphics;

import org.joml.Vector4f;
import org.joml.Vector3f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the state and operations required by sprite sheet within the game runtime.
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
     * Returns a brightened midtone colour from an individual sprite frame.
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
        float[] luminances = new float[frameWidth * frameHeight];
        int colorCount = 0;
        for (int y = startY; y < startY + frameHeight; y++) {
            for (int x = startX; x < startX + frameWidth; x++) {
                int pixel = sourceImage.getRGB(x, y);
                if (((pixel >>> 24) & 0xFF) < 26) continue;
                luminances[colorCount++] = luminance(pixel);
            }
        }
        if (colorCount == 0) return new Vector3f(1.0f);

        Arrays.sort(luminances, 0, colorCount);
        float minLuminance = luminances[(int) (colorCount * 0.20f)];
        float maxLuminance = luminances[Math.max(0, (int) (colorCount * 0.80f) - 1)];
        float red = 0.0f, green = 0.0f, blue = 0.0f, weight = 0.0f;
        for (int y = startY; y < startY + frameHeight; y++) {
            for (int x = startX; x < startX + frameWidth; x++) {
                int pixel = sourceImage.getRGB(x, y);
                float alpha = ((pixel >>> 24) & 0xFF) / 255.0f;
                float luminance = luminance(pixel);
                if (alpha < 0.10f || luminance < minLuminance || luminance > maxLuminance) continue;
                red += ((pixel >>> 16) & 0xFF) * alpha;
                green += ((pixel >>> 8) & 0xFF) * alpha;
                blue += (pixel & 0xFF) * alpha;
                weight += alpha;
            }
        }
        Vector3f color = weight == 0.0f ? new Vector3f(1.0f)
                : new Vector3f(red / (255.0f * weight), green / (255.0f * weight),
                        blue / (255.0f * weight));
        color.mul(1.15f);
        color.x = Math.min(color.x, 1.0f);
        color.y = Math.min(color.y, 1.0f);
        color.z = Math.min(color.z, 1.0f);
        frameColors.put(frameIndex, color);
        return new Vector3f(color);
    }

    private static float luminance(int pixel) {
        return (((pixel >>> 16) & 0xFF) * 0.2126f
                + ((pixel >>> 8) & 0xFF) * 0.7152f
                + (pixel & 0xFF) * 0.0722f) / 255.0f;
    }

    /**
     * Releases the resources associated with this object.
     */
    public void dispose() {
        texture.dispose();
    }
}
