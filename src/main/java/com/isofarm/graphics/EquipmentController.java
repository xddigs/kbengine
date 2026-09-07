package com.isofarm.graphics;

import com.isofarm.data.Tier;
import com.isofarm.data.ToolType;
import com.isofarm.graphics.gltf.GLTFModel;
import com.isofarm.graphics.gltf.GLTFNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the state and operations required by equipment controller within the game runtime.
 */
public class EquipmentController {
    private final Map<String, GLTFNode> equipmentNodes = new HashMap<>();
    private GLTFNode currentActiveNode = null;

    /**
     * Initializes the component.
     * @param playerModel the {@link GLTFModel} supplied as {@code playerModel}
     */
    public void init(GLTFModel playerModel) {
        String[] materials = new String[Tier.values().length];
        String[] types = new String[ToolType.values().length];

        for (int i = 0; i < materials.length; i++) {
            materials[i] = Tier.values()[i].name().toLowerCase();
        }

        for (int i = 0; i < types.length; i++) {
            types[i] = ToolType.values()[i].name().toLowerCase();
        }

        for (String type : types) {
            for (String material : materials) {
                String nodeName = material + "_" + type;
                GLTFNode node = playerModel.findNode(nodeName);

                if (node != null) {
                    node.setVisible(false);
                    equipmentNodes.put(nodeName, node);
                }
            }
        }
        currentActiveNode = null;
    }

    /**
     * Applies equip and updates the affected character or item state.
     * @param material the {@link String} supplied as {@code material}
     * @param type the {@link String} supplied as {@code type}
     */
    public void equip(String material, String type) {
        if (currentActiveNode != null) {
            currentActiveNode.setVisible(false);
            currentActiveNode = null;
        }

        if (material == null || type == null) {
            return;
        }

        String nodeName = material.toLowerCase() + "_" + type.toLowerCase();
        GLTFNode node = equipmentNodes.get(nodeName);
        if (node == null) {
            return;
        }

        node.setVisible(true);
        currentActiveNode = node;
    }
}
