package p42svn;

/**
 * @author Pavel Belevich
 *         Date: 5/14/11
 *         Time: 9:10 PM
 */
public class ChangeListProcessorException extends Exception {

    public ChangeListProcessorException() {
    }

    public ChangeListProcessorException(String message) {
        super(message);
    }

    public ChangeListProcessorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChangeListProcessorException(Throwable cause) {
        super(cause);
    }
}
