package groovyx.acme.json;

import java.io.IOException;
import java.io.Writer;

public class AcmeJsonWriteHandler implements AcmeJsonHandler{

    Writer writer;
    boolean space=false;  //without indent by default
    int indent = 0;

    //private char [] buf = new char[512]; //to bufferize writing to writer
    //private int bufpos = 0;

    public AcmeJsonWriteHandler(Writer writer){
        this.writer=writer;
    }

    public AcmeJsonWriteHandler(Writer writer, boolean prettyPrint){
        this.space=prettyPrint;
        this.writer=writer;
    }

    public AcmeJsonWriteHandler setPrettyPrint(boolean prettyPrint){
        this.space = prettyPrint;
        return this;
    }

    @Override
    public void onObjectStart(AbstractJsonPath jpath) throws IOException {
        if(jpath.size()>0) {
            if(jpath.peek().getIndex()>0) writer.write(',');
            if(space) printIndent();
            if(jpath.peek().isKey()) {
                printName(jpath.peek().getKey());
                if(space) indent += 2;
            }
        }
        if(space) indent++;
        writer.write('{');
    }


    @Override
    public void onObjectEnd(AbstractJsonPath jpath) throws IOException{
        if(space) indent--;
        if(space) printIndent();
        writer.write('}');
        if(space && jpath.size()>0 && jpath.peek().isKey()) indent-=2;
        //if(jpath.size()==0)flush();
    }

    @Override
    public void onArrayStart(AbstractJsonPath jpath) throws IOException{
        if(jpath.size()>0) {
            if (jpath.peek().getIndex() > 0) writer.write(',');
            if(space)printIndent();
            if(jpath.peek().isKey()) printName(jpath.peek().getKey());
        }
        if(space) indent++;
        writer.write('[');
    }

    @Override
    public void onArrayEnd(AbstractJsonPath jpath) throws IOException{
        if(space) indent--;
        if(space)printIndent();
        writer.write(']');
        if(space && jpath.size()>0 && jpath.peek().isKey()) indent-=2;
        //if(jpath.size()==0)flush();
    }



    @Override
    public void onValue(AbstractJsonPath jpath, Object value) throws IOException {
        if (jpath.peek().getIndex() > 0) writer.write(',');
        if(space)printIndent();
        if(jpath.peek().isKey()) printName(jpath.peek().getKey());
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
        }else{
            printString(o.toString(),writer);
        }
    }
    private void printName(String n) throws IOException {
        printString(n,writer);
        writer.write(':');
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
        for (int i = 0; i < indent; i++)writer.write("  ");
    }
}
