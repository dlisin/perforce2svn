package p42svn;

import java.util.concurrent.ConcurrentMap;

/**
 * @author Pavel Belevich
 *         Date: 5/24/11
 *         Time: 7:18 PM
 */
public final class ConcurrentMapUtils {

    private ConcurrentMapUtils() {
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

    public static <T> int get(ConcurrentMap<T, Integer> map, T key) {
        return get(map, key, 0);
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
}
