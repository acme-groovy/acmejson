/***/
package groovyx.acme.json

import groovy.json.JsonOutput

public class AcmeJsonTest extends groovy.util.GroovyTestCase {

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
        def o = new TestGsonParser().parseText("[\""+("abcdefgh1234567890"*200)+"\","+json+"]");
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
        def p = new TestJsonPath().push(0,'aa').push(0,'bb').push(0,'cc')
        assert m.matches(p)
    }

    public void testPathMatcher2(){
        def m = new JsonPathMatcher('$..bb.cc')
        assert m.tokens.size()==3
        assert m.tokens[0]==JsonPathMatcher.T_DEEP
        def p = new TestJsonPath().push(0,'aa').push(0,'bb').push(0,'cc')
        assert m.matches(p)
    }

    public void testPathMatcher3(){
        def m = new JsonPathMatcher('$..cc')
        assert m.tokens.size()==2
        assert m.tokens[0]==JsonPathMatcher.T_DEEP
        def p = new TestJsonPath().push(0,'aa').push(0,'bb').push(0,'cc')
        assert m.matches(p)
    }

    public void testPathMatcher3_1(){
        def m = new JsonPathMatcher('$..cc')
        assert m.tokens.size()==2
        assert m.tokens[0]==JsonPathMatcher.T_DEEP
        def p = new TestJsonPath().push(0,'aa').push(0,'bb').push(3,null).push(0,'cc')
        assert m.matches(p)
    }

    public void testPathMatcher3_2(){
        def m = new JsonPathMatcher('$.aa..')
        assert m.tokens.size()==2
        assert m.tokens[1]==JsonPathMatcher.T_DEEP
        def p = new TestJsonPath().push(0,'aa').push(0,'bb').push(3,null).push(0,'cc')
        assert m.matches(p)
    }

    public void testPathMatcher4(){
        def m = new JsonPathMatcher('$[*].bb.cc')
        assert m.tokens.size()==3
        assert m.tokens[0]==JsonPathMatcher.T_ANY
        def p = new TestJsonPath().push(0,'aa').push(0,'bb').push(0,'cc')
        assert m.matches(p)
    }

    public void testPathMatcher5(){
        def m = new JsonPathMatcher('$[*].a.*["b"].*')
        assert m.tokens.size()==5
        assert m.tokens[0]==JsonPathMatcher.T_ANY
        assert m.tokens[1].key=='a'
        assert m.tokens[2]==JsonPathMatcher.T_ANY
        assert m.tokens[3].key=="b"
        assert m.tokens[4]==JsonPathMatcher.T_ANY
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
                return  v
            }
            build()
        }.parseText("[\""+("abcdefgh1234567890"*200)+"\","+json+"]")

        assert o[1].i1==12345
        assert o[1].d4==-0.123
        assert o[1].i2==-12345
        assert o[1].a1[4]==[xxx:11,yyy:22]
        assert o[0]==("abcdefgh1234567890"*200)
    }

    public void testFilterAndWrite1(){
        def json1 = '''
            [
                {"x":1, "y":[1,2,{"z1":111, "z2":222, "z3":[123,456]}]},
                [11,22]
            ]
        '''
        def w = new StringWriter();
        new AcmeJsonParser().withFilter {
            write(w).setIndent('    ')
        }.parseText(json1)
        //println w.toString()
        //println groovy.json.JsonOutput.prettyPrint(json1)
        assert groovy.json.JsonOutput.prettyPrint(json1) == w.toString()
    }

    public void testFilterAndWrite2(){
        def w = new StringWriter();
        new AcmeJsonParser().withFilter {
            onValue('$..a1[*]'){v,p->
                if(v instanceof Map)v.zzz=['a','bb','ccc','dddd',[asdf:-123.45]]
                v
            }
            write(w,true)
        }.parseText("[\""+("abcdefgh1234567890"*200)+"\","+json+"]")
        def j=new groovy.json.JsonSlurper().parseText(w.toString())
        assert j[1].a1[4].zzz==['a','bb','ccc','dddd',[asdf:-123.45]]
    }

    public void testAcmeJsonOutput(){
        def j=['a','bb','ccc','dddd',[asdf:-123.45]]
        def s1 = AcmeJsonOutput.toJson(j)
        def s2 = JsonOutput.toJson(j)
        assert s1== s2
    }

    public void testAcmeJsonOutput1(){
        def s1 = AcmeJsonOutput.toJson("hello world")
        assert s1=='"hello world"'
    }

    public void testAcmeJsonOutput2(){
        def s1 = AcmeJsonOutput.toJson("hello\nworld\t!")
        assert s1=='"hello\\nworld\\t!"'
    }

    public void testProcessArrayOfMaps(){
        def j='[{"a":1},{"a":2},{"a":3}]'
        def w = new StringWriter();
        new AcmeJsonParser().withFilter{
            onValue('$.*'){obj->
                return obj
            }
            write(w,false)
        }.parseText(j)
        assert w.toString()==j
    }

    public void test_AcmeJsonWriter(){
        def json = new AcmeJsonWriter(new StringWriter(),false).object{
            key("event").value( [name:'test', message: 'ipsum lorem' ] )
            key("array").value( 1..5 )
        }.writer.toString()
        assert json == '{"event":{"name":"test","message":"ipsum lorem"},"array":[1,2,3,4,5]}'
    }


    /*
    public void testJsonWrite(){
        def f = new File("./build/tmp.json")
        new File("./build/tmp.json").withWriter("UTF-8"){w->
            def jw = new AcmeJsonWriter(w,true);
            jw.object {
                jw.key("meta")
                jw.value(
                        "requestid":"request1000",
                        "http_code":200,
                        "network":"twitter",
                        "query_type":"realtime",
                        "limit":10,
                        "page":0
                )
                jw.key("posts")
                jw.array{
                    for(int i=0;i<2000;i++){
                        jw.value(
                                "network":"twitter",
                                "posted":"posted"+i,
                                "postid":"id"+i,
                                "text":"text"+i,
                                "lang":"lang"+i,
                                "type":"type"+i,
                                "sentiment":"sentiment"+i,
                                "url":"url"+i
                        )
                    }
                }
            }
        }
    }

    private void jsonWriteLarge() {
        println 'jsonWriteLarge'
        def j = new AcmeJsonParser().parseText(json)
        def f = new File("./build/tmp.json")
        if(f.exists())return
        new File("./build/tmp.json").withWriter("UTF-8"){w->
            def jw = new AcmeJsonWriter(w,true);
            jw.array {
                for(int i=0;i<1000000;i++){
                    j.sindex = "index# "+i
                    jw.value(j)
                }
            }
        }
    }

    public void testScanLarge(){
        jsonWriteLarge()
        def f = new File("./build/tmp.json")
        int count = 0
        new AcmeJsonParser().withFilter {
            onValue('$[*]'){
                count++
            }
        }.parse(f)
        assert count==1000000

    }
    */
}
