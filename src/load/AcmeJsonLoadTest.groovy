/***/
//@Grab(group= "org.codehaus.groovy",   module= "groovy-all"   	        , version= "2.4.12")
//@Grab(group='com.google.code.gson', module='gson', version='2.8.5')

//import groovy.json.*;
import groovyx.acme.json.*;
import com.google.gson.Gson;

@groovy.transform.CompileStatic
public class AcmeJsonLoadTest {
	static String json = "{\"x\":\"y\\n\\tz\",\"o\":"+R(" \t",220000)+"{\"aaa\":1,\"b\":[21,{\"22\":2}"+R(",23,24,255,266,9991,9992,9993,9994",100)+"],\"c\":3,\"d\":\"y\\n\\tz\"}}";
	static int count = 50000;
	static long sleep = 10000; //ms
	static String clazzName = "groovyx.acme.json.AcmeJsonParser";
	static Class clazz = null;

	static int i=0;
	static int iPrev=Integer.MAX_VALUE;
	static final int delay = 1000;
	static double perSecondMax=-1;
	static double perSecondMin=Integer.MAX_VALUE;

	static Timer timer=new Timer("monitor",true);
	static TimerTask timerTask = {
		int iCurrent = i;
		if(iPrev<iCurrent){
			double perSecond = 1000.0 * (iCurrent - iPrev) /delay
			if(perSecond>perSecondMax)perSecondMax = perSecond
			if(perSecond<perSecondMin)perSecondMin = perSecond
			//print("\npersec $perSecond")
		}
		iPrev = i;
	} as TimerTask;


	public static void main(String[] arg){
		if(arg.length==1){
			clazzName = "groovyx.acme.json."+arg[0];
		}
		def cl = AcmeJsonLoadTest.class.getClassLoader();
		clazz = cl.loadClass(clazzName);
		new AcmeJsonLoadTest().testLoadALL();
	}

	static String R(String s, int r){
		StringBuilder sb = new StringBuilder(r*s.length()+2);
		for(int i=0;i<r;i++)sb.append(s);
		return sb.toString();
	}

	private void load(int count, boolean verbose)throws Exception{
		long t=0;
		AcmeJsonNullHandler h=new AcmeJsonNullHandler();

		if(verbose)print(clazz);
		//System.gc();
		Thread.sleep(1000);
		t=System.currentTimeMillis();
		if(verbose){
			timer.schedule(timerTask,delay,delay)
		}
		for(i=0;i<count;i++) {
			//new WJsonParser(h).parse(json);
			((AbstractJsonParser)clazz.newInstance([h] as Object[])).parseText(json);
		}
		if(verbose){
			println(" t = "+ ((System.currentTimeMillis()-t)/1000.0)+ " sec. \tpersec \tmin=" +(int)perSecondMin + " \tavg=" +(int)( 1000*count/(System.currentTimeMillis()-t) )+ " \tmax="+(int)perSecondMax)
		}

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
		load(500, false);
		System.gc();

		load(count, true);
	}
}
