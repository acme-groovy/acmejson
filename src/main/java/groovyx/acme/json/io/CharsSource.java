package groovyx.acme.json.io;

public abstract class CharsSource {
    /**returns relative next character*/
    public abstract int next();
    /**starts capturing of the source characters */
    public abstract void captureStart() throws IllegalStateException;
    /**stops capturing of the source characters and returns captured buffer as CharSequence*/
    public abstract CharSequence captureEnd() throws IllegalStateException;
}
