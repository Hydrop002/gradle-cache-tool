package org.utm.gct;

import org.gradle.messaging.serialize.Serializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class SerializerFactory {

    private static Map<String, Serializer<?>> keySerializerMap = new HashMap<>();
    private static Map<String, Serializer<?>> valueSerializerMap = new HashMap<>();

    static {
        keySerializerMap.put("module-metadata", getInstance("org.gradle.api.internal.artifacts.ivyservice.modulecache.DefaultModuleMetaDataCache$RevisionKeySerializer"));
        valueSerializerMap.put("module-metadata", getInstance("org.gradle.api.internal.artifacts.ivyservice.modulecache.ModuleDescriptorCacheEntrySerializer"));
        keySerializerMap.put("module-versions", getInstance("org.gradle.api.internal.artifacts.ivyservice.dynamicversions.SingleFileBackedModuleVersionsCache$ModuleKeySerializer"));
        valueSerializerMap.put("module-versions", getInstance("org.gradle.api.internal.artifacts.ivyservice.dynamicversions.SingleFileBackedModuleVersionsCache$ModuleVersionsCacheEntrySerializer"));
        keySerializerMap.put("artifact-at-repository", getInstance("org.gradle.internal.resource.cached.ivy.ArtifactAtRepositoryCachedArtifactIndex$ArtifactAtRepositoryKeySerializer"));
        valueSerializerMap.put("artifact-at-repository", getInstance("org.gradle.internal.resource.cached.ivy.ArtifactAtRepositoryCachedArtifactIndex$CachedArtifactSerializer"));
        keySerializerMap.put("taskArtifacts", getInstance("org.gradle.messaging.serialize.BaseSerializerFactory$StringSerializer"));
        valueSerializerMap.put("taskArtifacts", getInstance("org.gradle.api.internal.changedetection.state.CacheBackedTaskHistoryRepository$TaskHistorySerializer"));
        keySerializerMap.put("module-artifacts", getInstance("org.gradle.api.internal.artifacts.ivyservice.modulecache.DefaultModuleArtifactsCache$ModuleArtifactsKeySerializer"));
        valueSerializerMap.put("module-artifacts", getInstance("org.gradle.api.internal.artifacts.ivyservice.modulecache.DefaultModuleArtifactsCache$ModuleArtifactsCacheEntrySerializer"));
        keySerializerMap.put("outputFileStates", getInstance("org.gradle.messaging.serialize.BaseSerializerFactory$StringSerializer"));
        valueSerializerMap.put("outputFilesStates", getInstance("org.gradle.messaging.serialize.LongSerializer"));
        keySerializerMap.put("fileHashes", getInstance("org.gradle.messaging.serialize.BaseSerializerFactory$FileSerializer"));
        valueSerializerMap.put("fileHashes", getInstance("org.gradle.api.internal.changedetection.state.CachingFileSnapshotter$FileInfoSerializer"));
        keySerializerMap.put("fileSnapshots", getInstance("org.gradle.messaging.serialize.BaseSerializerFactory$LongSerializer"));
        valueSerializerMap.put("fileSnapshots", getInstance("org.gradle.messaging.serialize.DefaultSerializerRegistry$TaggedTypeSerializer"));
    }

    private static Serializer<?> getInstance(String className) {
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

    public static Serializer<?> getKeySerializer(String filename) {
        return keySerializerMap.get(filename);
    }

    public static Serializer<?> getValueSerializer(String filename) {
        return valueSerializerMap.get(filename);
    }

}
