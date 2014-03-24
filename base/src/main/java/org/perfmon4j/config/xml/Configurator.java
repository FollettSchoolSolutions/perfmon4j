package org.perfmon4j.config.xml;

import java.io.File;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.perfmon4j.InvalidConfigException;
import org.perfmon4j.ThreadTraceConfig;
import org.perfmon4j.XMLPerfMonConfiguration;
import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.ThreadTraceConfig.Trigger;
import org.perfmon4j.config.xml.TriggerConfigElement.Type;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;

public class Configurator {
	private static final Configurator singleton = new Configurator();
	private static final Logger logger = LoggerFactory.initLogger(Configurator.class);
	
	public static Configurator getConfigurator() {
		return singleton;
	}
	
	public void setRootConfiguration(File file) {
		
	}
	
	
	// Package level for Testing
	static XMLPerfMonConfiguration processConfig(ConfigElement configElement) {
	
		XMLPerfMonConfiguration result = new XMLPerfMonConfiguration();
	
		result.setEnabled(configElement.isEnabled());
		for (AppenderConfigElement appender : configElement.getAppenders()) {
//			if (appender.isActive()) {
				result.defineAppender(appender.getName(), appender.getClassName(), appender.getInterval(), appender.getAttributes());
//			} else {
//				// Add to inactive appenders set.
//			}
		}
		
		for (MonitorConfigElement monitor : configElement.getMonitors()) {
			String monitorName = monitor.getName();
			result.defineMonitor(monitorName);

			for (AppenderMappingElement mapping : monitor.getAppenders()) {
				try {
					result.attachAppenderToMonitor(monitorName, mapping.getName(), mapping.getPattern());
				} catch (InvalidConfigException e) {
					logger.logDebug("Error attaching monitor", e);
				}
			}
		}
		
		for (SnapShotConfigElement ssElement : configElement.getSnapShots()) {
			result.defineSnapShotMonitor(ssElement.getName(), ssElement.getClassName(), ssElement.getAttributes());
			
			for (AppenderMappingElement mapping : ssElement.getAppenders()) {
				try {
					result.attachAppenderToSnapShotMonitor(ssElement.getName(), mapping.getName());
				} catch (InvalidConfigException e) {
					logger.logDebug("Error attaching monitor", e);
				}
			}
		}
		
		for (ThreadTraceConfigElement ttConfig : configElement.getThreadTraces()) {
			ThreadTraceConfig tmp = new ThreadTraceConfig();
			tmp.setMaxDepth(Integer.parseInt(ttConfig.getMaxDepth()));
			tmp.setMinDurationToCapture((int)MiscHelper.convertIntervalStringToMillis(ttConfig.getMinDurationToCapture(), 0, "millis"));
			tmp.setRandomSamplingFactor(Integer.parseInt(ttConfig.getRandomSamplingFactor()));

			for (AppenderMappingElement mapping : ttConfig.getAppenders()) {
                AppenderID appenderID = result.getAppenderForName(mapping.getName());
                if (appenderID == null) {
                	logger.logError("Appender: \"" + mapping.getName() + "\" not defined. Attaching ThreadTraceMonitor \"" 
                			+ ttConfig.getMonitorName() + "\" to the default text appender." );

                	appenderID = result.getOrCreateDefaultAppender();
                }
                tmp.addAppender(appenderID);
			}
			
			List<Trigger> triggers = new ArrayList<Trigger>();
			for (TriggerConfigElement triggerElement : ttConfig.getTriggers()) {
				if (triggerElement.getType().equals(Type.COOKIE_TRIGGER)) {
					triggers.add(new ThreadTraceConfig.HTTPCookieTrigger(triggerElement.getName(), triggerElement.getValue()));
				} else if (triggerElement.getType().equals(Type.REQUEST_TRIGGER)) {
					triggers.add(new ThreadTraceConfig.HTTPRequestTrigger(triggerElement.getName(), triggerElement.getValue()));
				} else if (triggerElement.getType().equals(Type.SESSION_TRIGGER)) {
					triggers.add(new ThreadTraceConfig.HTTPSessionTrigger(triggerElement.getName(), triggerElement.getValue()));
				} else if (triggerElement.getType().equals(Type.THREAD_PROPERTY_TRIGGER)) {
					triggers.add(new ThreadTraceConfig.ThreadPropertytTrigger(triggerElement.getName(), triggerElement.getValue()));
				} else if (triggerElement.getType().equals(Type.THREAD_TRIGGER)) {
					triggers.add(new ThreadTraceConfig.ThreadNameTrigger(triggerElement.getName()));
				} 
			}
			tmp.setTriggers(triggers.toArray(new Trigger[]{}));			
			
			result.addThreadTraceConfig(ttConfig.getMonitorName(), tmp);
		}

		result.addDefaultAppendersToMonitors();
		
		return result;
	}
	
	static XMLPerfMonConfiguration processConfig(Reader xmlReader) throws InvalidConfigException{
		return processConfig(XMLConfigurationParser2.parseXML(xmlReader));
	}
	
}
