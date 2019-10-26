package groovyx.acme.json.io

class CharsSourceReaderTest extends groovy.util.GroovyTestCase  {
    void test1(){
        def src = new StringReader('123456789012345678901234567890abc')
        def r = new CharsSourceReader(src, 10);
        assert r.next()=='1'
        assert r.next()=='2'
        assert r.next()=='3'
        r.captureStart()
        assert r.next()=='4'
        assert r.next()=='5'
        assert r.next()=='6'
        assert r.captureEnd().toString()=="45"
    }
    void test2(){
        def src = new StringReader('123456789012345678901234567890abc')
        def r = new CharsSourceReader(src, 10);
        assert r.next()=='1'
        assert r.next()=='2'
        assert r.next()=='3'
        r.captureStart()
        assert r.next()=='4'
        assert r.captureEnd().toString()==""
    }
    void testLoop() {
        int start = 3
        def src = '123456789012345678901234567890abc'
        for (int n = start; n < 23; n++) {
            def r = new CharsSourceReader(new StringReader(src), 10);
            for(int i=0;i<start;i++) {
                assert r.next() == (int)src.charAt(i)
            }
            r.captureStart()
            for(int i=0;i<n;i++) {
                assert r.next() == (int)src.charAt(i + start)
            }
            assert r.captureEnd().toString()==src.substring(start,start+n-1)
        }
    }
}
