package net.not_thefirst.story_mode_clouds.utils.rendering.utils;

import org.joml.*;

import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;

import java.nio.ByteBuffer;
import org.apache.logging.log4j.Logger;

/**
 * Utility class to read and print JOML math types from a ByteBuffer for debugging.
 * Uses absolute offset reading to prevent modifying the buffer's position.
 */
public final class JOMLBufferReader {

    // Retrieve the Log4j logger instance via your provider
    private static final Logger logger = LoggerProvider.get();

    // Prevent instantiation
    private JOMLBufferReader() {}

    // --- ABSOLUTE OFFSET READING METHODS ---

    /**
     * Reads a Vector2f from an absolute byte offset.
     */
    public static Vector2f readVector2f(ByteBuffer buffer, int offset) {
        return new Vector2f(
            buffer.getFloat(offset), 
            buffer.getFloat(offset + 4)
        );
    }

    /**
     * Reads a Vector3f from an absolute byte offset.
     */
    public static Vector3f readVector3f(ByteBuffer buffer, int offset) {
        return new Vector3f(
            buffer.getFloat(offset), 
            buffer.getFloat(offset + 4), 
            buffer.getFloat(offset + 8)
        );
    }

    /**
     * Reads a Vector4f from an absolute byte offset.
     */
    public static Vector4f readVector4f(ByteBuffer buffer, int offset) {
        return new Vector4f(
            buffer.getFloat(offset), 
            buffer.getFloat(offset + 4), 
            buffer.getFloat(offset + 8), 
            buffer.getFloat(offset + 12)
        );
    }

    /**
     * Reads a Vector2i from an absolute byte offset.
     */
    public static Vector2i readVector2i(ByteBuffer buffer, int offset) {
        return new Vector2i(
            buffer.getInt(offset), 
            buffer.getInt(offset + 4)
        );
    }

    /**
     * Reads a Vector3i from an absolute byte offset.
     */
    public static Vector3i readVector3i(ByteBuffer buffer, int offset) {
        return new Vector3i(
            buffer.getInt(offset), 
            buffer.getInt(offset + 4), 
            buffer.getInt(offset + 8)
        );
    }

    /**
     * Reads a Vector4i from an absolute byte offset.
     */
    public static Vector4i readVector4i(ByteBuffer buffer, int offset) {
        return new Vector4i(
            buffer.getInt(offset), 
            buffer.getInt(offset + 4), 
            buffer.getInt(offset + 8), 
            buffer.getInt(offset + 12)
        );
    }

    /**
     * Reads a Matrix3f (column-major order) from an absolute byte offset.
     */
    public static Matrix3f readMatrix3f(ByteBuffer buffer, int offset) {
        return new Matrix3f(
            buffer.getFloat(offset),      buffer.getFloat(offset + 4),  buffer.getFloat(offset + 8),
            buffer.getFloat(offset + 12), buffer.getFloat(offset + 16), buffer.getFloat(offset + 20),
            buffer.getFloat(offset + 24), buffer.getFloat(offset + 28), buffer.getFloat(offset + 32)
        );
    }

    /**
     * Reads a Matrix4f (column-major order) from an absolute byte offset.
     */
    public static Matrix4f readMatrix4f(ByteBuffer buffer, int offset) {
        return new Matrix4f(
            buffer.getFloat(offset),      buffer.getFloat(offset + 4),  buffer.getFloat(offset + 8),  buffer.getFloat(offset + 12),
            buffer.getFloat(offset + 16), buffer.getFloat(offset + 20), buffer.getFloat(offset + 24), buffer.getFloat(offset + 28),
            buffer.getFloat(offset + 32), buffer.getFloat(offset + 36), buffer.getFloat(offset + 40), buffer.getFloat(offset + 44),
            buffer.getFloat(offset + 48), buffer.getFloat(offset + 52), buffer.getFloat(offset + 56), buffer.getFloat(offset + 60)
        );
    }

