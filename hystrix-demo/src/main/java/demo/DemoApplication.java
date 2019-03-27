package demo;

import io.undertow.Undertow;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.reflections.Reflections;

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
	
	public static void main(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("p", "port", true, "port number");
        options.addOption("n", "hostname", true, "hostname, e.g. localhost");

        CommandLine cmd = new DefaultParser().parse(options, args);

        int port = Integer.parseInt(cmd.getOptionValue("p", "8080"));
        String hostname = cmd.getOptionValue("n", "127.0.0.1");

        UndertowJaxrsServer ut = new UndertowJaxrsServer();
        ut.deploy(new Application() {
            @Override
            public Set<Class<?>> getClasses() {
            	Reflections reflections = new Reflections("demo");
            	Set<Class<?>> classes = new HashSet<>();
            	
            	classes.addAll(reflections.getTypesAnnotatedWith(ApplicationPath.class));
            	classes.addAll(reflections.getTypesAnnotatedWith(Provider.class));
            	classes.addAll(reflections.getTypesAnnotatedWith(Path.class));
            	
                return classes;
            }
        }, "HystrixDemo");

        
        ut.start(Undertow.builder()
        		.addHttpListener(port, hostname));
        System.out.println(String.format("Started server at http://%s:%d/  Hit ^C to stop", hostname, port));
    }	
}
