package groovyx.acme.json;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Map;

/**
 * Class that provides common parser functions and requires implementation of the parse(Reader) method.
 */
public abstract class AbstractJsonParser {

    /**
     * handler to catch parser events
     */
    protected AcmeJsonHandler handler=null;

    public AbstractJsonParser(){
    }

    /**
     * By default AcmeJsonBuildHandler used to build parsed json object in memory. Note that this method could not be called twice.
     * @param handler will listen to json parser events
     * @return self
     */
    public AbstractJsonParser setHandler(AcmeJsonHandler handler){
        if(this.handler!=null)throw new IllegalStateException("Handler already set to "+this.handler+"");
        this.handler=handler;
        return this;
    }

    public Object parse(Reader reader) throws AcmeJsonParserException{
        if(handler==null)handler=new AcmeJsonBuildHandler();
        if(reader==null)throw new NullPointerException("reader == null");
        try {
            return doParse(reader);
        }catch(IOException e){
            throw new AcmeJsonParserException( e.toString() , e );
        }
    }

    /**
     * perform json parsing from reader in descendants
     * @param reader
     * @return
     * @throws AcmeJsonParserException
     */
    protected abstract Object doParse(Reader reader) throws AcmeJsonParserException, IOException;


    public Object parseText(String text) throws AcmeJsonParserException {
        if (text == null || text.length() == 0) {
            throw new IllegalArgumentException("The JSON input text should neither be null nor empty.");
        }
        return parse(new StringReader(text));
    }

    public Object parse(File file) throws AcmeJsonParserException {
        return parse(file, null);
    }


    public Object parse(File file, String charset) throws AcmeJsonParserException {
        Reader reader = null;
        try {
            if (charset == null || charset.length() == 0) charset = "UTF-8";
            reader = ResourceGroovyMethods.newReader(file, charset);
            return parse(reader);
        } catch(IOException e) {
            throw new AcmeJsonParserException("Failed to read file `"+file+"`: "+e.getMessage(), e);
        } finally {
            if (reader != null) {
                DefaultGroovyMethodsSupport.closeWithWarning(reader);
            }
        }
    }

    public Object parse(URL url) throws AcmeJsonParserException {
        return parseURL(url, null, null);
    }


    public Object parse(URL url, Map params) throws AcmeJsonParserException {
        return parseURL(url, params, null);
    }

    public Object parse(URL url, String charset) throws AcmeJsonParserException {
        return parseURL(url, null, charset);
    }


    public Object parse(URL url, Map params, String charset) throws AcmeJsonParserException {
        return parseURL(url, params, charset);
    }


    private Object parseURL(URL url, Map params, String charset) throws AcmeJsonParserException {
        Reader reader = null;
        try {
            if (charset == null || charset.length() == 0) charset = "UTF-8";
            reader = ResourceGroovyMethods.newReader(url, params, charset);
            return parse(reader);
        } catch(IOException e) {
            throw new AcmeJsonParserException("Failed to open URL `"+url+"`: "+e.getMessage(), e);
        } finally {
            if (reader != null) {
                DefaultGroovyMethodsSupport.closeWithWarning(reader);
            }
        }
    }

    /**
     * init handler to be AcmeJsonFilterHandler that allows to intercept and substitute values and transfer events to delegate handler.
     * @param builder the closure to init AcmeJsonFilterHandler. see AcmeJsonFilterHandler.Builder for details.
     * @return self with initialized handler
     */
    public AbstractJsonParser withFilter(Closure builder){
        AcmeJsonFilterHandler handler = new AcmeJsonFilterHandler();
        Object helper = handler.builder();
        //init handler through helper
        builder.rehydrate(helper, builder.getOwner(), helper).call(helper);
        //default delegate is null
        if(handler.getDelegate()==null)handler.setDelegate(new AcmeJsonNullHandler());
        this.setHandler(handler);
        return this;
    }

}
