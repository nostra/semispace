package org.semispace;

import org.terracotta.annotations.AutolockWrite;
import org.terracotta.annotations.InstrumentedClass;
import org.terracotta.annotations.Root;

@InstrumentedClass
public class EventDistributorRoot {
	@Root
	private static EventDistributorRoot instance = new EventDistributorRoot();
	
	private EventDistributor dist = null;
	
	@AutolockWrite
	public synchronized EventDistributor getDistributor() {
		if (dist == null) {
			dist = new EventDistributor();
		}
		return dist;
	}
	
	public static EventDistributorRoot getInstance() {
		return instance;
	}
}
