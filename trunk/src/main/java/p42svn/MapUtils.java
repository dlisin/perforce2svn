package p42svn;

import java.io.*;
import java.util.Map;

/**
 * @author Pavel Belevich
 *         Date: 5/24/11
 *         Time: 7:23 PM
 */
public class MapUtils {
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

    public static class ChangeInfoConverter implements Converter<SVNListener.ChangeInfo> {

        public String toString(SVNListener.ChangeInfo s) {
            return s.getFilePath() + ":" + s.getAction();
        }

        public SVNListener.ChangeInfo fromString(String s) {
            String[] split = s.split(":");
            return new SVNListener.ChangeInfo(split[0],split[1]);
        }
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
}
