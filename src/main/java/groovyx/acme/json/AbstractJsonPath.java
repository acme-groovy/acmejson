package groovyx.acme.json;

import java.io.IOException;

abstract class AbstractJsonPath {
    public AbstractJsonPath(){}

    abstract public int size();
    abstract public Element get(int i);
    abstract public Element peek();
    abstract public String toString();

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

        public String getKey() { return key; }
        public int getIndex() { return index; }
        public boolean isKey() { return _isKey; }

        protected Element(){
            init(-1,null,false);
        }
        protected Element(int index, String key, boolean isKey){
            init(index,key,isKey);
        }

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
