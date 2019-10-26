package groovyx.acme.json.io;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;


/** provides (better?) buffering for readers  and implements CharsSource */
public class CharsSourceReader extends CharsSource {

    private final Reader src;
    private final LinkedList<char[]> chunkList;
    private final int chunkSize;
    private int pos=0; //current reading pos among chunks
    private int chunkedCount=0; //number of chunked/buffered chars
    private int capture = -1; //not started

    public CharsSourceReader(Reader src, int chunkSize){
        this.src = src;
        this.chunkList = new LinkedList<>();
        this.chunkSize = chunkSize;
    }


    final int chunkNo(int i){
        return i/chunkSize;
    }

    final int chunkPos(int i){
        return i%chunkSize;
    }

    final int setChunkNo(int chunkNo, int i){
        return chunkNo*chunkSize + chunkPos(i);
    }

    @Override
    public int next() {
        if(pos<chunkedCount)return chunkList.get( chunkNo(pos) )[ chunkPos(pos++) ];
        //have to read/extend buffer
        if(capture==-1){
            //capture not started, so, reset buffer
            pos=0;
            //while(chunkList.size()>1)chunkList.removeLast(); //avoid cleanup memory up to the end of parsing
        }else{
            int chunkNo = chunkNo(capture);
            if(chunkNo>0){
                //remove first chunk because unused
                char[] chunk0 = chunkList.removeFirst();
                chunkNo--;
                pos          = setChunkNo( chunkNo, pos );
                chunkedCount = setChunkNo( chunkNo, chunkedCount );
                capture      = setChunkNo( chunkNo, capture );
                //add it to the end. we could avoid releasing chunks during parsing, so memory would be more stable
                chunkList.add( chunk0 );
            }
        }
        //ensure total chunks size
        if( chunkList.size()*chunkSize <=chunkedCount ){
            //add one more chunk
            chunkList.add( new char[chunkSize] );
        }
        //read into last chunk
        try {
            int start = chunkPos(chunkedCount);
            int read = src.read(  chunkList.getLast(), start, chunkSize - start );
            if(read<0)return -1; //no more chars
            chunkedCount+=read;
            return chunkList.get( chunkNo(pos) )[ chunkPos(pos++) ];
        } catch (IOException e) {
            throw new RuntimeException("Failed to read: "+e, e);
        }
    }

    @Override
    public void captureStart() throws IllegalStateException {
        if(capture!=-1)throw new IllegalStateException("capturing already started at pos = "+capture);
        capture = pos;
    }

    @Override
    public CharSequence captureEnd() throws IllegalStateException {
        if(capture==-1)throw new IllegalStateException("capturing not started but `end` requested at pos = "+pos);
        int realpos = pos-1;
        int len = realpos - capture;
        if(len==0)return "";
        if(len<0)throw new RuntimeException("unexpected negative length of captured text");
        int firstChunk = chunkNo(capture);
        int lastChunk = chunkNo(realpos);
        StringBuilder s = new StringBuilder(len+1);
        for(int i=firstChunk; i<=lastChunk; i++){
            int posStart = i==firstChunk ? chunkPos(capture) : 0;
            int posEnd   = i==lastChunk  ? chunkPos(realpos) : chunkSize;
            s.append( chunkList.get(i), posStart , posEnd-posStart);
        }
        return s;
    }
}
