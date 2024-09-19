package org.perfmon4j.util.mbean;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

class MBeanAttributeExtractor {
	private final WeakReference<MBeanServer> mBeanServer;
	private final ObjectName objectName;
	private final MBeanQuery query;
	private static final Logger logger = LoggerFactory.initLogger(MBeanAttributeExtractor.class);
	
	public MBeanAttributeExtractor(MBeanServer mBeanServer, ObjectName objectName,
			MBeanQuery query) {
		super();
		this.mBeanServer = new WeakReference<>(mBeanServer);
		this.objectName = objectName;
		this.query = query;
	}
	
	MBeanDatum<?>[] extractAttributes() {
		List<MBeanDatum<?>> result = new ArrayList<MBeanDatum<?>>();
		MBeanServer mbs = mBeanServer.get();
		if (mbs != null) {
			try {
				MBeanInfo info = mbs.getMBeanInfo(objectName);
//				info.getAttributes()[0].
				
				
				
			} catch (IntrospectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstanceNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ReflectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		} else {
			logger.logDebug("MBean server has been garbage collected.  Unable to return attributes for JMX Object: " + objectName.getCanonicalName());
		}
		
		
		
		return null;
	}
}
