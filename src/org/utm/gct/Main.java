package org.utm.gct;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        if (args.length < 2) {
            printHelpMessage();
            return;
        }
        File cacheFile = new File(args[1]);
        String filename = cacheFile.getName().split(".bin")[0];
        GradleCacheTool gct = new GradleCacheTool(filename);
        // todo version (CacheLayout)

        Map<String, String> options = new HashMap<>();
        for (int i = 2; i < args.length; ++i) {
            String[] kvPair = args[i].split("=");
            if (kvPair.length != 2) {
                System.out.println("Invalid option: " + args[i]);
                continue;
            }
            options.put(kvPair[0], kvPair[1]);
        }

        if (args[0].equalsIgnoreCase("dump")) {
            try {
                gct.dump(cacheFile);
            } catch (IOException | ReflectiveOperationException e) {
                e.printStackTrace();
            }
        } else if (args[0].equalsIgnoreCase("remap")) {
            String oldHome = options.get("--old-home").replace("/", "\\");
            String newHome = options.get("--new-home").replace("/", "\\");
            try {
                gct.remap(cacheFile, oldHome, newHome);
            } catch (IOException | ReflectiveOperationException e) {
                e.printStackTrace();
            }
        } else if (args[0].equalsIgnoreCase("add")) {
            ;
        } else if (args[0].equalsIgnoreCase("remove")) {
            ;
        } else {}
    }

    public static void printHelpMessage() {
        System.out.println("Usage: java -jar gct.jar <dump|remap|add|remove> <source> [-<option>=<value>]*");
        System.out.println("Options:");
        System.out.println("[dump]");
        System.out.println("[remap]");
        System.out.println("--old-home\t\tHelloWorld");
        System.out.println("--new-home\t\tHelloWorld");
        System.out.println("-f, --filename\t\tHelloWorld");
        System.out.println("[add]");
        System.out.println("[remove]");
    }

}
