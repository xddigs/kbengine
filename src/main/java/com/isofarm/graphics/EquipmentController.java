package com.isofarm.graphics;

import com.isofarm.graphics.gltf.GLTFModel;
import com.isofarm.graphics.gltf.GLTFNode;
import com.isofarm.item.Item;
import com.isofarm.item.Tool;
import com.isofarm.utils.Settings;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Manages player equipment attachment and mesh generation for item rendering.
 */
public class EquipmentController {
    public static final EquipmentController eq = new EquipmentController();
    private static final int THICKNESS_LAYERS = 24;
    private static final float LAYER_DEPTH = 0.0025f;

    private static final float ITEM_SCALE = 0.6f;
    private static final float ITEM_X = 0.10f;
    private static final float ITEM_Y = -0.55f;
    private static final float ITEM_Z = -0.25f;

    private GLTFNode currentActiveNode = null;

    /** Returns the singleton instance of {@code EquipmentController}. */
    private EquipmentController() {}

    /**
     * Initializes the equipment controller and attaches the multi-layered item node hierarchy.
     * @param playerModel the player's GLTF model reference
     */
    public void init(GLTFModel playerModel) {
        if (playerModel == null) return;

        GLTFNode arm = playerModel.findNode("Right Arm");
        if (arm == null) {
            arm = playerModel.getNodes().stream()
                    .filter(n -> n.getName() != null && n.getName().toLowerCase().contains("right")
                            && n.getName().toLowerCase().contains("arm"))
                    .findFirst()
                    .orElse(null);
        }

        if (arm == null) {
            throw new NullPointerException("Arm node not found");
        }

        int meshIndex = playerModel.getMeshes().size();
        playerModel.addMesh(createSingleQuadMesh());

        currentActiveNode = new GLTFNode("selected_item_root", -1,
                new Vector3f(0.0f, -0.3f, 0.0f),
                new Quaternionf().rotateY((float) Math.PI),
                new Vector3f(0.5f));

        for (int i = 0; i < THICKNESS_LAYERS; i++) {
            float zOffset = (i - THICKNESS_LAYERS / 2.0f) * LAYER_DEPTH;
            GLTFNode layerNode = new GLTFNode("selected_item_layer_" + i, meshIndex,
                    new Vector3f(0.0f, 0.0f, zOffset),
                    new Quaternionf(),
                    new Vector3f(1.0f));
            currentActiveNode.addChild(layerNode);
        }

        currentActiveNode.setVisible(false);
        arm.addChild(currentActiveNode);
    }

    /**
     * Equips and updates the active item transform and textures across all layer nodes.
     */
    public void equip() {
        if (currentActiveNode == null) return;

        Item item = Settings.selectedItem;
        SpriteSheet sheet = ResourceManager.getItemSpriteSheet(item);
        if (sheet == null) {
            currentActiveNode.setVisible(false);
            return;
        }

        int textureId = sheet.getTextureId();
        org.joml.Vector4f uvBounds = sheet.getUVBounds(ResourceManager.getItemFrame(item));

        for (GLTFNode layer : currentActiveNode.getChildren()) {
            layer.setTextureOverride(textureId, uvBounds);
        }

        float offset = -0.20f;
        currentActiveNode.setTranslation(new Vector3f(ITEM_X, ITEM_Y, ITEM_Z));
        if (item instanceof Tool) {
            currentActiveNode.setTranslation(new Vector3f(ITEM_X, ITEM_Y + offset, ITEM_Z));
            currentActiveNode.setRotation(new Quaternionf()
                    .rotateX((float) Math.toRadians(30.0f))
                    .rotateZ((float) Math.toRadians(30.0f)));
        } else {
            currentActiveNode.setRotation(new Quaternionf().rotateX((float) Math.toRadians(90.0f)));
        }

        currentActiveNode.setScale(new Vector3f(ITEM_SCALE));
        currentActiveNode.setVisible(true);
    }

    /**
     * Creates a standard centered 2D quad mesh.
     * @return the {@link GLTFModel.GLTFMesh} representing the quad
     */
    public static GLTFModel.GLTFMesh createSingleQuadMesh() {
        float[] v = {
                -0.5f, -0.5f, 0.0f,   0.0f, 0.0f, 1.0f,   0.0f, 1.0f,
                0.5f, -0.5f, 0.0f,   0.0f, 0.0f, 1.0f,   1.0f, 1.0f,
                0.5f,  0.5f, 0.0f,   0.0f, 0.0f, 1.0f,   1.0f, 0.0f,
                -0.5f,  0.5f, 0.0f,   0.0f, 0.0f, 1.0f,   0.0f, 0.0f,

                0.5f, -0.5f, 0.0f,   0.0f, 0.0f, -1.0f,  1.0f, 1.0f,
                -0.5f, -0.5f, 0.0f,   0.0f, 0.0f, -1.0f,  0.0f, 1.0f,
                -0.5f,  0.5f, 0.0f,   0.0f, 0.0f, -1.0f,  0.0f, 0.0f,
                0.5f,  0.5f, 0.0f,   0.0f, 0.0f, -1.0f,  1.0f, 0.0f
        };

        int[] i = {
                0, 1, 2, 2, 3, 0,
                4, 5, 6, 6, 7, 4
        };

        int vao = glGenVertexArrays();
        FloatBuffer b = MemoryUtil.memAllocFloat(v.length);
        IntBuffer ib = MemoryUtil.memAllocInt(i.length);
        try {
            b.put(v).flip();
            ib.put(i).flip();
            int vb = glGenBuffers();
            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vb);
            glBufferData(GL_ARRAY_BUFFER, b, GL_STATIC_DRAW);

            int stride = 8 * Float.BYTES;
            glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
            glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);

            glEnableVertexAttribArray(0);
            glEnableVertexAttribArray(1);
            glEnableVertexAttribArray(2);

            int eb = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eb);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);

            glBindVertexArray(0);
            return new GLTFModel.GLTFMesh(vao, vb, eb, i.length, 0);
        } finally {
            MemoryUtil.memFree(b);
            MemoryUtil.memFree(ib);
        }
    }
}