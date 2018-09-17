/***/
//@Grab(group= "org.codehaus.groovy",   module= "groovy-all"   	        , version= "2.4.12")

//import groovy.json.*;
import groovyx.acme.json.*;

@groovy.transform.CompileStatic
public class AcmeJsonLoadTest {
	static String json = "{\"x\":\"y\\n\\tz\",\"o\":"+R(" \t",220000)+"{\"aaa\":1,\"b\":[21,{\"22\":2}"+R(",23,24,255,266,9991,9992,9993,9994",100)+"],\"c\":3,\"d\":\"y\\n\\tz\"}}";
	static int count = 50000;
	static long sleep = 10000; //ms

	public static void main(String[] arg){
		new AcmeJsonLoadTest().testLoadALL();
	}

	static String R(String s, int r){
		StringBuilder sb = new StringBuilder(r*s.length()+2);
		for(int i=0;i<r;i++)sb.append(s);
		return sb.toString();
	}

	private void load(int count, boolean verbose)throws Exception{
		long t=0;
		AcmeNullHandler h=new AcmeNullHandler();

		if(verbose)print("testLoadY");
		//System.gc();
		Thread.sleep(1000);
		t=System.currentTimeMillis();
		for(int i=0;i<count;i++) {
			new YJsonParser(h).parse(json);
		}
		if(verbose)println(" t = "+ ((System.currentTimeMillis()-t)/1000.0)+ " sec");

		// if(verbose)println("testLoadW");
		// //System.gc();
		// Thread.sleep(1000);
		// t=System.currentTimeMillis();
		// for(int i=0;i<count;i++) {
			// new WJsonParser(h).parse(json);
		// }
		// if(verbose)println("t = "+ ((System.currentTimeMillis()-t)/1000.0)+ " sec");
		
		/*
		 */
	}

	public void testLoadALL()throws Exception{
		if(sleep>0)Thread.sleep(sleep);
		load(3000, false);
		System.gc();

		load(count, true);
	}
}
