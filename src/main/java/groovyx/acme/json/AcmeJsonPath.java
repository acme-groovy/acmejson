package groovyx.acme.json;

import java.util.Stack;

public class AcmeJsonPath extends Stack<AcmeJsonPath.Element>{

    int indent=0;

    public AcmeJsonPath(){super();}


    public String toString(){
        String res="$";
        for (Object a: this
                ) {
            res+=a.toString();
        }
        return res;
    }




    public Element push(int index, String key){
        Element e = new Element(index, key);
        return (Element)this.push(e);
    }




    public static class Element{

        boolean isKey;
        String key;
        int index;

        public Element(int index, String key){
            if(key==null) isKey=false;
            else isKey=true;
            this.index=index;
            this.key=key;
        }

        public String toString(){
            if(isKey) return"."+ key;
            else return "["+index+"]";
        }
    }

}
