package demo.simple;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import demo.simple.hystrix.EchoCommand;


@Path("/simple/demo")
public class EchoService {

	public EchoService() {
	}
	
	
	/** Example Path:  http://localhost:8080/HystrixDemo/api/simple/demo/echo/HelloAll **/
	@Path("/echo/{whatToEcho}")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String echo(@PathParam("whatToEcho") String whatToEcho) {
		EchoCommand cmd = new EchoCommand(whatToEcho);
		return cmd.execute();
	}
}
