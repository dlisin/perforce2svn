package p42svn;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;

/**
 * @author Pavel Belevich
 *         Date: 5/14/11
 *         Time: 10:03 PM
 */
public interface Listener {

    void handleChangeList(IChangelist changeList) throws Exception;

    void handleFile(IFileSpec fileSpec) throws FileProcessorException;

    void afterDumping() throws Exception;

    void assembleDump() throws Exception;

}
