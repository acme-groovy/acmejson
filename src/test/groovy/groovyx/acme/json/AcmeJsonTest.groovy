/***/
package groovyx.acme.json;

import groovy.json.*

import java.util.regex.Matcher
import java.util.regex.Pattern;

class AcmeJsonTest extends GroovyTestCase {

    static String json = "{\"x\":\"y\\n\\tz\",\"o\":"+(' '*220000)+"{\"aaa\":1,\"b\":[21,{\"22\":2}"+(",23,24,255,266,37777"*100)+"],\"c\":3,\"d\":\"y\\n\\tz\"}}"
    String path = "\$.o.b";
    int count=200000;
    long sleep = 0; //ms
/*
    public void testCapturer(){
        def r = new StringReader("1234567890abcdefghijklmnopqrstuv");
        def c = new Capturer(r, 10);
        assert c.read()==(char)'1'
        assert c.read()==(char)'2'
        assert c.read()==(char)'3'
        c.startCapture();
        assert c.read()==(char)'4'
        assert c.read()==(char)'5'
        assert c.read()==(char)'6'
        assert c.read()==(char)'7'
        assert c.read()==(char)'8'
        assert c.read()==(char)'9'
        assert c.read()==(char)'0'
        assert c.read()==(char)'a'
        assert c.read()==(char)'b'
        assert c.read()==(char)'c'
        assert c.read()==(char)'d'
        assert c.read()==(char)'e'
        c.endCapture().toString()=="4567890abcd";
    }
    public void testCapturer2(){
        def r = new StringReader("1234567890abcdefghijklmnopqrstuv");
        def c = new Capturer(r, 10);
        assert c.read()==(char)'1'
        assert c.read()==(char)'2'
        assert c.read()==(char)'3'

        c.startCapture();
        assert c.read()==(char)'4'
        assert c.read()==(char)'5'
        c.endCapture().toString()=="4";
        assert c.read()==(char)'6'

        assert c.read()==(char)'7'
        assert c.read()==(char)'8'
        assert c.read()==(char)'9'
        c.startCapture();
        assert c.read()==(char)'0'
        assert c.read()==(char)'a'
        assert c.read()==(char)'b'
        assert c.read()==(char)'c'
        assert c.read()==(char)'d'
        assert c.read()==(char)'e'
        c.endCapture().toString()=="0abcd";
    }

    public void testX0(){
        def out = new StringWriter()
        new XJsonParser(new AcmeJsonWriter(out,true)).parse(json)
        println out;
    }
    public void testY0(){
        println "Y::"
        def out = new StringWriter()
        new YJsonParser(new AcmeJsonWriter(out,true)).parse(json)
        println out;
    }
    public void testW0() {
        println "W::"
        def out = new StringWriter()
        new WJsonParser(new AcmeJsonWriter(out, true)).parse(json)
        println out;
    }
    public void testW0() {
        println "W::"
        def out = new StringWriter()
        new WJsonParser(new AcmeJsonWriter(out, true)).parse(json)
        println out;
    }

    public void testZ0() {
        println "Z::"
        def out = new StringWriter()
        new ZJsonParser(new AcmeJsonWriter(out, true)).parse(json)
        println out;
    }


*/



    @groovy.transform.CompileStatic
    public void testLoadALL(){
        long t=0;
        def out=null;
        if(sleep)Thread.sleep(sleep)



        println "testLoadW"
        out = new CharArrayWriter()
        System.gc();
        Thread.sleep(1000);
        t=System.currentTimeMillis()
        for(int i=0;i<count;i++) {
            out.reset();
            new WJsonParser(new AcmeJsonWriter(out)).parse(json)
        }
        println "t = ${(System.currentTimeMillis()-t)/1000.0} sec"

        println "testLoadY"
        out = new CharArrayWriter()
        System.gc();
        Thread.sleep(1000);
        t=System.currentTimeMillis()
        for(int i=0;i<count;i++) {
            out.reset();
            new YJsonParser(new AcmeJsonWriter(out)).parse(json)
        }
        println "t = ${(System.currentTimeMillis()-t)/1000.0} sec"

        println "testLoadZ"
        out = new CharArrayWriter()
        System.gc();
        Thread.sleep(1000);
        t=System.currentTimeMillis()
        for(int i=0;i<count;i++) {
            out.reset();
            new ZJsonParser(new AcmeJsonWriter(out)).parse(json)
        }
        println "t = ${(System.currentTimeMillis()-t)/1000.0} sec"

    }
    /*
    @groovy.transform.CompileStatic
    public void testLoadYJP(){
        println "testLoadYJP"
        if(sleep)Thread.sleep(sleep)
        def out = new CharArrayWriter()
        for(int i=0;i<count;i++) {
            out.reset();
            new YJsonParser(new AcmeJsonWriter(out)).parse(json)
        }
    }
    @groovy.transform.CompileStatic
    public void testLoadZJP(){
        println "testLoadZJP"
        if(sleep)Thread.sleep(sleep)
        def out = new CharArrayWriter()
        for(int i=0;i<count;i++) {
            out.reset();
            new ZJsonParser(new AcmeJsonWriter(out)).parse(json)
        }
    }
    @groovy.transform.CompileStatic
    public void testLoadWJP(){
        println "testLoadWJP"
        if(sleep)Thread.sleep(sleep)
        def out = new CharArrayWriter()
        for(int i=0;i<count;i++) {
            out.reset();
            new WJsonParser(new AcmeJsonWriter(out)).parse(json)
        }
    }

    /*

    /*
    public void testLoadXJP(){
        def out = new StringWriter()
        for(int i=0;i<count;i++) {
            new XJsonParser(new AcmeJsonWriter(out)).parse(json)
        }
    }
    public void testLoadAJP(){
        def out = new StringWriter()
        for(int i=0;i<count;i++) {
            new AcmeJsonParser().target(new AcmeJsonWriter(out)).parseText(json)
        }
    }
/**/


    /*
    public void testClassic(){
        println "Classic\n"
        JsonSlurperClassic parser = new JsonSlurperClassic();
        def res = parser.parseText(json);
        println JsonOutput.toJson(res);
        println "\n"
    }
    */

    /*

    public void testAcmeParserValue(){
        println "\nParser using onValue:"
        println "was:\n"+json+"\nout:"
        println new AcmeJsonParser().onValue(path){jpath, value->1111}.onValue{jpath, value->value=="z"? "zzz" : value}.target(new StringWriter(), true).parseText(json)
    }


    public void testAcmeOutput(){
        println "\nOutput:"
        println "was:\n"+json+"\nout:"
        println new AcmeJsonOutput(new AcmeJsonParser().parseText(json)).setIndent(true).writeTo(new StringWriter())
    }

    public void testAcmeParserEach(){
        println "\nParser using each:"
        println "jpath: "+path
        new AcmeJsonParser().each(path){jpath, obj->println new AcmeJsonOutput(obj).setIndent(true).writeTo(new StringWriter())}.parseText(json);
    }
    */
}
