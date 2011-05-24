package p42svn;

import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.server.IServer;

import java.io.File;
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

    private P4 p4 = new P4();

    private Map<String, String> branches = new HashMap<String, String>();
    private boolean mungeKeyword;
    private boolean convertEOL;

    private TimeZone timeZone = TimeZone.getDefault();

    private String changelistsDumpDirectoryPath = "tmp";
    private String dumpFileName = "dump";
    private int simultaneousProcessedChangelistCount = 10;

    private boolean addOriginalChangeListId = false;
    private String originalChangeListInfoString = "Converted from original Perforce changelist ";

    private RevisionManager revisionManager = new RevisionManager();
    private EventDispatcher eventDispatcher = new EventDispatcher();
    private FilesManager filesManager = new FilesManager();

    public void dumpChangeLists() throws Exception {
        System.out.println("Start");

        TimeZone.setDefault(timeZone);

        File changeListsDumpDirectory = new File(changelistsDumpDirectoryPath);
        changeListsDumpDirectory.mkdirs();

        ExecutorService executorService = Executors.newFixedThreadPool(simultaneousProcessedChangelistCount);

        List<IChangelistSummary> changeListSummaries = p4GetChanges();
        CountDownLatch cdl = new CountDownLatch(changeListSummaries.size());

        System.out.println(changeListSummaries.size());

        for (IChangelistSummary changeListSummary : changeListSummaries) {
            revisionManager.putChangeListIdIntoQueue(changeListSummary.getId());

            executorService.submit(new ChangeListProcessorRunnableWrapper(new ChangeListProcessor(this, changeListSummary), cdl));
        }
        cdl.await();
        executorService.shutdown();
        System.out.println("Finish");
        eventDispatcher.afterDumping();
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
                sorted.put(changelistSummary.getId(), changelistSummary);
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

    public FilesManager getFilesManager() {
        return filesManager;
    }

}
