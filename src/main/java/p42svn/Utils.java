package p42svn;

import com.perforce.p4java.core.file.IFileSpec;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Pavel Belevich
 *         Date: 5/8/11
 *         Time: 6:01 AM
 */
public final class Utils {

    private Utils() {
    }

    public static String svnPropertiesToString(Properties properties) {
        String result = "";
        if (properties != null) {
            for (Object key : properties.keySet()) {
                Object value = properties.get(key);
                result += "K " + ((String) key).length() + "\n" + key + "\n";
                result += "V " + ((String) value).length() + "\n" + value + "\n";
            }
        }
        result += "PROPS-END";
        return result;
    }

    public static String getParentDirectory(String path) {
        path = path.trim();
        int i = path.lastIndexOf("/", path.length() - 2);
        if (i < 0) {
            return null;
        } else {
            return path.substring(0, i + 1);
        }
    }

    public static <T> int get(ConcurrentMap<T, Integer> map, T key) {
        return get(map, key, 0);
    }

    public static <T> int get(ConcurrentMap<T, Integer> map, T key, Integer defaultValue) {
        Integer result = map.get(key);
        if (result == null) {
            result = map.putIfAbsent(key, defaultValue);
            if (result == null) {
                result = defaultValue;
            }
        }
        return result;
    }

    public static <T> void inc(ConcurrentMap<T, Integer> map, T key) {
        int i;
        do {
            i = get(map, key);
        } while (!map.replace(key, i, i + 1));
    }

    public static <T> int getAndInc(ConcurrentMap<T, Integer> map, T path) {
        return getAndInc(map, path, 0);
    }

    public static <T> int getAndInc(ConcurrentMap<T, Integer> map, T key, Integer defaultValue) {
        int i;
        do {
            i = get(map, key, defaultValue);
        } while (!map.replace(key, i, i + 1));
        return i;
    }

    public static <T> int dec(ConcurrentMap<T, Integer> map, T key) {
        int i;
        do {
            i = get(map, key);
        } while (!map.replace(key, i, i - 1));
        return i - 1;
    }

    public static void svnDelete(OutputStream outputStream, String path/*, ConcurrentMap<String, Integer> dirUsage*/) throws Exception {
        PrintWriter printWriter = new PrintWriter(outputStream);
        printWriter.print("Node-path: " + path + "\n");
        printWriter.print("Node-action: delete\n");
        printWriter.print("\n");
        printWriter.flush();
    }

    public static String depotToSVNPath(Map<String, String> branches, String depotPath) {
        for (String perforceBranch : branches.keySet()) {
            if (depotPath.startsWith(perforceBranch)) {
                String s = branches.get(perforceBranch) + depotPath.substring(perforceBranch.length());
                return s.replaceAll("\\xa0", "\u00c2\u00a0");
            }
        }
        return null;
    }

    public static boolean isWantedFile(Map<String, String> branches, String depotPath) {
        return depotToSVNPath(branches, depotPath) != null;
    }

    public static List<IFileSpec> filterWantedFiles(Map<String, String> branches, List<IFileSpec> fileSpecs) {
        List<IFileSpec> result = new ArrayList<IFileSpec>();
        for (IFileSpec fileSpec : fileSpecs) {
            if (isWantedFile(branches, fileSpec.getDepotPathString())) {
                result.add(fileSpec);
            }
        }
        return result;
    }

    public static String[] getParentDirectories(String path) {
        String parent = Utils.getParentDirectory(path);
        List<String> parents = new ArrayList<String>();
        while (parent != null) {
            parents.add(parent);
            parent = Utils.getParentDirectory(parent);
        }
        String[] result = parents.toArray(new String[parents.size()]);
        Arrays.sort(result);
        return result;
    }

    public static interface Converter<T> {
        String toString(T t);

        T fromString(String s);
    }

    public static class FileConverter implements Converter<File> {

        public String toString(File o) {
            return o.getPath();
        }

        public File fromString(String s) {
            return new File(s);
        }
    }

    public static class StringConverter implements Converter<String> {

        public String toString(String s) {
            return s;
        }

        public String fromString(String s) {
            return s;
        }
    }

    public static class ChangeInfoConverter implements Converter<SVNListener.ChangeInfo> {

        public String toString(SVNListener.ChangeInfo s) {
            return s.getFilePath() + ":" + s.getAction();
        }

        public SVNListener.ChangeInfo fromString(String s) {
            String[] split = s.split(":");
            return new SVNListener.ChangeInfo(split[0],split[1]);
        }
    }

    public static <K, V> void writeMap(Writer writer, Map<K, V> map, Converter<K> keyConverter, Converter<V> valueConverter) {
        PrintWriter printWriter = new PrintWriter(writer);
        for (K key : map.keySet()) {
            V value = map.get(key);
            printWriter.println(keyConverter.toString(key));
            printWriter.println(valueConverter.toString(value));
        }
    }

    public static <K, V> Map<K, V> readMap(Reader reader, Map<K, V> map, Converter<K> keyConverter, Converter<V> valueConverter) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line1;
        String line2;
        while ((line1 = bufferedReader.readLine()) != null && (line2 = bufferedReader.readLine()) != null) {
            map.put(keyConverter.fromString(line1), valueConverter.fromString(line2));
        }
        return map;
    }

    public static void writeMap(Writer writer, Map<File, String> map) {
        writeMap(writer, map, new FileConverter(), new StringConverter());
    }

    public static Map<File, String> readMap(Reader reader, Map<File, String> map) throws IOException {
        return readMap(reader, map, new FileConverter(), new StringConverter());
    }

    public static void writeMapChangeInfo(Writer writer, Map<File, SVNListener.ChangeInfo> map) {
        writeMap(writer, map, new FileConverter(), new ChangeInfoConverter());
    }

    public static Map<File, SVNListener.ChangeInfo> readMapChangeInfo(Reader reader, Map<File, SVNListener.ChangeInfo> map) throws IOException {
        return readMap(reader, map, new FileConverter(), new ChangeInfoConverter());
    }

}
