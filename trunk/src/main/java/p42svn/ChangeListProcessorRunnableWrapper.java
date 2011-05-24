package p42svn;

import java.util.concurrent.CountDownLatch;

/**
 * @author Pavel Belevich
 *         Date: 5/14/11
 *         Time: 8:57 PM
 */
public class ChangeListProcessorRunnableWrapper implements Runnable {

    private ChangeListProcessor changeListProcessor;
    private CountDownLatch cdl;

    public ChangeListProcessorRunnableWrapper(ChangeListProcessor changeListProcessor, CountDownLatch cdl) {
        this.changeListProcessor = changeListProcessor;
        this.cdl = cdl;
    }

    public void run() {
        try {
            changeListProcessor.process();
        } catch (ChangeListProcessorException e) {
            e.printStackTrace();
            System.exit(-1);
        } finally {
            cdl.countDown();
        }
    }
}
