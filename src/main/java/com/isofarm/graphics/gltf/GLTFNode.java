package com.isofarm.graphics.gltf;

import com.isofarm.graphics.Shader;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the state and operations required by gltfnode within the game runtime.
 */
public class GLTFNode {

    private final String name;
    private final int meshIndex;

    private final Vector3f translation;
    private final Quaternionf rotation;
    private final Vector3f scale;

    private final Matrix4f localMatrix;
    private final Matrix4f worldMatrix;

    private final List<GLTFNode> children;

    private boolean isVisible = true;

    private int textureOverride = 0;
    private Vector4f uvOverride = null;
    private Vector3f colorTint = null;

    /**
     * Creates a new {@code GLTFNode} instance.
     * @param name the {@link String} supplied as {@code name}
     * @param meshIndex the {@code int} supplied as {@code meshIndex}
     * @param translation the {@link Vector3f} supplied as {@code translation}
     * @param rotation the {@link Quaternionf} supplied as {@code rotation}
     * @param scale the {@link Vector3f} supplied as {@code scale}
     */
    public GLTFNode(String name, int meshIndex, Vector3f translation, Quaternionf rotation, Vector3f scale) {
        this.name = name;
        this.meshIndex = meshIndex;

        this.translation = new Vector3f(translation);
        this.rotation = new Quaternionf(rotation);
        this.scale = new Vector3f(scale);

        this.localMatrix = new Matrix4f();
        this.worldMatrix = new Matrix4f();

        this.children = new ArrayList<>();

        updateLocalMatrix();
    }

    /**
     * Updates the local matrix.
     */
    private void updateLocalMatrix() {
        localMatrix.identity()
                .translate(translation)
                .rotate(rotation)
                .scale(scale);
    }

    /**
     * Updates the transform.
     */
    public void updateTransform() {
        updateLocalMatrix();

        for (GLTFNode child : children) {
            child.updateTransform();
        }
    }

    /**
     * Renders this object in the requested render pass.
     * @param model the {@link GLTFModel} supplied as {@code model}
     * @param parentMatrix the {@link Matrix4f} supplied as {@code parentMatrix}
     * @param shader the {@link Shader} supplied as {@code shader}
     */
    public void render(GLTFModel model, Matrix4f parentMatrix, Shader shader) {
        worldMatrix.set(parentMatrix).mul(localMatrix);
        if (!isVisible) {
            return;
        }

        if (meshIndex >= 0) {
            if (textureOverride != 0 && uvOverride != null && colorTint != null) {
                model.renderMesh(meshIndex, worldMatrix, shader, textureOverride, uvOverride, colorTint);
            } else if (textureOverride != 0 && uvOverride != null) {
                model.renderMesh(meshIndex, worldMatrix, shader, textureOverride, uvOverride);
            } else if (colorTint != null) {
                model.renderMesh(meshIndex, worldMatrix, shader, colorTint);
            } else {
                model.renderMesh(meshIndex, worldMatrix, shader);
            }
        }

        for (GLTFNode child : children) {
            child.render(model, worldMatrix, shader);
        }
    }

    /**
     * Returns find.
     * @param nodeName the {@link String} supplied as {@code nodeName}
     * @return the {@link GLTFNode} representing the find result
     */
    public GLTFNode find(String nodeName) {

        if (name != null && name.equals(nodeName)) {
            return this;
        }

        for (GLTFNode child : children) {

            GLTFNode result = child.find(nodeName);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * Adds the child.
     * @param child the {@link GLTFNode} supplied as {@code child}
     */
    public void addChild(GLTFNode child) {
        children.add(child);
    }

    /**
     * Returns the name.
     * @return the {@link String} representing the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the mesh index.
     * @return {@code int}; the mesh index
     */
    public int getMeshIndex() {
        return meshIndex;
    }

    /**
     * Returns the translation.
     * @return the {@link Vector3f} representing the translation
     */
    public Vector3f getTranslation() {
        return translation;
    }

    /**
     * Sets the translation.
     * @param value the {@link Vector3f} supplied as {@code value}
     */
    public void setTranslation(Vector3f value) {
        translation.set(value);
        updateLocalMatrix();
    }

    /**
     * Returns the rotation.
     * @return the {@link Quaternionf} representing the rotation
     */
    public Quaternionf getRotation() {
        return rotation;
    }

    /**
     * Sets the rotation.
     * @param value the {@link Quaternionf} supplied as {@code value}
     */
    public void setRotation(Quaternionf value) {
        rotation.set(value);
        updateLocalMatrix();
    }

    /**
     * Returns the scale.
     * @return the {@link Vector3f} representing the scale
     */
    public Vector3f getScale() {
        return scale;
    }

    /**
     * Sets the scale.
     * @param value the {@link Vector3f} supplied as {@code value}
     */
    public void setScale(Vector3f value) {
        scale.set(value);
        updateLocalMatrix();
    }

    /**
     * Returns the local matrix.
     * @return the {@link Matrix4f} representing the local matrix
     */
    public Matrix4f getLocalMatrix() {
        return localMatrix;
    }

    /**
     * Returns the world matrix.
     * @return the {@link Matrix4f} representing the world matrix
     */
    public Matrix4f getWorldMatrix() {
        return worldMatrix;
    }

    /**
     * Returns the children.
     * @return the {@link List} representing the children
     */
    public List<GLTFNode> getChildren() {
        return children;
    }

    /**
     * Checks whether the visible condition is met.
     * @return {@code true} if visible; otherwise {@code false}
     */
    public boolean isVisible() {
        return isVisible;
    }

    /**
     * Sets the visible.
     * @param visible the {@code boolean} supplied as {@code visible}
     */
    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    /**
     * Sets the texture override.
     * @param textureId the {@code int} supplied as {@code textureId}
     * @param uvBounds the {@link Vector4f} supplied as {@code uvBounds}
     */
    public void setTextureOverride(int textureId, Vector4f uvBounds) {
        this.textureOverride = textureId;
        this.uvOverride = uvBounds == null ? null : new Vector4f(uvBounds);
    }

    /**
     * Clears the texture override.
     */
    public void clearTextureOverride() {
        textureOverride = 0;
        uvOverride = null;
    }

    /**
     * Returns the texture override.
     * @return {@code int}; the texture override
     */
    public int getTextureOverride() {
        return textureOverride;
    }

    /**
     * Returns the uv override.
     * @return the {@link Vector4f} representing the uv override
     */
    public Vector4f getUvOverride() {
        return uvOverride;
    }

    /** Sets the multiplicative material tint used while rendering this node. */
    public void setColorTint(Vector3f value) {
        colorTint = value == null ? null : new Vector3f(value);
    }

    /** Clears this node's material tint. */
    public void clearColorTint() {
        colorTint = null;
    }
}
