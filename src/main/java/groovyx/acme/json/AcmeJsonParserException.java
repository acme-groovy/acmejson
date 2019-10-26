package groovyx.acme.json;

public class AcmeJsonParserException extends RuntimeException {
    AcmeJsonParserException(String message) {
        super(message);
    }

    AcmeJsonParserException(String message, Throwable cause) {
        super(message);
    }
}
