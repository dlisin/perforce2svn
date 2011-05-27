package p42svn;

/**
 * @author Pavel Belevich
 *         Date: 5/14/11
 *         Time: 10:13 PM
 */
public class SimpleRevisionManager implements RevisionManager {

    private int maxChangeListId = 0;

    public void putChangeListIdIntoQueue(int changeListId) {
        maxChangeListId = Math.max(maxChangeListId, changeListId);
    }

    /* (non-Javadoc)
     * @see p42svn.RevisionManager#getMaxChangeListId()
     */
    public int getMaxChangeListId() {
        return maxChangeListId;
    }

    /* (non-Javadoc)
     * @see p42svn.RevisionManager#createRevisionIdForChangeListId(int)
     */
    public int createRevisionIdForChangeListId(int changeListId) {
        return createRevisionIdForChangeListId(changeListId, true);
    }

    /* (non-Javadoc)
     * @see p42svn.RevisionManager#skipRevisionIdForChangeListId(int)
     */
    public void skipRevisionIdForChangeListId(int changeListId) {
        createRevisionIdForChangeListId(changeListId, false);
    }

    private int createRevisionIdForChangeListId(int changeListId, boolean create) {
        return changeListId;
    }

    /* (non-Javadoc)
     * @see p42svn.RevisionManager#getRevisionIdForChangeListId(int)
     */
    public int getRevisionIdForChangeListId(int changeListId) {
        return changeListId;
    }

}
