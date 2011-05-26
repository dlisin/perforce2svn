package p42svn;

import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * @author Pavel Belevich
 *         Date: 5/24/11
 *         Time: 7:17 PM
 */
public final class SVNUtils {

    private SVNUtils() {
    }

    public static String svnPropertiesToString(Properties properties, Charset charset) {
        String result = "";
        if (properties != null) {
            for (Object key : properties.keySet()) {
                Object value = properties.get(key);
                result += "K " + ((String) key).getBytes(charset).length + "\n" + key + "\n";
                result += "V " + ((String) value).getBytes(charset).length + "\n" + value + "\n";
            }
        }
        result += "PROPS-END";
        return result;
    }

    public static void svnDelete(PrintWriter printWriter, String path) throws Exception {
        printWriter.print("Node-path: " + path + "\n");
        printWriter.print("Node-action: delete\n");
        printWriter.print("\n");
        printWriter.flush();
    }
}
