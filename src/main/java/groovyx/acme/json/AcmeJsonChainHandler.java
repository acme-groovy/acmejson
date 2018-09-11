package groovyx.acme.json;

import groovy.lang.Closure;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AcmeJsonChainHandler implements AcmeJsonHandler {

    AcmeJsonHandler next = null;
    Closure closure = null;
    Pattern pattern = null;
    String path = null;
    boolean hasNext = false;
    AcmeJsonBuilder builder=null;
    boolean onValue;

    public AcmeJsonChainHandler(){}

    public AcmeJsonChainHandler(Closure closure, String path, boolean value){
        this.closure=closure;
        this.path = path;
        this.onValue = value;
    }

    public AcmeJsonChainHandler(Closure closure, Pattern pattern, boolean value){
        this.closure=closure;
        this.pattern = pattern;
        this.onValue = value;
    }

    public AcmeJsonChainHandler (Closure closure, boolean value){
        this.closure=closure;
        this.onValue = value;
    }

    public AcmeJsonChainHandler setNext(AcmeJsonHandler next){
        this.next = next;
        hasNext=true;
        return this;
    }

    @Override
    public void onObjectStart(AcmeJsonPath jpath) {
        if(!onValue) {
            if (builder == null) {
                if (pattern != null) {
                    Matcher m = pattern.matcher(jpath.toString());
                    if (m.matches()) builder = new AcmeJsonBuilder();
                }
                else if (path != null) {if (jpath.toString().equals(path)) builder = new AcmeJsonBuilder();}
                else if (closure != null) builder = new AcmeJsonBuilder();
            }
            if (builder != null && !builder.isDone()) builder.onObjectStart(jpath);
        }
        if(this.hasNext){
            next.onObjectStart(jpath);
        }
    }

    @Override
    public void onObjectEnd(AcmeJsonPath jpath) {
        if(!onValue) {
            if (builder != null && !builder.isDone()) {
                builder.onObjectEnd(jpath);
                if (builder.isDone()) closure.call(jpath, builder.getRoot());
            }
        }
        if(this.hasNext){
            next.onObjectEnd(jpath);
        }
    }

    @Override
    public void onArrayStart(AcmeJsonPath jpath) {
        if(!onValue) {
            if (builder == null) {
                if (pattern != null) {
                    Matcher m = pattern.matcher(jpath.toString());
                    if (m.matches()) builder = new AcmeJsonBuilder();
                }
                else if (path != null) {if (jpath.toString().equals(path)) builder = new AcmeJsonBuilder();}
                else if (closure != null) builder = new AcmeJsonBuilder();
            }
            if (builder != null && !builder.isDone()) builder.onArrayStart(jpath);
        }
        if(this.hasNext){
            next.onArrayStart(jpath);
        }
    }

    @Override
    public void onArrayEnd(AcmeJsonPath jpath) {
        if(!onValue) {
            if (builder != null && !builder.isDone()) {
                builder.onArrayEnd(jpath);
                if (builder.isDone()) closure.call(jpath, builder.getRoot());
            }
        }
        if(this.hasNext){
            next.onArrayEnd(jpath);
        }
    }

    @Override
    public void onValue(AcmeJsonPath jpath, Object value) {
        if(onValue) {
            if (pattern != null) {
                Matcher m = pattern.matcher(jpath.toString());
                if (m.matches()) {
                    value = closure.call(jpath, value);
                }
            } else if (path != null) {
                if (jpath.toString().startsWith(path)) value = closure.call(jpath, value);
            } else if (closure != null) value = closure.call(jpath, value);
        }
        else {
            if (builder==null){
                if (pattern != null) {
                    Matcher m = pattern.matcher(jpath.toString());
                    if (m.matches()) builder = new AcmeJsonBuilder();
                }
                else if (path != null) {if (jpath.toString().equals(path)) builder = new AcmeJsonBuilder();}
                else if (closure != null) builder = new AcmeJsonBuilder();
            }
            if(builder!=null && !builder.isDone()) builder.onValue(jpath, value);
        }
        if(this.hasNext){
            next.onValue(jpath, value);
        }
    }

    @Override
    public Object getRoot() {
        return next.getRoot();
    }
}
