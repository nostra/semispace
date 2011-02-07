package org.semispace;

import org.terracotta.annotations.DMI;
import org.terracotta.annotations.InstrumentedClass;
import org.terracotta.annotations.Root;

@InstrumentedClass
public class EventDistributor {
	@DMI
	public void distributeEvent(DistributedEvent event) {
		((SemiSpace)SemiSpace.retrieveSpace()).notifyListeners(event);
	}
}
