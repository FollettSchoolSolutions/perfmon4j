package demo;

import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.netflix.hystrix.Hystrix;

@ApplicationPath("/api")
@WebListener  // Note for WebListener to be automatically found look at web.xml in this project 
public class DemoApplication extends Application implements ServletContextListener {

	public DemoApplication() {
	}
	
	public void contextInitialized(ServletContextEvent event) {
		System.out.println("Initialized Servlet Context Event");
		
		Hystrix.reset(1, TimeUnit.SECONDS);
	}

	public void contextDestroyed(ServletContextEvent event) {
		System.out.println("Destroyed Servlet Context Event");
	}
}
