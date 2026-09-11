package org.kbeng.ui;

import org.kbeng.data.GodObject;
import com.isofarm.graphics.*;
import org.kbeng.graphics.*;
import org.kbeng.input.Mouse;
import org.kbeng.item.Item;
import org.kbeng.item.Tool;
import org.kbeng.utils.K;
import org.kbeng.utils.Settings;
import org.kbeng.utils.Utils;
import org.kbeng.wrld.GameMaster;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

/**
 * Represents the ui component of the Isofarm runtime.
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
@GodObject
public class Frontend {
    private static final Shader shader = new Shader(K.Paths.UI_VERTEX_SHADER, K.Paths.UI_FRAG_SHADER);
    private static final Mesh mesh = Mesh.createQuad();
    private static final Matrix4f projection = new Matrix4f();
    private static final Matrix4f model = new Matrix4f();
    private static final Map<NineSliceKey, Texture> nineSliceTextures = new HashMap<>();

    private static final float FONT_SMALL = 16.0f;
    private static final float FONT_NORMAL = 24.0f;
    private static final float FONT_BIG = 32.0f;
    private static final float TEXT_SHADOW_OFFSET = 2.0f;
    private static final float TEXT_SHADOW_ALPHA = 0.65f;

    private static final UIFont small = new UIFont(K.Paths.FONT, FONT_SMALL);
    private static final UIFont normal = new UIFont(K.Paths.FONT, FONT_NORMAL);
    private static final UIFont big = new UIFont(K.Paths.FONT, FONT_BIG);

    private static final UIFont smallBold = new UIFont(K.Paths.FONT_BOLD, FONT_SMALL);
    private static final UIFont normalBold = new UIFont(K.Paths.FONT_BOLD, FONT_NORMAL);
    private static final UIFont bigBold = new UIFont(K.Paths.FONT_BOLD, FONT_BIG);
    private static final float CURSOR_ICON_OFFSET = 10.0f;

    private static int screenWidth;
    private static int screenHeight;
    private static boolean wasCursorIconDrawn = false;

    /**
     * Creates a new {@code GUI} instance.
     */
    private Frontend() {
    }

    /**
     * Activates this object and prepares any state it requires.
     * @param screenWidth the {@code float} supplied as {@code screenWidth}
     * @param screenHeight the {@code float} supplied as {@code screenHeight}
     */
    public static void begin(float screenWidth, float screenHeight) {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        glActiveTexture(GL_TEXTURE0);
        shader.bind();
        Frontend.screenWidth = (int) screenWidth;
        Frontend.screenHeight = (int) screenHeight;
        updateProjection();
        shader.setUniform("uProjection", projection);
        shader.setUniform("uTexture", 0);
        shader.setUniform("uUseFont", false);
        shader.setUniform("uUseSilhouette", false);
        shader.setUniform("uUsePageTransform", false);
    }

    /**
     * Deactivates this object and releases its transient state.
     */
    public static void end() {
        shader.unbind();
        glDepthMask(true);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    /**
     * Draws the line.
     * @param x1 the {@code float} supplied as {@code x1}
     * @param y1 the {@code float} supplied as {@code y1}
     * @param x2 the {@code float} supplied as {@code x2}
     * @param y2 the {@code float} supplied as {@code y2}
     * @param thickness the {@code float} supplied as {@code thickness}
     * @param color the {@link Vector4f} supplied as {@code color}
     */
    public static void drawLine(float x1, float y1, float x2, float y2,
                                float thickness, Vector4f color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length <= 0.0f || thickness <= 0.0f) {
            return;
        }

        float angle = (float) Math.atan2(dy, dx);
        model.identity()
                .translate(x1, y1, 0.0f)
                .rotateZ(angle)
                .translate(0.0f, -thickness * 0.5f, 0.0f)
                .scale(length, thickness, 1.0f);

        shader.setUniform("uModel", model);
        shader.setUniform("uColor", color);
        shader.setUniform("uUseTexture", false);
        shader.setUniform("uUseFont", false);

        mesh.render();
    }

    /**
     * Draws the border.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     * @param color the {@link Vector4f} supplied as {@code color}
     * @param borderWidth the {@code float} supplied as {@code borderWidth}
     */
    public static void drawBorder(float x, float y, float width, float height,
                                  Vector4f color, float borderWidth) {
        drawBorder(x, y, width, height, color, borderWidth, 0.0f);
    }

    /**
     * Draws the border.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     * @param color the {@link Vector4f} supplied as {@code color}
     * @param borderWidth the {@code float} supplied as {@code borderWidth}
     * @param arc the {@code float} supplied as {@code arc}
     */
    public static void drawBorder(float x, float y, float width, float height,
                                  Vector4f color, float borderWidth, float arc) {
        if (width <= 0.0f || height <= 0.0f || borderWidth <= 0.0f) {
            return;
        }

        float radius = Math.clamp(arc, 0.0f, Math.min(width, height) * 0.5f);
        model.identity().translate(x, y, 0.0f).scale(width, height, 1.0f);
        shader.setUniform("uModel", model);
        shader.setUniform("uColor", color);
        shader.setUniform("uBorderColor", color);
        shader.setUniform("uBorderWidth", borderWidth);
        shader.setUniform("uUseTexture", false);
        shader.setUniform("uUseFont", false);
        shader.setUniform("uUseRoundedRect", true);
        shader.setUniform("uRectSize", width, height);
        shader.setUniform("uCornerRadius", radius);
        shader.setUniform("uBorderOnly", true);
        mesh.render();
        shader.setUniform("uUseRoundedRect", false);
        shader.setUniform("uBorderOnly", false);
    }

    /**
     * Draws the rect.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     * @param color the {@link Vector4f} supplied as {@code color}
     */
    public static void drawRect(float x, float y, float width, float height,
                                Vector4f color) {
        drawRect(x, y, width, height, color, 0.0f, null, 0.0f);
    }

    /**
     * Draws the rect.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     * @param color the {@link Vector4f} supplied as {@code color}
     * @param borderColor the {@link Vector4f} supplied as {@code borderColor}
     * @param borderWidth the {@code float} supplied as {@code borderWidth}
     */
    public static void drawRect(float x, float y, float width, float height,
                                Vector4f color, Vector4f borderColor, float borderWidth) {
        drawRect(x, y, width, height, color, 0.0f, borderColor, borderWidth);
    }

    /**
     * Draws the rect.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     * @param color the {@link Vector4f} supplied as {@code color}
     * @param arc the {@code float} supplied as {@code arc}
     */
    public static void drawRect(float x, float y, float width, float height,
                                Vector4f color, float arc) {
        drawRect(x, y, width, height, color, arc, null, 0.0f);
    }

    /**
     * Draws the rect.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     * @param color the {@link Vector4f} supplied as {@code color}
     * @param arc the {@code float} supplied as {@code arc}
     * @param borderColor the {@link Vector4f} supplied as {@code borderColor}
     * @param borderWidth the {@code float} supplied as {@code borderWidth}
     */
    public static void drawRect(float x, float y, float width, float height,
                                Vector4f color, float arc,
                                Vector4f borderColor, float borderWidth) {
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }

        float radius = Math.clamp(arc, 0.0f, Math.min(width, height) * 0.5f);

        float border = Math.max(0.0f, borderWidth);
        boolean rounded = radius > 0.0f || border > 0.0f;
        model.identity().translate(x, y, 0.0f).scale(width, height, 1.0f);
        shader.setUniform("uModel", model);
        shader.setUniform("uColor", color);
        shader.setUniform("uBorderColor", borderColor != null ? borderColor : color);
        shader.setUniform("uBorderWidth", border);
        shader.setUniform("uUseTexture", false);
        shader.setUniform("uUseFont", false);
        shader.setUniform("uUseRoundedRect", rounded);
        shader.setUniform("uRectSize", width, height);
        shader.setUniform("uCornerRadius", radius);
        shader.setUniform("uBorderOnly", false);
        mesh.render();
        shader.setUniform("uUseRoundedRect", false);
    }

    /**
     * Draws the texture.
     * @param texture the {@link Texture} supplied as {@code texture}
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     * @param tint the {@link Vector4f} supplied as {@code tint}
     */
    public static void drawTexture(Texture texture, float x, float y,
                                   float width, float height, Vector4f tint) {
        if (texture == null) return;
        texture.bind();

        model.identity()
                .translate(x, y, 0.0f)
                .scale(width, height, 1.0f);

        shader.setUniform("uModel", model);
        shader.setUniform("uColor", tint);
        shader.setUniform("uUseTexture", true);
        shader.setUniform("uUseFont", false);
        shader.setUniform("uUVBounds", new Vector4f(0.0f, 1.0f, 1.0f, 0.0f));
        mesh.render();
        texture.unbind();
    }

    /**
     * Creates or returns a cached nine-slice texture generated from a 16x16
     * source. Corners are preserved, edges stretch on one axis and the center
     * stretches on both axes.
     * @param source the 16x16 source texture
     * @param width the resulting texture width in pixels
     * @param height the resulting texture height in pixels
     * @param sliceSize the inset of each source border in pixels
     * @return the generated {@link Texture}
     */
    public static Texture createNineSliceTexture(Texture source, int width,
                                                  int height, int sliceSize) {
        if (source == null) {
            throw new IllegalArgumentException("Nine-slice source cannot be null");
        }
        if (source.getWidth() != 16 || source.getHeight() != 16) {
            throw new IllegalArgumentException("Nine-slice source must be 16x16 pixels");
        }
        if (sliceSize <= 0 || sliceSize * 2 >= source.getWidth()) {
            throw new IllegalArgumentException("Invalid nine-slice border size: " + sliceSize);
        }
        if (width < sliceSize * 2 || height < sliceSize * 2) {
            throw new IllegalArgumentException("Nine-slice target is smaller than its borders");
        }
        if (width == source.getWidth() && height == source.getHeight()) {
            return source;
        }

        NineSliceKey key = new NineSliceKey(source.getId(), width, height, sliceSize);
        return nineSliceTextures.computeIfAbsent(key,
                ignored -> buildNineSliceTexture(source, width, height, sliceSize));
    }

    private static Texture buildNineSliceTexture(Texture source, int width,
                                                  int height, int sliceSize) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        ByteBuffer sourcePixels = MemoryUtil.memAlloc(sourceWidth * sourceHeight * 4);
        ByteBuffer targetPixels = MemoryUtil.memAlloc(width * height * 4);

        try {
            source.bind();
            glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, sourcePixels);
            source.unbind();

            for (int y = 0; y < height; y++) {
                int sourceY = mapNineSliceCoordinate(y, height, sourceHeight, sliceSize);
                for (int x = 0; x < width; x++) {
                    int sourceX = mapNineSliceCoordinate(x, width, sourceWidth, sliceSize);
                    int sourceIndex = (sourceY * sourceWidth + sourceX) * 4;
                    int targetIndex = (y * width + x) * 4;
                    for (int channel = 0; channel < 4; channel++) {
                        targetPixels.put(targetIndex + channel,
                                sourcePixels.get(sourceIndex + channel));
                    }
                }
            }

            return new Texture(width, height, targetPixels);
        } finally {
            source.unbind();
            MemoryUtil.memFree(sourcePixels);
            MemoryUtil.memFree(targetPixels);
        }
    }

    private static int mapNineSliceCoordinate(int coordinate, int targetSize,
                                               int sourceSize, int sliceSize) {
        if (coordinate < sliceSize) return coordinate;
        if (coordinate >= targetSize - sliceSize) {
            return sourceSize - (targetSize - coordinate);
        }

        int sourceCenterSize = sourceSize - sliceSize * 2;
        int targetCenterSize = targetSize - sliceSize * 2;
        return sliceSize + (coordinate - sliceSize) * sourceCenterSize / targetCenterSize;
    }

    /**
     * Draws the sprite.
     * @param spriteSheet the {@link SpriteSheet} supplied as {@code spriteSheet}
     * @param column the {@code int} supplied as {@code column}
     * @param row the {@code int} supplied as {@code row}
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     * @param tint the {@link Vector4f} supplied as {@code tint}
     */
    public static void drawSprite(SpriteSheet spriteSheet, int column, int row,
                                  float x, float y, float width, float height,
                                  Vector4f tint) {
        drawSprite(spriteSheet, column, row, x, y, width, height, tint, false);
    }

    /** Draws a sprite with the optional paper-mode tool darkening. */
    public static void drawSprite(SpriteSheet spriteSheet, int column, int row,
                                  float x, float y, float width, float height,
                                  Vector4f tint, boolean paperTool) {
        if (spriteSheet == null) return;
        int cols = spriteSheet.getCols();
        int rows = spriteSheet.getRows();

        column = Math.clamp(column, 0, cols - 1);
        row = Math.clamp(row, 0, rows - 1);

        int frameIndex = row * cols + column;

        Vector4f uv = spriteSheet.getUVBounds(frameIndex);
        Vector4f uvBounds = new Vector4f(uv.x, uv.w, uv.z, uv.y);

        spriteSheet.bind();
        model.identity().translate(x, y, 0.0f).scale(width, height, 1.0f);
        shader.setUniform("uModel", model);
        shader.setUniform("uColor", tint);
        shader.setUniform("uUseTexture", true);
        shader.setUniform("uUseFont", false);
        shader.setUniform("uPaperTool", paperTool && Settings.doEnablePaper());
        shader.setUniform("uUVBounds", uvBounds);

        mesh.render();
        shader.setUniform("uPaperTool", false);
        spriteSheet.unbind();
    }

    /**
     * Draws the sprite.
     * @param spriteSheet the {@link SpriteSheet} supplied as {@code spriteSheet}
     * @param frame the {@code int} supplied as {@code frame}
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     * @param tint the {@link Vector4f} supplied as {@code tint}
     */
    public static void drawSprite(SpriteSheet spriteSheet, int frame,
                                  float x, float y, float width, float height, Vector4f tint) {
        drawSprite(spriteSheet, frame % spriteSheet.getCols(),
                frame / spriteSheet.getCols(), x, y, width, height, tint);
    }

    /** Draws a frame with the optional paper-mode tool darkening. */
    public static void drawSprite(SpriteSheet spriteSheet, int frame,
                                  float x, float y, float width, float height,
                                  Vector4f tint, boolean paperTool) {
        if (spriteSheet == null) return;
        drawSprite(spriteSheet, frame % spriteSheet.getCols(),
                frame / spriteSheet.getCols(), x, y, width, height, tint, paperTool);
    }

    /**
     * Draws a sprite as a solid color while preserving its texture alpha.
     */
    public static void drawSpriteSilhouette(SpriteSheet spriteSheet, int frame,
                                            float x, float y, float width, float height,
                                            Vector4f color) {
        shader.setUniform("uUseSilhouette", true);
        drawSprite(spriteSheet, frame, x, y, width, height, color);
        shader.setUniform("uUseSilhouette", false);
    }

    /**
     * Draws an outline shaped by a sprite's alpha channel.
     * @param spriteSheet the sprite sheet containing the icon
     * @param frame the icon frame
     * @param x the icon x position
     * @param y the icon y position
     * @param width the icon width
     * @param height the icon height
     * @param outlineSize the outline thickness in screen pixels
     * @param color the outline color
     */
    public static void drawSpriteOutline(SpriteSheet spriteSheet, int frame,
                                         float x, float y, float width, float height,
                                         float outlineSize, Vector4f color) {
        if (spriteSheet == null || outlineSize <= 0.0f || color == null) return;

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                if (offsetX == 0 && offsetY == 0) continue;
                drawSpriteSilhouette(spriteSheet, frame,
                        x + offsetX * outlineSize,
                        y + offsetY * outlineSize,
                        width, height, color);
            }
        }
    }

    /**
     * Applies a temporary horizontal fold and curve around a book spine.
     * Every UI primitive rendered until {@link #endPageTransform()} receives
     * the same deformation.
     */
    public static void beginPageTransform(float pivotX, float scaleX,
                                          float pageWidth, float curve) {
        shader.setUniform("uPagePivotX", pivotX);
        shader.setUniform("uPageScaleX", Math.max(0.0f, scaleX));
        shader.setUniform("uPageWidth", Math.max(1.0f, pageWidth));
        shader.setUniform("uPageCurve", curve);
        shader.setUniform("uUsePageTransform", true);
    }

    /**
     * Restores normal UI rendering after a page fold.
     */
    public static void endPageTransform() {
        shader.setUniform("uUsePageTransform", false);
    }

    /**
     * Draws the string with a dark offset shadow.
     * @param text the {@link String} supplied as {@code text}
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param font the {@link UIFont} supplied as {@code font}
     * @param color the {@link Vector4f} supplied as {@code color}
     */
    public static void drawString(String text, float x, float y,
                                  UIFont font, Vector4f color) {
        drawString(text, x, y, font, color, 1.0f);
    }

    /**
     * Draws the scaled string with a dark offset shadow.
     * @param text the {@link String} supplied as {@code text}
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param font the {@link UIFont} supplied as {@code font}
     * @param color the {@link Vector4f} supplied as {@code color}
     * @param textScale the {@code float} supplied as {@code textScale}
     */
    public static void drawString(String text, float x, float y,
                                  UIFont font, Vector4f color, float textScale) {
        if (text == null || text.isEmpty() || font == null) {
            return;
        }

        font.bind();
        shader.setUniform("uUseTexture", true);
        shader.setUniform("uUseFont", true);
        shader.setUniform("uFrameIndex", 0);
        shader.setUniform("uTotalFrames", 1);

        float shadowOffset = TEXT_SHADOW_OFFSET * textScale;
        drawStringPass(text, x + shadowOffset, y + shadowOffset, font,
                new Vector4f(0.0f, 0.0f, 0.0f,
                        color.w * TEXT_SHADOW_ALPHA), textScale);
        drawStringPass(text, x, y, font, color, textScale);

        font.unbind();
        shader.setUniform("uUseFont", false);
    }

    /**
     * Renders one text-color pass using the currently bound font atlas.
     * @param text the text to render
     * @param x the horizontal origin
     * @param y the vertical origin
     * @param font the bound font
     * @param color the pass color
     * @param textScale the glyph scale
     */
    private static void drawStringPass(String text, float x, float y,
                                       UIFont font, Vector4f color,
                                       float textScale) {
        shader.setUniform("uColor", color);

        float cursorX = x;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            STBTTBakedChar glyph = font.getGlyph(codePoint);
            if (glyph == null) {
                cursorX += (font.getSize() * 0.5f) * textScale;
                i += Character.charCount(codePoint);
                continue;
            }

            float width = (glyph.x1() - glyph.x0()) * textScale;
            float height = (glyph.y1() - glyph.y0()) * textScale;

            if (width > 0.0f && height > 0.0f) {
                float glyphX = cursorX + (glyph.xoff() * textScale);
                float glyphY = y + (glyph.yoff() * textScale);

                float u0 = (float) glyph.x0() / font.getAtlasWidth();
                float v0 = (float) glyph.y0() / font.getAtlasHeight();
                float u1 = (float) glyph.x1() / font.getAtlasWidth();
                float v1 = (float) glyph.y1() / font.getAtlasHeight();

                model.identity()
                        .translate(glyphX, glyphY, 0.0f)
                        .scale(width, height, 1.0f);

                shader.setUniform("uModel", model);
                shader.setUniform("uGlyphUV", new Vector4f(u0, v0, u1, v1));

                mesh.render();
            }

            cursorX += glyph.xadvance() * textScale;
            i += Character.charCount(codePoint);
        }
    }

    /**
     * Draws the small string.
     * @param text the {@link String} supplied as {@code text}
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param color the {@link Vector4f} supplied as {@code color}
     */
    public static void drawSmallString(String text, float x, float y, Vector4f color) {
        drawString(text, x, y, small, color);
    }

    /**
     * Draws the normal string.
     * @param text the {@link String} supplied as {@code text}
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param color the {@link Vector4f} supplied as {@code color}
     */
    public static void drawNormalString(String text, float x, float y, Vector4f color) {
        drawString(text, x, y, normal, color);
    }

    /**
     * Draws the bold string.
     * @param line the {@link String} supplied as {@code line}
     * @param textX the {@code float} supplied as {@code textX}
     * @param textY the {@code float} supplied as {@code textY}
     * @param uiBookTextColor the {@link Vector4f} supplied as {@code uiBookTextColor}
     */
    public static void drawBoldString(String line, float textX, float textY, Vector4f uiBookTextColor) {
        drawString(line, textX, textY, normalBold, uiBookTextColor);
    }

    /**
     * Draws the big string.
     * @param text the {@link String} supplied as {@code text}
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param color the {@link Vector4f} supplied as {@code color}
     */
    public static void drawBigString(String text, float x, float y, Vector4f color) {
        drawString(text, x, y, big, color);
    }

    /**
     * Returns the string width.
     * @param text the {@link String} supplied as {@code text}
     * @param font the {@link UIFont} supplied as {@code font}
     * @return {@code float}; the string width
     */
    public static float getStringWidth(String text, UIFont font) {
        return getStringWidth(text, font, 1.0f);
    }

    /**
     * Returns the string width.
     * @param text the {@link String} supplied as {@code text}
     * @param font the {@link UIFont} supplied as {@code font}
     * @param textScale the {@code float} supplied as {@code textScale}
     * @return {@code float}; the string width
     */
    public static float getStringWidth(String text, UIFont font, float textScale) {
        if (text == null || text.isEmpty() || font == null) {
            return 0.0f;
        }

        float width = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            STBTTBakedChar glyph = font.getGlyph(character);
            if (glyph == null) {
                width += (font.getSize() * 0.5f) * textScale;
                continue;
            }
            width += glyph.xadvance() * textScale;
        }
        return width;
    }

    /**
     * Returns the string height.
     * @param text the {@link String} supplied as {@code text}
     * @param normalFont the {@link UIFont} supplied as {@code normalFont}
     * @return {@code float}; the string height
     */
    public static float getStringHeight(String text, UIFont normalFont) {
        return normalFont.getSize() * text.length();
    }

    /**
     * Returns the centered text y.
     * @param text the {@link String} supplied as {@code text}
     * @param font the {@link UIFont} supplied as {@code font}
     * @param boxY the {@code float} supplied as {@code boxY}
     * @param boxHeight the {@code float} supplied as {@code boxHeight}
     * @return {@code float}; the centered text y
     */
    public static float getCenteredTextY(String text, UIFont font,
                                         float boxY, float boxHeight) {
        if (text == null || text.isEmpty() || font == null) {
            return boxY;
        }

        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            STBTTBakedChar glyph = font.getGlyph(codePoint);

            if (glyph != null) {
                float top = glyph.yoff();
                float bottom = glyph.yoff() + (glyph.y1() - glyph.y0());

                minY = Math.min(minY, top);
                maxY = Math.max(maxY, bottom);
            }

            i += Character.charCount(codePoint);
        }

        if (minY == Float.MAX_VALUE) {
            return boxY + font.getSize();
        }

        float textHeight = maxY - minY;
        float centeredTop = boxY + (boxHeight - textHeight) * 0.5f;
        return centeredTop - minY;
    }

    /**
     * Updates text or selection state for wrap text.
     * @param text the {@link String} supplied as {@code text}
     * @param maxWidth the {@code float} supplied as {@code maxWidth}
     * @param font the {@link UIFont} supplied as {@code font}
     * @return an array of {@link String} values; the wrap text result
     */
    public static String[] wrapText(String text, float maxWidth, UIFont font) {
        if (text == null || text.isEmpty() || maxWidth <= 0.0f) {
            return new String[0];
        }

        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) continue;
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (getStringWidth(testLine, font) <= maxWidth) {
                if (!currentLine.isEmpty()) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                    currentLine.setLength(0);
                }
                currentLine.append(word);
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines.toArray(new String[0]);
    }

    /**
     * Adds scissor to the corresponding collection or processing queue.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     */
    public static void pushScissor(float x, float y, float width, float height) {
        int windowHeight = getScreenHeight();
        glEnable(GL_SCISSOR_TEST);
        glScissor((int) x, windowHeight - (int) (y + height),
                (int) width, (int) height);
    }

    /**
     * Removes scissor and updates any dependent state.
     */
    public static void popScissor() {
        glDisable(GL_SCISSOR_TEST);
    }

    /**
     * Renders this object in the requested render pass.
     * @param element the {@link UIElement} supplied as {@code element}
     */
    public static void render(UIElement element) {
        if (element == null || !element.isActuallyVisible()) {
            return;
        }

        if (element.hasStaticSprite()) {
            drawTexture(
                    element.getSprite(),
                    element.getAbsoluteX(),
                    element.getAbsoluteY(),
                    element.getAbsoluteWidth(),
                    element.getAbsoluteHeight(),
                    element.getTint()
            );
        }

        if (element.hasSpriteSheet()) {
            drawSprite(
                    element.getSpriteSheet(),
                    element.getSpriteCol(),
                    element.getSpriteRow(),
                    element.getAbsoluteX(),
                    element.getAbsoluteY(),
                    element.getAbsoluteWidth(),
                    element.getAbsoluteHeight(),
                    element.getTint()
            );
        }

        element.render();
        element.renderChildren();
    }

    /**
     * Transforms this object according to the supplied values.
     * @param width the {@code int} supplied as {@code width}
     * @param height the {@code int} supplied as {@code height}
     */
    public static void resize(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        updateProjection();
    }

    /**
     * Updates the projection.
     */
    private static void updateProjection() {
        projection.identity().ortho2D(
                0.0f,
                screenWidth,
                screenHeight,
                0.0f
        );
    }

    /**
     * Draws the cursor.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     */
    public static void drawCursor(GameMaster gameMaster) {
        if (gameMaster == null) return;
        if (wasCursorIconDrawn) return;

        var hotbarUI = GameUIService.ui.getHotbarUI();
        if (hotbarUI == null) return;

        Item selectedItem = Settings.selectedItem;
        if (selectedItem == null) return;

        SpriteSheet spriteSheet = ResourceManager.getItemSpriteSheet(selectedItem);
        if (spriteSheet == null) return;

        int frameIndex = ResourceManager.getItemFrame(selectedItem);
        float iconSize = 32.0f;

        float renderX = Mouse.getX() + CURSOR_ICON_OFFSET;
        float renderY = Mouse.getY() + CURSOR_ICON_OFFSET;
        drawSprite(spriteSheet, frameIndex, renderX, renderY, iconSize, iconSize,
                new Vector4f(1.0f), (selectedItem instanceof Tool));
        wasCursorIconDrawn = true;
    }

    /**
     * Sets the value of the {@code wasCursorIconDrawn} field.
     * @param wasCursorIconDrawn the new value of the {@code wasCursorIconDrawn} field
     */
    public static void setWasCursorIconDrawn(boolean wasCursorIconDrawn) {
        Frontend.wasCursorIconDrawn = wasCursorIconDrawn;
    }

    /**
     * Returns the screen width.
     * @return {@code int}; the screen width
     */
    public static int getScreenWidth() {
        return screenWidth;
    }

    /**
     * Returns the screen height.
     * @return {@code int}; the screen height
     */
    public static int getScreenHeight() {
        return screenHeight;
    }

    /**
     * Returns the shader.
     * @return the {@link Shader} representing the shader
     */
    public static Shader getShader() {
        return shader;
    }

    /**
     * Returns the small font.
     * @return the {@link UIFont} representing the small font
     */
    public static UIFont getSmallFont() {
        return small;
    }

    /**
     * Returns the normal font.
     * @return the {@link UIFont} representing the normal font
     */
    public static UIFont getNormalFont() {
        return normal;
    }

    /**
     * Returns the big font.
     * @return the {@link UIFont} representing the big font
     */
    public static UIFont getBigFont() {
        return big;
    }

    /**
     * Returns the small bold font.
     * @return the {@link UIFont} representing the small bold font
     */
    public static UIFont getSmallBoldFont() {
        return smallBold;
    }

    /**
     * Returns the normal bold font.
     * @return the {@link UIFont} representing the normal bold font
     */
    public static UIFont getNormalBoldFont() {
        return normalBold;
    }

    /**
     * Returns the big bold font.
     * @return the {@link UIFont} representing the big bold font
     */
    public static UIFont getBigBoldFont() {
        return bigBold;
    }

    /**
     * Releases the resources associated with this object.
     */
    public static void dispose() {
        nineSliceTextures.values().forEach(Texture::dispose);
        nineSliceTextures.clear();
        mesh.dispose();
        shader.dispose();
    }

    /**
     * Draws a texture.
     * @param sourceId the {@code int} supplied as {@code sourceId}
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     * @param sliceSize the {@code float} supplied as {@code sliceSize}
     */
    private record NineSliceKey(int sourceId, int width, int height, int sliceSize) {}
}
