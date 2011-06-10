package p42svn;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.*;
import com.perforce.p4java.exception.RequestException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.perforce.p4java.core.file.FileAction.*;
import static p42svn.ConcurrentMapUtils.*;
import static p42svn.SVNUtils.svnPropertiesToString;

/**
 * @author Pavel Belevich
 *         Date: 5/14/11
 *         Time: 10:22 PM
 */
public class SVNListener implements Listener {

    private static final Map<String, String> KEYWORD_MAP = new HashMap<String, String>() {{
        put("Author", "LastChangedBy");
        put("Date", "LastChangedDate");
        put("Revision", "LastChangedRevision");
        put("File", "HeadURL");
        put("Id", "Id");
    }};

    private P42SVN p42SVN;

    private ConcurrentMap<Integer, File> changelistDumpDirsByChangeListId =
            new ConcurrentHashMap<Integer, File>();
    private ConcurrentMap<Integer, Integer> partByChangeListId =
            new ConcurrentHashMap<Integer, Integer>();
    private ConcurrentMap<Integer, ConcurrentMap<String, Integer>> localDirSeenByChangeListId =
            new ConcurrentHashMap<Integer, ConcurrentMap<String, Integer>>();

//    private Map<File, ChangeInfo> directories = Collections.synchronizedMap(new HashMap<File, ChangeInfo>());

