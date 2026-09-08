package com.taskmanager.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BaseEntity Tests")
class BaseEntityTest {

    private static class ConcreteEntity extends BaseEntity {
        private static final long serialVersionUID = 1L;
    }

    private static class AnotherConcreteEntity extends BaseEntity {
        private static final long serialVersionUID = 1L;
    }

    @Test
    @DisplayName("Default constructor initializes createdAt and updatedAt to non-null recent timestamps")
    void testConstructorTimestamps() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        ConcreteEntity entity = new ConcreteEntity();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertFalse(entity.getCreatedAt().isBefore(before));
        assertFalse(entity.getCreatedAt().isAfter(after));
        assertFalse(entity.getUpdatedAt().isBefore(before));
        assertFalse(entity.getUpdatedAt().isAfter(after));
    }

    @Test
    @DisplayName("Getters and setters for id, createdAt, updatedAt behave correctly")
    void testGettersAndSetters() {
        ConcreteEntity entity = new ConcreteEntity();
        entity.setId(42);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(tomorrow);

        assertEquals(42, entity.getId());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(tomorrow, entity.getUpdatedAt());
    }

    @Test
    @DisplayName("equals and hashCode contract based strictly on id")
    void testEqualsAndHashCode() {
        ConcreteEntity entity1 = new ConcreteEntity();
        entity1.setId(10);

        ConcreteEntity entity2 = new ConcreteEntity();
        entity2.setId(10);

        ConcreteEntity entity3 = new ConcreteEntity();
        entity3.setId(20);

        // Reflexive
        assertEquals(entity1, entity1);

        // Symmetric & equal id
        assertEquals(entity1, entity2);
        assertEquals(entity2, entity1);
        assertEquals(entity1.hashCode(), entity2.hashCode());

        // Different id
        assertNotEquals(entity1, entity3);
        assertNotEquals(entity1.hashCode(), entity3.hashCode());

        // Null comparison
        assertNotEquals(null, entity1);

        // Different type comparison with same id
        AnotherConcreteEntity otherTypeEntity = new AnotherConcreteEntity();
        otherTypeEntity.setId(10);
        assertNotEquals(entity1, otherTypeEntity);
    }

    @Test
    @DisplayName("Verify serialVersionUID is 1L via reflection and verify actual serialization")
    void testSerialization() throws Exception {
        Field field = BaseEntity.class.getDeclaredField("serialVersionUID");
        field.setAccessible(true);
        assertTrue(Modifier.isStatic(field.getModifiers()));
        assertTrue(Modifier.isFinal(field.getModifiers()));
        assertEquals(1L, field.getLong(null));

        ConcreteEntity original = new ConcreteEntity();
        original.setId(99);
        LocalDateTime originalCreated = LocalDateTime.now();
        original.setCreatedAt(originalCreated);
        original.setUpdatedAt(originalCreated);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            Object deserialized = ois.readObject();
            assertTrue(deserialized instanceof ConcreteEntity);
            ConcreteEntity copy = (ConcreteEntity) deserialized;
            assertEquals(original.getId(), copy.getId());
            assertEquals(original.getCreatedAt(), copy.getCreatedAt());
            assertEquals(original.getUpdatedAt(), copy.getUpdatedAt());
        }
    }
}
