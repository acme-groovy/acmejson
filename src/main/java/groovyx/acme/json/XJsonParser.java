package groovyx.acme.json;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;


/**
 * A streaming parser for JSON text. The parser reports all events to a given handler.
 */
public class XJsonParser {
    private static final int DEFAULT_MAX_NESTING_LEVEL = 1000;

  private final AcmeJsonHandler handler;
  private Reader reader;
  private int line;
  private int column;
  private int current;
  private int maxNestLevel = DEFAULT_MAX_NESTING_LEVEL;

  private CharArrayBuffer captureBuffer = new CharArrayBuffer(32);
  private boolean captureStart;
  private AcmeJsonPath path = new AcmeJsonPath();

  @SuppressWarnings("unchecked")
  public XJsonParser(AcmeJsonHandler handler) {
    if (handler == null) {
      throw new NullPointerException("handler is null");
    }
    this.handler = handler;
  }

   public XJsonParser setMaxNestLevel(int maxNestLevel){
      this.maxNestLevel = maxNestLevel<=0 ? DEFAULT_MAX_NESTING_LEVEL : maxNestLevel;
      return this;
   }

  /**
   * Parses the given input string. The input must contain a valid JSON value, optionally padded
   * with whitespace.
   *
   * @param string
   *          the input string, must be valid JSON
   */
  public void parse(String string) throws IOException {
    if (string == null) {
      throw new NullPointerException("string is null");
    }
    parse(new AcmeCharSequenceReader(string));
  }


  /**
   * Reads the entire input from the given reader and parses it as JSON. The input must contain a
   * valid JSON value, optionally padded with whitespace.
   * <p>
   * Characters are read in chunks into an input buffer of the given size. Hence, wrapping a reader
   * in an additional <code>BufferedReader</code> likely won't improve reading performance.
   * </p>
   *
   * @param reader
   *          the reader to read the input from
   * @throws IOException
   *           if an I/O error occurs in the reader
   */
  public void parse(Reader reader) throws IOException {
    if (reader == null) {
      throw new NullPointerException("reader is null");
    }
    if( reader instanceof AcmeCharSequenceReader || reader instanceof BufferedReader || reader instanceof StringReader || reader instanceof CharArrayReader){
        this.reader = reader;
    }else {
        this.reader = new BufferedReader(reader);
    }
    //System.out.println(""+reader.getClass());
    line = 1;
    column = 0;
    captureStart=false;
    read();
    skipWhiteSpace();
    readValue();
    skipWhiteSpace();
    if (!isEndOfText()) {
      throw error("Unexpected character");
    }
  }

  private void readValue() throws IOException {
    switch (current) {
      case 'n':
        readNull();
        break;
      case 't':
        readTrue();
        break;
      case 'f':
        readFalse();
        break;
      case '"':
        readString();
        break;
      case '[':
        readArray();
        break;
      case '{':
        readObject();
        break;
      case '-':
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
        readNumber();
        break;
      default:
        throw expected("value");
    }
  }

  private void readArray() throws IOException {
    handler.onArrayStart(path);
    read();
    skipWhiteSpace();
    if (readChar(']')) {
      handler.onArrayEnd(path);
      return;
    }
    int index=0;
    do {
      this.path.push(index,null);
      if(this.path.size()>maxNestLevel)throw error("Nesting too deep");
      skipWhiteSpace();
      readValue();
      this.path.pop();
      skipWhiteSpace();
      index++;
    } while (readChar(','));
    if (!readChar(']'))throw expected("',' or ']'");
    handler.onArrayEnd(path);
  }

  private void readObject() throws IOException {
    handler.onObjectStart(path);
    read();
    skipWhiteSpace();
    if (readChar('}')) {
      handler.onObjectEnd(path);
      return;
    }
    int index=0;
    do {
      skipWhiteSpace();
      String name = readName();
      skipWhiteSpace();
      if (!readChar(':')) {
        throw expected("':'");
      }
      skipWhiteSpace();
      path.push(index,name);
      if(this.path.size()>maxNestLevel)throw error("Nesting too deep");
      readValue();
      path.pop();
      skipWhiteSpace();
      index++;
    } while (readChar(','));
    if (!readChar('}')) {
      throw expected("',' or '}'");
    }
    handler.onObjectEnd(path);
  }

  private String readName() throws IOException {
    if (current != '"') {
      throw expected("name");
    }
    return readStringInternal();
  }

  private void readNull() throws IOException {
    read();
    readRequiredChar('u');
    readRequiredChar('l');
    readRequiredChar('l');
    handler.onValue(path,null);
  }

  private void readTrue() throws IOException {
    read();
    readRequiredChar('r');
    readRequiredChar('u');
    readRequiredChar('e');
    handler.onValue(path,true);
  }

