package org.kbeng.engine.graphics.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;

/**
 * GLTFLoader provides gltfloader capabilities within the graphics subsystem.
 * It contributes to OpenGL resource ownership, render-state setup, or frame-pipeline execution.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@SuppressWarnings("all")
public final class GLTFLoader {

    /**
     * Creates a new {@code GLTFLoader} instance.
     */
    private GLTFLoader() {}

    /**
     * Loads load.
     * @param path the {@link String} supplied as {@code path}
     * @return the {@link GLTFModel} representing the load result
     */
    public static GLTFModel load(String path) {
        try {
            String json = loadText(path);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            return loadModel(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load GLTF: " + path, e);
        }
    }

    /**
     * Loads the text.
     * @param path the {@link String} supplied as {@code path}
     * @return the {@link String} representing the load text result
     * @throws IOException if the operation cannot be completed
     */
    private static String loadText(String path) throws IOException {
        Path file = Path.of(path);

        if (Files.exists(file)) {
            return Files.readString(file);
        }

        var resource = GLTFLoader.class.getClassLoader().getResourceAsStream(path);
        if (resource == null) {
            throw new IOException("GLTF resource not found: " + path);
        }

        return new String(resource.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Loads the model.
     * @param root the {@link JsonObject} supplied as {@code root}
     * @return the {@link GLTFModel} representing the load model result
     */
    private static GLTFModel loadModel(JsonObject root) {
        GLTFModel model = new GLTFModel();
        JsonArray buffersJson = root.getAsJsonArray("buffers");
        JsonArray bufferViewsJson = root.getAsJsonArray("bufferViews");
        JsonArray accessorsJson = root.getAsJsonArray("accessors");
        JsonArray meshesJson = root.getAsJsonArray("meshes");
        JsonArray nodesJson = root.getAsJsonArray("nodes");
        List<ByteBuffer> buffers = loadBuffers(buffersJson);
        List<Integer> textureIds = new ArrayList<>();
        if (root.has("images")) {
            JsonArray images = root.getAsJsonArray("images");
            for (int i = 0; i < images.size(); i++) {
                textureIds.add(0);
            }
        }

        for (int meshIndex = 0; meshIndex < meshesJson.size(); meshIndex++) {
            JsonObject meshJson = meshesJson.get(meshIndex).getAsJsonObject();
            JsonArray primitives = meshJson.getAsJsonArray("primitives");

            for (int primitiveIndex = 0; primitiveIndex < primitives.size(); primitiveIndex++) {
                JsonObject primitive = primitives.get(primitiveIndex).getAsJsonObject();
                JsonObject attributes = primitive.getAsJsonObject("attributes");
                int positionAccessor = attributes.get("POSITION").getAsInt();
                int normalAccessor = attributes.get("NORMAL").getAsInt();
                int uvAccessor = attributes.get("TEXCOORD_0").getAsInt();
                int indexAccessor = primitive.get("indices").getAsInt();
                float[] positions = readFloatAccessor(positionAccessor, accessorsJson, bufferViewsJson, buffers);
                float[] normals = readFloatAccessor(normalAccessor, accessorsJson, bufferViewsJson, buffers);
                float[] uvs = readFloatAccessor(uvAccessor, accessorsJson, bufferViewsJson, buffers);
                int[] indices = readIndexAccessor(indexAccessor, accessorsJson, bufferViewsJson, buffers);
                int textureId = getTextureIdForPrimitive(root, primitive, textureIds);
                GLTFModel.GLTFMesh glMesh = createMesh(positions, normals, uvs, indices, textureId);
                model.addMesh(glMesh);
            }
        }

        List<GLTFNode> nodes = new ArrayList<>();
        for (int nodeIndex = 0; nodeIndex < nodesJson.size(); nodeIndex++) {
            JsonObject nodeJson = nodesJson.get(nodeIndex).getAsJsonObject();
            String name = null;
            if (nodeJson.has("name")) {
                name = nodeJson.get("name").getAsString();
            }
            int meshIndex = -1;
            if (nodeJson.has("mesh")) {
                meshIndex = nodeJson.get("mesh").getAsInt();
            }

            Vector3f translation = readVector3(nodeJson, "translation", new Vector3f());
            Vector3f scale = readVector3(nodeJson, "scale", new Vector3f(1.0f));
            Quaternionf rotation = readQuaternion(nodeJson);
            GLTFNode node = new GLTFNode(name, meshIndex, translation, rotation, scale);

            nodes.add(node);
            model.addNode(node);
        }

        boolean[] hasParent = new boolean[nodes.size()];
        for (int nodeIndex = 0; nodeIndex < nodesJson.size(); nodeIndex++) {
            JsonObject nodeJson = nodesJson.get(nodeIndex).getAsJsonObject();
            if (!nodeJson.has("children")) {
                continue;
            }

            JsonArray children = nodeJson.getAsJsonArray("children");
            GLTFNode parent = nodes.get(nodeIndex);
            for (int i = 0; i < children.size(); i++) {
                int childIndex = children.get(i).getAsInt();
                GLTFNode child = nodes.get(childIndex);
                parent.addChild(child);
                hasParent[childIndex] = true;
            }
        }

        JsonObject scene = root.getAsJsonArray("scenes").get(root.get("scene").getAsInt()).getAsJsonObject();
        JsonArray sceneNodes = scene.getAsJsonArray("nodes");
        for (int i = 0; i < sceneNodes.size(); i++) {
            int nodeIndex = sceneNodes.get(i).getAsInt();
            model.addRootNode(nodes.get(nodeIndex));
        }

        return model;
    }

    /**
     * Loads the buffers.
     * @param buffersJson the {@link JsonArray} supplied as {@code buffersJson}
     * @return the {@link List} representing the load buffers result
     */
    private static List<ByteBuffer> loadBuffers(JsonArray buffersJson) {
        List<ByteBuffer> buffers = new ArrayList<>();
        for (int i = 0; i < buffersJson.size(); i++) {
            JsonObject buffer = buffersJson.get(i).getAsJsonObject();
            String uri = buffer.get("uri").getAsString();
            ByteBuffer data;

            if (uri.startsWith("data:")) {
                int comma = uri.indexOf(',');
                String base64 = uri.substring(comma + 1);
                byte[] decoded = Base64.getDecoder().decode(base64);
                data = ByteBuffer.allocateDirect(decoded.length).order(ByteOrder.LITTLE_ENDIAN);
                data.put(decoded);
                data.flip();
            } else {
                throw new IllegalArgumentException("External GLTF buffers are not supported yet: " + uri);
            }
            buffers.add(data);
        }
        return buffers;
    }

    /**
     * Loads float accessor and converts it into the runtime representation.
     * @param accessorIndex the {@code int} supplied as {@code accessorIndex}
     * @param accessors the {@link JsonArray} supplied as {@code accessors}
     * @param bufferViews the {@link JsonArray} supplied as {@code bufferViews}
     * @param buffers the {@link List} supplied as {@code buffers}
     * @return an array of {@code float} values; the read float accessor result
     */
    private static float[] readFloatAccessor(int accessorIndex, JsonArray accessors, JsonArray bufferViews,
                                             List<ByteBuffer> buffers) {
        JsonObject accessor = accessors.get(accessorIndex).getAsJsonObject();
        int bufferViewIndex = accessor.get("bufferView").getAsInt();

        JsonObject bufferView = bufferViews.get(bufferViewIndex).getAsJsonObject();
        int bufferIndex = bufferView.get("buffer").getAsInt();

        ByteBuffer buffer = buffers.get(bufferIndex).duplicate().order(ByteOrder.LITTLE_ENDIAN);
        int accessorOffset = accessor.has("byteOffset") ? accessor.get("byteOffset").getAsInt() : 0;
        int viewOffset = bufferView.has("byteOffset") ? bufferView.get("byteOffset").getAsInt() : 0;

        String type = accessor.get("type").getAsString();
        int components = componentCount(type);
        int count = accessor.get("count").getAsInt();

        int componentSize = 4;
        int elementSize = components * componentSize;

        int stride = bufferView.has("byteStride") ? bufferView.get("byteStride").getAsInt() : elementSize;
        float[] result = new float[count * components];
        int baseOffset = viewOffset + accessorOffset;
        for (int i = 0; i < count; i++) {
            int elementOffset = baseOffset + i * stride;

            for (int component = 0; component < components; component++) {
                result[i * components + component] = buffer.getFloat(elementOffset + component * 4);
            }
        }
        return result;
    }

    /**
     * Loads index accessor and converts it into the runtime representation.
     * @param accessorIndex the {@code int} supplied as {@code accessorIndex}
     * @param accessors the {@link JsonArray} supplied as {@code accessors}
     * @param bufferViews the {@link JsonArray} supplied as {@code bufferViews}
     * @param buffers the {@link List} supplied as {@code buffers}
     * @return an array of {@code int} values; the read index accessor result
     */
    private static int[] readIndexAccessor(int accessorIndex, JsonArray accessors,
                                           JsonArray bufferViews, List<ByteBuffer> buffers) {
        JsonObject accessor = accessors.get(accessorIndex).getAsJsonObject();
        int bufferViewIndex = accessor.get("bufferView").getAsInt();
        JsonObject bufferView = bufferViews.get(bufferViewIndex).getAsJsonObject();
        int bufferIndex = bufferView.get("buffer").getAsInt();
        ByteBuffer buffer = buffers.get(bufferIndex).duplicate().order(ByteOrder.LITTLE_ENDIAN);
        int accessorOffset = accessor.has("byteOffset") ? accessor.get("byteOffset").getAsInt() : 0;
        int viewOffset = bufferView.has("byteOffset") ? bufferView.get("byteOffset").getAsInt() : 0;
        int count = accessor.get("count").getAsInt();
        int componentType = accessor.get("componentType").getAsInt();
        int componentSize = componentSize(componentType);
        int stride = bufferView.has("byteStride") ? bufferView.get("byteStride").getAsInt() : componentSize;
        int baseOffset = viewOffset + accessorOffset;
        int[] result = new int[count];

        for (int i = 0; i < count; i++) {
            int offset = baseOffset + i * stride;
            result[i] = switch (componentType) {
                case 5121 -> buffer.get(offset) & 0xFF;
                case 5123 -> buffer.getShort(offset) & 0xFFFF;
                case 5125 -> buffer.getInt(offset);
                default -> throw new IllegalArgumentException("Unsupported index component type: " + componentType);
            };
        }

        return result;
    }

    /**
     * Returns the number or extent represented by component count.
     * @param type the {@link String} supplied as {@code type}
     * @return {@code int}; the component count result
     */
    private static int componentCount(String type) {
        return switch (type) {
            case "SCALAR" -> 1;
            case "VEC2" -> 2;
            case "VEC3" -> 3;
            case "VEC4" -> 4;
            default -> throw new IllegalArgumentException("Unsupported accessor type: " + type);
        };
    }

    /**
     * Returns the number or extent represented by component size.
     * @param componentType the {@code int} supplied as {@code componentType}
     * @return {@code int}; the component size result
     */
    private static int componentSize(int componentType) {
        return switch (componentType) {
            case 5121 -> 1;
            case 5123 -> 2;
            case 5125, 5126 -> 4;

            default -> throw new IllegalArgumentException("Unsupported component type: " + componentType);
        };
    }

    /**
     * Creates and returns the mesh.
     * @param positions an array of {@code float} values supplied as {@code positions}
     * @param normals an array of {@code float} values supplied as {@code normals}
     * @param uvs an array of {@code float} values supplied as {@code uvs}
     * @param indices an array of {@code int} values supplied as {@code indices}
     * @param textureId the {@code int} supplied as {@code textureId}
     * @return the {@link GLTFModel.GLTFMesh} representing the created mesh
     */
    private static GLTFModel.GLTFMesh createMesh(float[] positions, float[] normals,
                                                 float[] uvs, int[] indices, int textureId) {
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        int ebo = glGenBuffers();
        glBindVertexArray(vao);

        int vertexCount = positions.length / 3;
        float[] vertexData = new float[vertexCount * 8];
        for (int i = 0; i < vertexCount; i++) {
            int vertexOffset = i * 8;
            int posOffset = i * 3;
            int uvOffset = i * 2;
            vertexData[vertexOffset] = positions[posOffset];
            vertexData[vertexOffset + 1] = positions[posOffset + 1];
            vertexData[vertexOffset + 2] = positions[posOffset + 2];
            vertexData[vertexOffset + 3] = normals[posOffset];
            vertexData[vertexOffset + 4] = normals[posOffset + 1];
            vertexData[vertexOffset + 5] = normals[posOffset + 2];
            vertexData[vertexOffset + 6] = uvs[uvOffset];
            vertexData[vertexOffset + 7] = uvs[uvOffset + 1];
        }

        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer vertexBuffer = org.lwjgl.BufferUtils.createFloatBuffer(vertexData.length);
        vertexBuffer.put(vertexData).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        int stride = 8 * Float.BYTES;
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer indexBuffer = org.lwjgl.BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return new GLTFModel.GLTFMesh(vao, vbo, ebo, indices.length, textureId);
    }

    /**
     * Loads the texture.
     * @param root the {@link JsonObject} supplied as {@code root}
     * @param imageIndex the {@code int} supplied as {@code imageIndex}
     * @return {@code int}; the load texture result
     */
    private static int loadTexture(JsonObject root, int imageIndex) {
        JsonArray images = root.getAsJsonArray("images");

        if (images == null || imageIndex < 0 || imageIndex >= images.size()) {
            return 0;
        }

        JsonObject image = images.get(imageIndex).getAsJsonObject();

        if (!image.has("uri")) {
            throw new IllegalArgumentException("GLTF image without URI is not supported yet: " + imageIndex);
        }

        String uri = image.get("uri").getAsString();

        if (!uri.startsWith("data:image")) {
            throw new IllegalArgumentException("Only embedded GLTF images are supported");
        }

        int comma = uri.indexOf(',');
        String base64 = uri.substring(comma + 1);

        byte[] imageData = Base64.getDecoder().decode(base64);

        ByteBuffer imageBuffer = ByteBuffer.allocateDirect(imageData.length);
        imageBuffer.put(imageData).flip();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            var width = stack.mallocInt(1);
            var height = stack.mallocInt(1);
            var channels = stack.mallocInt(1);

            stbi_set_flip_vertically_on_load(false);
            ByteBuffer pixels = stbi_load_from_memory(imageBuffer, width, height, channels, 4);
            if (pixels == null) {
                throw new RuntimeException("Failed to decode GLTF texture: " + stbi_failure_reason());
            }

            int texture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texture);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width.get(0), height.get(0), 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, pixels);

            glGenerateMipmap(GL_TEXTURE_2D);
            stbi_image_free(pixels);
            glBindTexture(GL_TEXTURE_2D, 0);
            return texture;
        }
    }

    /**
     * Returns the texture id for primitive.
     * @param root the {@link JsonObject} supplied as {@code root}
     * @param primitive the {@link JsonObject} supplied as {@code primitive}
     * @param textureIds the {@link List} supplied as {@code textureIds}
     * @return {@code int}; the texture id for primitive
     */
    private static int getTextureIdForPrimitive(JsonObject root, JsonObject primitive, List<Integer> textureIds) {
        if (!primitive.has("material")) {
            return 0;
        }

        JsonArray materials = root.getAsJsonArray("materials");
        if (materials == null) {
            return 0;
        }

        int materialIndex = primitive.get("material").getAsInt();

        if (materialIndex < 0 || materialIndex >= materials.size()) {
            return 0;
        }

        JsonObject material = materials.get(materialIndex).getAsJsonObject();
        if (!material.has("pbrMetallicRoughness")) {
            return 0;
        }

        JsonObject pbr = material.getAsJsonObject("pbrMetallicRoughness");
        if (!pbr.has("baseColorTexture")) {
            return 0;
        }

        JsonObject baseColorTexture = pbr.getAsJsonObject("baseColorTexture");
        int textureIndex = baseColorTexture.get("index").getAsInt();
        JsonArray textures = root.getAsJsonArray("textures");
        if (textures == null || textureIndex < 0 || textureIndex >= textures.size()) {
            return 0;
        }

        JsonObject texture = textures.get(textureIndex).getAsJsonObject();
        int imageIndex = texture.get("source").getAsInt();

        while (textureIds.size() <= imageIndex) {
            textureIds.add(0);
        }

        int textureId = textureIds.get(imageIndex);
        if (textureId == 0) {
            textureId = loadTexture(root, imageIndex);
            textureIds.set(imageIndex, textureId);
        }

        return textureId;
    }

    /**
     * Loads vector3 and converts it into the runtime representation.
     * @param object the {@link JsonObject} supplied as {@code object}
     * @param property the {@link String} supplied as {@code property}
     * @param defaultValue the {@link Vector3f} supplied as {@code defaultValue}
     * @return the {@link Vector3f} representing the read vector3 result
     */
    private static Vector3f readVector3(JsonObject object, String property, Vector3f defaultValue) {
        if (!object.has(property)) {
            return new Vector3f(defaultValue);
        }
        JsonArray array = object.getAsJsonArray(property);
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    /**
     * Loads quaternion and converts it into the runtime representation.
     * @param object the {@link JsonObject} supplied as {@code object}
     * @return the {@link Quaternionf} representing the read quaternion result
     */
    private static Quaternionf readQuaternion(JsonObject object) {
        if (!object.has("rotation")) {
            return new Quaternionf();
        }

        JsonArray array = object.getAsJsonArray("rotation");
        return new Quaternionf(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat(), array.get(3).getAsFloat());
    }
}
