package p42svn;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Pavel Belevich
 *         Date: 5/14/11
 *         Time: 10:13 PM
 */
public class RevisionManager {

    private Queue<Integer> changeListsQueue = new LinkedList<Integer>();
    private Collection<Integer> changeLists = new ArrayList<Integer>();
    private Lock lock = new ReentrantLock();
    private Condition condition1 = lock.newCondition();
    private Condition condition2 = lock.newCondition();
    private int revisionId = 0;
    private Map<Integer, Integer> revisionByChangeListId = new HashMap<Integer, Integer>();
    private int maxChangeListId = 0;


    public void putChangeListIdIntoQueue(int changeListId) {
//        lock.lock();
//        try {
        changeListsQueue.add(changeListId);
        changeLists.add(changeListId);
        maxChangeListId = Math.max(maxChangeListId, changeListId);
//            condition1.signalAll();
//        } finally {
//            lock.unlock();
//        }
    }

    public int getMaxChangeListId() {
        return maxChangeListId;
    }

    public int createRevisionIdForChangeListId(int changeListId) {
        return createRevisionIdForChangeListId(changeListId, true);
    }

    public void skipRevisionIdForChangeListId(int changeListId) {
        createRevisionIdForChangeListId(changeListId, false);
    }

    private int createRevisionIdForChangeListId(int changeListId, boolean create) {
        lock.lock();
        try {
            int result = 0;
            if (changeListsQueue.contains(changeListId)) {
                while (!Integer.valueOf(changeListId).equals(changeListsQueue.peek())) {
                    try {
                        condition1.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();  //TODO Warning!
                    }
                }
                changeListsQueue.poll();
                result = /*changeListId;//*/create ? ++revisionId : 0;
                revisionByChangeListId.put(changeListId, result);
                condition1.signalAll();
                condition2.signalAll();
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    public int getRevisionIdForChangeListId(int changeListId) {
        lock.lock();
        try {
            int revisionId = 0;
            if (changeLists.contains(changeListId)) {
                while (!revisionByChangeListId.containsKey(changeListId)) {
                    try {
                        condition2.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();  //TODO Warning!
                    }
                }
                revisionId = revisionByChangeListId.get(changeListId);
            }
            return revisionId;
        } finally {
            lock.unlock();
        }
    }

}
