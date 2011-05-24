package p42svn;

/**
 * @author Pavel Belevich
 *         Date: 5/7/11
 *         Time: 6:33 PM
 */
public class P4Exception extends Exception {

    public P4Exception() {
    }

    public P4Exception(String message) {
        super(message);
    }

    public P4Exception(String message, Throwable cause) {
        super(message, cause);
    }

    public P4Exception(Throwable cause) {
        super(cause);
    }
}
