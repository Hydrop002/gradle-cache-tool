package org.utm.gct;

import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ModuleSource;
import org.gradle.internal.io.ClassLoaderObjectInputStream;
import org.gradle.internal.io.RandomAccessFileInputStream;
import org.gradle.internal.io.RandomAccessFileOutputStream;
import org.gradle.messaging.serialize.DefaultSerializer;
import org.gradle.messaging.serialize.Serializer;
import org.gradle.messaging.serialize.kryo.KryoBackedDecoder;
import org.gradle.messaging.serialize.kryo.KryoBackedEncoder;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.zip.CRC32;

public class GradleCacheTool {

    private Serializer<?> keySerializer;
    private Serializer<?> valueSerializer;

    public GradleCacheTool(String filename) {
        this.keySerializer = SerializerFactory.getKeySerializer(filename);
        this.valueSerializer = SerializerFactory.getValueSerializer(filename);
    }

    // FileBackedBlockStore.BlockImpl
    public void dump(File cacheFile) throws IOException, ReflectiveOperationException {
        RandomAccessFile file = new RandomAccessFile(cacheFile, "r");
        long pos = 0;
        while (true) {
            file.seek(pos);
            InputStream checkSumInputStream = getCrc32InputStream(new BufferedInputStream(new RandomAccessFileInputStream(file)));
            DataInputStream inputStream = new DataInputStream(checkSumInputStream);
            byte blockMarker = inputStream.readByte();
            assert blockMarker == 0xCC;
            System.out.println("······Begin Block······");
            System.out.println("当前文件指针：" + pos);
            byte payloadType = inputStream.readByte();
            System.out.println("载荷类型：" + payloadType);
            int payloadSize = inputStream.readInt();
            System.out.println("载荷大小：" + payloadSize);
            System.out.println("载荷信息：");
            switch (payloadType) {
            case 0x44:  // FreeListBlock
                long nextBlockPos = inputStream.readLong();
                System.out.println("\tnextBlockPos：" + nextBlockPos);
                int largestInNextBlock = inputStream.readInt();
                System.out.println("\tlargestInNextBlock：" + largestInNextBlock);
                int freeListEntryCount = inputStream.readInt();
                System.out.println("\tfreeListEntryCount：" + freeListEntryCount);
                for (int i = 0; i < freeListEntryCount; ++i) {
                    System.out.println("\t\ti：" + i);
                    long entryPos = inputStream.readLong();
                    System.out.println("\t\tentryPos：" + entryPos);
                    int entrySize = inputStream.readInt();
                    System.out.println("\t\tentrySize：" + entrySize);
                }
                break;
            case 0x55:  // HeaderBlock
                long indexRootPos = inputStream.readLong();
                System.out.println("\tindexRootPos：" + indexRootPos);
                short actualChildIndexEntries = inputStream.readShort();
                System.out.println("\tactualChildIndexEntries：" + actualChildIndexEntries);
                break;
            case 0x77:  // IndexBlock
                int indexEntryCount = inputStream.readInt();
                System.out.println("\tindexEntryCount：" + indexEntryCount);
                for (int i = 0; i < indexEntryCount; ++i) {
                    System.out.println("\t\ti：" + i);
                    long entryHashCode = inputStream.readLong();
                    System.out.println("\t\tentryHashCode：" + entryHashCode);
                    long entryDataBlockPos = inputStream.readLong();
                    System.out.println("\t\tentryDataBlockPos：" + entryDataBlockPos);
                    long entryChildIndexBlockPos = inputStream.readLong();
                    System.out.println("\t\tentryChildIndexBlockPos：" + entryChildIndexBlockPos);
                }
                long tailPos = inputStream.readLong();
                System.out.println("\ttailPos：" + tailPos);
                break;
            case 0x33:  // DataBlock
                int dataSize = inputStream.readInt();
                System.out.println("\tdataSize：" + dataSize);
                int dataBytes = inputStream.readInt();
                System.out.println("\tdataBytes：" + dataBytes);
                byte[] serialisedValue = new byte[dataBytes];
                inputStream.readFully(serialisedValue);
                System.out.println("\tserialisedValue：");
                KryoBackedDecoder decoder = new KryoBackedDecoder(new ByteArrayInputStream(serialisedValue));
                if (isInstance(this.valueSerializer, "org.gradle.api.internal.artifacts.ivyservice.modulecache.ModuleDescriptorCacheEntrySerializer")) {
                    byte type = decoder.readByte();
                    System.out.println("\t\ttype：" + type);
                    boolean isChanging = decoder.readBoolean();
                    System.out.println("\t\tisChanging：" + isChanging);
                    String packaging = decoder.readNullableString();
                    System.out.println("\t\tpackaging：" + packaging);
                    long createTimestamp = decoder.readLong();
                    System.out.println("\t\tcreateTimestamp：" + createTimestamp);
                    ClassLoaderObjectInputStream objectInputStream = new ClassLoaderObjectInputStream(decoder.getInputStream(), DefaultSerializer.class.getClassLoader());
                    ModuleSource moduleSource = (ModuleSource) objectInputStream.readObject();
                    System.out.println("\t\tmoduleSource：" + moduleSource);
                    objectInputStream.close();
                    byte[] encodedHash = decoder.readBinary();
                    System.out.println("\t\tencodedHash：" + new BigInteger(encodedHash));
                } else if (isInstance(this.valueSerializer, "org.gradle.api.internal.artifacts.ivyservice.dynamicversions.SingleFileBackedModuleVersionsCache$ModuleVersionsCacheEntrySerializer")) {
                    int size = decoder.readInt();
                    System.out.println("\t\tsize：" + size);
                    for (int i = 0; i < size; ++i) {
                        String version = decoder.readString();
                        System.out.println("\t\t\tversion：" + version);
                    }
                    long createTimestamp = decoder.readLong();
                    System.out.println("\t\tcreateTimestamp：" + createTimestamp);
                } else if (isInstance(this.valueSerializer, "org.gradle.internal.resource.cached.ivy.ArtifactAtRepositoryCachedArtifactIndex$CachedArtifactSerializer")) {
                    boolean isMissing = decoder.readBoolean();
                    System.out.println("\t\tisMissing：" + isMissing);
                    String filename = decoder.readString();
                    System.out.println("\t\tfilename：" + filename);
                    long createTimestamp = decoder.readLong();
                    System.out.println("\t\tcreateTimestamp：" + createTimestamp);
                    byte[] encodedHash = decoder.readBinary();
                    System.out.println("\t\tencodedHash：" + new BigInteger(encodedHash));
                } else if (isInstance(this.valueSerializer, "org.gradle.api.internal.changedetection.state.CacheBackedTaskHistoryRepository$TaskHistorySerializer")) {
                    byte executions = decoder.readByte();
                    System.out.println("\t\texecutions：" + executions);
                    for (int i = 0; i < executions; ++i) {
                        long inputFilesSnapshotId = decoder.readLong();
                        System.out.println("\t\t\tinputFilesSnapshotId：" + inputFilesSnapshotId);
                        long outputFilesSnapshotId = decoder.readLong();
                        System.out.println("\t\t\toutputFilesSnapshotId：" + outputFilesSnapshotId);
                        String taskClass = decoder.readString();
                        System.out.println("\t\t\ttaskClass：" + taskClass);
                        int outputFiles = decoder.readInt();
                        System.out.println("\t\t\toutputFiles：" + outputFiles);
                        for (int j = 0; j < outputFiles; ++j) {
                            String outputFile = decoder.readString();
                            System.out.println("\t\t\t\toutputFile：" + outputFile);
                        }
                        boolean inputProperties = decoder.readBoolean();
                        System.out.println("\t\t\tinputProperties：" + inputProperties);
                        if (inputProperties) {
                            int mapSize = decoder.readInt();
                            System.out.println("\t\t\tmapSize：" + mapSize);
                            for (int j = 0; j < mapSize; ++j) {
                                String mapKey = decoder.readString();
                                System.out.println("\t\t\t\tmapKey：" + mapKey);
                                ClassLoaderObjectInputStream objectInputStream = new ClassLoaderObjectInputStream(decoder.getInputStream(), DefaultSerializer.class.getClassLoader());
                                Object mapValue = objectInputStream.readObject();
                                System.out.println("\t\t\t\tmapValue：" + mapValue);
                                objectInputStream.close();
                            }
                        }
                    }
                } else if (isInstance(this.valueSerializer, "org.gradle.api.internal.artifacts.ivyservice.modulecache.DefaultModuleArtifactsCache$ModuleArtifactsCacheEntrySerializer")) {
                    long createTimestamp = decoder.readLong();
                    System.out.println("\t\tcreateTimestamp：" + createTimestamp);
                    byte[] encodedHash = decoder.readBinary();
                    System.out.println("\t\tencodedHash：" + new BigInteger(encodedHash));
                    int size = decoder.readInt();
                    for (int i = 0; i < size; ++i) {
                        byte id = decoder.readByte();
                        System.out.println("\t\t\tid：" + id);
                        if (id == 2) {
                            String projectPath = decoder.readString();
                            System.out.println("\t\t\tprojectPath：" + projectPath);
                        } else if (id == 1) {
                            String group = decoder.readString();
                            System.out.println("\t\t\tgroup：" + group);
                            String module = decoder.readString();
                            System.out.println("\t\t\tmodule：" + module);
                            String version = decoder.readString();
                            System.out.println("\t\t\tversion：" + version);
                        } else {
                            System.out.println("\t\t\t无法找到对应id的ComponentIdentifier！");
                        }
                        String artifactName = decoder.readString();
                        System.out.println("\t\t\tartifactName：" + artifactName);
                        String type = decoder.readString();
                        System.out.println("\t\t\ttype：" + type);
                        String extension = decoder.readNullableString();
                        System.out.println("\t\t\textension：" + extension);
                        int mapSize = decoder.readInt();
                        System.out.println("\t\t\tmapSize：" + mapSize);
                        for (int j = 0; j < mapSize; ++j) {
                            String mapKey = decoder.readString();
                            System.out.println("\t\t\t\tmapKey：" + mapKey);
                            String mapValue = decoder.readString();
                            System.out.println("\t\t\t\tmapValue：" + mapValue);
                        }
                    }
                } else if (isInstance(this.valueSerializer, "org.gradle.messaging.serialize.LongSerializer")) {
                    long state = decoder.readLong();
                    System.out.println("\t\tstate：" + state);
                } else if (isInstance(this.valueSerializer, "org.gradle.api.internal.changedetection.state.CachingFileSnapshotter$FileInfoSerializer")) {
                    byte[] hash = decoder.readBinary();
                    System.out.print("\t\thash：");
                    for (int i = 0; i < hash.length; ++i) {
                        System.out.print(hash[i] + ", ");
                        if (i > 10) {
                            System.out.print("...");
                            break;
                        }
                    }
                    System.out.println();
                    long timestamp = decoder.readLong();
                    System.out.println("\t\ttimestamp：" + timestamp);
                    long length = decoder.readLong();
                    System.out.println("\t\tlength：" + length);
                } else if (isInstance(this.valueSerializer, "org.gradle.messaging.serialize.DefaultSerializerRegistry$TaggedTypeSerializer")) {
                    byte tag = decoder.readByte();
                    System.out.println("\t\ttag：" + tag);
                    if (tag == 1) {
                        int rootFileIdsCount = decoder.readSmallInt();
                        System.out.println("\t\trootFileIdsCount：" + rootFileIdsCount);
                        for (int i = 0; i < rootFileIdsCount; ++i) {
                            String key = decoder.readString();
                            System.out.println("\t\t\tkey：" + key);
                            boolean notNull = decoder.readBoolean();
                            System.out.println("\t\t\tnotNull：" + notNull);
                            long value = decoder.readLong();
                            System.out.println("\t\t\tvalue：" + value);
                        }
                        int snapshotsCount = decoder.readSmallInt();
                        System.out.println("\t\tsnapshotsCount：" + snapshotsCount);
                        for (int i = 0; i < snapshotsCount; ++i) {
                            String key = decoder.readString();
                            System.out.println("\t\t\tkey：" + key);
                            byte fileSnapshotKind = decoder.readByte();
                            System.out.println("\t\t\tfileSnapshotKind：" + fileSnapshotKind);
                            if (fileSnapshotKind == 1) {
                            } else if (fileSnapshotKind == 2) {
                            } else if (fileSnapshotKind == 3) {
                                byte hashSize = decoder.readByte();
                                System.out.println("\t\t\thashSize：" + hashSize);
                                byte[] hash = new byte[hashSize];
                                decoder.readBytes(hash);
                                System.out.print("\t\t\thash：");
                                for (int j = 0; j < hash.length; ++j) {
                                    System.out.print(hash[j] + ", ");
                                    if (j > 10) {
                                        System.out.print("...");
                                        break;
                                    }
                                }
                                System.out.println();
                            } else {
                                System.out.println("\t\t\t数据流中发现未知的值！");
                            }
                        }
                    } else {
                        System.out.println("\t\t发现未知类型的tag！");
                    }
                }
                break;
            }
            long actualCheckSum = getInputCheckSum(checkSumInputStream).getValue();
            System.out.println("CRC32检验和：" + actualCheckSum);
            long checkSum = inputStream.readLong();
            System.out.println("存储的检验和：" + checkSum);
            if (checkSum == actualCheckSum) {
                System.out.println("检验通过！");
            } else {
                System.out.println("检验失败！");
            }
            System.out.println("······End Block······");
            pos += 6 + payloadSize + 8;
            inputStream.close();
            if (pos >= file.length()) {
                break;
            }
        }
    }