    public SVNListener(P42SVN p42SVN) {
        this.p42SVN = p42SVN;
        if (!StringUtils.isEmpty(p42SVN.getPreviousDumpPath())) {
            try {
                loadDirectoriesAndFilesMetaInfo(p42SVN.getPreviousDumpPath());
                loadDirStatusesAndFileStatuses(p42SVN.getPreviousDumpPath());
//                restoreFilesStatuses();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

//    private void restoreFilesStatuses() {
//        for (Entry<File, ChangeInfo> entry : p42SVN.getFilesManager().getFiles().entrySet()) {
//            ChangeInfo changeInfo = entry.getValue();
//            if ("Add".equals(changeInfo.getAction()) || "Add Copy".equals(changeInfo.getAction())) {
//                inc(p42SVN.getFilesManager().getFileStatuses(), changeInfo.getFilePath());
//            }
//        }
//    }

    public void handleChangeList(IChangelist changeList) throws Exception {
        File changelistDumpDir = new File(p42SVN.getChangelistsDumpDirectoryPath(),
                String.valueOf(changeList.getId()));
        changelistDumpDirsByChangeListId.put(changeList.getId(), changelistDumpDir);
        if (p42SVN.isReuseChangelist()) {
            return;
        }
        FileUtils.deleteQuietly(changelistDumpDir);
        changelistDumpDir.mkdirs();   //TODO Warning!
        Properties properties = new Properties();
        String description = changeList.getDescription();
        if (p42SVN.isAddOriginalChangeListId()) {
            description += "\n" + p42SVN.getOriginalChangeListInfoString() + changeList.getId();
        }

        properties.put("svn:log", description);
        properties.put("svn:author", changeList.getUsername());

        SimpleDateFormat svnDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000000Z'");
        svnDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        properties.put("svn:date", svnDateFormat.format(changeList.getDate()));
        writeSVNRevision(changelistDumpDir, properties,
                p42SVN.getRevisionManager().getRevisionIdForChangeListId(changeList.getId()));
    }

    private void writeSVNRevision(File changeListDumpDir,
                                  Properties properties,
                                  int revision) throws Exception {
        OutputStream outputStream = new FileOutputStream(
                new File(changeListDumpDir, String.valueOf(0))
        );
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, p42SVN.getCharset()));
        String propertiesText = svnPropertiesToString(properties, p42SVN.getCharset());
        int propertiesLength = propertiesText.getBytes(p42SVN.getCharset()).length + 1;
        printWriter.print("Revision-number: " + revision + "\n");
        printWriter.print("Prop-content-length: " + propertiesLength + "\n");
        printWriter.print("Content-length: " + propertiesLength + "\n");
        printWriter.print("\n");
        printWriter.print(propertiesText + "\n");
        printWriter.print("\n");
        printWriter.flush();
    }

    public void handleFile(IFileSpec fileSpec) throws FileProcessorException {
        try {
            FileAction action = fileSpec.getAction();

            String svnPath = getSVNPath(fileSpec.getDepotPathString());

            if (ADD.equals(action)) {
                p4AddToSVN(fileSpec, svnPath);
            } else if (DELETE.equals(action)) {
                p4DeleteToSVN(fileSpec, svnPath);
            } else if (EDIT.equals(action)) {
                p4EditToSVN(fileSpec, svnPath);
            } else if (BRANCH.equals(action)) {
                p4BranchToSVN(fileSpec, svnPath);
            } else if (INTEGRATE.equals(action)) {
                p4IntegrateToSVN(fileSpec, svnPath);
            } else if (PURGE.equals(action)) {
                p4PurgeToSVN(fileSpec, svnPath);
            } else {
                throw new FileProcessorException("Unknown action");
            }
        } catch (Throwable e) {
            throw new FileProcessorException(Utils.fileSpecToString(fileSpec), e);
        }
    }

    private String getSVNPath(String depotPathString) {
        String svnPath = Utils.depotToSVNPath(p42SVN.getBranches(), depotPathString);

        //It was epic fail
//        if (svnPath != null) {
//            String result = paths.get(svnPath.toLowerCase());
//            if (result == null) {
//                result = paths.putIfAbsent(svnPath.toLowerCase(), svnPath);
//                if (result == null) {
//                    result = svnPath;
//                }
//            }
//            svnPath = result;
//        }

        return svnPath;
    }

    public void afterDumping() throws Exception {

        svnDeleteEmptyParentDirs();

//        Writer directoriesWriter = new FileWriter(new File(p42SVN.getChangelistsDumpDirectoryPath(), "directories"));
//        MapUtils.writeMapChangeInfo(directoriesWriter, this.directories);
//        directoriesWriter.close();

        Writer filesWriter = new FileWriter(new File(p42SVN.getChangelistsDumpDirectoryPath(), "files"));
        MapUtils.writeMapChangeInfo(filesWriter, p42SVN.getFilesManager().getFiles());
        filesWriter.close();

        Writer dirsUsageWriter = new FileWriter(new File(p42SVN.getChangelistsDumpDirectoryPath(), "dirsUsage"));
        MapUtils.writeMapStringAtomicInteger(dirsUsageWriter, p42SVN.getFilesManager().getDirsUsage());
        dirsUsageWriter.close();

        Writer revisionByChangeListIdWriter = new FileWriter(new File(p42SVN.getChangelistsDumpDirectoryPath(), "revisionByChangeListId"));
        MapUtils.writeMapInteger(revisionByChangeListIdWriter, p42SVN.getRevisionManager().getRevisionByChangeListId());
        revisionByChangeListIdWriter.close();

        Writer changeListsWriter = new FileWriter(new File(p42SVN.getChangelistsDumpDirectoryPath(), "changeLists"));
        MapUtils.writeListInteger(changeListsWriter, p42SVN.getRevisionManager().getChangeLists());
        changeListsWriter.close();
    }

    public void svnDeleteEmptyParentDirs() throws Exception {
        List<String> toDelete = new ArrayList<String>();
        for (String path : p42SVN.getFilesManager().getDirsUsage().keySet()) {
            int pathUsage = p42SVN.getFilesManager().getDirsUsage().get(path).intValue();
            if (pathUsage == 0) {
                toDelete.add(path);
            }
        }
        Collections.sort(toDelete);
        Collections.reverse(toDelete);

        int changeListId = p42SVN.getRevisionManager().getMaxChangeListId() + 1;

//        p42SVN.getRevisionManager().putChangeListIdIntoQueue(changeListId);
        int svnRevision = p42SVN.getRevisionManager().createRevisionId();

        File changeListDumpDir = new File(p42SVN.getChangelistsDumpDirectoryPath(),
                String.valueOf(changeListId));
        changeListDumpDir.mkdirs(); //TODO Warning!

        Properties properties = new Properties();
        properties.put("svn:log", "Deleting Empty Parent Directories");
        properties.put("svn:author", "p42svn");

        SimpleDateFormat svnDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000000Z'");
        svnDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        properties.put("svn:date", svnDateFormat.format(new Date()));
        writeSVNRevision(changeListDumpDir, properties, svnRevision);

        int fileNumber = 0;
        for (String deletedFile : toDelete) {
            File file = new File(changeListDumpDir, String.valueOf(++fileNumber));
            OutputStream outputStream = new FileOutputStream(file);
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, p42SVN.getCharset()));
            SVNUtils.svnDelete(printWriter, deletedFile);
            p42SVN.getFilesManager().getFiles().put(file, new ChangeInfo(deletedFile, "Delete"));
        }
    }

    private void loadDirectoriesAndFilesMetaInfo(String baseFir) throws Exception {
//        Reader directoriesReader = new FileReader(new File(baseFir, "directories"));
//        directories.clear();
//        directories = MapUtils.readMapChangeInfo(directoriesReader, directories);
//        directoriesReader.close();

        Reader filesReader = new FileReader(new File(baseFir, "files"));
        p42SVN.getFilesManager().getFiles().clear();
        MapUtils.readMapChangeInfo(filesReader, p42SVN.getFilesManager().getFiles());
        filesReader.close();

        Reader dirsUsageReader = new FileReader(new File(baseFir, "dirsUsage"));
        p42SVN.getFilesManager().getDirsUsage().clear();
        MapUtils.readMapStringAtomicInteger(dirsUsageReader, p42SVN.getFilesManager().getDirsUsage());
        dirsUsageReader.close();

        Reader revisionByChangeListIdReader = new FileReader(new File(baseFir, "revisionByChangeListId"));
        p42SVN.getRevisionManager().getRevisionByChangeListId().clear();
        MapUtils.readMapInteger(revisionByChangeListIdReader, p42SVN.getRevisionManager().getRevisionByChangeListId());
        revisionByChangeListIdReader.close();

        Reader changeListsReader = new FileReader(new File(baseFir, "changeLists"));
        p42SVN.getRevisionManager().getChangeLists().clear();
        MapUtils.readListInteger(changeListsReader, p42SVN.getRevisionManager().getChangeLists());
        changeListsReader.close();
    }

    private void loadDirStatusesAndFileStatuses(String baseFir) throws IOException {
//        Reader dirStatusesReader = new FileReader(new File(baseFir, "dirStatuses"));
//        dirStatuses.clear();
//        dirStatuses = new ConcurrentHashMap<String, Integer>(MapUtils.readMapStringInteger(dirStatusesReader, dirStatuses));
//        dirStatusesReader.close();

        Reader fileStatusesReader = new FileReader(new File(baseFir, "fileStatuses"));
        p42SVN.getFilesManager().getFileStatuses().clear();
        p42SVN.getFilesManager().getFileStatuses().putAll(MapUtils.readMapStringInteger(fileStatusesReader, p42SVN.getFilesManager().getFileStatuses()));
        fileStatusesReader.close();
    }

    public void assembleDump() throws Exception {
        loadDirectoriesAndFilesMetaInfo(p42SVN.getChangelistsDumpDirectoryPath());

        File changeListsDumpDirectory = new File(p42SVN.getChangelistsDumpDirectoryPath());
//        OutputStream outputStream = new FileOutputStream(p42SVN.getDumpFileName());
//        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, p42SVN.getCharset()));
//        printWriter.print("SVN-fs-dump-format-version: 1\n\n");
//        printWriter.flush();

        File[] changeListDumpDirectories = changeListsDumpDirectory.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                boolean isNumber = true;
                try {
                    Integer.valueOf(pathname.getName());
                } catch (NumberFormatException e) {
                    isNumber = false;
                }
                return pathname.isDirectory() && isNumber;
            }
        });
        Arrays.sort(changeListDumpDirectories, new Comparator<File>() {
            public int compare(File file1, File file2) {
                return Integer.valueOf(file1.getName()) - Integer.valueOf(file2.getName());
            }
        });

        int changeListCounter = 0;
        int dumpsCounter = 0;
        OutputStream outputStream = null;

        for (File changeListDumpDirectory : changeListDumpDirectories) {

            if ((p42SVN.getSplitBy() == 0 && changeListCounter == 0) || (p42SVN.getSplitBy() != 0 && changeListCounter % p42SVN.getSplitBy() == 0)) {
                if (outputStream != null) {
                    outputStream.close();
                }
                outputStream = new FileOutputStream(new File(p42SVN.getDumpFileName() + (p42SVN.getSplitBy() != 0 ? ++dumpsCounter : "")));
                PrintWriter printWriter = new PrintWriter(outputStream);
                printWriter.print("SVN-fs-dump-format-version: 1\n\n");
                printWriter.flush();
            }

            File[] parts = changeListDumpDirectory.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    boolean isNumber = true;
                    try {
                        Integer.valueOf(pathname.getName());
                    } catch (NumberFormatException e) {
                        isNumber = false;
                    }
                    return pathname.isFile() && isNumber;
                }
            });
            Arrays.sort(parts, new Comparator<File>() {
                public int compare(File file1, File file2) {
                    return Integer.valueOf(file1.getName()) - Integer.valueOf(file2.getName());
                }
            });

            for (File partFile : parts) {
                boolean allow = true;

//                ChangeInfo dirPath = directories.get(partFile);
//                if (dirPath != null) {
//                    allow = getAndInc(dirStatuses, dirPath) == 0;
//                }

                ChangeInfo changeInfo = p42SVN.getFilesManager().getFiles().get(partFile);
                if (changeInfo != null) {
                    int status = get(p42SVN.getFilesManager().getFileStatuses(), changeInfo.getFilePath());
                    if ("Add".equals(changeInfo.getAction()) || "Add Copy".equals(changeInfo.getAction())) {
                        if (status == 0) {
                            inc(p42SVN.getFilesManager().getFileStatuses(), changeInfo.getFilePath());
                            allow = true;
                        } else {
                            allow = false;
                        }
                    } else if ("Delete".equals(changeInfo.getAction())) {
                        if (status == 1) {
                            dec(p42SVN.getFilesManager().getFileStatuses(), changeInfo.getFilePath());
                            allow = true;
                        } else {
                            allow = false;
                        }
                    } else if ("Edit".equals(changeInfo.getAction()) || "Replace Copy".equals(changeInfo.getAction())) {
                        if (status == 1) {
                            allow = true;
                        } else {
                            allow = false;
                        }
                    }
                }

                if (allow) {
                    byte[] bytes = IOUtils.toByteArray(new FileInputStream(partFile));
                    outputStream.write(bytes);
                    outputStream.flush();
                }
            }
            changeListCounter++;
        }

        if (outputStream != null) {
            outputStream.close();
        }

