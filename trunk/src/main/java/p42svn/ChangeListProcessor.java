package p42svn;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.server.IServer;

import java.util.List;

import static p42svn.Utils.*;

/**
 * @author Pavel Belevich
 *         Date: 5/14/11
 *         Time: 8:58 PM
 */
public class ChangeListProcessor {

    private P42SVN p42SVN;

    private IChangelistSummary changeListSummary;

    public ChangeListProcessor(P42SVN p42SVN, IChangelistSummary changeListSummary) {
        this.p42SVN = p42SVN;
        this.changeListSummary = changeListSummary;
    }

    public void process() throws ChangeListProcessorException {
        try {

            IChangelist changeList = getChangelistDetails(changeListSummary.getId());

            List<IFileSpec> allFileSpecs = changeList.getFiles(true);
            List<IFileSpec> filteredFileSpecs = filterWantedFiles(p42SVN.getBranches(), allFileSpecs);
            if (!filteredFileSpecs.isEmpty()) {
                int revisionId = p42SVN.getRevisionManager().createRevisionIdForChangeListId(changeList.getId());
                p42SVN.getEventDispatcher().handleChangeList(changeList);
                for (IFileSpec fileSpec : filteredFileSpecs) {
                    p42SVN.getEventDispatcher().handleFile(fileSpec);
                }
                System.out.println(revisionId);
            } else {
                p42SVN.getRevisionManager().skipRevisionIdForChangeListId(changeList.getId());
            }
        } catch (Exception e) {
            throw new ChangeListProcessorException(e);
        }
    }

    public IChangelist getChangelistDetails(int changeId) throws Exception {
        IServer server = p42SVN.getP4().getServer();
        return server.getChangelist(changeId);
    }

}
