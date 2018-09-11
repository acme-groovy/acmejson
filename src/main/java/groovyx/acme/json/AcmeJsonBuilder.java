package groovyx.acme.json;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Stack;

public class AcmeJsonBuilder implements AcmeJsonHandler {
    Stack<Object> objectStack;
    Object current;

    public AcmeJsonBuilder(){
        current=null;
        objectStack = new Stack<>();
    }

    @Override
    public void onObjectStart(AcmeJsonPath jpath) {
        LinkedHashMap<String,Object> map = new LinkedHashMap<>();
        objectStack.push(map);
        if(current!=null) {
            if(jpath.peek().isKey) ((LinkedHashMap) current).put(jpath.peek().key, map);
            else ((ArrayList)current).add(map);
        }
        current = map;
    }

    @Override
    public void onObjectEnd(AcmeJsonPath jpath) {
        objectStack.pop();
        if(!objectStack.empty())current = objectStack.peek();
    }

    @Override
    public void onArrayStart(AcmeJsonPath jpath) {
        ArrayList<Object> arr = new ArrayList<>();
        objectStack.push(arr);
        if(current!=null) {
            if(jpath.peek().isKey) ((LinkedHashMap) current).put(jpath.peek().key, arr);
            else ((ArrayList)current).add(arr);
        }
        current = arr;
    }

    @Override
    public void onArrayEnd(AcmeJsonPath jpath) {
        objectStack.pop();
        if(!objectStack.empty())current = objectStack.peek();
    }


    @Override
    public void onValue(AcmeJsonPath jpath, Object value) {
        if(jpath.peek().isKey) ((LinkedHashMap) current).put(jpath.peek().key, value);
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
