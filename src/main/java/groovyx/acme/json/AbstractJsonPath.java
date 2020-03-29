package groovyx.acme.json;

import java.io.IOException;

/**
 * json path object that is accessible in json events. if's possible to get current context this object.
 */
public abstract class AbstractJsonPath {
    public AbstractJsonPath(){}

    /**
     * get size of the json path. root not included. 0 - means we are at the root.
     * @return size of the json path
     */
    abstract public int size();

    /**
     * get json path element at position `i`
     * @param i index
     * @return json path element
     */
    abstract public Element get(int i);

    /**
     * returns last element of the json path or null if json path is empty
     * @return json path element
     */
    abstract public Element peek();

    /**
     * get string representation of json path
     * @return string representation of json path
     */
    abstract public String toString();

    /**
     * represents one element of a json path
     */
    public class Element{
        //boolean isKey; //
        private String key; //key if it's a map-key element null if array
        private int index; //zero-based order of this element
        private boolean _isKey;

        protected void init(int index, String key, boolean isKey){
            this.index=index;
            this.key=key;
            this._isKey=isKey;
        }

        /**
         * object key of the current element
         * @return object key or null
         */
        public String getKey() { return key; }

        /**
         * index of the current element valid for json-array element and for json-object element
         * @return index of current json-path element
         */
        public int getIndex() { return index; }

        /**
         * returns true if current element is a key of json-object, false - element of json-array
         * @return true - key of object , false - element of array
         */
        public boolean isKey() { return _isKey; }

        protected Element(){
            init(-1,null,false);
        }

        protected Element(int index, String key, boolean isKey){
            init(index,key,isKey);
        }

        /**
         * appends current element strng representation to appendable
         * @param w where to append
         */
        public void appendTo(Appendable w) {
            try {
                if (_isKey) {
                    w.append('.');
                    w.append((String)key);
                } else {
                    w.append('[');
                    w.append(Integer.toString(index));
                    w.append(']');
                }
            }catch (IOException e){
                throw new RuntimeException(e.toString(),e);
            }
        }

        public String toString(){
            if(_isKey) return "."+ key;
            else return "["+index+"]";
        }
    }

}
