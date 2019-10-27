package groovyx.acme.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import com.google.gson.Gson;

/**class for test purposes only to compare speed of parsing to gson*/
public class TestGsonParser extends AbstractJsonParser{

    public TestGsonParser() {}
    public TestGsonParser(AcmeJsonHandler handler) {}


    @Override
    protected Object doParse(Reader reader) throws IOException {
        Gson g = new Gson();
        return g.fromJson(reader,Object.class);
    }
}
