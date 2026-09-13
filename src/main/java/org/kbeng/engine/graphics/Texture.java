package org.kbeng.engine.graphics;

import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.stb.STBImage.*;

/**
 * Texture provides texture capabilities within the graphics subsystem.
 * It contributes to OpenGL resource ownership, render-state setup, or frame-pipeline execution.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public class Texture {
    private static final Logger log = LoggerFactory.getLogger(Texture.class);
    private final int id;
    private final int width;
    private final int height;

    /**
     * Creates a new {@code Texture} instance.
     * @param resourcePath the {@link String} supplied as {@code resourcePath}
     */
    public Texture(String resourcePath) {
        stbi_set_flip_vertically_on_load(true);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            IntBuffer pChannels = stack.mallocInt(1);

            byte[] rawData;
            try (InputStream in = Texture.class.getClassLoader()
                    .getResourceAsStream(resourcePath)) {
                if (in == null) {
                    throw new IllegalArgumentException("Resource not found" +
                            " on classpath: " + resourcePath);
                }
                rawData = in.readAllBytes();
            } catch (Exception e) {
                throw new RuntimeException("Failed to read texture file " +
                        "[" + resourcePath + "]", e);
            }

            ByteBuffer rawBuffer = stack.malloc(rawData.length);
            rawBuffer.put(rawData).flip();

            ByteBuffer imageBuffer = stbi_load_from_memory(rawBuffer,
                    pWidth, pHeight, pChannels, 4);
            if (imageBuffer == null) {
                throw new RuntimeException("Failed to decode texture " +
                        "[" + resourcePath + "]: " + stbi_failure_reason());
            }

            this.width = pWidth.get(0);
            this.height = pHeight.get(0);

            this.id = upload(width, height, imageBuffer);

            stbi_image_free(imageBuffer);
        }

        log.info("Texture loaded successfully [{}] ({}x{} px, ID: {})",
                resourcePath, width, height, id);
    }

    /**
     * Creates a texture from RGBA pixel data.
     * @param width the texture width in pixels
     * @param height the texture height in pixels
     * @param pixels the RGBA pixel buffer
     */
    public Texture(int width, int height, ByteBuffer pixels) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be positive");
        }
        if (pixels == null || pixels.remaining() < width * height * 4) {
            throw new IllegalArgumentException("Insufficient RGBA pixel data");
        }

        this.width = width;
        this.height = height;
        this.id = upload(width, height, pixels);
    }

    private static int upload(int width, int height, ByteBuffer pixels) {
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        glBindTexture(GL_TEXTURE_2D, 0);
        return textureId;
    }

    /**
     * Returns the texture long
     * @param texture the {@link Texture} argument; path to the texture
     * @return {@link Long} the texture long
     */
    public static long getTextureLong(Texture texture) {
        return texture.getId();
    }

    /**
     * Binds this object to the active runtime context.
     */
    public void bind() {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, id);
    }

    /**
     * Unbinds this object from the active runtime context.
     */
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * Releases the resources associated with this object.
     */
    public void dispose() {
        glDeleteTextures(id);
        log.info("Texture resource deleted (ID: {})", id);
    }

    /**
     * Returns the id.
     * @return {@code int}; the id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the width.
     * @return {@code int}; the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height.
     * @return {@code int}; the height
     */
    public int getHeight() {
        return height;
    }
}
