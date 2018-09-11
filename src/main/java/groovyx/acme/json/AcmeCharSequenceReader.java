package groovyx.acme.json;
//package java.io;

import java.io.IOException;
import java.io.Reader;

/**
 * This class implements a charsequence represented as non synchronized reader.
 */
public class AcmeCharSequenceReader extends Reader {
    /** The character buffer. */
    protected CharSequence buf;

    /** The current buffer position. */
    protected int pos;

    /**
     *  The index of the end of this buffer.  There is not valid
     *  data at or beyond this index.
     */
    protected int count;

    /**
     * Creates a CharArrayReader from the specified array of chars.
     * @param buf       Input buffer (not copied)
     */
    public AcmeCharSequenceReader(CharSequence buf) throws IOException {
    	if(buf==null)throw new IOException("The source character stream is null");
        this.buf = buf;
        this.pos = 0;
        this.count = buf.length();
    }

    /** Checks to make sure that the stream has not been closed */
    private void ensureOpen() throws IOException {
        if (buf == null)
            throw new IOException("Stream closed");
    }

    /**
     * Reads a single character.
     *
     * @exception   IOException  If an I/O error occurs
     */
    public int read() throws IOException {
            if (buf == null || pos >= count)
                return -1;
            else
                return buf.charAt(pos++);
        }

    /**
     * Reads characters into a portion of an array.
     * @param b  Destination buffer
     * @param off  Offset at which to start storing characters
     * @param len   Maximum number of characters to read
     * @return  The actual number of characters read, or -1 if
     *          the end of the stream has been reached
     *
     * @exception   IOException  If an I/O error occurs
     */
    public int read(char b[], int off, int len) throws IOException {
            ensureOpen();
            if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException("b["+b.length+"], off:"+off+", len:"+len);
            } else if (len == 0) {
                return 0;
            }

            if (pos >= count) {
                return -1;
            }
            if (pos + len > count) {
                len = count - pos;
            }
            if (len <= 0) {
                return 0;
            }
            for(int i=0;i<len;i++) {
                b[off + i] = buf.charAt(i);
            }
            pos += len;
            return len;
    }

    /**
     * Skips characters.  Returns the number of characters that were skipped.
     *
     * <p>The <code>n</code> parameter may be negative, even though the
     * <code>skip</code> method of the {@link Reader} superclass throws
     * an exception in this case. If <code>n</code> is negative, then
     * this method does nothing and returns <code>0</code>.
     *
     * @param n The number of characters to skip
     * @return       The number of characters actually skipped
     * @exception  IOException If the stream is closed, or an I/O error occurs
     */
    public long skip(long n) throws IOException {
        ensureOpen();
        if (pos + n > count) {
            n = count - pos;
        }
        if (n < 0) {
            return 0;
        }
        pos += n;
        return n;
    }

    /**
     * Tells whether this stream is ready to be read.  Character-array readers
     * are always ready to be read.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public boolean ready() throws IOException {
        ensureOpen();
        return (count - pos) > 0;
    }

    /**
     * Tells whether this stream supports the mark() operation, which it does.
     */
    public boolean markSupported() {
        return false;
    }

    /**
     * Marks the present position in the stream.  Subsequent calls to reset()
     * will reposition the stream to this point.
     *
     * @param  readAheadLimit  Limit on the number of characters that may be
     *                         read while still preserving the mark.  Because
     *                         the stream's input comes from a character array,
     *                         there is no actual limit; hence this argument is
     *                         ignored.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void mark(int readAheadLimit) throws IOException {
        throw new IOException("not supported");
    }

    /**
     * Resets the stream to the most recent mark, or to the beginning if it has
     * never been marked.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void reset() throws IOException {
        throw new IOException("not supported");
    }

    /**
     * Closes the stream and releases any system resources associated with
     * it.  Once the stream has been closed, further read(), ready(),
     * mark(), reset(), or skip() invocations will throw an IOException.
     * Closing a previously closed stream has no effect.
     */
    public void close() {
        buf = null;
    }
}