  private void readFalse() throws IOException {
    read();
    readRequiredChar('a');
    readRequiredChar('l');
    readRequiredChar('s');
    readRequiredChar('e');
    handler.onValue(path,false);
  }

  private void readRequiredChar(char ch) throws IOException {
    if (!readChar(ch)) {
      throw expected("'" + ch + "'");
    }
  }

  private void readString() throws IOException {
    handler.onValue(path,readStringInternal());
  }

  private String readStringInternal() throws IOException {
    read();
    startCapture();
    while (current != '"') {
      if (current == '\\') {
        pauseCapture();
        readEscape();
        startCapture();
      } else if (current < 0x20) {
        throw expected("valid string character");
      } else {
        read();
      }
    }
    String string = endCapture();
    read();
    return string;
  }

  private void readEscape() throws IOException {
    read();
    switch (current) {
      case '"':
      case '/':
      case '\\':
        captureBuffer.append((char)current);
        break;
      case 'b':
        captureBuffer.append('\b');
        break;
      case 'f':
        captureBuffer.append('\f');
        break;
      case 'n':
        captureBuffer.append('\n');
        break;
      case 'r':
        captureBuffer.append('\r');
        break;
      case 't':
        captureBuffer.append('\t');
        break;
      case 'u':
        char[] hexChars = new char[4];
        for (int i = 0; i < 4; i++) {
          read();
          if (!isHexDigit()) {
            throw expected("hexadecimal digit");
          }
          hexChars[i] = (char)current;
        }
        captureBuffer.append((char)Integer.parseInt(new String(hexChars), 16));
        break;
      default:
        throw expected("valid escape sequence");
    }
    read();
  }

  private void readNumber() throws IOException {
    startCapture();
    readChar('-');
    int firstDigit = current;
    if (!readDigit()) {
      throw expected("digit");
    }
    if (firstDigit != '0') {
      while (readDigit()) {
      }
    }
    readFraction();
    readExponent();
    String capture = endCapture();
    try {
        handler.onValue(path, new BigDecimal(capture));
    }catch(NumberFormatException e){
        throw error("Error parsing number `"+capture+"`",e);
    }
  }

  private boolean readFraction() throws IOException {
    if (!readChar('.')) {
      return false;
    }
    if (!readDigit()) {
      throw expected("digit");
    }
    while (readDigit()) {
    }
    return true;
  }

  private boolean readExponent() throws IOException {
    if (!readChar('e') && !readChar('E')) {
      return false;
    }
    if (!readChar('+')) {
      readChar('-');
    }
    if (!readDigit()) {
      throw expected("digit");
    }
    while (readDigit()) {}
    return true;
  }

  /*checks if current char is required one then reads next one and returns true otherwise returns false*/
  private boolean readChar(char ch) throws IOException {
    if (current != ch) {
      return false;
    }
    read();
    return true;
  }

    /*checks if current char is digit then reads next one and returns true otherwise returns false*/
  private boolean readDigit() throws IOException {
    if (current < '0' || current > '9') {
      return false;
    }
    read();
    return true;
  }

    private void readWhileDigit() throws IOException {
      do{read();}while(current >= '0' && current <= '9');
    }

    /* reads next char while current one is space*/
  private void skipWhiteSpace() throws IOException {
      while ( current == ' ' || current == '\t' || current == '\n' || current == '\r')read();
  }

  /*reads next char from reader. appends it to capture buffer if required. counts position.*/
  private void read() throws IOException {
      current = reader.read();
      if (current != -1 && captureStart==true) {
          captureBuffer.append((char)current);
      }
      column++;
      if (current == '\n') {
          //just to count lines/columns
          line++;
          column=0;
      }
  }

  /*starts capturing from the current char */
  private void startCapture() {
    captureStart = true;
    captureBuffer.append((char)current);
  }

  /*pop last captured char from buffer and pause capturing. used to read escaped chars*/
  private void pauseCapture() {
    captureBuffer.pop();
    captureStart = false;
  }

    /*pop the last captured char from buffer and stop capturing. clear buffer and return captured as a string*/
  private String endCapture() {
      captureBuffer.pop();
      String captured = captureBuffer.toString();
      captureBuffer.reset();
      captureStart = false;
      return captured;
  }

  private ParseException expected(String expected) {
    if (isEndOfText()) {
      return error("Unexpected end of input");
    }
    return error("Expected " + expected);
  }

    private ParseException error(String message) {
        return new ParseException(message, line, column);
    }
    private ParseException error(String message, Throwable t) {
        return (ParseException) new ParseException(message, line, column).initCause(t);
    }

  private boolean isHexDigit() {
    return current >= '0' && current <= '9'
        || current >= 'a' && current <= 'f'
        || current >= 'A' && current <= 'F';
  }

  private boolean isEndOfText() {
    return current == -1;
  }

}
