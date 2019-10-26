package groovyx.acme.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Stack;

public class AcmeJsonBuildHandler implements AcmeJsonHandler {
    Stack<Object> objectStack;
    Object current;

    public AcmeJsonBuildHandler(){
        current=null;
        objectStack = new Stack<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onObjectStart(AbstractJsonPath jpath) {
        LinkedHashMap<String,Object> map = new LinkedHashMap<>();
        objectStack.push(map);
        if(current!=null) {
            if(jpath.peek().isKey()) ((LinkedHashMap) current).put(jpath.peek().getKey(), map);
            else ((ArrayList)current).add(map);
        }
        current = map;
    }

    @Override
    public void onObjectEnd(AbstractJsonPath jpath) {
        objectStack.pop();
        if(!objectStack.empty())current = objectStack.peek();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onArrayStart(AbstractJsonPath jpath) {
        ArrayList<Object> arr = new ArrayList<>();
        objectStack.push(arr);
        if(current!=null) {
            if(jpath.peek().isKey()) ((LinkedHashMap) current).put(jpath.peek().getKey(), arr);
            else ((ArrayList)current).add(arr);
        }
        current = arr;
    }

    @Override
    public void onArrayEnd(AbstractJsonPath jpath) {
        objectStack.pop();
        if(!objectStack.empty())current = objectStack.peek();
    }


    @Override
    @SuppressWarnings("unchecked")
    public void onValue(AbstractJsonPath jpath, Object value) {
        if(jpath.peek().isKey()) ((LinkedHashMap) current).put(jpath.peek().getKey(), value);
        else ((ArrayList)current).add(value);
    }


    @Override
    public Object getRoot(){
        return current;
    }


    public boolean isDone(){
        if(objectStack.empty() && current!=null) return true;
        else return false;
    }

}
