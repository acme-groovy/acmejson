package groovyx.acme.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;

/**
 * A streaming parser for JSON text. The parser reports all events to a given handler.
 */
public class YJsonParser {
    public static final int[] CHARS = initChars();
    public static final int SPACE_CHARS = 1;
    public static final int DIGIT_CHARS = 2;
    public static final int HEX_CHARS = 4;
    public static final int SIGN_CHARS = 8;
    private final static int [] initChars(){
        int[]cc=new int[256*256];
        cc[' ']  = SPACE_CHARS;
        cc['\r'] = SPACE_CHARS;
        cc['\n'] = SPACE_CHARS;
        cc['\t'] = SPACE_CHARS;
        cc['+']  = SIGN_CHARS;
        cc['-']  = SIGN_CHARS;
        for(char i='0';i<='9';i++)cc[i] = DIGIT_CHARS|HEX_CHARS;
        for(char i='a';i<='f';i++)cc[i] = HEX_CHARS;
        for(char i='A';i<='F';i++)cc[i] = HEX_CHARS;
        return cc;
    }

  public static final boolean isChar(int ch, int type){
  	return ch>0 && (CHARS[ch]&type)==type;
  }

  private void skipWhiteSpace() throws IOException {
    while (isWhiteSpace()) {
    //while( isChar(current,SPACE_CHARS) ){
        read();
    }
  }

  private static final int MAX_NESTING_LEVEL = 1000;
  private static final int MIN_BUFFER_SIZE = 10;
  private static final int DEFAULT_BUFFER_SIZE = 1024;

  private final AcmeJsonHandler handler;
  private Reader reader;
  private char[] buffer;
  private int bufferOffset;
  private int index;
  private int fill;
  private int line;
  private int lineOffset;
  private int current;
  private StringBuilder captureBuffer;
  private int captureStart;
  private int nestingLevel;

  private AcmeJsonPath path = new AcmeJsonPath();

  /*
   * |                      bufferOffset
   *                        v
   * [a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t]        < input
   *                       [l|m|n|o|p|q|r|s|t|?|?]    < buffer
   *                          ^               ^
   *                       |  index           fill
   */

  /**
   * Creates a new JsonParser with the given handler. The parser will report all parser events to
   * this handler.
   *
   * @param handler
   *          the handler to process parser events
   */
  @SuppressWarnings("unchecked")
  public YJsonParser(AcmeJsonHandler handler) {
    if (handler == null) {
      throw new NullPointerException("handler is null");
    }
    this.handler = (AcmeJsonHandler)handler;
  }

  /**
   * Parses the given input string. The input must contain a valid JSON value, optionally padded
   * with whitespace.
   *
   * @param string
   *          the input string, must be valid JSON
   */
  public void parse(String string) {
    if (string == null) {
      throw new NullPointerException("string is null");
    }
    int bufferSize = Math.max(MIN_BUFFER_SIZE, Math.min(DEFAULT_BUFFER_SIZE, string.length()));
    try {
      parse(new StringReader(string), bufferSize);
    } catch (IOException exception) {
      // StringReader does not throw IOException
      throw new RuntimeException(exception);
    }
  }

