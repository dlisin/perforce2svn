package p42svn;

/**
* @author Pavel Belevich
*         Date: 5/30/11
*         Time: 2:03 PM
*/
public class ChangeInfo {
    private String filePath;
    private String action;

    public ChangeInfo(String filePath, String action) {
        this.filePath = filePath;
        this.action = action;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getAction() {
        return action;
    }
}
