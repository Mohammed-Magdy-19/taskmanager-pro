package com.taskmanager.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

/**
 * Generic serialization utility facilitating deep object graph persistence and retrieval.
 * Uses standard Java Object serialization with automated resource management.
 *
 * @param <T> the type of object to serialize and deserialize
 */
public class SerializationUtil<T> {

    /**
     * Default constructor for the generic serialization utility.
     */
    public SerializationUtil() {
        // Generic utility instance
    }

    /**
     * Serializes an object to the specified filesystem destination.
     *
     * @param object the object to persist
     * @param filePath the absolute or relative file destination path
     * @throws IOException if an I/O error occurs during serialization
     * @throws NullPointerException if object or filePath is null
     */
    public void serialize(T object, String filePath) throws IOException {
        Objects.requireNonNull(object, "Object to serialize cannot be null");
        Objects.requireNonNull(filePath, "File path cannot be null");

        try (FileOutputStream fos = new FileOutputStream(filePath);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(object);
            oos.flush();
        }
    }

    /**
     * Deserializes an object from the specified file path.
     *
     * @param filePath the file path of the serialized object
     * @return the deserialized object instance
     * @throws IOException if an I/O error occurs or the stream is corrupted
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     * @throws NullPointerException if filePath is null
     */
    @SuppressWarnings("unchecked")
    public T deserialize(String filePath) throws IOException, ClassNotFoundException {
        Objects.requireNonNull(filePath, "File path cannot be null");

        try (FileInputStream fis = new FileInputStream(filePath);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (T) ois.readObject();
        }
    }

    /**
     * Deserializes an object from the specified file path and validates that the resulting instance
     * matches the expected class type.
     *
     * @param <R> the expected return type
     * @param filePath the file path of the serialized object
     * @param expectedType the expected class of the deserialized object
     * @return the deserialized object cast to the expected type
     * @throws IOException if an I/O error occurs or the stream is corrupted
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     * @throws ClassCastException if the deserialized object cannot be cast to expectedType
     * @throws NullPointerException if filePath or expectedType is null
     */
    public <R> R deserialize(String filePath, Class<R> expectedType) throws IOException, ClassNotFoundException {
        Objects.requireNonNull(filePath, "File path cannot be null");
        Objects.requireNonNull(expectedType, "Expected type cannot be null");

        try (FileInputStream fis = new FileInputStream(filePath);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            Object obj = ois.readObject();
            if (obj == null) {
                return null;
            }
            if (!expectedType.isInstance(obj)) {
                throw new ClassCastException("Deserialized object of type " + obj.getClass().getName()
                        + " is not an instance of expected type " + expectedType.getName());
            }
            return expectedType.cast(obj);
        }
    }
}
