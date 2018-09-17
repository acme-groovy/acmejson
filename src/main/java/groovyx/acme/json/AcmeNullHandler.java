package groovyx.acme.json;

import java.io.IOException;

public class AcmeNullHandler implements AcmeJsonHandler{

    @Override
    public void onObjectStart(AcmeJsonPath jpath) throws IOException {

    }

    @Override
    public void onObjectEnd(AcmeJsonPath jpath) throws IOException {

    }

    @Override
    public void onArrayStart(AcmeJsonPath jpath) throws IOException {

    }

    @Override
    public void onArrayEnd(AcmeJsonPath jpath) throws IOException {

    }

    @Override
    public void onValue(AcmeJsonPath jpath, Object value) throws IOException {

    }

    @Override
    public Object getRoot() {
        return null;
    }
}
