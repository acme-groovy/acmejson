package groovyx.acme.json;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.CharBuffer;
import java.util.Arrays;

/**
 * Created by dm on 04.09.2018.
 */
public class Capturer {
    private Reader in;
    private final int readAheadSize;
    private char [] buf;
    private int bufpos; //current read position in buffer
    private int bufend; //points to position where to append buffer on read
    private int captureStart; //where capture started (from bufpos-1)
    private boolean eof; //eof encountered
    
    private int line;
    private int column;
    
    public static final int[] CHARS;
    public static final int SPACE_CHARS=1;
    public static final int DIGIT_CHARS=2;
    public static final int HEX_CHARS=4;
    public static final int SIGN_CHARS=8;
    
    static{
    	int[]cc=new int[256];
    	cc[' ']  = SPACE_CHARS;
    	cc['\r'] = SPACE_CHARS;
    	cc['\n'] = SPACE_CHARS;
    	cc['\t'] = SPACE_CHARS;
    	cc['+']  = SIGN_CHARS;
    	cc['-']  = SIGN_CHARS;
    	for(char i='0';i<='9';i++)cc[i] = DIGIT_CHARS|HEX_CHARS;
    	for(char i='a';i<='f';i++)cc[i] = HEX_CHARS;
    	for(char i='A';i<='F';i++)cc[i] = HEX_CHARS;
    	CHARS=cc;
    }

    public Capturer(Reader in, int readAheadSize) {
        this.readAheadSize = readAheadSize;
        this.in = in;
        this.buf = new char[readAheadSize];
        this.bufpos = 0;
        this.bufend = 0;
        this.captureStart = -1; //not started
        this.eof = false;
        this.line=1;
        this.column=0;
    }
    
    public String getPosition(){
    	return "["+line+":"+column+"]";
    }

    private void fill() throws IOException{
        if(eof || bufpos<bufend)return;
        if(captureStart==-1){
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
    private void compact() {
        if(captureStart==-1 && bufpos>readAheadSize){
            if(bufend - bufpos>0)System.arraycopy(  buf, bufpos, buf, 0,  bufend - bufpos);
            bufend -= bufpos;
            bufpos = 0;
            //System.out.println("compact bufpos="+bufpos+" bufend="+bufend+" size="+buf.length);
        }
    }
    //if capture started all read willbe captured
    public void readWhile(int charsType){
    }

    public int read() throws IOException {
        if(captureStart==-1 && bufpos>readAheadSize)compact();
        int next = -1;
        if(bufpos<bufend)next=buf[bufpos++];
        else {
	        fill();
    	    if(bufpos<bufend)next=buf[bufpos++];
        }
        column++;
        return next;
    }

    public void startCapture(){
        if(captureStart!=-1)throw new IllegalStateException("startCapture() already called");
        if(bufpos<1)throw new IllegalStateException("`bufpos` must be greater then zero startCapture() called");
        captureStart=bufpos-1;
    }

    /** returns captured buffer */
    public String endCapture(){
        if(captureStart==-1)throw new IllegalStateException("startCapture() not called");
        String capture = new String(buf, captureStart, bufpos-captureStart-1);
        captureStart=-1;
        return capture;
    }
    public Writer endCapture(Writer w) throws IOException {
        if(captureStart==-1)throw new IllegalStateException("startCapture() not called");
        if(w==null)w=new CharArrayBuffer(32);
        w.write(buf, captureStart, bufpos-captureStart-1 );
        captureStart=-1;
        return w;
    }
}
