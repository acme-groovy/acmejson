package groovyx.acme.json;

import groovy.lang.Closure;
import groovy.lang.Writable;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

/**
 * streaming json writer that could be used to write large json objects dynamically directly to output writer.
 * <pre>{@code
 *  def json = new AcmeJsonWriter(new StringWriter(),false).object{
 *      key("event").value( [name:'test', message: 'ipsum lorem' ] )
 *      key("array").value( 1..5 )
 *  }.writer.toString()
 *  assert json == '{"event":{"name":"test","message":"ipsum lorem"},"array":[1,2,3,4,5]}'
 * }</pre>
 */
public class AcmeJsonWriter {
    AcmeJsonWriteHandler writer;
    JsonPath jpath  = new JsonPath();

    /**
     * creates json writer
     * @param out where to write output
     * @param prettyPrint true if the json must be formatted
     */
    public AcmeJsonWriter(Writer out, boolean prettyPrint){
        writer = new AcmeJsonWriteHandler(out);
        writer.setPrettyPrint(prettyPrint);
    }

    /**
     * initialize with external (delegated) write handler
     * @param w the json write handler
     */
    public AcmeJsonWriter(AcmeJsonWriteHandler w){
        writer = w;
    }

    /**
     * returns writer used to write json. the writer flushed just before return.
     * @return writer
     */
    public Writer getWriter(){
        Writer w = (Writer)writer.getRoot();
        try {
            if(w!=null)w.flush();
        }catch (IOException e){}
        return w;
    }

    /**
     * writes array start bracket to the underlying write handler
     * @return this object
     * @throws IOException if IO error occurred
     */
    public AcmeJsonWriter arrayStart() throws IOException{
        jpath.assertReadyForValue();
        writer.onArrayStart(jpath);
        jpath.pushFirst(false); //push element for the next json array item
        return this;
    }

    /**
     * writes array end bracket to the underlying write handler
     * @return this object
     * @throws IOException if IO error occurred
     */
    public AcmeJsonWriter arrayEnd() throws IOException{
        jpath.pop(false);
        writer.onArrayEnd(jpath);
        jpath.prepareNext();
        return this;
    }

    /**
     * writes object start bracket to the underlying write handler
     * @return this object
     * @throws IOException if IO error occurred
     */
    public AcmeJsonWriter objectStart() throws IOException{
        jpath.assertReadyForValue();
        writer.onObjectStart(jpath);
        jpath.pushFirst(true); //push element for next json key-value item
        return this;
    }

    /**
     * writes object end bracket to the underlying write handler
     * @return this object
     * @throws IOException if IO error occurred
     */
    public AcmeJsonWriter objectEnd() throws IOException{
        jpath.pop(true);
        writer.onObjectEnd(jpath);
        jpath.prepareNext();
        return this;
    }

    /**
     * prepares to write json object key
     * @param key the key of the json object to write
     * @return this object
     * @throws IOException if io error occurred
     */
    public AcmeJsonWriter key(String key) throws IOException{
        jpath.setCurrentKey(key);
        return this;
    }

    /**
     * writes plain value, json object, or json array value to output
     * @param value the value to write to json
     * @return this object
     * @throws IOException if io error occurred
     */
    public AcmeJsonWriter value(Object value) throws IOException{
        jpath.assertReadyForValue();
        printValue(value);
        jpath.prepareNext();
        return this;
    }

    /**
     * writes array start, executes closure, and writes array end
     * @param c closure to write content of the array by using {@code value(...)} or other methods to write nested objcts/arrays
     * @return this object
     * @throws IOException if io error occurred
     */
    public AcmeJsonWriter array(Closure c) throws IOException{
        arrayStart();
        c.setDelegate(this);
        c.call();
        arrayEnd();
        return this;
    }

    /**
     * writes object-start, executes closure, and writes object-end
     * @param c closure that called between object-start and object-end.
     *          use method {@code key('keyName')} to write key and {@code value(someValue)} to write value or other methods to write nested objcts/arrays.
     * @return this object
     * @throws IOException if io error occurred
     */
    public AcmeJsonWriter object(Closure c) throws IOException{
        objectStart();
        c.setDelegate(this);
        c.call();
        objectEnd();
        return this;
    }


    /**
     * internal method to print object to json writer.
     * @param map object to print
     * @throws IOException if error occurred
     */
    protected void printMap(Map<Object,Object> map) throws IOException {
        writer.onObjectStart(jpath);
        Iterator<AbstractMap.Entry<Object,Object>> entries = map.entrySet().iterator();
        for(int i=0;entries.hasNext();i++) {
            AbstractMap.Entry<Object,Object> e = entries.next();
            jpath.push(i, (String)e.getKey(), true);
            printValue(e.getValue());
            jpath.pop(true);
        }
        writer.onObjectEnd(jpath);
    }


