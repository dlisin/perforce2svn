package p42svn;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pavel Belevich
 *         Date: 5/23/11
 *         Time: 9:04 PM
 */
public class FilesManager {

    private ConcurrentMap<String, AtomicInteger> dirsUsage = new DefaultConcurrentMap<String, AtomicInteger>(
            new ConcurrentHashMap<String, AtomicInteger>(), new DefaultConcurrentMap.DefaultValueFactory<String, AtomicInteger>() {
                public AtomicInteger getDefaultValue(String key) {
                    return new AtomicInteger(0);
                }
            }
    );

    public ConcurrentMap<String, AtomicInteger> getDirsUsage() {
        return dirsUsage;
    }

    public void incUsage(String path) {
        usage(path, true);
    }

    public void decUsage(String path) {
        usage(path, false);
    }

    private void usage(String path, boolean add) {
        for (String parent : Utils.getParentDirectories(path)) {
            if (add) dirsUsage.get(parent).incrementAndGet();
            else dirsUsage.get(parent).decrementAndGet();
        }
    }

}
