package org.perfmon4j.emitter;

import org.perfmon4j.PerfMon;

public class MyDemoEmitter implements Emitter, Runnable {
	private EmitterController controller = null;
	private int value = 0;

	@Override
	public void run() {
		// When emitter is attached to a controller this method
		// will be called every minute.
		
		EmitterData data = controller.initData("MyInstanceName");
		data.addData("iops", ++value);
		data.addData("myString", "bogus");
		data.addData("myFloatingPoint", 10.0/value);
		controller.emit(data);
	}

	@Override
	public void acceptController(EmitterController controller) {
		this.controller = controller;
	}
	

	public static void main(String[] args) throws Exception {
		System.setProperty("PerfMon4j.verboseEnabled", "true");
		
        final String PERFMON_CONFIG =
                "<Perfmon4JConfig enabled='true'>\r\n" +
                "   <appender name='5 minute' className='org.perfmon4j.TextAppender' interval='5 min'/>\r\n" +
//        		"	<appender name='influx-appender' className='org.perfmon4j.influxdb.InfluxAppender' interval='1 minute'>\r\n" +
//        		"		<attribute name='baseURL'>http://localhost:8888/influxdb</attribute>\r\n" +
//        		"		<attribute name='database'>perfmon4j</attribute>\r\n" +
//        		"		<attribute name='groups'>perfmon4j-demo</attribute>\r\n" +
//        		"		<attribute name='retentionPolicy'>one_week</attribute>\r\n" +
//        		"		<attribute name='userName'>influxUser</attribute>\r\n" +
//        		"		<attribute name='password'>pw4influxUser</attribute>\r\n" +
//        		"	</appender>\r\n" +
                "   <emitterMonitor name='MyDemo' className='org.perfmon4j.emitter.MyDemoEmitter'>\r\n" +
                "       <appender name='5 minute'/>\r\n" +
//                "       <appender name='influx-appender'/>\r\n" +
                "   </emitterMonitor>\r\n" +
                "</Perfmon4JConfig>";

System.out.println(PERFMON_CONFIG);
        
        PerfMon.configureFromStringXML(PERFMON_CONFIG);

		EmitterRegistry emitterRegistry = EmitterRegistry.getSingleton();

        /*** This should not be done outside of a test, but it changes the run interval to once per second **/ 
		emitterRegistry.setDefaultTimerIntervalMillis(1000);
        /****************************************************************************************************/ 
		
		Emitter myEmitter = new MyDemoEmitter();
		emitterRegistry.register(myEmitter);
		
		System.out.println("Emitter will run once per second for the next 5 seconds...");
		Thread.sleep(5000);  
		
		emitterRegistry.deRegister(myEmitter);
		System.out.println("Emitter will no longer run, once disabled.  We will wait 5 seconds to ensure it is not called again.");
		
		Thread.sleep(5000);  // Emitter has been disabled and will no longer run.
	}
}
