package groovyx.acme.json;

public class AcmeJsonException extends RuntimeException {
    AcmeJsonException(String message) {
        super(message);
    }

    AcmeJsonException(String message, Throwable cause) {
        super(message);
    }
}
