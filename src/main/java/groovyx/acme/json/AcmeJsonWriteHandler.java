package groovyx.acme.json;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

/**
 * json handler that writes json events to output writer
 */
public class AcmeJsonWriteHandler implements AcmeJsonHandler{

    Writer writer;
    boolean space=false;  //without indent by default
    int indent = 0;
    CharSequence indentChars = "  ";

    //private char [] buf = new char[512]; //to bufferize writing to writer
    //private int bufpos = 0;

    public AcmeJsonWriteHandler(Writer writer){
        this.writer=writer;
    }

    /**
     * creates new write handler and initializes with the same parameters as the `other` handler (writer, indent size, pretty print, etc)
     * @param other the other handler to inherit parameters from
     */
    public AcmeJsonWriteHandler(AcmeJsonWriteHandler other){
        this.writer=other.writer;
        this.indent=other.indent;
        this.indentChars=other.indentChars;
        this.space=other.space;
    }

    /**
     * create new handler with writer and prettyPrint flag
     * @param writer writer where to write json
     * @param prettyPrint true if we need to format output json
     */
    public AcmeJsonWriteHandler(Writer writer, boolean prettyPrint){
        this.space=prettyPrint;
        this.writer=writer;
    }

    /**
     * set prettyPrint flag
     * @param prettyPrint true if we need to format output json
     * @return this object
     */
    public AcmeJsonWriteHandler setPrettyPrint(boolean prettyPrint){
        this.space = prettyPrint;
        return this;
    }

    /**
     * set character sequence to be used as indent value. automatically sets PrettyPrint to true.
     * @param s char sequence to use as indent value ( {@code default="  "} )
     * @return returns self
     */
    public AcmeJsonWriteHandler setIndent(CharSequence s){
        this.space = true;
        this.indentChars = s;
        return this;
    }

    @Override
    public void onObjectStart(AbstractJsonPath jpath) throws IOException {
        if(jpath.size()>0) {
            if(jpath.peek().getIndex()>0) writer.write(',');
            if(space) printIndent();
            if(jpath.peek().isKey()) {
                printName(jpath.peek().getKey());
                //if(space) indent += 1;
            }
        }
        writer.write('{');
        if(space) indent++;
    }


    @Override
    public void onObjectEnd(AbstractJsonPath jpath) throws IOException{
        if(space) indent--;
        if(space) printIndent();
        writer.write('}');
        //if(space && indent>0) indent-=1;
        //if(jpath.size()==0)flush();
    }

    @Override
    public void onArrayStart(AbstractJsonPath jpath) throws IOException{
        if(jpath.size()>0) {
            if (jpath.peek().getIndex() > 0) writer.write(',');
            if(space)printIndent();
            if(jpath.peek().isKey()) {
                printName(jpath.peek().getKey());
            }
        }
        writer.write('[');
        if(space) indent++;
    }

    @Override
    public void onArrayEnd(AbstractJsonPath jpath) throws IOException{
        if(space) indent--;
        if(space)printIndent();
        writer.write(']');
        //if(space && jpath.size()>0 && jpath.peek().isKey()) indent-=2;
        //if(jpath.size()==0)flush();
    }



    @Override
    public void onValue(AbstractJsonPath jpath, Object value) throws IOException {
        if(jpath.size()>0) {
            if (jpath.peek().getIndex() > 0) writer.write(',');
            if(space)printIndent();
            if(jpath.peek().isKey()) printName(jpath.peek().getKey());
        }
        printValue(value);
        //if(jpath.size()==0)flush();
    }

    @Override
    public Writer getRoot() {
        return writer;
    }

    /**prints the simple value: null, boolean, string, number*/
    private void printValue(Object o) throws IOException {
        if(o==null){
            writer.write("null");
        }else if(o instanceof Number){
            writer.write(o.toString());
        }else if(o instanceof Boolean){
            writer.write(o.toString());
        }else if(o instanceof CharSequence){
            printString(o.toString(),writer);
        }else if(o instanceof Map || o instanceof Iterator || o instanceof Iterable){
            AcmeJsonWriter w = new AcmeJsonWriter(this);
            w.printValue(o);
        }else{
            printString(o.toString(),writer);
        }
    }
    private void printName(String n) throws IOException {
        printString(n,writer);
        writer.write(':');
        if(space)writer.write(' ');
    }
    private static final char[] hex = "0123456789ABCDEF".toCharArray();
    //prints json-escaped string ignoring unicode escaping
    private static void printString(String o, Writer w) throws IOException {
        int size = o.length();
        int i = 0; //iterator position
        int z = 0; //char that should be written
        w.write('"');
        for(;i<size;i++){
            int c = o.charAt(i);
            if(c<' ' || c=='"' || c=='\\'){
                if(i-z>0)w.write(o, z, i-z);
                z=i+1;
                switch (c){
                    case '\t': w.write("\\t");break;
                    case '\r': w.write("\\r");break;
                    case '\n': w.write("\\n");break;
                    case '\b': w.write("\\b");break;
                    case '\f': w.write("\\f");break;
                    default:
                        w.write("\\u");
                        //write four hex representation of char
                        w.write( hex[(c&0xF000)>>12] );
                        w.write( hex[(c&0x0F00)>> 8] );
                        w.write( hex[(c&0x00F0)>> 4] );
                        w.write( hex[(c&0x000F)    ] );
                }
            }
        }
        if(i-z>0)w.write(o, z, i-z);
        w.write('"');
    }

    private void printIndent() throws IOException {
        writer.write('\n');
        for (int i = 0; i < indent; i++)writer.append(indentChars);
    }
}
