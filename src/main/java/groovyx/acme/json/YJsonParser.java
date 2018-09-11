package groovyx.acme.json;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;

/**
 * A streaming parser for JSON text. The parser reports all events to a given handler.
 */
public class YJsonParser {
    private static final int DEFAULT_MAX_NESTING_LEVEL = 1000;
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private final AcmeJsonHandler handler;
    private Capturer reader;
    private int line;
    private int column;
    private int current;
    private int maxNestLevel = DEFAULT_MAX_NESTING_LEVEL;
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private AcmeJsonPath path = new AcmeJsonPath();

    public YJsonParser(AcmeJsonHandler handler) {
        if (handler == null) {
            throw new NullPointerException("handler is null");
        }
        this.handler = handler;
    }

    public YJsonParser setMaxNestLevel(int maxNestLevel) {
        this.maxNestLevel = maxNestLevel <= 0 ? DEFAULT_MAX_NESTING_LEVEL : maxNestLevel;
        return this;
    }

    public YJsonParser setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize <= 100 ? DEFAULT_MAX_NESTING_LEVEL : maxNestLevel;
        return this;
    }

    /**
     * Parses the given input string. The input must contain a valid JSON value, optionally padded
     * with whitespace.
     *
     * @param string the input string, must be valid JSON
     */
    public void parse(String string) throws IOException {
        if (string == null) {
            throw new NullPointerException("string is null");
        }
        parse(new StringReader(string));
    }

    /**
     * Reads the entire input from the given reader and parses it as JSON. The input must contain a
     * valid JSON value, optionally padded with whitespace.
     * <p>
     * Characters are read in chunks into an input buffer of the given size. Hence, wrapping a reader
     * in an additional <code>BufferedReader</code> likely won't improve reading performance.
     * </p>
     *
     * @param reader the reader to read the input from
     * @throws IOException if an I/O error occurs in the reader
     */
    public void parse(Reader reader) throws IOException {
        if (reader == null) {
            throw new NullPointerException("reader is null");
        }
        this.reader = new Capturer(reader,bufferSize);
        //System.out.println(""+reader.getClass());
        line = 1;
        column = 0;
        read();
        skipWhiteSpace(this);
        readValue();
        skipWhiteSpace(this);
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
        skipWhiteSpace(this);
        if (readChar(']')) {
            handler.onArrayEnd(path);
            return;
        }
        int index = 0;
        do {
            this.path.push(index, null);
            if (this.path.size() > maxNestLevel) {
                throw error("Nesting too deep");
            }
            skipWhiteSpace(this);
            readValue();
            this.path.pop();
            skipWhiteSpace(this);
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
        skipWhiteSpace(this);
        if (readChar('}')) {
            handler.onObjectEnd(path);
            return;
        }
        int index = 0;
        do {
            skipWhiteSpace(this);
            String name = readName();
            skipWhiteSpace(this);
            if (!readChar(':')) {
                throw expected("':'");
            }
            skipWhiteSpace(this);
            path.push(index, name);
            if (this.path.size() > maxNestLevel) {
                throw error("Nesting too deep");
            }
            readValue();
            path.pop();
            skipWhiteSpace(this);
            index++;
        } while (readChar(','));
        if (!readChar('}')) {
            throw expected("',' or '}'");
        }
        handler.onObjectEnd(path);
    }

    private String readName() throws IOException {
        if (current != '"') {
            throw expected("double-quoted name");
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
        Writer buf=null;
        read();
        reader.startCapture();
        while (current != '"') {
            if (current == '\\') {
                buf = reader.endCapture(buf);
                buf.write( readEscape() );
                reader.startCapture();
            } else if (current < 0x20) {
                throw expected("valid string character");
            } else {
                read();
            }
        }
        String ret;
        if(buf==null)ret = reader.endCapture();
        else ret = reader.endCapture(buf).toString();
        read();
        return ret;
    }

    private int readEscape() throws IOException {
        int ret = -1;
        read();
        switch (current) {
        case '"':
        case '/':
        case '\\':
            ret = current;
            break;
        case 'b':
            ret = '\b';
            break;
        case 'f':
            ret = '\f';
            break;
        case 'n':
            ret = '\n';
            break;
        case 'r':
            ret = '\r';
            break;
        case 't':
            ret = '\t';
            break;
        case 'u':
            char[] hexChars = new char[4];
            for (int i = 0; i < 4; i++) {
                read();
                if (!isHexDigit()) {
                    throw expected("hexadecimal digit");
                }
                hexChars[i] = (char) current;
            }
            ret = Integer.parseInt(new String(hexChars), 16);
            break;
        default:
            throw expected("valid escape sequence","\\"+(char)current );
        }
        read();
        return ret;
    }

    private void readNumber() throws IOException {
        reader.startCapture();
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
        String capture = reader.endCapture();
        try {
            handler.onValue(path, new BigDecimal(capture));
        } catch (NumberFormatException e) {
            throw error("Error parsing number `" + capture + "`", e);
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
        while (readDigit()) {
        }
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
        while (current >= '0' && current <= '9')read();
    }

    /* reads next char while current one is space*/
    private static final void skipWhiteSpace(YJsonParser it) throws IOException {
        while (it.current == ' ' || it.current == '\t' || it.current == '\n' || it.current == '\r')
            it.read();
    }

    /*reads next char from reader. appends it to capture buffer if required. counts position.*/
    private void read() throws IOException {
        current = reader.read();
        column++;
        if (current == '\n') {
            //just to count lines/columns
            line++;
            column = 0;
        }
    }

    private ParseException expected(String expected) {
        return expected(expected, null);
    }
    private ParseException expected(String expected, String got) {
        if (isEndOfText()) {
            return error("Unexpected end of input");
        }
        if(got==null)got=""+(char)current;
        return error("Expected " + expected + ", got: '" + got + "'");
    }

    private ParseException error(String message) {
        return new ParseException(message, line, column);
    }

    private ParseException error(String message, Throwable t) {
        return (ParseException) new ParseException(message, line, column).initCause(t);
    }

    private boolean isHexDigit() {
        return current >= '0' && current <= '9' || current >= 'a' && current <= 'f' || current >= 'A' && current <= 'F';
    }

    private boolean isEndOfText() {
        return current == -1;
    }

}
