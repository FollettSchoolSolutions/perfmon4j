package org.perfmon4j.util.mbean;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

public class NewMBeanServerExample {

    public static void main(String[] args) {
        try {
            // Create a new MBeanServer
            MBeanServer mbs = MBeanServerFactory.createMBeanServer();

            // You can now register MBeans with this new server
            // Example MBean (for demonstration purposes)
            class ExampleMBean {
                public String getMessage() {
                    return "Hello from a custom MBean Server";
                }
            }

            // Register the example MBean
            ObjectName name = new ObjectName("com.example:type=ExampleMBean");
            mbs.registerMBean(new ExampleMBean(), name);

            System.out.println("New MBeanServer created and MBean registered.");

            // To verify, you can query the new MBeanServer
            System.out.println("Querying MBean: " + mbs.getAttribute(name, "Message"));

            // If you want to list all MBeanServers
            for (MBeanServer server : MBeanServerFactory.findMBeanServer(null)) {
                System.out.println("MBeanServer: " + server);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}