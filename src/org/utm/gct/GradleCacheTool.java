package org.utm.gct;

import org.gradle.messaging.serialize.Serializer;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.zip.CRC32;

public abstract class GradleCacheTool {

    protected Serializer<?> keySerializer;
    protected Serializer<?> valueSerializer;

    public abstract void dump(File cacheFile) throws IOException, ReflectiveOperationException;

    public abstract void remap(File cacheFile, String oldHome, String newHome) throws IOException, ReflectiveOperationException;

    public static InputStream getCrc32InputStream(InputStream inputStream) throws ReflectiveOperationException {
        Class<?> clazz = Class.forName("org.gradle.cache.internal.btree.FileBackedBlockStore$Crc32InputStream");
        Constructor<?> constructor = clazz.getDeclaredConstructor(InputStream.class);
        constructor.setAccessible(true);
        return (InputStream) constructor.newInstance(inputStream);
    }

    public static OutputStream getCrc32OutputStream(OutputStream outputStream) throws ReflectiveOperationException {
        Class<?> clazz = Class.forName("org.gradle.cache.internal.btree.FileBackedBlockStore$Crc32OutputStream");
        Constructor<?> constructor = clazz.getDeclaredConstructor(OutputStream.class);
        constructor.setAccessible(true);
        return (OutputStream) constructor.newInstance(outputStream);
    }

    public static CRC32 getInputCheckSum(InputStream inputStream) throws ReflectiveOperationException {
        Class<?> clazz = Class.forName("org.gradle.cache.internal.btree.FileBackedBlockStore$Crc32InputStream");
        Field checksum = clazz.getDeclaredField("checksum");
        checksum.setAccessible(true);
        return (CRC32) checksum.get(inputStream);
    }

    public static CRC32 getOutputCheckSum(OutputStream outputStream) throws ReflectiveOperationException {
        Class<?> clazz = Class.forName("org.gradle.cache.internal.btree.FileBackedBlockStore$Crc32OutputStream");
        Field checksum = clazz.getDeclaredField("checksum");
        checksum.setAccessible(true);
        return (CRC32) checksum.get(outputStream);
    }

    public static boolean isInstance(Object obj, String className) throws ReflectiveOperationException {
        Class<?> clazz = Class.forName(className);
        return clazz.isInstance(obj);
    }

}
