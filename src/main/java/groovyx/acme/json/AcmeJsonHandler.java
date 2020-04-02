package groovyx.acme.json;

import java.io.IOException;

/**
 * interface with json parsing events definition used in parsers, writers, builders, etc.
 */
public interface AcmeJsonHandler {
    void onObjectStart(AbstractJsonPath jpath)throws IOException;
    void onObjectEnd(AbstractJsonPath jpath)throws IOException;
    void onArrayStart(AbstractJsonPath jpath)throws IOException;
    void onArrayEnd(AbstractJsonPath jpath)throws IOException;
    void onValue(AbstractJsonPath jpath, Object value)throws IOException;
    Object getRoot();
}
