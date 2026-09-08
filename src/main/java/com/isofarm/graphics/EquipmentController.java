package com.isofarm.graphics;

import com.isofarm.graphics.gltf.GLTFModel;
import com.isofarm.graphics.gltf.GLTFNode;
import com.isofarm.item.Item;
import com.isofarm.item.Tool;
import com.isofarm.utils.Settings;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

/**
 * Manages player equipment attachment and mesh generation for item rendering.
 */
public class EquipmentController {
    public static final EquipmentController eq = new EquipmentController();
    private static final int THICKNESS_LAYERS = 24;
    private static final float LAYER_DEPTH = 0.0025f;

    private static final float ITEM_SCALE = 0.65f;
    private static final float ITEM_X = 0.10f;
    private static final float ITEM_Y = -0.55f;
    private static final float ITEM_Z = -0.25f;

    private GLTFNode currentActiveNode = null;
    private DynamicEquipmentMesh equipmentMesh;

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
        equipmentMesh = new DynamicEquipmentMesh();
        playerModel.addMesh(equipmentMesh);

        currentActiveNode = new GLTFNode("selected_item_root", meshIndex,
                new Vector3f(0.0f, -0.3f, 0.0f),
                new Quaternionf().rotateY((float) Math.PI),
                new Vector3f(0.5f));

