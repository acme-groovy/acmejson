package groovyx.acme.json;

/**
 * An unchecked exception to indicate that an input does not qualify as valid JSON.
 */
@SuppressWarnings("serial") // use default serial UID
public class ParseException extends RuntimeException {
  private final int line;
  private final int column;

  ParseException(String message, int line, int column) {
    super(message + " at line " + line + " column "+column);
    this.line = line;
    this.column = column;
  }


  public int getLine() {
    return this.line;
  }

  public int getColumn() {
    return this.column;
  }

}
