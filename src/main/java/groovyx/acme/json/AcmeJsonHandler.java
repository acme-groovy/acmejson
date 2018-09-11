package groovyx.acme.json;

public interface AcmeJsonHandler {
    void onObjectStart(AcmeJsonPath jpath);
    void onObjectEnd(AcmeJsonPath jpath);
    void onArrayStart(AcmeJsonPath jpath);
    void onArrayEnd(AcmeJsonPath jpath);
    void onValue(AcmeJsonPath jpath, Object value);
    Object getRoot();
}
