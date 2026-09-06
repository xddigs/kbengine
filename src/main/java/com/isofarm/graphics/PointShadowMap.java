package com.isofarm.graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static org.lwjgl.opengl.GL30.*;

/** Depth cubemap used by one shadow-casting point light. */
public final class PointShadowMap {
    public static final int SIZE = 256;
    private final int framebuffer = glGenFramebuffers();
    private final int depthTexture = glGenTextures();

    /**
     * Creates a new {@code PointShadowMap} instance.
     */
    public PointShadowMap() {
        glBindTexture(GL_TEXTURE_CUBE_MAP, depthTexture);
        for (int face = 0; face < 6; face++) {
            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, 0, GL_DEPTH_COMPONENT32F,
                    SIZE, SIZE, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0);
        }
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
    }

    /**
     * Binds this object to the active runtime context.
     * @param face the {@code int} supplied as {@code face}
     */
    public void bindFace(int face) {
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
                GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, depthTexture, 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
        glViewport(0, 0, SIZE, SIZE);
        glClear(GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Returns the depth texture.
     * @return {@code int}; the depth texture
     */
    public int getDepthTexture() { return depthTexture; }

    /**
     * Releases the resources associated with this object.
     */
    public void dispose() {
        glDeleteFramebuffers(framebuffer);
        glDeleteTextures(depthTexture);
    }
}
