package com.isofarm.graphics;

import com.isofarm.graphics.gltf.GLTFModel;
import com.isofarm.graphics.gltf.GLTFNode;
import com.isofarm.item.Item;
import com.isofarm.item.Tool;
import com.isofarm.utils.Settings;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Encapsulates the state and operations required by equipment controller within the game runtime.
 */
public class EquipmentController {
    private GLTFNode currentActiveNode = null;

    /**
     * Initializes the component.
     * @param playerModel the {@link GLTFModel} supplied as {@code playerModel}
     */
    public void init(GLTFModel playerModel) {
        currentActiveNode = null;
        if (playerModel == null) return;
        GLTFNode arm = null;
        for (GLTFNode node : playerModel.getNodes()) {
            if ("Right Arm".equals(node.getName()) && !node.getChildren().isEmpty()) {
                arm = node;
                break;
            }
        }
        if (arm == null) return;
        int meshIndex = playerModel.getMeshes().size();
        playerModel.addMesh(createItemMesh());
        currentActiveNode = new GLTFNode("selected_item", meshIndex,
                new Vector3f(.125f, -.4375f, -.0625f),
                new Quaternionf(-.27059805f, .6532815f, -.27059805f, .6532815f),
                new Vector3f(.72f));
        currentActiveNode.setVisible(false);
        arm.addChild(currentActiveNode);
    }

    /**
     * Applies equip and updates the affected character or item state.
     */
    public void equip() {
        if (currentActiveNode == null) return;
        Item item = Settings.selectedItem;
        if (!(item instanceof Tool)) {
            currentActiveNode.setVisible(false);
            return;
        }
        SpriteSheet sheet = ResourceManager.getItemSpriteSheet(item);
        if (sheet == null) {
            currentActiveNode.setVisible(false);
            return;
        }
        currentActiveNode.setTextureOverride(sheet.getTextureId(),
                sheet.getUVBounds(ResourceManager.getItemFrame(item)));
        currentActiveNode.setVisible(true);
    }

    private GLTFModel.GLTFMesh createItemMesh() {
        float[] vertices = {
                -.5f,-.5f,0, 0,0,1, 0,1,
                 .5f,-.5f,0, 0,0,1, 1,1,
                 .5f, .5f,0, 0,0,1, 1,0,
                -.5f, .5f,0, 0,0,1, 0,0
        };
        int[] i = {0,1,2, 2,3,0, 2,1,0, 0,3,2};
        int vao = glGenVertexArrays();
        FloatBuffer vb = MemoryUtil.memAllocFloat(vertices.length);
        IntBuffer ib = MemoryUtil.memAllocInt(i.length);
        try {
            vb.put(vertices).flip(); ib.put(i).flip();
            int vbo = glGenBuffers();
            glBindVertexArray(vao); glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, vb, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 8 * Float.BYTES, 0);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 8 * Float.BYTES, 3L * Float.BYTES);
            glVertexAttribPointer(2, 2, GL_FLOAT, false, 8 * Float.BYTES, 6L * Float.BYTES);
            glEnableVertexAttribArray(0); glEnableVertexAttribArray(1); glEnableVertexAttribArray(2);
            int eb = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eb);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);
            glBindVertexArray(0);
            return new GLTFModel.GLTFMesh(vao, vbo, eb, i.length, 0);
        } finally { MemoryUtil.memFree(vb); MemoryUtil.memFree(ib); }
    }
}
