package groovyx.acme.json;

import java.util.ArrayList;

/**simple implementation for test purposes*/
public class TestJsonPath extends AbstractJsonPath{
    private ArrayList<Element> path = new ArrayList<Element>();
    @Override
    public int size() {
        return path.size();
    }

    @Override
    public Element get(int i) {
        return path.get(i);
    }

    @Override
    public Element peek() {
        return path.get( path.size()-1 );
    }

    public TestJsonPath push(int index, String key){
        return push(index,key, key!=null);
    }
    public TestJsonPath push(int index, String key, boolean isKey){
        Element e = new Element(index, key, isKey);
        this.path.add(e);
        return this;
    }
    /**removes last*/
    public Element pop(){
        return path.remove(path.size()-1);
    }
    /**removes last or if empty returns null*/
    public Element popOrNull(){
        if(path.size()>0)return path.remove(path.size()-1);
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.size()*17+3);
        sb.append('$');
        for (Element a: this.path) {
            a.appendTo(sb);
        }
        return sb.toString();
    }
}
