package p42svn;

/**
 * @author Pavel Belevich
 *         Date: 5/7/11
 *         Time: 5:12 PM
 */
public class FileProcessorException extends Exception {

    public FileProcessorException() {
    }

    public FileProcessorException(String message) {
        super(message);
    }

    public FileProcessorException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileProcessorException(Throwable cause) {
        super(cause);
    }
}
