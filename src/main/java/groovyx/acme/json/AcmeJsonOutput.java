package groovyx.acme.json;

import groovy.lang.Writable;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.*;

public class AcmeJsonOutput implements Writable{
    AcmeJsonWriter writer;
    AcmeJsonPath jpath;
    boolean indent = false;
    Object root = null;

    public AcmeJsonOutput(Object root){
        writer = null;
        jpath = new AcmeJsonPath();
        this.root = root;
    }


    @Override
    public Writer writeTo(Writer out) throws IOException {
        writer = new AcmeJsonWriter(out);
        writer.setIndent(indent);
        parseValue(root);
        return out;
    }


    public AcmeJsonOutput setIndent(boolean indent){
        this.indent = indent;
        return this;
    }


    private void parseMap(Object object){
        writer.onObjectStart(jpath);
        Map map = (Map)object;
        Iterator keys = map.keySet().iterator();
        for(int i=0;keys.hasNext();i++) {
            jpath.push(new AcmeJsonPath.Element(i, (String)keys.next()));
            Object value = map.get(jpath.peek().key);
            parseValue(value);
            jpath.pop();
        }
        writer.onObjectEnd(jpath);

        return ;
    }


    private void parseIterator(Object object){
        writer.onArrayStart(jpath);
        Iterator arr = (Iterator)object;

        for(int i=0;arr.hasNext();i++) {
            Object value = arr.next();
            jpath.push(new AcmeJsonPath.Element(i, null));
            parseValue(value);
            jpath.pop();
        }
        writer.onArrayEnd(jpath);
        return ;
    }

    private void parseValue(Object value) {
        if (value instanceof Map) parseMap(value);
        else if (value instanceof Iterable) parseIterator(((Iterable) value).iterator());
        else if (value instanceof Iterator) parseIterator(value);
        else writer.onValue(jpath, value);
    }
}