    /**
     * internal method to print array to json writer.
     * @param arr array to print
     * @throws IOException if error occurred
     */
    protected void printIterator(Iterator arr) throws IOException {
        writer.onArrayStart(jpath);

        for(int i=0;arr.hasNext();i++) {
            Object value = arr.next();
            jpath.push(i, null, false);
            printValue(value);
            jpath.pop(false);
        }
        writer.onArrayEnd(jpath);
    }

    /**
     * internal method to print plain value, json object, or json array to writer. possible to intercept this method to customize and extend supported object types
     * @param value object to print
     * @throws IOException if error occurred
     */
    @SuppressWarnings("unchecked")
    protected void printValue(Object value) throws IOException {
        if (value instanceof Map) printMap((Map<Object, Object>) value);
        else if (value instanceof Iterable) printIterator( ((Iterable) value).iterator() );
        else if (value instanceof Iterator) printIterator( (Iterator)value);
        else writer.onValue(jpath, value);
    }

    class JsonPath extends AbstractJsonPath{
        private String[]    keys = new String[32];
        private int[]    indices = new int[32];
        private boolean[] bKeys  = new boolean[32];
        private int size = 0;
        private final String NEXT_KEY = "<NEXT_KEY>";

        Element e = new Element();

        @Override
        public int size(){
            return size;
        }

        @Override
        public Element get(int i){
            if(i<0 || i>=size)throw new IllegalStateException("wrong json path position to get: "+i+"; size = "+size);
            e.init(indices[i], keys[i], bKeys[i]);
            return e;
        }

        //removes lats path element
        void pop(){
            if(size<=0)throw new IllegalStateException("got empty json path when expecting something");
            size--;
        }

        //removes lats path element with type object/array check
        void pop(boolean isKey){
            if(size<=0)throw new IllegalStateException("got empty json path when expecting something");
            if(bKeys[size-1]!=isKey)throw new IllegalStateException("expecting to pop "+(isKey?"object":"array")+" but got: "+this.toString());
            size--;
        }

        void push(int index, String key, boolean isKey){
            if (size == indices.length) {
                int newLength = size * 2;
                indices = Arrays.copyOf(indices, newLength);
                keys = Arrays.copyOf(keys, newLength);
                bKeys = Arrays.copyOf(bKeys, newLength);
            }
            indices[size]=index;
            keys[size]=key;
            bKeys[size]=isKey;
            size++;
        }

        //validates if current path is ready for value
        void assertReadyForValue(){
            int i = size-1;
            if(i>=0){
                //if(!bKeys[i])throw new IllegalStateException("expected to be in object but got: "+this.toString());
                if(bKeys[i] && keys[i]==NEXT_KEY)throw new IllegalStateException("key was not set for json object but trying to write value...");
            }
        }

        /**pushes first element for the next potential item. isKey = isObject key expected*/
        void pushFirst(boolean isKey) {
            push(0, isKey?NEXT_KEY:null, isKey);
        }

        //sets key name of the current jpath element ready for that
        void setCurrentKey(String key) {
            int i = size-1;
            if(i<0)throw new IllegalStateException("expected to be in object but got empty json path");
            if(!bKeys[i])throw new IllegalStateException("expected to be in object but got: "+this.toString());
            if(keys[i]!=NEXT_KEY)throw new IllegalStateException("should not happen");
            keys[i]=key;
        }

        //increases the current index of the object/array element
        void prepareNext() {
            int i = size-1;
            if(i<0)return; //throw new IllegalStateException("expected to be in array/object but got empty json path");
            //if(bKeys[i]!=isKey)throw new IllegalStateException("expected to be in "+(isKey?"object":"array")+" but got: "+this.toString());
            keys[i]=bKeys[i]?NEXT_KEY:null;
            indices[i]=indices[i]+1;
        }

        //actually returns next to the last element. because we need to read current token and then trigger handler
        // so this method returns content of currently read object
        @Override
        public Element peek(){
            return get(size-1);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(size*17+3);
            sb.append('$');
            for (int i=0; i<size; i++) {
                if(bKeys[i]){
                    sb.append('.');
                    sb.append(keys[i]);
                }else{
                    sb.append('[');
                    sb.append(indices[i]);
                    sb.append(']');
                }
            }
            return sb.toString();
        }
    }

}
