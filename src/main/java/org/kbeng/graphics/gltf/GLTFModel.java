package org.kbeng.graphics.gltf;

import org.kbeng.graphics.Shader;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;

/**
 * GLTFModel provides gltfmodel capabilities within the graphics subsystem.
 * It contributes to OpenGL resource ownership, render-state setup, or frame-pipeline execution.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public class GLTFModel {
    private final List<GLTFMesh> meshes;
    private final List<GLTFNode> nodes;
    private final List<GLTFNode> rootNodes;
    private final boolean doesOwnMeshes;

    /**
     * Creates a new {@code GLTFModel} instance.
     */
    public GLTFModel() {
        this(true);
    }

    private GLTFModel(boolean doesOwnMeshes) {
        this.meshes = new ArrayList<>();
        this.nodes = new ArrayList<>();
        this.rootNodes = new ArrayList<>();
        this.doesOwnMeshes = doesOwnMeshes;
    }

    /**
     * Adds the mesh.
     * @param mesh the {@link GLTFMesh} supplied as {@code mesh}
     */
    public void addMesh(GLTFMesh mesh) {
        meshes.add(mesh);
    }

    /**
     * Adds the node.
     * @param node the {@link GLTFNode} supplied as {@code node}
     */
    public void addNode(GLTFNode node) {
        nodes.add(node);
    }

    /**
     * Adds the root node.
     * @param node the {@link GLTFNode} supplied as {@code node}
     */
    public void addRootNode(GLTFNode node) {
        rootNodes.add(node);
    }

    /**
     * Renders this object in the requested render pass.
     * @param shader the {@link Shader} supplied as {@code shader}
     * @param modelMatrix the {@link Matrix4f} supplied as {@code modelMatrix}
     */
    public void render(Shader shader, Matrix4f modelMatrix) {
        shader.bind();

        for (GLTFNode node : rootNodes) {
            node.render(this, modelMatrix, shader);
        }
    }

    /**
     * Renders the mesh.
     * @param meshIndex the {@code int} supplied as {@code meshIndex}
     * @param worldMatrix the {@link Matrix4f} supplied as {@code worldMatrix}
     * @param shader the {@link Shader} supplied as {@code shader}
     */
    public void renderMesh(int meshIndex, Matrix4f worldMatrix, Shader shader) {
        if (meshIndex < 0 || meshIndex >= meshes.size()) {
            return;
        }

        shader.setUniform("uModel", worldMatrix);
        meshes.get(meshIndex).render(shader);
    }

    /** Renders one mesh with a multiplicative material tint. */
    public void renderMesh(int meshIndex, Matrix4f worldMatrix, Shader shader,
                           Vector3f colorTint) {
        if (meshIndex < 0 || meshIndex >= meshes.size()) return;
        shader.setUniform("uModel", worldMatrix);
        GLTFMesh mesh = meshes.get(meshIndex);
        mesh.render(shader, mesh.getTextureId(),
                new Vector4f(0.0f, 0.0f, 1.0f, 1.0f), colorTint);
    }

    /**
     * Renders the mesh.
     * @param meshIndex the {@code int} supplied as {@code meshIndex}
     * @param worldMatrix the {@link Matrix4f} supplied as {@code worldMatrix}
     * @param shader the {@link Shader} supplied as {@code shader}
     * @param textureId the {@code int} supplied as {@code textureId}
     * @param uvBounds the {@link Vector4f} supplied as {@code uvBounds}
     */
    public void renderMesh(int meshIndex, Matrix4f worldMatrix, Shader shader, int textureId, Vector4f uvBounds) {
        if (meshIndex < 0 || meshIndex >= meshes.size()) {
            return;
        }

        shader.setUniform("uModel", worldMatrix);

        meshes.get(meshIndex).render(shader, textureId, uvBounds);
    }

    /** Renders one mesh with texture overrides and a material tint. */
    public void renderMesh(int meshIndex, Matrix4f worldMatrix, Shader shader,
                           int textureId, Vector4f uvBounds, Vector3f colorTint) {
        if (meshIndex < 0 || meshIndex >= meshes.size()) return;
        shader.setUniform("uModel", worldMatrix);
        meshes.get(meshIndex).render(shader, textureId, uvBounds, colorTint);
    }

    /**
     * Creates an independently transformable instance of this model.
     * <p>GPU mesh resources are shared with the source model, while the
     * node hierarchy and all mutable transform state are copied.</p>
     * @return independent render instance backed by the same GPU meshes
     */
    public GLTFModel createInstance() {
        GLTFModel instance = new GLTFModel(false);
        instance.meshes.addAll(meshes);

        for (GLTFNode root : rootNodes) {
            GLTFNode rootCopy = root.copy();
            instance.rootNodes.add(rootCopy);
            instance.collectNodes(rootCopy);
        }

        instance.updateTransforms();
        return instance;
    }

    /**
     * Registers a copied node hierarchy in the flat node collection.
     */
    private void collectNodes(GLTFNode node) {
        nodes.add(node);

        for (GLTFNode child : node.getChildren()) {
            collectNodes(child);
        }
    }

    /**
     * Finds and returns the node.
     * @param name the {@link String} supplied as {@code name}
     * @return the {@link GLTFNode} representing the located node
     */
    public GLTFNode findNode(String name) {
        for (GLTFNode node : rootNodes) {
            GLTFNode result = node.find(name);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * Returns the texture.
     * @return {@code int}; the texture
     */
    public int getTexture() {
        for (GLTFMesh mesh : meshes) {
            if (mesh.getTextureId() > 0) {
                return mesh.getTextureId();
            }
        }

        return 0;
    }

    /**
     * Returns the nodes.
     * @return the {@link List} representing the nodes
     */
    public List<GLTFNode> getNodes() {
        return nodes;
    }

    /**
     * Returns the root nodes.
     * @return the {@link List} representing the root nodes
     */
    public List<GLTFNode> getRootNodes() {
        return rootNodes;
    }

    /**
     * Returns the meshes.
     * @return the {@link List} representing the meshes
     */
    public List<GLTFMesh> getMeshes() {
        return meshes;
    }

    /**
     * Returns the {@code ownsMeshes} value
     * @return {@link boolean} value of {@code ownsMeshes}
     */
    public boolean isDoesOwnMeshes() {
        return doesOwnMeshes;
    }

    /**
     * Updates the transforms.
     */
    public void updateTransforms() {
        for (GLTFNode node : rootNodes) {
            node.updateTransform();
        }
    }

    /**
     * Releases the resources associated with this object.
     */
    public void dispose() {
        if (doesOwnMeshes) {
            for (GLTFMesh mesh : meshes) {
                mesh.dispose();
            }
        }

        meshes.clear();
        nodes.clear();
        rootNodes.clear();
    }

    /**
 * Represents the gltfmesh component of the kbengine runtime.
 * <p>This type centralizes the state, lifecycle and behavior required by its callers,
 * keeping domain rules together with the data they operate on.
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li><b>State:</b> Owns the data needed to represent the component consistently.</li>
 *   <li><b>Lifecycle:</b> Exposes the operations used to create, update and release its state.</li>
 *   <li><b>Integration:</b> Coordinates with the surrounding game, rendering or UI systems through its public API.</li>
 * </ul>
 * <p>Callers should use the documented public operations and allow this type to preserve
 * its invariants rather than modifying implementation details directly.
 */
    public static class GLTFMesh {
        private final int vao;
        private final int vbo;
        private final int ebo;

        private final int indexCount;
        private final int textureId;

        /**
         * Creates a new {@code GLTFMesh} instance.
         * @param vao the {@code int} supplied as {@code vao}
         * @param vbo the {@code int} supplied as {@code vbo}
         * @param ebo the {@code int} supplied as {@code ebo}
         * @param indexCount the {@code int} supplied as {@code indexCount}
         * @param textureId the {@code int} supplied as {@code textureId}
         */
        public GLTFMesh(int vao, int vbo, int ebo, int indexCount, int textureId) {
            this.vao = vao;
            this.vbo = vbo;
            this.ebo = ebo;
            this.indexCount = indexCount;
            this.textureId = textureId;
        }

        /**
         * Returns the texture id.
         * @return {@code int}; the texture id
         */
        public int getTextureId() {
            return textureId;
        }

        /**
         * Renders this object in the requested render pass.
         * @param shader the {@link Shader} supplied as {@code shader}
         */
        public void render(Shader shader) {
            render(shader, textureId, new Vector4f(0.0f, 0.0f, 1.0f, 1.0f), null);
        }

        /**
         * Renders this object in the requested render pass.
         * @param shader the {@link Shader} supplied as {@code shader}
         * @param renderTextureId the {@code int} supplied as {@code renderTextureId}
         * @param uvBounds the {@link Vector4f} supplied as {@code uvBounds}
         */
        public void render(Shader shader, int renderTextureId,
                           Vector4f uvBounds) {
            render(shader, renderTextureId, uvBounds, null);
        }

        /** Renders this mesh with optional texture and color overrides. */
        public void render(Shader shader, int renderTextureId,
                           Vector4f uvBounds, Vector3f colorTint) {
            glActiveTexture(GL_TEXTURE0);

            shader.setUniform("uUseColorTint", colorTint != null);
            if (colorTint != null) shader.setUniform("uColorTint", colorTint);

            if (renderTextureId > 0) {
                glBindTexture(GL_TEXTURE_2D, renderTextureId);

                shader.setUniform("uTexture", 0);
                shader.setUniform("uUseTexture", true);
                shader.setUniform("uUVBounds", uvBounds);
            } else {
                shader.setUniform("uUseTexture", false);
                shader.setUniform("uUVBounds",
                        new Vector4f(0.0f, 0.0f, 1.0f, 1.0f));
            }

            glBindVertexArray(vao);
            glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);
        }

        /**
         * Releases the resources associated with this object.
         */
        public void dispose() {
            glDeleteVertexArrays(vao);
            glDeleteBuffers(vbo);
            glDeleteBuffers(ebo);

            if (textureId > 0) {
                glDeleteTextures(textureId);
            }
        }
    }
}
