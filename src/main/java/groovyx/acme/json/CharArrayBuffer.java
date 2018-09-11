package groovyx.acme.json;

import java.io.CharArrayWriter;
import java.nio.CharBuffer;

/**
 * Created by dm on 04.09.2018.
 */
public class CharArrayBuffer extends CharArrayWriter {
    public CharArrayBuffer(int initialSize) {
        super(initialSize);
    }
    /**/
    public int pop(){
        if(this.count>0)return this.buf[this.count--];
        return -1;
    }
/*
    public void write(CharBuffer buf){
        if(buf.length()<1)return;
        if(buf.hasArray()){
            int offset = buf.arrayOffset();
            int length = buf.length();
            char[] cbuf = buf.array();
            this.write(cbuf, offset, length);
        }
        else this.append(buf);
    }
    */
}

