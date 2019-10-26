package groovyx.acme.json.io

class CharsSourceSequenceTest extends groovy.util.GroovyTestCase  {
    void test1(){
        def src = '123456789012345678901234567890abc'
        def r = new CharsSourceSequence(src);
        assert r.next()=='1'
        assert r.next()=='2'
        assert r.next()=='3'
        r.captureStart()
        r.next()=='4'
        r.next()=='5'
        r.next()=='6'
        assert r.captureEnd().toString()=="45"
    }
}