        for (int i = 0; i < THICKNESS_LAYERS; i++) {
            float zOffset = (i - THICKNESS_LAYERS / 2.0f) * LAYER_DEPTH;
            GLTFNode layerNode = new GLTFNode("selected_item_layer_" + i, -1,
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

        Item item = Settings.getSelectedItem();
        if (item == null) {
            currentActiveNode.setVisible(false);
            return;
        }
        SpriteSheet sheet = ResourceManager.getItemSpriteSheet(item);
        if (sheet == null) {
            currentActiveNode.setVisible(false);
            return;
        }

        int textureId = sheet.getTextureId();
        int frame = Math.clamp(ResourceManager.getItemFrame(item), 0, sheet.getTotalFrames() - 1);
        Vector4f uvBounds = sheet.getUVBounds(frame);

        equipmentMesh.update(sheet, frame, uvBounds);
        currentActiveNode.setTextureOverride(textureId, uvBounds);
        for (GLTFNode layer : currentActiveNode.getChildren()) {
            layer.setTextureOverride(textureId, uvBounds);
        }

        float offset = 0.05f;
        currentActiveNode.setTranslation(new Vector3f(ITEM_X, ITEM_Y, ITEM_Z));
        if (item instanceof Tool) {
            currentActiveNode.setTranslation(new Vector3f(ITEM_X, ITEM_Y + offset, ITEM_Z - offset));
            currentActiveNode.setRotation(new Quaternionf()
                    .rotateX((float) Math.toRadians(30.0f))
                    .rotateY((float) Math.toRadians(90.0f)));
            currentActiveNode.setScale(new Vector3f(ITEM_SCALE + 0.2f));
        } else {
            currentActiveNode.setRotation(new Quaternionf().rotateX((float) Math.toRadians(90.0f)));
            currentActiveNode.setScale(new Vector3f(ITEM_SCALE));
        }

        currentActiveNode.setVisible(true);
    }

    /**
     * Creates a mesh for the specified sprite sheet and UV bounds.
     * @param sheet the sprite sheet to create the mesh for
     * @param uvBounds the UV bounds of the sprite sheet
     * @return the {@link Mesh} representing the created mesh
     */
    private static Mesh createItemMesh(SpriteSheet sheet, Vector4f uvBounds) {
        int width = sheet.getFrameWidth();
        int height = sheet.getFrameHeight();
        boolean[][] solid = readAlphaMask(sheet, uvBounds, width, height);
        int faceCount = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!solid[y][x]) continue;
                faceCount += 2;
                if (x == 0 || !solid[y][x - 1]) faceCount++;
                if (x == width - 1 || !solid[y][x + 1]) faceCount++;
                if (y == 0 || !solid[y - 1][x]) faceCount++;
                if (y == height - 1 || !solid[y + 1][x]) faceCount++;
            }
        }

        MeshBuilder builder = new MeshBuilder(Math.max(faceCount, 1));
        float depth = THICKNESS_LAYERS * LAYER_DEPTH * 0.5f;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!solid[y][x]) continue;

                float x0 = -0.5f + (float) x / width;
                float x1 = -0.5f + (float) (x + 1) / width;
                float y1 = 0.5f - (float) y / height;
                float y0 = 0.5f - (float) (y + 1) / height;
                float u0 = (float) x / width;
                float u1 = (float) (x + 1) / width;
                float v0 = (float) y / height;
                float v1 = (float) (y + 1) / height;
                float uc = (u0 + u1) * 0.5f;
                float vc = (v0 + v1) * 0.5f;

                builder.face(new float[]{x0, y0, depth, x1, y0, depth, x1, y1, depth, x0, y1, depth},
                        0, 0, 1, new float[]{u0, v1, u1, v1, u1, v0, u0, v0});
                builder.face(new float[]{x1, y0, -depth, x0, y0, -depth, x0, y1, -depth, x1, y1, -depth},
                        0, 0, -1, new float[]{u1, v1, u0, v1, u0, v0, u1, v0});

                if (x == 0 || !solid[y][x - 1]) {
                    builder.face(new float[]{x0, y0, -depth, x0, y0, depth, x0, y1, depth, x0, y1, -depth},
                            -1, 0, 0, new float[]{uc, v1, uc, v1, uc, v0, uc, v0});
                }
                if (x == width - 1 || !solid[y][x + 1]) {
                    builder.face(new float[]{x1, y0, depth, x1, y0, -depth, x1, y1, -depth, x1, y1, depth},
                            1, 0, 0, new float[]{uc, v1, uc, v1, uc, v0, uc, v0});
                }
                if (y == 0 || !solid[y - 1][x]) {
                    builder.face(new float[]{x0, y1, depth, x1, y1, depth, x1, y1, -depth, x0, y1, -depth},
                            0, 1, 0, new float[]{u0, vc, u1, vc, u1, vc, u0, vc});
                }
                if (y == height - 1 || !solid[y + 1][x]) {
                    builder.face(new float[]{x0, y0, -depth, x1, y0, -depth, x1, y0, depth, x0, y0, depth},
                            0, -1, 0, new float[]{u0, vc, u1, vc, u1, vc, u0, vc});
                }
            }
        }

        if (faceCount == 0) builder.degenerateFace();
        return builder.build();
    }

    /**
     * Reads the alpha mask from the sprite sheet and returns a boolean array representing the solid state of each pixel.
     * @param sheet 2D sprite sheet
     * @param uvBounds the UV bounds of the sprite sheet
     * @param width 2D sprite sheet width
     * @param height 2D sprite sheet height
     * @return a boolean array representing the solid state of each pixel in the sprite sheet
     */
    private static boolean[][] readAlphaMask(SpriteSheet sheet, Vector4f uvBounds, int width, int height) {
        int textureWidth = (int) sheet.getWidth();
        int textureHeight = (int) sheet.getHeight();
        ByteBuffer pixels = MemoryUtil.memAlloc(textureWidth * textureHeight * 4);
        int previousActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE);
        glActiveTexture(GL_TEXTURE0);
        int previousTexture = glGetInteger(GL_TEXTURE_BINDING_2D);

        try {
            glBindTexture(GL_TEXTURE_2D, sheet.getTextureId());
            glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            boolean[][] solid = new boolean[height][width];
            for (int y = 0; y < height; y++) {
                float localV = (y + 0.5f) / height;
                int textureY = Math.clamp((int) ((uvBounds.y + (uvBounds.w - uvBounds.y) * localV) * textureHeight),
                        0, textureHeight - 1);
                for (int x = 0; x < width; x++) {
                    float localU = (x + 0.5f) / width;
                    int textureX = Math.clamp((int) ((uvBounds.x + (uvBounds.z - uvBounds.x) * localU) * textureWidth),
                            0, textureWidth - 1);
                    solid[y][x] = (pixels.get((textureY * textureWidth + textureX) * 4 + 3) & 0xFF) >= 3;
                }
            }
            return solid;
        } finally {
            glBindTexture(GL_TEXTURE_2D, previousTexture);
            glActiveTexture(previousActiveTexture);
            MemoryUtil.memFree(pixels);
        }
    }

    /** {@inheritDoc} */
    private static final class DynamicEquipmentMesh extends GLTFModel.GLTFMesh {
        private Mesh mesh = Mesh.createCenteredQuad();
        private int textureId = -1;
        private int frame = -1;

        /** {@inheritDoc} */
        private DynamicEquipmentMesh() {
            super(0, 0, 0, 0, 0);
        }

        /** {@inheritDoc} */
        private void update(SpriteSheet sheet, int selectedFrame, Vector4f uvBounds) {
            if (textureId == sheet.getTextureId() && frame == selectedFrame) return;
            Mesh replacement = createItemMesh(sheet, uvBounds);
            mesh.dispose();
            mesh = replacement;
            textureId = sheet.getTextureId();
            frame = selectedFrame;
        }

        /** {@inheritDoc} */
        @Override
        public void render(Shader shader, int renderTextureId, Vector4f uvBounds) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, renderTextureId);
            shader.setUniform("uTexture", 0);
            shader.setUniform("uUseTexture", true);
            shader.setUniform("uUVBounds", uvBounds);
            mesh.render();
        }

        @Override
        public void dispose() {
            mesh.dispose();
        }
    }

    private static final class MeshBuilder {
        private final float[] positions;
        private final float[] normals;
        private final float[] uvs;
        private final int[] indices;
        private int vertex;
        private int face;

        private MeshBuilder(int faceCount) {
            positions = new float[faceCount * 12];
            normals = new float[faceCount * 12];
            uvs = new float[faceCount * 8];
            indices = new int[faceCount * 6];
        }

        /**
         * Adds a face with the specified vertices, normals, and texture coordinates.
         * @param vertices the vertices of the face
         * @param nx the normal vector's x component
         * @param ny the normal vector's y component
         * @param nz the normal vector's z component
         * @param textureCoordinates the texture coordinates of the face
         */
        private void face(float[] vertices, float nx, float ny, float nz, float[] textureCoordinates) {
            System.arraycopy(vertices, 0, positions, face * 12, 12);
            System.arraycopy(textureCoordinates, 0, uvs, face * 8, 8);
            for (int i = 0; i < 4; i++) {
                int normal = face * 12 + i * 3;
                normals[normal] = nx;
                normals[normal + 1] = ny;
                normals[normal + 2] = nz;
            }
            int index = face * 6;
            indices[index] = vertex;
            indices[index + 1] = vertex + 1;
            indices[index + 2] = vertex + 2;
            indices[index + 3] = vertex + 2;
            indices[index + 4] = vertex + 3;
            indices[index + 5] = vertex;
            vertex += 4;
            face++;
        }

        /**
         * Adds a degenerate face with no vertices.
         */
        private void degenerateFace() {
            face(new float[12], 0, 0, 1, new float[8]);
        }

        /**
         * Builds the mesh from the accumulated vertices and indices.
         * @return the {@link Mesh} representing the built mesh
         */
        private Mesh build() {
            return new Mesh(positions, normals, uvs, indices);
        }
    }
}
