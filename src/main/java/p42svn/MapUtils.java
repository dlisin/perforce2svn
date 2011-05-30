package p42svn;

import java.io.*;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

    public static void writeMapFileString(Writer writer, Map<File, String> map) {
        writeMap(writer, map, new FileConverter(), new StringConverter());
    }

    public static Map<File, String> readMapFileString(Reader reader, Map<File, String> map) throws IOException {
        return readMap(reader, map, new FileConverter(), new StringConverter());
    }

    public static void writeMapStringInteger(Writer writer, Map<String, Integer> map) {
        writeMap(writer, map, new StringConverter(), new IntegerConverter());
    }

    public static Map<String, Integer> readMapStringInteger(Reader reader, Map<String, Integer> map) throws IOException {
        return readMap(reader, map, new StringConverter(), new IntegerConverter());
    }

    public static void writeMapStringAtomicInteger(Writer writer, Map<String, AtomicInteger> map) {
        writeMap(writer, map, new StringConverter(), new AtomicIntegerConverter());
    }

    public static Map<String, AtomicInteger> readMapStringAtomicInteger(Reader reader, Map<String, AtomicInteger> map) throws IOException {
        return readMap(reader, map, new StringConverter(), new AtomicIntegerConverter());
    }

    public static void writeMapInteger(Writer writer, Map<Integer, Integer> map) {
        writeMap(writer, map, new IntegerConverter(), new IntegerConverter());
    }

    public static Map<Integer, Integer> readMapInteger(Reader reader, Map<Integer, Integer> map) throws IOException {
        return readMap(reader, map, new IntegerConverter(), new IntegerConverter());
    }

    public static void writeMapChangeInfo(Writer writer, Map<File, ChangeInfo> map) {
        writeMap(writer, map, new FileConverter(), new ChangeInfoConverter());
    }

    public static Map<File, ChangeInfo> readMapChangeInfo(Reader reader, Map<File, ChangeInfo> map) throws IOException {
        return readMap(reader, map, new FileConverter(), new ChangeInfoConverter());
    }

    public static <V> void writeList(Writer writer, Collection<V> list, Converter<V> valueConverter) {
        PrintWriter printWriter = new PrintWriter(writer);
        for (V value : list) {
            printWriter.println(valueConverter.toString(value));
        }
    }

    public static <V> Collection<V> readList(Reader reader, Collection<V> list, Converter<V> valueConverter) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            list.add(valueConverter.fromString(line));
        }
        return list;
    }

    public static void writeListInteger(Writer writer, Collection<Integer> list) {
        writeList(writer, list, new IntegerConverter());
    }

    public static Collection<Integer> readListInteger(Reader reader, Collection<Integer> list) throws IOException {
        return readList(reader, list, new IntegerConverter());
    }

    public static class ChangeInfoConverter implements Converter<ChangeInfo> {

        public String toString(ChangeInfo s) {
            return s.getFilePath() + ":" + s.getAction();
        }

        public ChangeInfo fromString(String s) {
            String[] split = s.split(":");
            return new ChangeInfo(split[0],split[1]);
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

    public static class IntegerConverter implements Converter<Integer> {

        public String toString(Integer s) {
            return String.valueOf(s);
        }

        public Integer fromString(String s) {
            return Integer.valueOf(s);
        }
    }

    public static class AtomicIntegerConverter implements Converter<AtomicInteger> {

        public String toString(AtomicInteger atomicInteger) {
            return String.valueOf(atomicInteger.intValue());
        }

        public AtomicInteger fromString(String s) {
            return new AtomicInteger(Integer.valueOf(s));
        }
    }

}
