package groovyx.acme.json;

import groovy.lang.Closure;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;


/**
 * provides easy way to build a sequence of closures to manipulate parsing values with closures and pass result into the delegate
 */
public class AcmeJsonFilterHandler implements AcmeJsonHandler {
    protected int size=0; //number of filters
    protected ArrayList<Filter> filters = new ArrayList<>(); //map dedicated for values catchers
    protected AcmeJsonHandler delegate=null;

    protected AcmeJsonBuildHandler builder = null; //used to build complex value if needed
    protected Filter filter4builder = null;   //used when started building complex value to know who is interested in result

    public AcmeJsonFilterHandler(){}

    /**
     * sets the delegation handler that will be called as a handler after all filtered values
     * @param delegate
     * @return self
     */
    public AcmeJsonFilterHandler setDelegate(AcmeJsonHandler delegate){
        this.delegate=delegate;
        return this;
    }

    /**
     * returns current delegate or null
     */
    public AcmeJsonHandler getDelegate(){
        return delegate;
    }

    /**
     * adds filter triggered on value that matches path. the closure could return a new value of the item.
     * @param path string that represents the absolute dot-notated json path to the item to catch
     * @param closure code to call if matched path found. there are should be two parameters for the closure: jsonPath, and value
     *                closure must return the value that considered as a new value at this json path
     * @return self
     */
    public AcmeJsonFilterHandler addValueFilter(String path, Closure closure){
        filters.add( new Filter(path,closure) );
        return this;
    }

    private Filter findMatchingFilter(AbstractJsonPath jpath){
        for(Filter f: filters){
            if(f.matcher.matches(jpath))return f;
        }
        return null;
    }

    @Override
    public void onObjectStart(AbstractJsonPath jpath) throws IOException {
        if (builder != null) {
            builder.onObjectStart(jpath); //we are in state of building object
        } else {
            filter4builder = findMatchingFilter(jpath);
            if(filter4builder!=null) {
                if(filter4builder.matcher.endless()){
                    //just delegate
                    delegate.onObjectStart(jpath);
                }else {
                    builder = new AcmeJsonBuildHandler();
                    builder.onObjectStart(jpath); //we are in state of building object
                }
            }else {
                delegate.onObjectStart(jpath);
            }
        }
    }

    @Override
    public void onObjectEnd(AbstractJsonPath jpath) throws IOException {
        if (builder != null) {
            builder.onObjectEnd(jpath); //we are in state of building object
            if(builder.isDone()){
                //if object built notify the filter
                Object newValue = filter4builder.call(builder.getRoot(), jpath);
                delegate.onValue(jpath, newValue);
                builder = null;
            }
        } else {
            delegate.onObjectEnd(jpath);
        }
    }

    @Override
    public void onArrayStart(AbstractJsonPath jpath) throws IOException {
        if (builder != null) {
            builder.onArrayStart(jpath); //we are in state of building object
        } else {
            filter4builder = findMatchingFilter(jpath);
            if(filter4builder!=null) {
                if(filter4builder.matcher.endless()){
                    //just delegate
                    delegate.onArrayStart(jpath);
                }else {
                    builder = new AcmeJsonBuildHandler();
                    builder.onArrayStart(jpath); //we are in state of building object
                }
            }else {
                delegate.onArrayStart(jpath);
            }
        }
    }

    @Override
    public void onArrayEnd(AbstractJsonPath jpath) throws IOException {
        if (builder != null) {
            builder.onArrayEnd(jpath); //we are in state of building object
            if(builder.isDone()){
                //if object built notify the filter
                Object newValue = filter4builder.call(builder.getRoot(), jpath);
                delegate.onValue(jpath, newValue);
                builder = null;
            }
        } else {
            delegate.onArrayEnd(jpath);
        }
    }

    @Override
    public void onValue(AbstractJsonPath jpath, Object value) throws IOException {
        if (builder != null) {
            builder.onValue(jpath, value);
        } else {
            filter4builder = findMatchingFilter(jpath);
            if(filter4builder!=null) {
                //notify the filter
                Object newValue = filter4builder.call(value, jpath);
                delegate.onValue(jpath, newValue);
            }else {
                delegate.onValue(jpath, value);
            }
        }

    }

    @Override
    public Object getRoot() {
        return delegate.getRoot();
    }

    public Builder builder(){
        return new Builder();
    }

    final static class Filter{
        final JsonPathMatcher matcher;
        final Closure closure;
        Filter(String p, Closure c){
            this.matcher=new JsonPathMatcher(p);
            this.closure=c;
        }
        final Object call(Object value, AbstractJsonPath jpath){
            Object newValue = null;
            if(closure.getMaximumNumberOfParameters()==1){
                newValue = closure.call( value );
            }else{
                newValue = closure.call( value, jpath );
            }
            return newValue;
        }
    }

    /**
     * helper class to support building
     */
    public final class Builder{
        /**
         * registers another filter to intercept when json path matches json source
         * @param path a simple json path see: JsonPathMatcher
         * @param closure a one or two parameter closure that will be called when path matches json source.
         *                first parameter - value, second optional parameter - json path.
         *                the returned value of closure will replace original value before passing to delegated handler.
         */
        public void onValue(String path, Closure closure){
            addValueFilter(path,closure);
        }

        /**
         * defines the delegate handler to be AcmeJsonBuildHandler that builds object in memory from json source
         */
        public void build(){
            setDelegate( new AcmeJsonBuildHandler());
        }
        /**
         * defines the delegate handler to be AcmeJsonWriteHandler that writes json to writer from json source
         */
        public AcmeJsonWriteHandler write(Writer w, boolean prettyPrint){
            if(w==null){
                setDelegate( new AcmeJsonNullHandler() );
                return null;
            } else {
                AcmeJsonWriteHandler h = new AcmeJsonWriteHandler(w,prettyPrint);
                setDelegate( h );
                return h;
            }
        }
        /**
         * defines the delegate handler to be AcmeJsonWriteHandler that writes json to writer from json source
         */
        public AcmeJsonWriteHandler write(Writer w){
            return write(w,false);
        }

    }

}
