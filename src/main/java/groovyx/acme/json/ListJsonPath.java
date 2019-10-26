package groovyx.acme.json;

import java.util.LinkedList;

public class ListJsonPath extends AbstractJsonPath{
    private LinkedList<Element> path = new LinkedList();
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
        return path.peek();
    }

    public ListJsonPath push(int index, String key){
        return push(index,key, key!=null);
    }
    public ListJsonPath push(int index, String key, boolean isKey){
        Element e = new Element(index, key, isKey);
        this.path.add(e);
        return this;
    }
    public Element pop(){
        return path.pop();
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
