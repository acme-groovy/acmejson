package groovyx.acme.json.io;

/** the class that wraps CharSequence to read from it */
public class CharsSourceSequence extends CharsSource{

    private final CharSequence buf;
    private int pos = 0;
    private int count;
    private int capture = -1; //not started

    public CharsSourceSequence(CharSequence cs){
        this.buf = cs;
        this.count = cs.length();
    }
    @Override
    public int next() {
        if(pos>count)return -1;
        return buf.charAt(pos++);
    }

    @Override
    public void captureStart() throws IllegalStateException {
        if(capture!=-1)throw new IllegalStateException("capturing already started at pos = "+capture);
        capture = pos;
    }

    @Override
    public CharSequence captureEnd() throws IllegalStateException {
        if(capture==-1)throw new IllegalStateException("capturing not started but end requested at pos = "+pos);
        //println
        //return buf.subSequence(capture, pos - capture -1);
        return buf.subSequence(capture, pos -1);
    }
}
