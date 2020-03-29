package groovyx.acme.json;

import java.io.IOException;

/**
 * json handler does nothing and returns null as a final object.
 */
public class AcmeJsonNullHandler implements AcmeJsonHandler{

    @Override
    public void onObjectStart(AbstractJsonPath jpath) throws IOException {

    }

    @Override
    public void onObjectEnd(AbstractJsonPath jpath) throws IOException {

    }

    @Override
    public void onArrayStart(AbstractJsonPath jpath) throws IOException {

    }

    @Override
    public void onArrayEnd(AbstractJsonPath jpath) throws IOException {

    }

    @Override
    public void onValue(AbstractJsonPath jpath, Object value) throws IOException {

    }

    @Override
    public Object getRoot() {
        return null;
    }
}
