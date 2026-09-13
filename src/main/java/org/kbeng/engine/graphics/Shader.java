package org.kbeng.engine.graphics;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;

/**
 * Shader provides shader capabilities within the graphics subsystem.
 * It contributes to OpenGL resource ownership, render-state setup, or frame-pipeline execution.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public class Shader {
    private static final Logger log = LoggerFactory.getLogger(Shader.class);
    private final int programId;
    private final Map<String, Integer> uniformLocations = new HashMap<>();

    /**
     * Creates a new {@code Shader} instance.
     * @param vertexPath the {@link String} supplied as {@code vertexPath}
     * @param fragmentPath the {@link String} supplied as {@code fragmentPath}
     */
    public Shader(String vertexPath, String fragmentPath) {
        String vertexSource = load(vertexPath);
        String fragmentSource = load(fragmentPath);

        int vertexId = compile(GL_VERTEX_SHADER, vertexSource, vertexPath);
        int fragmentId = compile(GL_FRAGMENT_SHADER, fragmentSource, fragmentPath);

        this.programId = glCreateProgram();
        glAttachShader(programId, vertexId);
        glAttachShader(programId, fragmentId);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            String logInfo = glGetProgramInfoLog(programId);
            throw new RuntimeException("Error linking shader program: " + logInfo);
        }

        glDetachShader(programId, vertexId);
        glDetachShader(programId, fragmentId);
        glDeleteShader(vertexId);
        glDeleteShader(fragmentId);
        log.info("Shader program successfully compiled and linked [ID: {}]", programId);
    }

    /**
     * Loads load.
     * @param resourcePath the {@link String} supplied as {@code resourcePath}
     * @return the {@link String} representing the load result
     */
    private String load(String resourcePath) {
        try (var inputStream = Shader.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Resource not" +
                        " found on classpath: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader " +
                    "resource [" + resourcePath + "]", e);
        }
    }

    /**
     * Executes compile as part of the application lifecycle.
     * @param type the {@code int} supplied as {@code type}
     * @param source the {@link String} supplied as {@code source}
     * @param path the {@link String} supplied as {@code path}
     * @return {@code int}; the compile result
     */
    private int compile(int type, String source, String path) {
        int shaderId = glCreateShader(type);
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            String logInfo = glGetShaderInfoLog(shaderId);
            String shaderType = (type == GL_VERTEX_SHADER) ? "Vertex" : "Fragment";
            throw new RuntimeException("Error compiling " +
                    shaderType + " shader [" + path + "]: " + logInfo);
        }

        return shaderId;
    }

    /**
     * Binds this object to the active runtime context.
     */
    public void bind() {
        glUseProgram(programId);
    }

    /**
     * Unbinds this object from the active runtime context.
     */
    public void unbind() {
        glUseProgram(0);
    }

    /**
     * Releases the resources associated with this object.
     */
    public void dispose() {
        glUnbind();
        glDeleteProgram(programId);
        log.trace("Shader program deleted [ID: {}]", programId);
    }

    /**
     * Unbinds the active OpenGL object from its current target.
     */
    private void glUnbind() {
        glUseProgram(0);
    }

    /**
     * Sets the uniform.
     * @param name the {@link String} supplied as {@code name}
     * @param value the {@code int} supplied as {@code value}
     */
    public void setUniform(String name, int value) {
        int location = uniformLocation(name);
        glUniform1i(location, value);
    }

    /**
     * Sets the uniform.
     * @param name the {@link String} supplied as {@code name}
     * @param value the {@code float} supplied as {@code value}
     */
    public void setUniform(String name, float value) {
        int location = uniformLocation(name);
        glUniform1f(location, value);
    }

    /**
     * Sets the uniform.
     * @param name the {@link String} supplied as {@code name}
     * @param value the {@link Vector4f} supplied as {@code value}
     */
    public void setUniform(String name, Vector4f value) {
        int location = uniformLocation(name);
        glUniform4f(location, value.x, value.y, value.z, value.w);
    }

    /**
     * Sets the uniform.
     * @param name the {@link String} supplied as {@code name}
     * @param value the {@link Vector3f} supplied as {@code value}
     */
    public void setUniform(String name, Vector3f value) {
        int location = uniformLocation(name);
        glUniform3f(location, value.x, value.y, value.z);
    }

    /**
     * Sets the uniform.
     * @param name the {@link String} supplied as {@code name}
     * @param value the {@link Vector2f} supplied as {@code value}
     */
    public void setUniform(String name, Vector2f value) {
        int location = uniformLocation(name);
        glUniform2f(location, value.x, value.y);
    }

    /**
     * Sets the uniform.
     * @param name the {@link String} supplied as {@code name}
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     */
    public void setUniform(String name, float x, float y) {
        int location = uniformLocation(name);
        glUniform2f(location, x, y);
    }

    /**
     * Sets the uniform.
     * @param name the {@link String} supplied as {@code name}
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param z the {@code float} supplied as {@code z}
     */
    public void setUniform(String name, float x, float y, float z) {
        int location = uniformLocation(name);
        glUniform3f(location, x, y, z);
    }

    /**
     * Sets the uniform.
     * @param name the {@link String} supplied as {@code name}
     * @param values an array of {@code float} values supplied as {@code values}
     */
    public void setUniform(String name, float... values) {
        int location = uniformLocation(name);
        glUniform4f(location, values[0], values[1], values[2], values[3]);
    }

    /**
     * Sets the uniform.
     * @param name the {@link String} supplied as {@code name}
     * @param matrix the {@link Matrix4f} supplied as {@code matrix}
     */
    public void setUniform(String name, Matrix4f matrix) {
        int location = uniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            glUniformMatrix4fv(location, false, buffer);
        }
    }

    /**
     * Sets the uniform.
     * @param name the {@link String} supplied as {@code name}
     * @param value the {@code boolean} supplied as {@code value}
     */
    public void setUniform(String name, boolean value) {
        int location = uniformLocation(name);
        glUniform1i(location, value ? 1 : 0);
    }

    private int uniformLocation(String name) {
        return uniformLocations.computeIfAbsent(name,
                key -> glGetUniformLocation(programId, key));
    }
}