    /**
     * Reads a Quaternionf from an absolute byte offset.
     */
    public static Quaternionf readQuaternionf(ByteBuffer buffer, int offset) {
        return new Quaternionf(
            buffer.getFloat(offset), 
            buffer.getFloat(offset + 4), 
            buffer.getFloat(offset + 8), 
            buffer.getFloat(offset + 12)
        );
    }

    // --- LOGGING AND FORMATTING HELPERS ---

    /**
     * Reads and logs a Matrix4f at a specific offset as a formatted grid.
     */
    public static void logMatrix4f(String uniformName, ByteBuffer buffer, int offset) {
        Matrix4f mat = readMatrix4f(buffer, offset);
        logger.warn("Uniform '{}' [Offset: {}] (Matrix4f):\n" +
                     "  [{ micro, % micro, % micro, % micro ]\n" +
                     "  [  {},   {},   {},   {} ]\n" +
                     "  [  {},   {},   {},   {} ]\n" +
                     "  [  {},   {},   {},   {} ]",
                     uniformName, offset,
                     mat.m00(), mat.m10(), mat.m20(), mat.m30(),
                     mat.m01(), mat.m11(), mat.m21(), mat.m31(),
                     mat.m02(), mat.m12(), mat.m22(), mat.m32(),
                     mat.m03(), mat.m13(), mat.m23(), mat.m33());
    }

    /**
     * Reads and logs a Vector3f at a specific offset.
     */
    public static void logVector3f(String uniformName, ByteBuffer buffer, int offset) {
        Vector3f vec = readVector3f(buffer, offset);
        logger.warn("Uniform '{}' [Offset: {}] (Vector3f): [  {},   {},   {} ]", 
                     uniformName, offset, vec.x, vec.y, vec.z);
    }

    /**
     * Reads and logs a Vector4f at a specific offset.
     */
    public static void logVector4f(String uniformName, ByteBuffer buffer, int offset) {
        Vector4f vec = readVector4f(buffer, offset);
        logger.warn("Uniform '{}' [Offset: {}] (Vector4f): [  {},   {},   {},   {} ]", 
                     uniformName, offset, vec.x, vec.y, vec.z, vec.w);
    }

        /**
     * Reads a Matrix4f from an absolute byte offset and logs it formatted as integers.
     */
    public static void logMatrix4fAsInt(String uniformName, ByteBuffer buffer, int offset) {
        Matrix4f mat = readMatrix4f(buffer, offset);
        logger.warn("Uniform '{}' [Offset: {}] (Matrix4i):\n" +
                     "  [ {}, {}, {}, {} ]\n" +
                     "  [ {}, {}, {}, {} ]\n" +
                     "  [ {}, {}, {}, {} ]\n" +
                     "  [ {}, {}, {}, {} ]",
                     uniformName, offset,
                     (int) mat.m00(), (int) mat.m10(), (int) mat.m20(), (int) mat.m30(),
                     (int) mat.m01(), (int) mat.m11(), (int) mat.m21(), (int) mat.m31(),
                     (int) mat.m02(), (int) mat.m12(), (int) mat.m22(), (int) mat.m32(),
                     (int) mat.m03(), (int) mat.m13(), (int) mat.m23(), (int) mat.m33());
    }

    /**
     * Reads a Vector3f from an absolute byte offset and logs it formatted as integers.
     */
    public static void logVector3fAsInt(String uniformName, ByteBuffer buffer, int offset) {
        Vector3f vec = readVector3f(buffer, offset);
        logger.warn("Uniform '{}' [Offset: {}] (Vector3i): [ {}, {}, {} ]", 
                     uniformName, offset, (int) vec.x, (int) vec.y, (int) vec.z);
    }

    /**
     * Reads a Vector4f from an absolute byte offset and logs it formatted as integers.
     */
    public static void logVector4fAsInt(String uniformName, ByteBuffer buffer, int offset) {
        Vector4f vec = readVector4f(buffer, offset);
        logger.warn("Uniform '{}' [Offset: {}] (Vector4i): [ {}, {}, {}, {} ]", 
                     uniformName, offset, (int) vec.x, (int) vec.y, (int) vec.z, (int) vec.w);
    }

}
