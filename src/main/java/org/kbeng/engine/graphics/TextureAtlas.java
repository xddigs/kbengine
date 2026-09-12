package org.kbeng.engine.graphics;

import org.joml.Vector2f;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

/**
 * TextureAtlas provides texture atlas capabilities within the graphics subsystem.
 * It contributes to OpenGL resource ownership, render-state setup, or frame-pipeline execution.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public class TextureAtlas {
    private static final Logger log = LoggerFactory.getLogger(TextureAtlas.class);
    private final int textureId;
    private final Map<String, TextureRegion> regions = new HashMap<>();

    /**
     * Creates a new {@code TextureAtlas} instance.
     * @param imagePaths the {@link List} supplied as {@code imagePaths}
     * @param tileWidth the {@code int} supplied as {@code tileWidth}
     * @param tileHeight the {@code int} supplied as {@code tileHeight}
     */
    public TextureAtlas(List<String> imagePaths, int tileWidth, int tileHeight) {
        int count = imagePaths.size();
        int cols = (int) Math.ceil(Math.sqrt(count));
        int rows = (int) Math.ceil((double) count / cols);

        int atlasWidth = cols * tileWidth;
        int atlasHeight = rows * tileHeight;

        ByteBuffer atlasBuffer = BufferUtils.createByteBuffer(atlasWidth * atlasHeight * 4);
        STBImage.stbi_set_flip_vertically_on_load(false);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            for (int i = 0; i < count; i++) {
                String originalPath = imagePaths.get(i);
                ByteBuffer image = loadTextureFromResources(originalPath, w, h, comp);

                if (image == null) {
                    System.err.println("Failed to load texture: " + originalPath);
                    continue;
                }

                int imgW = w.get(0);
                int imgH = h.get(0);

                int col = i % cols;
                int row = i / cols;
                int offsetX = col * tileWidth;
                int offsetY = row * tileHeight;

                for (int y = 0; y < tileHeight; y++) {
                    for (int x = 0; x < tileWidth; x++) {
                        int srcX = (int) ((float) x / tileWidth * imgW);
                        int srcY = (int) ((float) y / tileHeight * imgH);

                        int srcPos = (srcY * imgW + srcX) * 4;
                        int destPos = ((offsetY + y) * atlasWidth + (offsetX + x)) * 4;

                        atlasBuffer.put(destPos, image.get(srcPos));
                        atlasBuffer.put(destPos + 1, image.get(srcPos + 1));
                        atlasBuffer.put(destPos + 2, image.get(srcPos + 2));
                        atlasBuffer.put(destPos + 3, image.get(srcPos + 3));
                    }
                }

                STBImage.stbi_image_free(image);
                float uMin = (float) offsetX / atlasWidth;
                float vMin = (float) offsetY / atlasHeight;
                float uMax = (float) (offsetX + tileWidth) / atlasWidth;
                float vMax = (float) (offsetY + tileHeight) / atlasHeight;

                regions.put(originalPath, new TextureRegion(new Vector2f(uMin, vMin),
                        new Vector2f(uMax, vMax), new Vector2f((float) tileWidth / atlasWidth,
                        (float) tileHeight / atlasHeight), new Vector2f(uMin, vMin)));
            }
        }

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, atlasWidth, atlasHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, atlasBuffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * Loads the texture from resources.
     * @param path the {@link String} supplied as {@code path}
     * @param w the {@link IntBuffer} supplied as {@code w}
     * @param h the {@link IntBuffer} supplied as {@code h}
     * @param comp the {@link IntBuffer} supplied as {@code comp}
     * @return the {@link ByteBuffer} representing the load texture from resources result
     */
    private ByteBuffer loadTextureFromResources(String path, IntBuffer w, IntBuffer h, IntBuffer comp) {
        String resourcePath = path.startsWith("/") ? path : "/" + path;
        try (InputStream in = TextureAtlas.class.getResourceAsStream(resourcePath)) {
            if (in == null) return null;

            byte[] bytes = in.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            ByteBuffer image = STBImage.stbi_load_from_memory(buffer, w, h, comp, 4);
            MemoryUtil.memFree(buffer);
            return image;
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * Binds this object to the active runtime context.
     */
    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    /**
     * Unbinds this object from the active runtime context.
     */
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * Returns the region.
     * @param path the {@link String} supplied as {@code path}
     * @return the {@link TextureRegion} representing the region
     */
    public TextureRegion getRegion(String path) {
        return regions.get(path);
    }

    /**
     * Releases the resources associated with this object.
     */
    public void dispose() {
        glDeleteTextures(textureId);
    }

    /**
     * Immutable value object containing texture region.
     */
    public record TextureRegion(Vector2f uvMin, Vector2f uvMax, Vector2f scale, Vector2f offset) {
    }
}
