package p42svn;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Pavel Belevich
 *         Date: 5/14/11
 *         Time: 10:18 PM
 */
public class EventDispatcher {

    private Collection<Listener> listeners = new ArrayList<Listener>();

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void handleChangeList(IChangelist changeList) throws Exception {
        for (Listener listener : listeners) {
            listener.handleChangeList(changeList);
        }
    }

    public void handleFile(IFileSpec fileSpec) throws Exception {
        for (Listener listener : listeners) {
            listener.handleFile(fileSpec);
        }
    }

    public void afterDumping() throws Exception {
        for (Listener listener : listeners) {
            listener.afterDumping();
        }
    }

    public void assembleDump() throws Exception {
        for (Listener listener : listeners) {
            listener.assembleDump();
        }
    }

}
