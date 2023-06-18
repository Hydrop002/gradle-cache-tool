package org.utm.gct;

import org.gradle.messaging.serialize.Serializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class SerializerFactory {

    protected Map<String, Serializer<?>> keySerializerMap = new HashMap<>();
    protected Map<String, Serializer<?>> valueSerializerMap = new HashMap<>();

    protected static Serializer<?> getInstance(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (Serializer<?>) constructor.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Serializer<?> getKeySerializer(String filename) {
        return this.keySerializerMap.get(filename);
    }

    public Serializer<?> getValueSerializer(String filename) {
        return this.valueSerializerMap.get(filename);
    }

}