//        Writer dirStatusesReader = new FileWriter(new File(p42SVN.getChangelistsDumpDirectoryPath(), "dirStatuses"));
//        MapUtils.writeMapStringInteger(dirStatusesReader, this.dirStatuses);
//        dirStatusesReader.close();

        Writer fileStatusesReader = new FileWriter(new File(p42SVN.getChangelistsDumpDirectoryPath(), "fileStatuses"));
        MapUtils.writeMapStringInteger(fileStatusesReader, p42SVN.getFilesManager().getFileStatuses());
        fileStatusesReader.close();

    }

//=================================================================================================

    private void p4AddToSVN(IFileSpec fileSpec, String svnPath) throws Exception {
        svnAddParentDirs(fileSpec.getChangelistId(), svnPath);
        p42SVN.getFilesManager().incUsage(svnPath);
        String type = fileSpec.getFileType();
        byte[] content = null;
        Properties properties = null;

        if (!p42SVN.isReuseChangelist()) {
            content = p4GetFileContent(fileSpec);
            if (p4HasTextFlag(type)) {
                content = mungeKeyword(content);
            }
            properties = properties(type, content);
        }

        if (type.contains("symlink")) {
            svnAddSymlink(fileSpec.getChangelistId(), svnPath, properties, content);
        } else {
            svnAddFile(fileSpec.getChangelistId(), svnPath, properties, content);
        }
    }

    private void p4DeleteToSVN(IFileSpec fileSpec, String svnPath) throws Exception {
        p42SVN.getFilesManager().decUsage(svnPath);
        svnDelete(fileSpec.getChangelistId(), svnPath);
//        deletedFiles.add(svnPath);
    }

    private void p4EditToSVN(IFileSpec fileSpec, String svnPath) throws Exception {
        String type = fileSpec.getFileType();
        byte[] content = null;
        Properties properties = null;

        if (!p42SVN.isReuseChangelist()) {
            content = p4GetFileContent(fileSpec);
            if (p4HasTextFlag(type)) {
                content = mungeKeyword(content);
            }
            properties = properties(type, content);
        }

        if (type.contains("symlink")) {
            svnEditSymlink(fileSpec.getChangelistId(), svnPath, properties, content);
        } else {
            svnEditFile(fileSpec.getChangelistId(), svnPath, properties, content);
        }
    }

    private void p4BranchToSVN(IFileSpec fileSpec, String svnPath) throws Exception {
        if (p42SVN.isReuseChangelist()) {
            //For restore mode there is no difference in which way the file has been added
            //This call p4AddToSVN will not affect resulting dump file
            p4AddToSVN(fileSpec, svnPath);
            return;
        }
        IFileSpec fromFileSpec = p4GetCopyFromFileRevision(fileSpec);
        String fromSvnPath = fromFileSpec != null && fromFileSpec.getDepotPathString() != null ? getSVNPath(fromFileSpec.getDepotPathString()) : null;
        int fromChangeListId = fromFileSpec != null && fromFileSpec.getDepotPathString() != null ? fromFileSpec.getChangelistId() : 0;
        int fromSvnRevision = fromChangeListId != 0 ? p42SVN.getRevisionManager().getRevisionIdForChangeListId(fromChangeListId) : 0;
        if (fromFileSpec != null && fromFileSpec.getDepotPathString() != null && p4FilesAreIdentical(fileSpec, fromFileSpec) && fromSvnPath != null && fromSvnRevision != 0) {
            p42SVN.getFilesManager().incUsage(svnPath);
            svnAddParentDirs(fileSpec.getChangelistId(), svnPath);
            svnAddCopy(fileSpec.getChangelistId(), svnPath, fromSvnPath, fromSvnRevision);
        } else {
            p4AddToSVN(fileSpec, svnPath);
        }
    }

    private void p4IntegrateToSVN(IFileSpec fileSpec, String svnPath) throws Exception {
        if (p42SVN.isReuseChangelist()) {
            //For restore mode there is no difference in which way the file has been added
            //This call p4EditToSVN will not affect resulting dump file
            p4EditToSVN(fileSpec, svnPath);
            return;
        }
        IFileSpec fromFileSpec = p4GetCopyFromFileRevision(fileSpec);
        String fromSvnPath = fromFileSpec != null && fromFileSpec.getDepotPathString() != null ? getSVNPath(fromFileSpec.getDepotPathString()) : null;
        int fromChangeListId = fromFileSpec != null && fromFileSpec.getDepotPathString() != null ? fromFileSpec.getChangelistId() : 0;
        int fromSvnRevision = fromChangeListId != 0 ? p42SVN.getRevisionManager().getRevisionIdForChangeListId(fromChangeListId) : 0;
        if (fromFileSpec != null && fromFileSpec.getDepotPathString() != null && p4FilesAreIdentical(fileSpec, fromFileSpec) && fromSvnPath != null && fromSvnRevision != 0) {
            svnReplaceCopy(fileSpec.getChangelistId(), svnPath, fromSvnPath, fromSvnRevision);
        } else {
            p4EditToSVN(fileSpec, svnPath);
        }
    }

    private void p4PurgeToSVN(IFileSpec fileSpec, String svnPath) throws Exception {
        svnAddParentDirs(fileSpec.getChangelistId(), svnPath);
        p42SVN.getFilesManager().incUsage(svnPath);
        byte[] content = "Placeholder for file purged by Perforce.".getBytes();
        String type = fileSpec.getFileType();
        Properties properties = properties(type, content);
        svnAddFile(fileSpec.getChangelistId(), svnPath, properties, content);
    }

