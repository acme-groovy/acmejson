package groovyx.acme.json;

import java.io.IOException;

public interface AcmeJsonHandler {
    void onObjectStart(AcmeJsonPath jpath)throws IOException;
    void onObjectEnd(AcmeJsonPath jpath)throws IOException;
    void onArrayStart(AcmeJsonPath jpath)throws IOException;
    void onArrayEnd(AcmeJsonPath jpath)throws IOException;
    void onValue(AcmeJsonPath jpath, Object value)throws IOException;
    Object getRoot();
}
