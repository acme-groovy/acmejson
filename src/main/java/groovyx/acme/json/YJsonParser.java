package groovyx.acme.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;

/**
 * A streaming parser for JSON text. The parser reports all events to a given handler.
 */
public class YJsonParser {
    static final int[] CHARS = initChars();
    public static final int SPACE_CHAR = 1;
    public static final int DIGIT_CHAR = 2;
    public static final int HEX_CHAR = 4;
    public static final int SIGN_CHAR = 8;
    public static final int MINUS_CHAR = 16;

    private final static int[] initChars() {
        int[] cc = new int[256];// * 256];
        cc[' '] = SPACE_CHAR;
        cc['\r'] = SPACE_CHAR;
        cc['\n'] = SPACE_CHAR;
        cc['\t'] = SPACE_CHAR;
        cc['+'] = SIGN_CHAR;
        cc['-'] = SIGN_CHAR | MINUS_CHAR;
        for (char i = '0'; i <= '9'; i++) cc[i] = DIGIT_CHAR | HEX_CHAR;
        for (char i = 'a'; i <= 'f'; i++) cc[i] = HEX_CHAR;
        for (char i = 'A'; i <= 'F'; i++) cc[i] = HEX_CHAR;
        return cc;
    }

    static final boolean isChar(int ch, int type) {
        try {
            return (CHARS[ch] & type) != 0;
        }catch(Throwable t){
            return false;
        }
    }


    private void readWhileSpace() throws IOException {
        while (isChar(current, SPACE_CHAR)) {
            read();
        }
    }
    private void readWhileDigit() throws IOException {
        while (isChar(current, DIGIT_CHAR)) {
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
    //private int nestingLevel;

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
     * @param handler the handler to process parser events
     */
    @SuppressWarnings("unchecked")
    public YJsonParser(AcmeJsonHandler handler) {
        if (handler == null) {
            throw new NullPointerException("handler is null");
        }
        this.handler = (AcmeJsonHandler) handler;
    }

    /**
     * Parses the given input string. The input must contain a valid JSON value, optionally padded
     * with whitespace.
     *
     * @param string the input string, must be valid JSON
     */
    public void parse(String string) {
        if (string == null) {
            throw new NullPointerException("string is null");
        }
        int bufferSize = Math.max(MIN_BUFFER_SIZE, Math.min(DEFAULT_BUFFER_SIZE, string.length()));
        try {
            parse(new StringReader(string), bufferSize);
            //parse(new AcmeCharSequenceReader(string), bufferSize);
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
     * @param reader the reader to read the input from
     * @throws IOException if an I/O error occurs in the reader
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
     * @param reader     the reader to read the input from
     * @param buffersize the size of the input buffer in chars
     * @throws IOException if an I/O error occurs in the reader
     */
    public void parse(Reader reader, int buffersize) throws IOException {
        if (reader == null) {
            throw new NullPointerException("reader is null");
        }
        if (buffersize <= MIN_BUFFER_SIZE) {
            buffersize = MIN_BUFFER_SIZE;
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
        readWhileSpace();
        readValue();
        readWhileSpace();
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
            default:
                if(isChar(current,DIGIT_CHAR|MINUS_CHAR))readNumber();
                else throw expected("value");
        }
    }

    private void readArray() throws IOException {
        handler.onArrayStart(path);
        read();
        if (this.path.size() > MAX_NESTING_LEVEL) {
            throw error("Nesting too deep");
        }
        readWhileSpace();
        if (readChar(']')) {
            handler.onArrayEnd(path);
            return;
        }
        int index = 0;
        do {
            readWhileSpace();
            this.path.push(index, null);
            readValue();
            this.path.pop();
            readWhileSpace();
            index++;
        } while (readChar(','));
        if (!readChar(']')) {
            throw expected("',' or ']'");
        }
        handler.onArrayEnd(path);
    }

    private void readObject() throws IOException {
        handler.onObjectStart(path);
        read();
        if (this.path.size() > MAX_NESTING_LEVEL) {
            throw error("Nesting too deep");
        }
        readWhileSpace();
        if (readChar('}')) {
            handler.onObjectEnd(path);
            return;
        }
        int index = 0;
        do {
            readWhileSpace();
            String name = readName();
            readWhileSpace();
            if (!readChar(':')) {
                throw expected("':'");
            }
            readWhileSpace();
            path.push(index, name);
            readValue();
            path.pop();
            readWhileSpace();
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
        handler.onValue(path, null);
    }

    private void readTrue() throws IOException {
        read();
        readRequiredChar('r');
        readRequiredChar('u');
        readRequiredChar('e');
        handler.onValue(path, true);
    }

    private void readFalse() throws IOException {
        read();
        readRequiredChar('a');
        readRequiredChar('l');
        readRequiredChar('s');
        readRequiredChar('e');
        handler.onValue(path, false);
    }

    private void readRequiredChar(char ch) throws IOException {
        if (!readChar(ch)) {
            throw expected("'" + ch + "'");
        }
    }

    private void readString() throws IOException {
        handler.onValue(path, readStringInternal());
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
                captureBuffer.append((char) current);
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
                    if (!isChar(current, HEX_CHAR)) {
                        throw expected("hexadecimal digit");
                    }
                    hexChars[i] = (char) current;
                }
                captureBuffer.append((char) Integer.parseInt(new String(hexChars), 16));
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
            readWhileDigit();
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
        readWhileDigit();
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
        readWhileDigit();
        return true;
    }

    private boolean readChar(char ch) throws IOException {
        if (current == ch) {
            read();
            return true;
        }
        return false;
    }

    private boolean readDigit() throws IOException {
        if (isChar(current, DIGIT_CHAR)) {
            read();
            return true;
        }
        return false;
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
        return new ParseException(message, line, 0);
    }

    private boolean isEndOfText() {
        return current == -1;
    }

}