//=================================================================================================

    private byte[] p4GetFileContent(IFileSpec filespec) {
        try {
            return IOUtils.toByteArray(filespec.getContents(true));
        } catch (RequestException re) {
            if (re.toString().contains("No such file or directory")) {
                String msgContentReplacement = "p42svn Import Error: Failed to get file contents, FileSpec: \n"
                    + ToStringBuilder.reflectionToString(filespec) + "\nCaused by: " + re;
                return msgContentReplacement.getBytes();
            } else {
                throw new RuntimeException(re);
            }
        } catch (Exception e) {
            try {
                e.printStackTrace(System.out);
                System.out.println("Trying to reconnect...");
                p42SVN.getP4().getServer(true);
                return IOUtils.toByteArray(filespec.getContents(true));
            } catch (Exception e1) {
                e1.printStackTrace(System.out);
                throw new RuntimeException(e1);
            }
        }
    }

    private void svnAddDir(int changeListId, String path, Properties properties) throws Exception {
        File file = new File(changelistDumpDirsByChangeListId.get(changeListId),
                String.valueOf(getAndInc(partByChangeListId, changeListId, 1))
        );
        p42SVN.getFilesManager().getFiles().put(file, new ChangeInfo(path, "Add"));
        if (p42SVN.isReuseChangelist()) {
            return;
        }

        OutputStream outputStream = new FileOutputStream(file);
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, p42SVN.getCharset()));
        String propertiesText = svnPropertiesToString(properties, p42SVN.getCharset());
        int propertiesLength = propertiesText.getBytes(p42SVN.getCharset()).length + 1; //TODO ???
        printWriter.print("Node-path: " + path + "\n");
        printWriter.print("Node-kind: dir\n");
        printWriter.print("Node-action: add\n");
        printWriter.print("Prop-content-length: " + propertiesLength + "\n");
        printWriter.print("Content-length: " + propertiesLength + "\n");
        printWriter.print("\n");
        printWriter.print(propertiesText + "\n");
        printWriter.print("\n");
        printWriter.close();
    }

    public void svnAddFile(int changeListId, String path, Properties properties, byte[] text) throws Exception {
        File file = new File(changelistDumpDirsByChangeListId.get(changeListId),
                String.valueOf(getAndInc(partByChangeListId, changeListId, 1))
        );
        p42SVN.getFilesManager().getFiles().put(file, new ChangeInfo(path, "Add"));
        if (p42SVN.isReuseChangelist()) {
            return;
        }

        OutputStream outputStream = new FileOutputStream(file);
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, p42SVN.getCharset()));
//        String parentDirectory = Utils.getParentDirectory(path);
//        if (parentDirectory != null) {
//            Utils.inc(dirUsage, parentDirectory);
//        }
        String propertiesText = svnPropertiesToString(properties, p42SVN.getCharset());
        int propertiesLength = propertiesText.getBytes(p42SVN.getCharset()).length + 1; //TODO ???
        int textLength = text.length;
        String textMD5 = md5(text);
        int contentLength = propertiesLength + textLength;

        printWriter.print("Node-path: " + path + "\n");
        printWriter.print("Node-kind: file\n");
        printWriter.print("Node-action: add\n");
        printWriter.print("Text-content-length: " + textLength + "\n");
        printWriter.print("Text-content-md5: " + textMD5 + "\n");
        printWriter.print("Prop-content-length: " + propertiesLength + "\n");
        printWriter.print("Content-length: " + contentLength + "\n");
        printWriter.print("\n");
        printWriter.print(propertiesText + "\n");
        printWriter.flush();
        try {
            outputStream.write(text);
            outputStream.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        printWriter.print("\n");
        printWriter.print("\n");
        printWriter.close();
    }

    public void svnAddSymlink(int changeListId, String path, Properties properties, byte[] text) throws Exception {
        properties.put("svn:special", "*");
        text = ("link " + new String(text)).getBytes();
        svnAddFile(changeListId, path, properties, text);
    }

    public void svnEditFile(int changeListId, String path, Properties properties, byte[] text) throws Exception {
        File file = new File(changelistDumpDirsByChangeListId.get(changeListId),
                String.valueOf(getAndInc(partByChangeListId, changeListId, 1))
        );
        p42SVN.getFilesManager().getFiles().put(file, new ChangeInfo(path, "Edit"));
        if (p42SVN.isReuseChangelist()) {
            return;
        }

        OutputStream outputStream = new FileOutputStream(file);
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, p42SVN.getCharset()));
        String propertiesText = svnPropertiesToString(properties, p42SVN.getCharset());
        int propertiesLength = propertiesText.getBytes(p42SVN.getCharset()).length + 1; //TODO ???
        int textLength = text.length;
        String textMD5 = md5(text);
        int contentLength = propertiesLength + textLength;

        printWriter.print("Node-path: " + path + "\n");
        printWriter.print("Node-kind: file\n");
        printWriter.print("Node-action: change\n");
        printWriter.print("Text-content-length: " + textLength + "\n");
        printWriter.print("Text-content-md5: " + textMD5 + "\n");
        printWriter.print("Prop-content-length: " + propertiesLength + "\n");
        printWriter.print("Content-length: " + contentLength + "\n");
        printWriter.print("\n");
        printWriter.print(propertiesText + "\n");
        printWriter.flush();
        try {
            outputStream.write(text);
            outputStream.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        printWriter.print("\n");
        printWriter.print("\n");
        printWriter.close();
    }

    public void svnEditSymlink(int changeListId, String path, Properties properties, byte[] text)
            throws Exception {
        properties.put("svn:special", "*");
        text = ("link " + new String(text)).getBytes();
        svnEditFile(changeListId, path, properties, text);
    }

    public void svnDelete(int changeListId, String path) throws Exception {
        File file = new File(changelistDumpDirsByChangeListId.get(changeListId),
                String.valueOf(getAndInc(partByChangeListId, changeListId, 1))
        );
        p42SVN.getFilesManager().getFiles().put(file, new ChangeInfo(path, "Delete"));
        if (p42SVN.isReuseChangelist()) {
            return;
        }
        OutputStream outputStream = new FileOutputStream(file);
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, p42SVN.getCharset()));
        SVNUtils.svnDelete(printWriter, path);
        outputStream.close();
    }

    public void svnAddCopy(int changeListId, String path, String fromPath, int fromRevision) throws Exception {
        File file = new File(changelistDumpDirsByChangeListId.get(changeListId),
                String.valueOf(getAndInc(partByChangeListId, changeListId, 1))
        );
        p42SVN.getFilesManager().getFiles().put(file, new ChangeInfo(path, "Add Copy"));
        if (p42SVN.isReuseChangelist()) {
            return;
        }
        OutputStream outputStream = new FileOutputStream(file);
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, p42SVN.getCharset()));
        printWriter.print("Node-path: " + path + "\n");
        printWriter.print("Node-kind: file\n");
        printWriter.print("Node-action: add\n");
        printWriter.print("Node-copyfrom-rev: " + fromRevision + "\n");
        printWriter.print("Node-copyfrom-path: " + fromPath + "\n");
        printWriter.print("\n");
        printWriter.close();
    }

    public void svnReplaceCopy(int changeListId, String path, String fromPath, int fromRevision) throws Exception {
        File file = new File(changelistDumpDirsByChangeListId.get(changeListId),
                String.valueOf(getAndInc(partByChangeListId, changeListId, 1))
        );
        p42SVN.getFilesManager().getFiles().put(file, new ChangeInfo(path, "Replace Copy"));
        if (p42SVN.isReuseChangelist()) {
            return;
        }
        OutputStream outputStream = new FileOutputStream(file);
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, p42SVN.getCharset()));
        printWriter.print("Node-path: " + path + "\n");
        printWriter.print("Node-kind: file\n");
        printWriter.print("Node-action: replace\n");
        printWriter.print("Node-copyfrom-rev: " + fromRevision + "\n");
        printWriter.print("Node-copyfrom-path: " + fromPath + "\n");
        printWriter.print("\n");
        printWriter.close();
    }

