package groovyx.acme.json;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class to parse and later match json path. It supports only simple paths like <code>$.key1.[*]["key2"]..key3</code>.
 * Where
 * <table summary="">
 *     <tr class="rowColor"><td>$</td><td>root of document and must be present</td></tr>
 *     <tr class="rowColor"><td>..</td><td>any depth must be followed by key</td></tr>
 *     <tr class="rowColor"><td>[*]</td><td>matches to any key including any element in array</td></tr>
 *     <tr class="rowColor"><td>["key"]</td><td>matches key. use this notation if key contains spec symbols (space, dot, etc)</td></tr>
 *     <tr class="rowColor"><td>[int]</td><td>matches exact array element where int could be greater or equals to 0</td></tr>
 * </table>
 *
 * Note: no spaces allowed.
 */
public class JsonPathMatcher {
    static final JPToken T_DEEP=new JPToken(0.1);  //  ..
    static final JPToken T_ANY =new JPToken(0.2);  //  [*]

    ArrayList<JPToken> tokens = new ArrayList<>();

    private static final int S_NONE=-1;
    private static final int S_DEF=0;  //default state
    private static final int S_BR_S=1; //brackets started
    private static final int S_BR_E=2; //brackets end expected
    private static final int S_DQ_S=3; //DQUOTE started
    private static final int S_DQ_E=4; //DQUOTE escaped char
    private static final int S_KEY=7;  //unquoted key
    private static final int S_DOT=8;  //last was dot
    private static final int S_INT=10; //started reading int in brackets
    private static final int S_DEEP=11;//deep was here





    /**
     * parses string path and builds internal list of predicates
     * @param spath the string representation of json path
     */
    public JsonPathMatcher(String spath){
        int len=spath.length();
        int state = -1; //-1 start,0 default (space), '"' string, '.' unquoted key expected, '[' brackets notation
        int start = S_NONE; //caprure start
        StringBuilder qStr=new StringBuilder();
        for(int i=0;i<len;i++){
            char c = spath.charAt(i);
            switch (state){
                case S_NONE:
                    if(c=='$')state=S_DEF;
                    else throw new RuntimeException("Json path must start with `$`");
                    break;
                case S_DEF:
                    if(c=='.'){
                        state=S_DOT;
                    }else if(c=='['){
                        state=S_BR_S;
                    }else throw new RuntimeException("Json path: unexpected character `"+c+"` at position "+i+" in path `"+spath+"`. Expected: `.`, `[`");
                    break;
                case S_DOT:
                    if(c=='.'){
                        add(T_DEEP);
                        state=S_DEEP;
                    }else if(c=='['){
                        state=S_BR_S;
                    }else{
                        state=S_KEY;
                        start=i; //we are in unquoted key state let's remember position to catch value
                    }
                    break;
                case S_DEEP:
                    if(c=='.'){
                        throw new RuntimeException("Json path: unexpected character `"+c+"` at position "+i+" in path `"+spath+"`. Expected: `[`, or any word char");
                    }else if(c=='['){
                        state=S_BR_S;
                    }else{
                        state=S_KEY;
                        start=i; //we are in unquoted key state let's remember position to catch value
                    }
                    break;
                case S_BR_S:
                    if(c=='*'){
                        add(T_ANY);
                        state=S_BR_E;
                    }else if(c=='"') {
                        state = S_DQ_S;
                    }else if(c>='0' && c<='9'){
                        state = S_INT;
                        start = i;
                    }else throw new RuntimeException("Json path: unexpected character `"+c+"` at position "+i+" in path `"+spath+"`. Expected: `*`, `\"`");
                    break;
                case S_INT:
                    if(c>='0' && c<='9'){
                        //nothing to do
                    }else if(c==']'){
                        String key=spath.substring(start,i);
                        add(new JPToken(new Integer(key)));
                        state = S_DEF;
                    }else throw new RuntimeException("Json path: unexpected character `"+c+"` at position "+i+" in path `"+spath+"`. Expected: `]`");
                    break;
                case S_KEY:
                    if(c=='.'){
                        String key=spath.substring(start,i);
                        if(key.equals("*"))add(T_ANY);
                        else add(new JPToken(key));
                        state=S_DOT;
                    }else if(c=='['){
                        String key=spath.substring(start,i);
                        if(key.equals("*"))add(T_ANY);
                        else add(new JPToken(key));
                        state=S_BR_S;
                    }else{
                        //nothing to do.
                    }
                    break;
                case S_DQ_S:
                    if(c=='"'){
                        add(new JPToken(qStr.toString()));
                        qStr.setLength(0);
                        state=S_BR_E;
                    }else if(c=='\\'){
                        state=S_DQ_E;
                    }else{
                        qStr.append(c);
                    }
                    break;
                case S_DQ_E:
                    if(c=='"')qStr.append('"');
                    else if(c=='\\')qStr.append('\\');
                    else if(c=='t')qStr.append('\t');
                    else if(c=='r')qStr.append('\r');
                    else if(c=='n')qStr.append('\n');
                    else throw new RuntimeException("Json path: unsupported escaped character `"+c+"` at position "+i+" in path `"+spath+"`. Expected: `\\`, `r`, `n`, `t`");
                    state=S_DQ_S;
                    break;
                case S_BR_E:
                    if(c!=']')throw new RuntimeException("Json path: unexpected character `"+c+"` at position "+i+" in path `"+spath+"`. Expected: `]`");
                    state=S_DEF;
                    break;
                default:
                    throw new RuntimeException("Json path: unexpected state `"+state+"` at position "+i+" in path `"+spath+"`");
            }
        }
        if(state==S_KEY) {
            String key = spath.substring(start);
            if(key.equals("*"))add(T_ANY);
            else add(new JPToken(key));
        }
    }

