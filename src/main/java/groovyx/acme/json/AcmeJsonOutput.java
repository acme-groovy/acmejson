package groovyx.acme.json;


import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/** helper json class to write object/array as json */
public class AcmeJsonOutput {
    /**
     * writes json to writer
     * @param o object to write
     * @param w writer where to write json
     * @param prettyPrint should apply pretty printing or not
     * @throws IOException if io error occurred
     */
    public static void writeJson(Object o, Writer w, boolean prettyPrint) throws IOException {
        new AcmeJsonWriter(w, prettyPrint).printValue(o);
        w.flush();
    }

    /**
     * writes compact json (no pretty print) to writer
     * @param o object to write
     * @param w writer where to write json
     * @throws IOException if io error occurred
     */
    public static void writeJson(Object o, Writer w) throws IOException {
        new AcmeJsonWriter(w, false).printValue(o);
        w.flush();
    }

    /**
     * converts input object to json representation string
     * @param o to convert
     * @param prettyPrint should we perform a pritty print
     * @return json
     */
    public static String toJson(Object o, boolean prettyPrint) {
        try {
            StringWriter w = new StringWriter();
            new AcmeJsonWriter(w, prettyPrint).printValue(o);
            return w.toString();
        } catch (IOException e) {
            throw new RuntimeException(e.toString(),e);
        }
    }

    /**
     * converts input object to json representation string
     * @param o to convert
     * @return json
     */
    public static String toJson(Object o) {
        String s = toJson(o,false);
        return s;
    }
}