//=================================================================================================

    public boolean p4HasKeywordExpansion(String type) {
        return type.matches("^k") || type.matches("\\+.*k");
    }

    public boolean p4HasExecutableFlag(String type) {
        return type.matches("^[cku]?x") || type.matches("\\+.*x");
    }

    public boolean p4HasTextFlag(String type) {
        return type.matches("text|unicode");
    }

//=================================================================================================

//  ########################################################################
//  # Return property list based on Perforce file type and (optionally)
//  # content MIME type.
//  ########################################################################

    public Properties properties(String type, byte[] content) {
        Properties properties = new Properties();
        if (p4HasKeywordExpansion(type)) {
            properties.put("svn:keywords", StringUtils.join(KEYWORD_MAP.values(), " "));
        }
        if (p4HasExecutableFlag(type)) {
            properties.put("svn:executable", "on");
        }
        if (p42SVN.isConvertEOL() && p4HasTextFlag(type)) {
            properties.put("svn:eol-style", "native");
        }
        return properties;
    }

//  ########################################################################
//  # Replace Perforce keywords in file content with equivalent Subversion
//  # keywords.
//  ########################################################################

    public byte[] mungeKeyword(byte[] content) {//suspicious
        if (p42SVN.isMungeKeyword()) {
            String contentString = new String(content);
            for (String key : KEYWORD_MAP.keySet()) {
                String value = KEYWORD_MAP.get(key);
                contentString = contentString.replaceAll(key, value);
            }
            return contentString.getBytes();
        } else {
            return content;
        }
    }

