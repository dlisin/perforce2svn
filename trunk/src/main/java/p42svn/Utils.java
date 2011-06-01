package p42svn;

import com.perforce.p4java.core.file.IFileSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author Pavel Belevich
 *         Date: 5/8/11
 *         Time: 6:01 AM
 */
public final class Utils {

    private Utils() {
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

    public static String depotToSVNPath(Map<String, String> branches, String depotPath) {
        for (String perforceBranch : branches.keySet()) {
            if (depotPath.startsWith(perforceBranch)) {
                String s = branches.get(perforceBranch) + depotPath.substring(perforceBranch.length());
                return s.replaceAll("\\xa0", "\u00c2\u00a0");         //Dirty fix
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

    public static String fileSpecToString(IFileSpec fileSpec) {
        if (fileSpec == null) {
            return "<null>";
        }
        String params = "\nFileSpec props: " + new ToStringBuilder(fileSpec)
        .append("getBaseRev()", fileSpec.getBaseRev())
        .append("getEndFromRev()", fileSpec.getEndFromRev())
        .append("getEndRevision()", fileSpec.getEndRevision())
        .append("getEndToRev()", fileSpec.getEndToRev())
        .append("getFromFile()", fileSpec.getFromFile())
        .append("getOriginalPathString()", fileSpec.getOriginalPathString())
        .append("getStartFromRev()", fileSpec.getStartFromRev())
        .append("getStartRevision()", fileSpec.getStartRevision())
        .append("getStartToRev()", fileSpec.getStartToRev())
        .append("getToFile()", fileSpec.getToFile())
        .append("isLocked()", fileSpec.isLocked())
        .toString();
        String objectDump = "\nFileSpec Reflection: " + ToStringBuilder.reflectionToString(fileSpec);
        return params + objectDump;
    }

}
