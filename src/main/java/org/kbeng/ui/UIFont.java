package org.kbeng.ui;

import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * Represents the uifont component of the kbengine runtime.
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
@SuppressWarnings("all")
public class UIFont {
    private static final int FIRST_CHAR = 32;
    private static final int LAST_CHAR = 255;
    private static final int CHAR_COUNT = LAST_CHAR - FIRST_CHAR + 1;

    private final String path;
    private final float size;
    private final int atlasWidth;
    private final int atlasHeight;
    private final int textureId;
    private final STBTTBakedChar.Buffer glyphs;

    /**
     * Creates a new {@code UIFont} instance.
     * @param path the {@link String} supplied as {@code path}
     * @param size the {@code float} supplied as {@code size}
     */
    public UIFont(String path, float size) {
        this(path, size, 1024, 1024);
    }

    /**
     * Creates a new {@code UIFont} instance.
     * @param path the {@link String} supplied as {@code path}
     * @param size the {@code float} supplied as {@code size}
     * @param atlasWidth the {@code int} supplied as {@code atlasWidth}
     * @param atlasHeight the {@code int} supplied as {@code atlasHeight}
     */
    public UIFont(String path, float size, int atlasWidth, int atlasHeight) {
        this.path = path;
        this.size = size;
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;

        ByteBuffer fontData = loadFont(path);
        glyphs = STBTTBakedChar.malloc(CHAR_COUNT);
        ByteBuffer bitmap = memAlloc(atlasWidth * atlasHeight);
        STBTruetype.stbtt_BakeFontBitmap(fontData, size, bitmap, atlasWidth,
                atlasHeight, FIRST_CHAR, glyphs);

        textureId = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, textureId);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, atlasWidth, atlasHeight,
                0, GL_RED, GL_UNSIGNED_BYTE, bitmap);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glBindTexture(GL_TEXTURE_2D, 0);

        memFree(bitmap);
        memFree(fontData);
    }

    /**
     * Loads the font.
     * @param path the {@link String} supplied as {@code path}
     * @return the {@link ByteBuffer} representing the load font result
     */
    private ByteBuffer loadFont(String path) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalArgumentException("Font not found: " + path);
            }

            byte[] data = input.readAllBytes();

            ByteBuffer buffer = memAlloc(data.length);
            buffer.put(data);
            buffer.flip();

            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load font: " + path, e);
        }
    }

    /**
     * Binds this object to the active runtime context.
     */
    public void bind() {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    /**
     * Unbinds this object from the active runtime context.
     */
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * Returns the texture id.
     * @return {@code int}; the texture id
     */
    public int getTextureId() {
        return textureId;
    }

    /**
     * Returns the size.
     * @return {@code float}; the size
     */
    public float getSize() {
        return size;
    }

    /**
     * Returns the atlas width.
     * @return {@code int}; the atlas width
     */
    public int getAtlasWidth() {
        return atlasWidth;
    }

    /**
     * Returns the atlas height.
     * @return {@code int}; the atlas height
     */
    public int getAtlasHeight() {
        return atlasHeight;
    }

    /**
     * Returns the glyph.
     * @param character the {@code int} supplied as {@code character}
     * @return the {@link STBTTBakedChar} representing the glyph
     */
    public STBTTBakedChar getGlyph(int character) {
        if (character < FIRST_CHAR || character > LAST_CHAR) {
            return null;
        }

        return glyphs.get(character - FIRST_CHAR);
    }

    /**
     * Returns the first char.
     * @return {@code int}; the first char
     */
    public int getFirstChar() {
        return FIRST_CHAR;
    }

    /**
     * Returns the last char.
     * @return {@code int}; the last char
     */
    public int getLastChar() {
        return LAST_CHAR;
    }

    /**
     * Releases the resources associated with this object.
     */
    public void dispose() {
        glDeleteTextures(textureId);
        glyphs.free();
    }
}
