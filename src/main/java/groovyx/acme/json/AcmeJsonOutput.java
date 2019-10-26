package groovyx.acme.json;

import groovy.lang.Writable;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class AcmeJsonOutput implements Writable{
    AcmeJsonWriteHandler writer;
    ListJsonPath jpath;
    boolean indent = false;
    Object root = null;

    public AcmeJsonOutput(Object root){
        writer = null;
        jpath = new ListJsonPath();
        this.root = root;
    }


    @Override
    public Writer writeTo(Writer out) throws IOException {
        writer = new AcmeJsonWriteHandler(out);
        writer.setPrettyPrint(indent);
        parseValue(root);
        return out;
    }


    public AcmeJsonOutput setIndent(boolean indent){
        this.indent = indent;
        return this;
    }


    private void parseMap(Object object) throws IOException {
        writer.onObjectStart(jpath);
        Map map = (Map)object;
        Iterator keys = map.keySet().iterator();
        for(int i=0;keys.hasNext();i++) {
            jpath.push(i, (String)keys.next(), true);
            Object value = map.get(jpath.peek().getKey());
            parseValue(value);
            jpath.pop();
        }
        writer.onObjectEnd(jpath);

        return ;
    }


    private void parseIterator(Object object) throws IOException {
        writer.onArrayStart(jpath);
        Iterator arr = (Iterator)object;

        for(int i=0;arr.hasNext();i++) {
            Object value = arr.next();
            jpath.push(i, null, false);
            parseValue(value);
            jpath.pop();
        }
        writer.onArrayEnd(jpath);
        return ;
    }

    private void parseValue(Object value) throws IOException {
        if (value instanceof Map) parseMap(value);
        else if (value instanceof Iterable) parseIterator(((Iterable) value).iterator());
        else if (value instanceof Iterator) parseIterator(value);
        else writer.onValue(jpath, value);
    }
}
