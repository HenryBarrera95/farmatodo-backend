package com.farmatodo;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * Utilidades compartidas para tests. Evita duplicar código de reflexión.
 */
public final class TestUtils {

    public static final String API_KEY_HEADER = "X-API-KEY";
    public static final String API_KEY_DEFAULT = "changeme";

    private TestUtils() {}

    /**
     * Asigna un UUID al campo privado "id" de una entidad.
     * Usado cuando las entidades JPA no exponen setters para id.
     */
    public static void setId(Object entity, UUID id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set id on " + entity.getClass().getSimpleName(), e);
        }
    }

    /**
     * Asigna un UUID generado al campo "id" de la entidad.
     */
    public static void setId(Object entity) {
        setId(entity, UUID.randomUUID());
    }
}