  /**
   * Reads the entire input from the given reader and parses it as JSON. The input must contain a
   * valid JSON value, optionally padded with whitespace.
   * <p>
   * Characters are read in chunks into a default-sized input buffer. Hence, wrapping a reader in an
   * additional <code>BufferedReader</code> likely won't improve reading performance.
   * </p>
   *
   * @param reader
   *          the reader to read the input from
   * @throws IOException
   *           if an I/O error occurs in the reader
   */
  public void parse(Reader reader) throws IOException {
    parse(reader, DEFAULT_BUFFER_SIZE);
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
   * @param buffersize
   *          the size of the input buffer in chars
   * @throws IOException
   *           if an I/O error occurs in the reader
   */
  public void parse(Reader reader, int buffersize) throws IOException {
    if (reader == null) {
      throw new NullPointerException("reader is null");
    }
    if (buffersize <= 0) {
      throw new IllegalArgumentException("buffersize is zero or negative");
    }
    this.reader = reader;
    buffer = new char[buffersize];
    bufferOffset = 0;
    index = 0;
    fill = 0;
    line = 1;
    lineOffset = 0;
    current = 0;
    captureStart = -1;
    read();
    while ( isChar(current, SPACE_CHARS) )read();//skipWhiteSpace();
    readValue();
    while ( isChar(current, SPACE_CHARS) )read();//skipWhiteSpace();
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
    if (++nestingLevel > MAX_NESTING_LEVEL) {
      throw error("Nesting too deep");
    }
    while ( isChar(current, SPACE_CHARS) )read();//skipWhiteSpace();
    if (readChar(']')) {
      nestingLevel--;
      handler.onArrayEnd(path);
      return;
    }
    int index=0;
    do {
      while ( isChar(current, SPACE_CHARS) )read();//skipWhiteSpace();
      this.path.push(index,null);
      readValue();
      this.path.pop();
      while ( isChar(current, SPACE_CHARS) )read();//skipWhiteSpace();
      index++;
    } while (readChar(','));
    if (!readChar(']')) {
      throw expected("',' or ']'");
    }
    nestingLevel--;
    handler.onArrayEnd(path);
  }

  private void readObject() throws IOException {
    handler.onObjectStart(path);
    read();
    if (++nestingLevel > MAX_NESTING_LEVEL) {
      throw error("Nesting too deep");
    }
    while ( isChar(current, SPACE_CHARS) )read();//skipWhiteSpace();
    if (readChar('}')) {
      nestingLevel--;
      handler.onObjectEnd(path);
      return;
    }
    int index=0;
    do {
      while ( isChar(current, SPACE_CHARS) )read();//skipWhiteSpace();
      String name = readName();
      while ( isChar(current, SPACE_CHARS) )read();//skipWhiteSpace();
      if (!readChar(':')) {
        throw expected("':'");
      }
      while ( isChar(current, SPACE_CHARS) )read();//skipWhiteSpace();
      path.push(index,name);
      readValue();
      path.pop();
      while ( isChar(current, SPACE_CHARS) )read();//skipWhiteSpace();
      index++;
    } while (readChar(','));
    if (!readChar('}')) {
      throw expected("',' or '}'");
    }
    nestingLevel--;
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
    handler.onValue(path, new BigDecimal(endCapture()));
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
    while (readDigit()) {
    }
    return true;
  }

  private boolean readChar(char ch) throws IOException {
    if (current != ch) {
      return false;
    }
    read();
    return true;
  }

  private boolean readDigit() throws IOException {
    if (!isDigit()) {
      return false;
    }
    read();
    return true;
  }

  private void read() throws IOException {
    if (index == fill) {
      if (captureStart != -1) {
        captureBuffer.append(buffer, captureStart, fill - captureStart);
        captureStart = 0;
      }
      bufferOffset += fill;
      fill = reader.read(buffer, 0, buffer.length);
      index = 0;
      if (fill == -1) {
        current = -1;
        index++;
        return;
      }
    }
    if (current == '\n') {
      line++;
      lineOffset = bufferOffset + index;
    }
    current = buffer[index++];
  }

  private void startCapture() {
    if (captureBuffer == null) {
      captureBuffer = new StringBuilder();
    }
    captureStart = index - 1;
  }

  private void pauseCapture() {
    int end = current == -1 ? index : index - 1;
    captureBuffer.append(buffer, captureStart, end - captureStart);
    captureStart = -1;
  }

  private String endCapture() {
    int start = captureStart;
    int end = index - 1;
    captureStart = -1;
    if (captureBuffer.length() > 0) {
      captureBuffer.append(buffer, start, end - start);
      String captured = captureBuffer.toString();
      captureBuffer.setLength(0);
      return captured;
    }
    return new String(buffer, start, end - start);
  }

  private ParseException expected(String expected) {
    if (isEndOfText()) {
      return error("Unexpected end of input");
    }
    return error("Expected " + expected);
  }

  private ParseException error(String message) {
    return new ParseException(message, line,0);
  }

  private boolean isWhiteSpace() {
    return current == ' ' || current == '\t' || current == '\n' || current == '\r';
  }

  private boolean isDigit() {
    return current >= '0' && current <= '9';
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
