package org.utm.gct;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class VersionManager {

    private static Map<String, String> metadataMap = new HashMap<>();

    static {  // CacheLayout
        metadataMap.put("2.12", "2.0");
        metadataMap.put("2.15", "2.7");
        metadataMap.put("2.16", "2.14");
    }

    public static String getVersionByPath(String filepath) {
        String[] dirList = filepath.split("[/\\\\]");
        String metadata = dirList[dirList.length - 2];
        if (metadata.startsWith("metadata-")) {
            String metadataVersion = metadata.split("metadata-")[1];
            return metadataMap.get(metadataVersion);
        }
        String projectPath = filepath.split("\\.gradle")[0];
        String propertiesPath = projectPath + "gradle\\wrapper\\gradle-wrapper.properties";
        File propertiesFile = new File(propertiesPath);
        if (propertiesFile.exists()) {
            try {
                RandomAccessFile file = new RandomAccessFile(propertiesFile, "r");
                while (true) {
                    String line = file.readLine();
                    if (line.isEmpty()) break;
                    if (line.startsWith("distributionUrl")) {
                        return line.split("gradle-")[1].split("-bin")[0];
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

}