//=================================================================================================

    private void svnAddParentDirs(int changeListId, String path) throws Exception {
        for (String parent : Utils.getParentDirectories(path)) {

            ConcurrentMap<String, Integer> localDirSeen = localDirSeenByChangeListId.get(changeListId);
            if (localDirSeen == null) {
                localDirSeen = new ConcurrentHashMap<String, Integer>();
                localDirSeenByChangeListId.put(changeListId, localDirSeen);
            }

            if ("/".equals(parent)) {
                continue;
            } else {
//                Utils.inc(dirSeen, parent);
                if (getAndInc(localDirSeen, parent) > 0) {
                    continue;
                }
            }

            svnAddDir(changeListId, parent, null);
        }
    }

    public String md5(byte[] text) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(text);
            StringBuffer hexString = new StringBuffer();
            for (byte aByte : bytes) {
                String hex = Integer.toHexString(0xFF & aByte);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private IFileSpec p4GetCopyFromFileRevision(IFileSpec fileSpec) throws Exception {
        IFileSpec fromFileSpec = null;
        Map<IFileSpec, List<IFileRevisionData>> revisionHistory = fileSpec.getRevisionHistory(1, true, true, true, false);
        String fromFile = null;
        int fromRevision = 0;
        for (IFileSpec f : revisionHistory.keySet()) {
            if (f == null || f.getDepotPathString() == null) {
                continue;
            }
            if (f.getDepotPathString().equals(fileSpec.getDepotPathString())) {
                List<IFileRevisionData> fileRevisionDataList = revisionHistory.get(f);
                if (fileRevisionDataList.size() == 1) {
                    IFileRevisionData fileRevisionData = fileRevisionDataList.get(0);
                    List<IRevisionIntegrationData> revisionIntegrationDataList = fileRevisionData.getRevisionIntegrationData();
                    if (revisionIntegrationDataList != null) {
                        for (IRevisionIntegrationData revisionIntegrationData : revisionIntegrationDataList) {
                            if (revisionIntegrationData.getHowFrom().contains("from")) {
                                fromFile = fromFile != null ? fromFile : revisionIntegrationData.getFromFile();
                                fromRevision = fromRevision != 0 ? fromRevision : revisionIntegrationData.getEndFromRev(); //TODO break
                            }
                        }
                    }
                }
            }
        }
        if (fromFile != null && fromRevision != 0) {
            fromFileSpec = p42SVN.getP4().getServer().getDepotFiles(FileSpecBuilder.makeFileSpecList(fromFile + "#" + fromRevision), true).get(0);
        }
        return fromFileSpec;
    }

    private boolean p4FilesAreIdentical(IFileSpec srcfilespec, IFileSpec destFilespec) {
        return ByteBuffer.wrap(p4GetFileContent(srcfilespec)).equals(
                ByteBuffer.wrap(p4GetFileContent(destFilespec)));
    }

}
