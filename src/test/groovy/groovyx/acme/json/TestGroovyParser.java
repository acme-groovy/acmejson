package groovyx.acme.json;

import com.google.gson.Gson;
import groovy.json.JsonSlurper;

import java.io.IOException;
import java.io.Reader;

/**class for test purposes only to compare speed of parsing to groovy*/
public class TestGroovyParser extends AbstractJsonParser{

    public TestGroovyParser() {}
    public TestGroovyParser(AcmeJsonHandler handler) {}


    @Override
    protected Object doParse(Reader reader) throws IOException {
        JsonSlurper g = new JsonSlurper();
        return g.parse(reader);
    }
}
