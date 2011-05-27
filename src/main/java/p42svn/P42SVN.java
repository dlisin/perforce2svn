package p42svn;

import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.server.IServer;

import java.io.File;
import java.nio.charset.Charset;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Pavel Belevich
 *         Date: 5/14/11
 *         Time: 8:59 PM
 */
public class P42SVN {

    private static final String KEY_LAST_CHANGELIST = "dump.last.changelist.id";

    private P4 p4 = new P4();

    private Map<String, String> branches = new HashMap<String, String>();
    private boolean mungeKeyword;
    private boolean convertEOL;

    private TimeZone timeZone = TimeZone.getDefault();
    private Charset charset;

    private String changelistsDumpDirectoryPath = "tmp";
    private String dumpFileName = "dump";
    private int simultaneousProcessedChangelistCount = 10;

    private boolean addOriginalChangeListId = false;
    private String originalChangeListInfoString = "Converted from original Perforce changelist ";

    private RevisionManager revisionManager = new SimpleRevisionManager();
    private EventDispatcher eventDispatcher = new EventDispatcher();
    private FilesManager filesManager = new FilesManager();

    private int fromChangeList = 0;
    private int toChangeList = Integer.MAX_VALUE;
    private String previousDumpPath = null;

    public void dumpChangeLists() throws Exception {
        System.out.println("Start");

        TimeZone.setDefault(timeZone);

        File changeListsDumpDirectory = new File(changelistsDumpDirectoryPath);
        changeListsDumpDirectory.mkdirs();

        ExecutorService executorService = Executors.newFixedThreadPool(simultaneousProcessedChangelistCount);

        List<IChangelistSummary> changeListSummaries = p4GetChanges();
        CountDownLatch cdl = new CountDownLatch(changeListSummaries.size());

        System.out.println("Total changelists: " + changeListSummaries.size());

        for (IChangelistSummary changeListSummary : changeListSummaries) {
            revisionManager.putChangeListIdIntoQueue(changeListSummary.getId());
        }

        for (IChangelistSummary changeListSummary : changeListSummaries) {
            executorService.submit(new ChangeListProcessorRunnableWrapper(new ChangeListProcessor(this, changeListSummary), cdl));
        }
        cdl.await();
        executorService.shutdown();
        System.out.println("Finish");

        Properties props = new Properties();
        props.setProperty(KEY_LAST_CHANGELIST, String.valueOf(changeListSummaries.get(changeListSummaries.size()-1).getId()));
        saveMetaInfo(props);

        eventDispatcher.afterDumping();
    }

    public void applyPropertiesFromPreviousDump() {
        try {
            Properties p = loadPreviousDumpMetaInfo();
            fromChangeList = Integer.parseInt(p.getProperty(KEY_LAST_CHANGELIST));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load meta-info from previous dump.", e);
        }
    }


    private Properties loadPreviousDumpMetaInfo() throws Exception {
        Properties props = new Properties();
        Reader metaInfoReader = new FileReader(new File(getPreviousDumpPath(), "meta-info"));
        props.load(metaInfoReader);
        metaInfoReader.close();
        return props;
    }

    private void saveMetaInfo(Properties props) throws Exception {
        Writer metaInfoWriter = new FileWriter(new File(getChangelistsDumpDirectoryPath(), "meta-info"));
        props.store(metaInfoWriter, "Dumping properties");
        metaInfoWriter.close();
    }

    public List<IChangelistSummary> p4GetChanges() {
        try {
            IServer server = p4.getServer();
            Collection<String> branches = new ArrayList<String>(this.branches.keySet().size());
            for (String branch : this.branches.keySet()) {
                branches.add(branch + "...");
            }
            List<IChangelistSummary> changelistSummaries =
                    server.getChangelists(0, FileSpecBuilder.makeFileSpecList(branches.toArray(new String[branches.size()])),
                            null, null, true, null, true);
            SortedMap<Integer, IChangelistSummary> sorted = new TreeMap<Integer, IChangelistSummary>();
            for (IChangelistSummary changelistSummary : changelistSummaries) {
                if (changelistSummary.getId() > fromChangeList
                        && changelistSummary.getId() <= toChangeList) {
                    sorted.put(changelistSummary.getId(), changelistSummary);
                }
            }

            return new ArrayList<IChangelistSummary>(sorted.values());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void assembleDumpFile() throws Exception {
        eventDispatcher.assembleDump();
    }

    public RevisionManager getRevisionManager() {
        return revisionManager;
    }

    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    public P4 getP4() {
        return p4;
    }

    public int getSimultaneousProcessedChangelistCount() {
        return simultaneousProcessedChangelistCount;
    }

    public void setSimultaneousProcessedChangelistCount(int simultaneousProcessedChangelistCount) {
        this.simultaneousProcessedChangelistCount = simultaneousProcessedChangelistCount;
    }

    public String getChangelistsDumpDirectoryPath() {
        return changelistsDumpDirectoryPath;
    }

    public void setChangelistsDumpDirectoryPath(String changelistsDumpDirectoryPath) {
        this.changelistsDumpDirectoryPath = changelistsDumpDirectoryPath;
    }

    public String getDumpFileName() {
        return dumpFileName;
    }

    public void setDumpFileName(String dumpFileName) {
        this.dumpFileName = dumpFileName;
    }

    public boolean isMungeKeyword() {
        return mungeKeyword;
    }

    public void setMungeKeyword(boolean mungeKeyword) {
        this.mungeKeyword = mungeKeyword;
    }

    public boolean isConvertEOL() {
        return convertEOL;
    }

    public void setConvertEOL(boolean convertEOL) {
        this.convertEOL = convertEOL;
    }

    public Map<String, String> getBranches() {
        return branches;
    }

    public void setBranches(Map<String, String> branches) {
        this.branches = branches;
    }

    public boolean isAddOriginalChangeListId() {
        return addOriginalChangeListId;
    }

    public void setAddOriginalChangeListId(boolean addOriginalChangeListId) {
        this.addOriginalChangeListId = addOriginalChangeListId;
    }

    public String getOriginalChangeListInfoString() {
        return originalChangeListInfoString;
    }

    public void setOriginalChangeListInfoString(String originalChangeListInfoString) {
        this.originalChangeListInfoString = originalChangeListInfoString;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public FilesManager getFilesManager() {
        return filesManager;
    }

    public void setFromChangeList(int fromChangeList) {
        this.fromChangeList = fromChangeList;
    }

    public void setToChangeList(int toChangeList) {
        this.toChangeList = toChangeList;
    }

    public String getPreviousDumpPath() {
        return previousDumpPath;
    }

    public void setPreviousDumpPath(String previousDumpPath) {
        this.previousDumpPath = previousDumpPath;
    }



}
