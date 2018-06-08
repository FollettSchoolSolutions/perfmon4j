package org.perfmon4j.hystrix;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;

import org.perfmon4j.PerfMon;

public class CommandStatsRegistry {
	private static final CommandStatsRegistry singleton = new CommandStatsRegistry();
	
	public static CommandStatsRegistry getRegistry() {
		return singleton;
	}

	private final Set<WeakReference<CommandStatsProvider>> providers = 
			Collections.synchronizedSet(new HashSet<WeakReference<CommandStatsProvider>>());
	
	/**
	 * Other than for unit testing you should use the singleton: 
	 * 		CommandStatsRegistry.getRegistry();
	 */
	/* package scope for unit testing */ CommandStatsRegistry() {
	}
	
	/**
	 * Since within an ApplicationServer Hystrix instances can/come and go 
	 * (Classes being loaded/Unloaded) we must maintain a weak reference 
	 * to the providers.  This will allow Hystrix classes to be garbage collected
	 * when their classloader is removed.
	 * @param provider
	 */
	public void registerProvider(CommandStatsProvider provider) {
		providers.add(new WeakReference<CommandStatsProvider>(provider));
	}

	public int getNumActiveProviders() {
		int result = 0;

		for (WeakReference<CommandStatsProvider> r : providers) {
			CommandStatsProvider provider = r.get();
			if (provider != null) {
				result++;
			}
		}
		
		return result;
	}
	
	
	public CommandStatsAccumulator getStats() {
		CommandStatsAccumulator result = new CommandStatsAccumulator();
		
		for (WeakReference<CommandStatsProvider> r : providers) {
			CommandStatsProvider provider = r.get();
			if (provider != null) {
				provider.collectStats(result);
			}
		}
		
		return result;
	}

	static {
		PerfMon.utilityTimer.schedule(new Timer(), 2000, 2000);
	}
	
	
	private static class Timer extends TimerTask {
		@Override
		public void run() {
			System.gc();
			CommandStatsRegistry registry = CommandStatsRegistry.getRegistry();
			System.out.println("*****************\r\n" 
					+ registry.getNumActiveProviders() + " Active Providers\r\n"   
					+ "*****************");
		}
	}
	
}
