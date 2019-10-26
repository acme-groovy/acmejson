package groovyx.acme.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import com.google.gson.Gson;

public class GsonParser extends AbstractJsonParser{

    public GsonParser() {}
    public GsonParser(AcmeJsonHandler handler) {}


    @Override
    protected Object doParse(Reader reader) throws IOException {
        Gson g = new Gson();
        return g.fromJson(reader,Object.class);
    }
}
