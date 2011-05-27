package p42svn;

public interface RevisionManager {

    void putChangeListIdIntoQueue(int changeListId);

    int getMaxChangeListId();

    int createRevisionIdForChangeListId(int changeListId);

    void skipRevisionIdForChangeListId(int changeListId);

    int getRevisionIdForChangeListId(int changeListId);

}