package p42svn;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Properties;

/**
 * @author Pavel Belevich
 *         Date: 5/24/11
 *         Time: 7:17 PM
 */
public final class SVNUtils {

    private SVNUtils() {
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

    public static void svnDelete(OutputStream outputStream, String path/*, ConcurrentMap<String, Integer> dirUsage*/) throws Exception {
        PrintWriter printWriter = new PrintWriter(outputStream);
        printWriter.print("Node-path: " + path + "\n");
        printWriter.print("Node-action: delete\n");
        printWriter.print("\n");
        printWriter.flush();
    }
}