    public void remap(File cacheFile, String oldHome, String newHome) throws IOException, ReflectiveOperationException {
        assert isInstance(this.keySerializer, "org.gradle.internal.resource.cached.ivy.ArtifactAtRepositoryCachedArtifactIndex$ArtifactAtRepositoryKeySerializer");
        assert isInstance(this.valueSerializer, "org.gradle.internal.resource.cached.ivy.ArtifactAtRepositoryCachedArtifactIndex$CachedArtifactSerializer");

        File outCacheFile = new File(cacheFile.getAbsolutePath() + ".fixed");
        RandomAccessFile file = new RandomAccessFile(cacheFile, "r");
        RandomAccessFile outFile = new RandomAccessFile(outCacheFile, "rw");

        ArrayList<Long> blockPosList = new ArrayList<>();
        ArrayList<Long> outBlockPosList = new ArrayList<>();

        long pos = 0;
        while (true) {
            blockPosList.add(pos);
            file.seek(pos);
            InputStream checkSumInputStream = getCrc32InputStream(new BufferedInputStream(new RandomAccessFileInputStream(file)));
            DataInputStream inputStream = new DataInputStream(checkSumInputStream);
            byte blockMarker = inputStream.readByte();
            assert blockMarker == 0xCC;
            byte payloadType = inputStream.readByte();
            int payloadSize = inputStream.readInt();
            pos += 6 + payloadSize + 8;
            inputStream.close();
            if (pos >= file.length()) {
                break;
            }
        }

        outBlockPosList.add(blockPosList.get(0));
        outBlockPosList.add(blockPosList.get(1));
        outBlockPosList.add(blockPosList.get(2));
        outBlockPosList.add(blockPosList.get(3));

        long outPos = 0;
        long entryPos = 0;
        int entrySize = 0;
        for (int p = 3; p < blockPosList.size(); ++p) {
            pos = blockPosList.get(p);
            file.seek(pos + 6);
            InputStream checkSumInputStream = getCrc32InputStream(new BufferedInputStream(new RandomAccessFileInputStream(file)));
            DataInputStream inputStream = new DataInputStream(checkSumInputStream);
            int dataSize = inputStream.readInt();
            int dataBytes = inputStream.readInt();
            byte[] serialisedValue = new byte[dataBytes];
            inputStream.readFully(serialisedValue);
            inputStream.close();

            KryoBackedDecoder decoder = new KryoBackedDecoder(new ByteArrayInputStream(serialisedValue));
            boolean isMissing = decoder.readBoolean();
            String filename = decoder.readString();
            long createTimestamp = decoder.readLong();
            byte[] encodedHash = decoder.readBinary();
            decoder.close();

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            KryoBackedEncoder encoder = new KryoBackedEncoder(byteArrayOutputStream);
            encoder.writeBoolean(isMissing);
            encoder.writeString(filename.replace(oldHome, newHome));  // Remap
            encoder.writeLong(createTimestamp);
            encoder.writeBinary(encodedHash);
            encoder.flush();
            byte[] outSerialisedValue = byteArrayOutputStream.toByteArray();
            encoder.close();

            outPos = outBlockPosList.get(p);
            outFile.seek(outPos);
            OutputStream checkSumOutputStream = getCrc32OutputStream(new BufferedOutputStream(new RandomAccessFileOutputStream(outFile)));
            DataOutputStream outputStream = new DataOutputStream(checkSumOutputStream);
            outputStream.writeByte(0xCC);
            outputStream.writeByte(0x33);
            outputStream.writeInt(8 + outSerialisedValue.length);
            outputStream.writeInt(outSerialisedValue.length);
            outputStream.writeInt(outSerialisedValue.length);
            outputStream.write(outSerialisedValue);
            long checkSum = getOutputCheckSum(checkSumOutputStream).getValue();
            outputStream.writeLong(checkSum);
            outputStream.close();
            long finalSize = outPos + 22 + outSerialisedValue.length;
            outBlockPosList.add(finalSize);
            if (outFile.length() < finalSize) {
                outFile.setLength(finalSize);
            }

            if (filename.contains("ForgeGradle")) {
                entryPos = outPos;
                entrySize = 22 + outSerialisedValue.length;
            }
        }

        file.seek(0);
        InputStream checkSumInputStream = getCrc32InputStream(new BufferedInputStream(new RandomAccessFileInputStream(file)));
        DataInputStream inputStream = new DataInputStream(checkSumInputStream);
        byte blockMarker = inputStream.readByte();
        byte payloadType = inputStream.readByte();
        int payloadSize = inputStream.readInt();
        long nextBlockPos = inputStream.readLong();
        int largestInNextBlock = inputStream.readInt();
        int freeListEntryCount = inputStream.readInt();
        inputStream.close();

        outFile.seek(0);
        OutputStream checkSumOutputStream = getCrc32OutputStream(new BufferedOutputStream(new RandomAccessFileOutputStream(outFile)));
        DataOutputStream outputStream = new DataOutputStream(checkSumOutputStream);
        outputStream.writeByte(0xCC);
        outputStream.writeByte(0x44);
        outputStream.writeInt(payloadSize);
        outputStream.writeLong(nextBlockPos);
        outputStream.writeInt(largestInNextBlock);
        outputStream.writeInt(freeListEntryCount);
        for (int i = 0; i < freeListEntryCount; ++i) {
            outputStream.writeLong(entryPos);
            outputStream.writeInt(entrySize);
        }
        long checkSum = getOutputCheckSum(checkSumOutputStream).getValue();
        outputStream.writeLong(checkSum);
        outputStream.close();

        file.seek(blockPosList.get(1));
        checkSumInputStream = getCrc32InputStream(new BufferedInputStream(new RandomAccessFileInputStream(file)));
        inputStream = new DataInputStream(checkSumInputStream);
        blockMarker = inputStream.readByte();
        payloadType = inputStream.readByte();
        payloadSize = inputStream.readInt();
        long indexRootPos = inputStream.readLong();
        short actualChildIndexEntries = inputStream.readShort();
        checkSum = inputStream.readLong();
        inputStream.close();

        outFile.seek(outBlockPosList.get(1));
        checkSumOutputStream = getCrc32OutputStream(new BufferedOutputStream(new RandomAccessFileOutputStream(outFile)));
        outputStream = new DataOutputStream(checkSumOutputStream);
        outputStream.writeByte(0xCC);
        outputStream.writeByte(0x55);
        outputStream.writeInt(payloadSize);
        outputStream.writeLong(indexRootPos);
        outputStream.writeShort(actualChildIndexEntries);
        outputStream.writeLong(checkSum);
        outputStream.close();

        file.seek(blockPosList.get(2));
        checkSumInputStream = getCrc32InputStream(new BufferedInputStream(new RandomAccessFileInputStream(file)));
        inputStream = new DataInputStream(checkSumInputStream);
        blockMarker = inputStream.readByte();
        payloadType = inputStream.readByte();
        payloadSize = inputStream.readInt();
        int indexEntryCount = inputStream.readInt();
        outFile.seek(outBlockPosList.get(2));
        checkSumOutputStream = getCrc32OutputStream(new BufferedOutputStream(new RandomAccessFileOutputStream(outFile)));
        outputStream = new DataOutputStream(checkSumOutputStream);
        outputStream.writeByte(0xCC);
        outputStream.writeByte(0x77);
        outputStream.writeInt(payloadSize);
        outputStream.writeInt(indexEntryCount);
        for (int i = 0; i < indexEntryCount; ++i) {
            long entryHashCode = inputStream.readLong();
            long entryDataBlockPos = inputStream.readLong();
            long entryChildIndexBlockPos = inputStream.readLong();
            outPos = outBlockPosList.get(blockPosList.indexOf(entryDataBlockPos));
            outputStream.writeLong(entryHashCode);
            outputStream.writeLong(outPos);
            outputStream.writeLong(entryChildIndexBlockPos);
        }
        long tailPos = inputStream.readLong();
        inputStream.close();
        outputStream.writeLong(tailPos);
        checkSum = getOutputCheckSum(checkSumOutputStream).getValue();
        outputStream.writeLong(checkSum);
        outputStream.close();

        System.out.println("文件已生成：" + outCacheFile.getAbsolutePath());
    }

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
