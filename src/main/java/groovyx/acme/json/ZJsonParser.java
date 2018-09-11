package groovyx.acme.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Arrays;

/**
 * A streaming parser for JSON text. The parser reports all events to a given handler.
 */
public class ZJsonParser {
    private static final int DEFAULT_MAX_NESTING_LEVEL = 1000;
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    public static final int[] CHARS = initChars();
    public static final int SPACE_CHARS = 1;
    public static final int DIGIT_CHARS = 2;
    public static final int HEX_CHARS = 4;
    public static final int SIGN_CHARS = 8;

    private int readAheadSize=DEFAULT_BUFFER_SIZE;
    private char [] buf;
    private int bufpos; //current read position in buffer
    private int bufend; //points to position where to append buffer on read
    private int _captureStart; //where capture started (from bufpos-1)
    private boolean eof; //eof encountered
    private Reader in;

    private int maxNestLevel = DEFAULT_MAX_NESTING_LEVEL;
    //human readable position of the current reading char
    private int line;
    private int column;

    private final AcmeJsonHandler handler;
    //private Reader reader;
    //private char[] buffer;
    //private int bufferOffset;
    //private int index;
    //private int fill;
    //private int line;
    //private int lineOffset;
    private int current;
    //private StringBuilder captureBuffer;
    //private int captureStart;
    //private int nestingLevel;

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

    /*
    private final void readWhile(int CHARS_TYPE) throws IOException {
        try {
            while( (CHARS[current]&CHARS_TYPE)==CHARS_TYPE ){
                read();
            }
        }catch(ArrayIndexOutOfBoundsException e){}
    }
    */
  private final void readWhileSpace() throws IOException {
    while (current == ' ' || current == '\t' || current == '\n' || current == '\r') {
      read();
    }
  }

    public ZJsonParser setMaxNestLevel(int maxNestLevel) {
        this.maxNestLevel = maxNestLevel <= 0 ? DEFAULT_MAX_NESTING_LEVEL : maxNestLevel;
        return this;
    }

    public ZJsonParser setBufferSize(int bufferSize) {
        this.readAheadSize = bufferSize <= 100 ? DEFAULT_MAX_NESTING_LEVEL : bufferSize;
        return this;
    }


    private AcmeJsonPath path = new AcmeJsonPath();

    public ZJsonParser(AcmeJsonHandler handler) {
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
        if (this.readAheadSize <= 10) this.readAheadSize=10;

        this.in = reader;
        this.buf = new char[this.readAheadSize + this.readAheadSize/5];
        this.bufpos = 0;
        this.bufend = 0;
        this._captureStart = -1; //not started
        this.eof = false;
        this.line=1;
        this.column=0;
        this.current = 0;

        read();
        readWhileSpace();//readWhile(SPACE_CHARS);
        readValue();
        readWhileSpace();//readWhile(SPACE_CHARS);
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
        readWhileSpace();//readWhile(SPACE_CHARS);
        if (readChar(']')) {
            handler.onArrayEnd(path);
            return;
        }
        int index = 0;
        do {
            this.path.push(index, null);
            if (this.path.size() > maxNestLevel) throw error("Nesting too deep");
            readWhileSpace();//readWhile(SPACE_CHARS);
            readValue();
            this.path.pop();
            readWhileSpace();//readWhile(SPACE_CHARS);
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
        readWhileSpace();//readWhile(SPACE_CHARS);
        if (readChar('}')) {
            handler.onObjectEnd(path);
            return;
        }
        int index = 0;
        do {
            readWhileSpace();//readWhile(SPACE_CHARS);
            String name = readName();
            readWhileSpace();//readWhile(SPACE_CHARS);
            if (!readChar(':')) {
                throw expected("':'");
            }
            readWhileSpace();//readWhile(SPACE_CHARS);
            path.push(index, name);
            if (this.path.size() > maxNestLevel)throw error("Nesting too deep");
            readValue();
            path.pop();
            readWhileSpace();//readWhile(SPACE_CHARS);
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
        startCapture(); //just mark current position to be kept in buf
        int bufendNew = bufend;
        while (current != '"') {
            switch (current) {
            case '\\':
                buf[bufendNew++] = (char) _readEscape();
                break;
            default:
                buf[bufendNew++] = (char) current;
                read();
                break;
            }
        }
        String string = new String(buf, _captureStart, bufpos-_captureStart-1);
        _endCapture();
        read();
        return string;
    }

    //returns escaped character
    private int _readEscape() throws IOException {
        int ret;
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
        startCapture();
        readChar('-');
        int firstDigit = current;
        if (!readDigit()) {
            throw expected("digit");
        }
        if (firstDigit != '0') {
            while (readDigit()) {}
        }
        readFraction();
        readExponent();
        BigDecimal num = new BigDecimal(buf, _captureStart, bufpos-_captureStart-1);
        _endCapture();
        handler.onValue(path, num);
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

    private void bufCompact() {
        if(_captureStart==-1 && bufpos>readAheadSize){
            if(bufend - bufpos>0)System.arraycopy(  buf, bufpos, buf, 0,  bufend - bufpos);
            bufend -= bufpos;
            bufpos = 0;
            //System.out.println("compact bufpos="+bufpos+" bufend="+bufend+" size="+buf.length);
        }
    }
    private void bufFill() throws IOException{
        if(eof || bufpos<bufend)return;
        if(_captureStart==-1){
            bufend=0;
            bufpos=0;
        }
        if(bufend+readAheadSize>buf.length){
            //System.out.println("  >>> fill resize buf");
            buf = Arrays.copyOf(buf, bufend+readAheadSize);
        }
        int r = in.read( buf, bufend, readAheadSize );
        if(r==-1){
            eof=true;
        }else{
            bufend += r;
        }
        //System.out.println("fill bufpos="+bufpos+" bufend="+bufend+" size="+buf.length);
    }


    //reads next char from buffer or from reader into `current` member.
    public void read() throws IOException {
        if(_captureStart==-1 && bufpos>readAheadSize)bufCompact();
        //int next = -1;
        if(bufpos<bufend)current=buf[bufpos++];
        else {
            bufFill();
            if(bufpos<bufend)current=buf[bufpos++];
            else current=-1;
        }
        column++;
        if (current == '\n') {
            //just to count lines/columns
            line++;
            column = 0;
        }
    }

    public void startCapture(){
        if(_captureStart!=-1)throw new IllegalStateException("startCapture() already called");
        if(bufpos<1)throw new IllegalStateException("`bufpos` must be greater then zero startCapture() called");
        _captureStart=bufpos-1;
    }

    /** returns captured buffer */
    public void _endCapture(){
        if(_captureStart==-1)throw new IllegalStateException("startCapture() not called");
        //String capture = new String(buf, captureStart, bufpos-captureStart-1);
        _captureStart=-1;
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
        return new ParseException(message, line, 0);
    }

    private boolean isWhiteSpace() {
        return current == ' ' || current == '\t' || current == '\n' || current == '\r';
    }

    private boolean isDigit() {
        return current >= '0' && current <= '9';
    }

    private boolean isHexDigit() {
        return current >= '0' && current <= '9' || current >= 'a' && current <= 'f' || current >= 'A' && current <= 'F';
    }

    private boolean isEndOfText() {
        return current == -1;
    }

}
