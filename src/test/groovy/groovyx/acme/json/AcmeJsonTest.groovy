/***/
package groovyx.acme.json
//@groovy.transform.CompileStatic
public class AcmeJsonTest extends groovy.util.GroovyTestCase {

    //static String json = "{\"x\":\"y\\n\\t\\u0420\\u0001z\",\"o\":"+R(" \t",220000)+"{\"aaa\":1,\"b\":[21,{\"22\":2}"+R(",23,24,255,266,9991,9992,9993,9994",100)+"],\"c\":3,\"d\":\"y\\n\\tz\"}}";
    static String json = '''
        {
            "s1":"123\\n\\t456\\u0420-\\u0001",
            "i1":12345,
            "i2":-12345,
            "i3":0,
            "n1":null,
            "b1":true,
            "b2":false,
            "d1":123.45,
            "d2":-123.45,
            "d3":0.123,
            "d4":-0.123,
            "d5":0.0,
            "a1":[11,22,33,44.0, {"xxx":11,"yyy":22}],
            "a2":[111,222,333]
        }
    ''';

    public AcmeJsonTest(){}


    public void testGson()throws Exception{
        def o = new GsonParser().parseText("[\""+("abcdefgh1234567890"*200)+"\","+json+"]");
        assert o[1].i1==12345
        assert o[1].d4==-0.123
        assert o[1].i2==-12345
        assert o[0]==("abcdefgh1234567890"*200)

        //println (out.toString());
    }

    public void testAcme()throws Exception{
        def o = new AcmeJsonParser().parseText("[\""+("abcdefgh1234567890"*200)+"\","+json+"]");
        assert o[1].i1==12345
        assert o[1].d4==-0.123
        assert o[1].i2==-12345
        assert o[0]==("abcdefgh1234567890"*200)

        //println (out.toString());
    }

    public void testPathMatcher(){
        def m = new JsonPathMatcher('$["aa.a"]["b.bb"][*].ccc[123]..xxx')
        assert m.tokens.size()==7
        assert m.tokens[0].key=='aa.a'
        assert m.tokens[1].key=='b.bb'
        assert m.tokens[2]==JsonPathMatcher.T_ANY
        assert m.tokens[4].key instanceof Integer
        assert m.tokens[5]==JsonPathMatcher.T_DEEP
        assert m.tokens[6].key=='xxx'
    }

    public void testPathMatcher1(){
        def m = new JsonPathMatcher('$.aa.bb.cc')
        assert m.tokens.size()==3
        assert m.tokens[0].key=='aa'
        assert m.tokens[1].key=='bb'
        assert m.tokens[2].key=='cc'
        def p = new ListJsonPath().push(0,'aa').push(0,'bb').push(0,'cc')
        assert m.matches(p)
    }

    public void testPathMatcher2(){
        def m = new JsonPathMatcher('$..bb.cc')
        assert m.tokens.size()==3
        assert m.tokens[0]==JsonPathMatcher.T_DEEP
        def p = new ListJsonPath().push(0,'aa').push(0,'bb').push(0,'cc')
        assert m.matches(p)
    }

    public void testPathMatcher3(){
        def m = new JsonPathMatcher('$..cc')
        assert m.tokens.size()==2
        assert m.tokens[0]==JsonPathMatcher.T_DEEP
        def p = new ListJsonPath().push(0,'aa').push(0,'bb').push(0,'cc')
        assert m.matches(p)
    }

    public void testPathMatcher3_1(){
        def m = new JsonPathMatcher('$..cc')
        assert m.tokens.size()==2
        assert m.tokens[0]==JsonPathMatcher.T_DEEP
        def p = new ListJsonPath().push(0,'aa').push(0,'bb').push(3,null).push(0,'cc')
        assert m.matches(p)
    }

    public void testPathMatcher3_2(){
        def m = new JsonPathMatcher('$.aa..')
        assert m.tokens.size()==2
        assert m.tokens[1]==JsonPathMatcher.T_DEEP
        def p = new ListJsonPath().push(0,'aa').push(0,'bb').push(3,null).push(0,'cc')
        assert m.matches(p)
    }