    public boolean matches(AbstractJsonPath path){
        int pi=0, plen=path.size();
        int ti=0, tlen=tokens.size();
        if(tlen==0 && plen==0)return true;

        while(pi<plen && ti<tlen){
            JPToken t = tokens.get(ti);
            if(t==T_ANY){
                ti++;
                pi++;
            }else if(t==T_DEEP){
                if(ti+1==tlen)return true;     //deep is the last item, so rest of path matches it
                JPToken tn = tokens.get(ti+1); //next token
                AbstractJsonPath.Element p = path.get(pi);
                if(p.isKey() && tn.key.equals(p.getKey()) ){
                    //next token matched current path element
                    ti+=2;
                }
                pi++;
            }else{
                //let's match token/index
                AbstractJsonPath.Element p = path.get(pi);
                if(t.key instanceof Integer){
                    if(p.isKey())return false; //path element not an index
                    if( ((Integer)t.key).intValue()!=p.getIndex())return false;
                }else{
                    if(!p.isKey())return false; //path element not a key
                    if(!t.key.equals(p.getKey()))return false;
                }
                ti++;
                pi++;
            }
        }
        return (pi==plen && ti==tlen);
    }

    public boolean endless(){
        return tokens.size()>0 && tokens.get(tokens.size()-1)==T_DEEP;
    }

    private void add(JPToken t){
        if(tokens.size()>0 && tokens.get(tokens.size()-1)==T_DEEP && (t==T_DEEP || t==T_ANY || t.key instanceof Number)){
            throw new RuntimeException("Json path: deep `..` must be followed with key");
        }
        tokens.add(t);
    }
    public String toString(){
        return tokens.toString();
    }

    static class JPToken{
        JPToken(Object key){
            this.key=key;
        }
        final Object key;
        public String toString(){
            if(this==T_DEEP)return "[..]";
            if(this==T_ANY)return "[*]";
            if(key instanceof Integer)return "["+key+"]";
            return (String)key;
        }
    }
}
