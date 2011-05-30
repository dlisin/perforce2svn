package p42svn;

import java.util.Collection;
import java.util.Map;

public interface RevisionManager {

    int getRevisionId();

    void setRevisionId(int revisionId);

    Map<Integer, Integer> getRevisionByChangeListId();

    Collection<Integer> getChangeLists();

    void putChangeListIdIntoQueue(int changeListId);

    int getMaxChangeListId();

    int createRevisionIdForChangeListId(int changeListId);

    void skipRevisionIdForChangeListId(int changeListId);

    int createRevisionId();

    int getRevisionIdForChangeListId(int changeListId);

}