    public void testPathMatcher4(){
        def m = new JsonPathMatcher('$[*].bb.cc')
        assert m.tokens.size()==3
        assert m.tokens[0]==JsonPathMatcher.T_ANY
        def p = new ListJsonPath().push(0,'aa').push(0,'bb').push(0,'cc')
        assert m.matches(p)
    }

    public void testFilter1(){
        def o = new AcmeJsonParser().withFilter {
            onValue('$..'){v,p->
                return v
            }
            build()
        }.parseText("[\""+("abcdefgh1234567890"*200)+"\","+json+"]")

        assert o[1].i1==12345
        assert o[1].d4==-0.123
        assert o[1].i2==-12345
        assert o[0]==("abcdefgh1234567890"*200)
    }

    public void testFilterSubstValues(){
        def o = new AcmeJsonParser().withFilter {
            onValue('$..'){Object v, AbstractJsonPath p->
                return p.peek().isKey() && p.peek().getKey().startsWith('d') ? 999 : v
            }
            build()
        }.parseText("[\""+("abcdefgh1234567890"*200)+"\","+json+"]")

        assert o[1].i1==12345
        assert o[1].d4==999
        assert o[1].i2==-12345
        assert o[0]==("abcdefgh1234567890"*200)
    }

    public void testFilterSubstValuesAndObj(){
        def o = new AcmeJsonParser().withFilter {
            onValue('$..a1[*]'){Object v, AbstractJsonPath p->
                println "$p -> $v"
                return  v
            }
            build()
        }.parseText("[\""+("abcdefgh1234567890"*200)+"\","+json+"]")

        assert o[1].i1==12345
        assert o[1].d4==-0.123
        assert o[1].i2==-12345
        assert o[0]==("abcdefgh1234567890"*200)
    }

    public void testFilterAndWrite(){
        new File("./build/tmp/write.json").withWriter("UTF-8") { w ->
            new AcmeJsonParser().withFilter {
                onValue('$..a1[*]') { Object v, AbstractJsonPath p ->
                    println "$p -> $v"
                    return v
                }
                write(w,true)
            }.parseText("[\"" + ("abcdefgh1234567890" * 200) + "\"," + json + "]")
        }
    }

    public void testJsonWrite(){
        new File("./build/tmp/write.json").withWriter("UTF-8"){w->
            def jw = new AcmeJsonWriteHandler(w,true);
            //jw.onArrayStart()
            for(int i=0;i<1000000;i++){

            }

        }
    }




    /*
    @groovy.transform.CompileStatic
    public void testLoadYJP(){
        println "testLoadYJP"
        if(sleep)Thread.sleep(sleep)
        def out = new CharArrayWriter()
        for(int i=0;i<count;i++) {
            out.reset();
            new YJsonParser(new AcmeJsonWriteHandler(out)).parse(json)
        }
    }
    @groovy.transform.CompileStatic
    public void testLoadZJP(){
        println "testLoadZJP"
        if(sleep)Thread.sleep(sleep)
        def out = new CharArrayWriter()
        for(int i=0;i<count;i++) {
            out.reset();
            new ZJsonParser(new AcmeJsonWriteHandler(out)).parse(json)
        }
    }
    @groovy.transform.CompileStatic
    public void testLoadWJP(){
        println "testLoadWJP"
        if(sleep)Thread.sleep(sleep)
        def out = new CharArrayWriter()
        for(int i=0;i<count;i++) {
            out.reset();
            new WJsonParser(new AcmeJsonWriteHandler(out)).parse(json)
        }
    }

    /*

    /*
    public void testLoadXJP(){
        def out = new StringWriter()
        for(int i=0;i<count;i++) {
            new XJsonParser(new AcmeJsonWriteHandler(out)).parse(json)
        }
    }
    public void testLoadAJP(){
        def out = new StringWriter()
        for(int i=0;i<count;i++) {
            new AcmeJsonParser().target(new AcmeJsonWriteHandler(out)).parseText(json)
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