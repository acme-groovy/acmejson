/***/
package groovyx.acme.json;

import groovy.json.*;

import java.io.CharArrayWriter;


//@groovy.transform.CompileStatic
public class AcmeJsonTest extends groovy.util.GroovyTestCase {

    static String json = "{\"x\":\"y\\n\\tz\",\"o\":"+R(" \t",220000)+"{\"aaa\":1,\"b\":[21,{\"22\":2}"+R(",23,24,255,266,37777",100)+"],\"c\":3,\"d\":\"y\\n\\tz\"}}";
    int count=30000;
    long sleep = 0; //ms

    static String R(String s, int r){
        StringBuilder sb = new StringBuilder(r*s.length()+2);
        for(int i=0;i<r;i++)sb.append(s);
        return sb.toString();
    }
    static void println(String s){ System.out.println(s);}
    public AcmeJsonTest(){}
/*
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



    public void testLoadALL()throws Exception{
        long t=0;
        CharArrayWriter out=null;
        if(sleep>0)Thread.sleep(sleep);


        println("testLoadW");
        out = new CharArrayWriter();
        System.gc();
        Thread.sleep(1000);
        t=System.currentTimeMillis();
        for(int i=0;i<count;i++) {
            out.reset();
            new WJsonParser(new AcmeJsonWriter(out)).parse(json);
        }
        println("t = "+ ((System.currentTimeMillis()-t)/1000.0)+ " sec");

        println("testLoadY");
        out = new CharArrayWriter();
        System.gc();
        Thread.sleep(1000);
        t=System.currentTimeMillis();
        for(int i=0;i<count;i++) {
            out.reset();
            new YJsonParser(new AcmeJsonWriter(out)).parse(json);
        }
        println("t = "+ ((System.currentTimeMillis()-t)/1000.0)+ " sec");
		/*
        */

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
