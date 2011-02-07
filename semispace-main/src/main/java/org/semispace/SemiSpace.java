/*
 * ============================================================================
 *
 *  File:     SemiSpace.java
 *----------------------------------------------------------------------------
 *
 * Copyright 2008 Erlend Nossum
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 *  Description:  See javadoc below
 *
 *  Created:      23. des.. 2007
 * ============================================================================ 
 */

package org.semispace;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.semispace.admin.InternalQuery;
import org.semispace.admin.SemiSpaceAdmin;
import org.semispace.admin.SemiSpaceAdminInterface;
import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiEvent;
import org.semispace.event.SemiExpirationEvent;
import org.semispace.event.SemiRenewalEvent;
import org.semispace.event.SemiTakenEvent;
import org.semispace.exception.SemiSpaceInternalException;
import org.semispace.exception.SemiSpaceObjectException;
import org.semispace.exception.SemiSpaceUsageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.annotations.AutolockRead;
import org.terracotta.annotations.AutolockWrite;
import org.terracotta.annotations.DMI;
import org.terracotta.annotations.HonorTransient;
import org.terracotta.annotations.InstrumentedClass;
import org.terracotta.annotations.Root;

/**
 * A tuple space implementation which can be distributed with terracotta. This is the main class
 * from which the SemiSpace interface is obtained.
 */
public class SemiSpace implements SemiSpaceInterface {

	private static final String ADMIN_GROUP_IS_FLAGGED = "adminGroupIsFlagged";
	private static final String NOEVENTS_IS_FLAGGED = "noEventsIsFlagged";

	private static final Logger log = LoggerFactory.getLogger(SemiSpace.class);

	public static final long ONE_DAY = 86400 * 1000;

	private static final SemiSpace instance = new SemiSpace();

	private HolderContainer elements = HolderContainer.retreiveContainer();

	private long listenerId = 0;
	private Map<Long, ListenerHolder> listeners = null;

	private SemiSpaceAdminInterface admin = null;

	private Map<String, Field[]> classFieldMap = null;

	private SemiSpaceStatistics statistics = null;

	private XStream xStream = null;
	
	private SemiSpace() {
		listeners = new ConcurrentHashMap<Long, ListenerHolder>();
		xStream = new XStream();
		classFieldMap = new WeakHashMap<String, Field[]>();
		checkedClassSet = new HashSet<String>();	
		statistics = new SemiSpaceStatistics();
		admin = new SemiSpaceAdmin(this);
	}

	/**
	 * Holder for sanity check of stored class. It should not be an inner class.
	 */
	private transient Set<String> checkedClassSet = null;

	/**
	 * @return Return the space
	 */
	public static synchronized SemiSpaceInterface retrieveSpace() {
		if (!instance.admin.hasBeenInitialized()) {
			instance.admin.performInitialization();
		}
		
		return instance;
	}

	/**
	 * None of the parameters can be null
	 * 
	 * @return Returning null if something went wrong or was wrong, a registration object otherwise.
	 * @see org.semispace.SemiSpaceInterface#notify(Object, SemiEventListener, long)
	 */
	@Override
	public SemiEventRegistration notify(Object tmpl, SemiEventListener listener, long duration) {
		if (tmpl == null) {
			log.warn("Not registering notification on null object.");
			return null;
		}
		Map<String, String> searchProps = getPropertiesForObject(tmpl);
		SemiEventRegistration registration = notify(searchProps, listener, duration);
		return registration;
	}

	/**
	 * Basically the same as the notify method demanded by the interface, except that it accepts
	 * search properties directly. Used from the web services class. None of the parameters can be
	 * null
	 * 
	 * @return Returning null if something went wrong or was wrong, a registration object otherwise.
	 */
	@AutolockWrite
	public SemiEventRegistration notify(Map<String, String> searchProps,
	        SemiEventListener listener, long duration) {
		if (listener == null) {
			log.warn("Not allowing listener to be null.");
			return null;
		}
		if (searchProps == null) {
			log.warn("Not allowing search props to be null");
			return null;
		}
		if (duration <= 0) {
			log.warn("Not registering notification when duration is <= 0. It was " + duration);
			return null;
		}

		ListenerHolder holder = null;
		synchronized (this) {
			listenerId++;
			holder = new ListenerHolder(listenerId, listener, duration + getAdmin().calculateTime(),
				searchProps);
        }
		if (listeners.put(Long.valueOf(holder.getId()), holder) != null) {
			throw new SemiSpaceInternalException(
				"Internal assertion error. Listener map already had element with id "
				+ holder.getId());
		}
		
		statistics.increaseNumberOfListeners();
		
		SemiLease lease = new ListenerLease(holder, this);
		SemiEventRegistration eventRegistration = new SemiEventRegistration(holder.getId(), lease);
		return eventRegistration;
	}

	/**
	 * Distributed notification method.
	 */
	public void notifyListeners(DistributedEvent distributedEvent) {
		final List<SemiEventListener> toNotify = new ArrayList<SemiEventListener>();
		ListenerHolder[] listenerArray = listeners.values().toArray(new ListenerHolder[0]);
		Arrays.sort(listenerArray, new ShortestTtlComparator());
		for (ListenerHolder listener : listenerArray) {
			if (listener.getLiveUntil() < getAdmin().calculateTime()) {

				cancelListener(listener);

			}
			else if (hasSubSet(distributedEvent.getEntrySet(), listener.getSearchMap())) {
				SemiEventListener notifyMe = listener.getListener();
				toNotify.add(notifyMe);
			}
		}
		final SemiEvent event = distributedEvent.getEvent();
		final Runnable notifyThread = new Runnable() {
			public void run() {
				for (SemiEventListener notify : toNotify) {
					if (!getAdmin().getThreadPool().isShutdown()) {
						try {
							notify.notify(event);
						}
						catch (ClassCastException ignored) {
							// Sadly enough, I need to ignore this due to type erasure.
						}
					}
					else {
						log.debug("Not notifying, as the thread pool has been shut down.");
					}
				}
			}
		};
		getAdmin().getThreadPool().execute(notifyThread);
		getAdmin().notifyAboutEvent(distributedEvent);
	}
	
	private void distributeEvent(final DistributedEvent distributedEvent) {
		final Runnable distRunnable = new Runnable() {
			public void run() {
				EventDistributorRoot.getInstance().getDistributor().distributeEvent(distributedEvent);
			}
		};
		
		getAdmin().getThreadPool().execute(distRunnable);
	}

	/**
	 * Notice that the lease time is the time in milliseconds the element is wants to live,
	 * <b>not</b> the system time plus the time to live.
	 * 
	 * @return Either the resulting lease or null if an error
	 */
	@SuppressWarnings("unchecked")
	public SemiLease write(final Object entry, final long leaseTimeMs) {
		if (entry == null) {
			return null;
		}

		WrappedInternalWriter write = new WrappedInternalWriter(entry, leaseTimeMs);

		Future<?> future = getAdmin().getThreadPool().submit(write);
		Exception exception = null;
		try {
			future.get();
		}
		catch (InterruptedException e) {
			log.error("Got exception", e);
			exception = e;
		}
		catch (ExecutionException e) {
			log.error("Got exception", e);
			exception = e;
		}

		if (write.getException() != null || exception != null) {
			String error = " Writing object (of type " + entry.getClass().getName()
			        + ") to space gave exception. XML version: " + objectToXml(entry);
			if (write.getException() != null) {
				exception = write.getException();
			}
			throw new SemiSpaceObjectException(error, exception);
		}
		return write.getLease();
	}

	private SemiLease writeInternally(Object entry, long leaseTimeMs) {
		String entryClassName = entry.getClass().getName();
		if (entry instanceof InternalQuery) {
			entryClassName = InternalQuery.class.getName();
		}
		String xml = objectToXml(entry);
		Map<String, String> searchMap = getPropertiesForObject(entry);
		return writeToElements(entryClassName, leaseTimeMs, xml, searchMap, !searchMap.containsKey(NOEVENTS_IS_FLAGGED));
	}

	/**
	 * This method is public for the benefit of the web services, which shortcuts the writing
	 * process. All values are expected to be non-null and valid upon entry.
	 */
	public SemiLease writeToElements(String entryClassName, long leaseTimeMs, String xml,
	        Map<String, String> searchMap, boolean doEvent) {
		Holder holder = null;
		
		synchronized (checkedClassSet) {
			if (!checkedClassSet.contains(entryClassName)) {
				checkedClassSet.add(entryClassName);
				if (xml.contains("<outer-class>")) {
					log.warn("It seems that "
					        + entryClassName
					        + " is an inner class. This is DISCOURAGED as it WILL serialize the outer "
					        + "class as well. If you did not intend this, note that what you store MAY be significantly larger than you "
					        + "expected. This warning is printed once for each class type.");
				}
			}
		}
		
		holder = elements.addHolder(xml, getAdmin().calculateTime() + leaseTimeMs, entryClassName, searchMap);
						
		SemiLease lease = new ElementLease(holder, this);
		statistics.increaseWrite();

		if (doEvent) {
			SemiAvailabilityEvent semiEvent = new SemiAvailabilityEvent(holder.getId());
			distributeEvent(new DistributedEvent(holder.getClassName(), semiEvent,
			        holder.getSearchMap()));
		}

		return lease;
	}

	@Override
	public <T> T read(T tmpl, long timeout) {
		String found = null;
		if (tmpl != null) {
			found = findOrWaitLeaseForTemplate(getPropertiesForObject(tmpl), timeout, false);
		}
		return (T) xmlToObject(found);
	}

	/**
	 * Public for the benefit of the webservices interface.
	 * 
	 * @param timeout how long to wait in milliseconds. If timeout is zero or negative, query once.
	 * @param isToTakeTheLease true if the element shall be marked as taken.
	 * @return XML version of data, if found, or null
	 */
	public String findOrWaitLeaseForTemplate(Map<String, String> templateSet, long timeout,
	        boolean isToTakeTheLease) {
		long until = getAdmin().calculateTime() + timeout;
		long subtract = 0;
		boolean firstTime = true;
		String found = null;
		String className = templateSet.get("class");
		if (templateSet.get(SemiSpace.ADMIN_GROUP_IS_FLAGGED) != null) {
			className = InternalQuery.class.getName();
		}

		/*
		SemiBlockingListener listener = new SemiBlockingListener();
		SemiEventRegistration eventReg = null;
		if (timeout > 0) {
			// Registering listener early in order not to miss any intermittent notifications.
			eventReg = notify(templateSet, listener, timeout);
		}
		*/
		long systime = getAdmin().calculateTime();
		do {
			final long duration = timeout - subtract;
			
			if (isToTakeTheLease) {
				statistics.increaseBlockingTake();
			}
			else {
				statistics.increaseBlockingRead();
			}

			found = findLeaseForTemplate(templateSet, isToTakeTheLease);

			if (found == null && duration > 0) {
				elements.waitHolder(className, duration);
			}
			if (isToTakeTheLease) {
				statistics.decreaseBlockingTake();
			}
			else {
				statistics.decreaseBlockingRead();
			}
			/*
			if (listener.hasBeenNotified() && found == null) {
				// Need to reset notification status
				listener.reset();
				found = findLeaseForTemplate(templateSet, isToTakeTheLease);
			}
			*/
			long now = getAdmin().calculateTime();
			subtract += now - systime;
			systime = now;
		} while (found == null && systime < until);
		
		/*
		if (eventReg != null) {
			eventReg.getLease().cancel();
		}
		*/
		return found;
	}

	public <T> T readIfExists(T tmpl) {
		return read(tmpl, 0);
	}

	/**
	 * @return Xml version of found object
	 */
	private String findLeaseForTemplate(Map<String, String> templateSet, boolean isToTakeTheLease) {
		Holder found = null;

		List<Holder> toEvict = new ArrayList<Holder>();

		// Read all elements until element is found. Side effect is to generate eviction list.
		if (templateSet.get("class") == null) {
			throw new SemiSpaceObjectException("Did not expect classname to be null");
		}
		String className = templateSet.get("class");
		if (templateSet.get(SemiSpace.ADMIN_GROUP_IS_FLAGGED) != null) {
			className = InternalQuery.class.getName();
		}

		HolderElement next = elements.next(className);
		
		if (next != null) {
			Iterator<Holder> it = next.iterator();
			while (found == null && it.hasNext()) {
				Holder elem = it.next();
				if (elem.getLiveUntil() < admin.calculateTime()) {
					toEvict.add(elem);
					elem = null;
				}
				if (elem != null && hasSubSet(elem.getSearchMap().entrySet(), templateSet)) {
					found = elem;
				}
			}
		}

		for (Holder evict : toEvict) {
			if (!cancelElement(Long.valueOf(evict.getId()), false, evict.getClassName(), !templateSet.containsKey(NOEVENTS_IS_FLAGGED))) {
				log.debug("Element with id "
				        + evict.getId()
				        + " should exist in most cases. This time, it is probably missing as it belongs to a timed out query.");
			}
		}
		boolean needToRetake = false;

		if (found != null) {
			if (isToTakeTheLease && !cancelElement(Long.valueOf(found.getId()), isToTakeTheLease, found.getClassName(), !templateSet.containsKey(NOEVENTS_IS_FLAGGED))) {
				log.info("Element with id " + found.getId() + " ceased to exist during take. "
					+ "This is not an error; Just an indication of a busy space. ");
				found = null;
				needToRetake = true;
			}
		}

		if (needToRetake) {
			// As element ceased to exist during take, I need to try again. The chances of this is
			// rather slim.
			// Nevertheless, this is needed as the query might have zero in timeout.
			return findLeaseForTemplate(templateSet, isToTakeTheLease);

		}
		else if (found != null) {
			if (isToTakeTheLease) {
				statistics.increaseTake();
			}
			else {
				statistics.increaseRead();
			}
		}
		else {
			if (isToTakeTheLease) {
				statistics.increaseMissedTake();
			}
			else {
				statistics.increaseMissedRead();
			}
		}
		if (found != null) {
			return found.getXml();
		}
		return null;
	}

	/**
	 * Used for retrieving element with basis in id
	 * 
	 * @return Element with given holder id, or null if not found (or expired
	 */
	public Holder readHolderById(long hId) {
		return elements.readHolderWithId(hId);
	}

	private boolean hasSubSet(Set<Entry<String, String>> containerEntrySet,
	        Map<String, String> templateSubSet) {
		if (templateSubSet == null) {
			throw new SemiSpaceUsageException("Did not expect template sub set to be null");
		}
		Set<Entry<String, String>> templateEntrySet = templateSubSet.entrySet();
		return containerEntrySet.containsAll(templateEntrySet);
	}

	@Override
	public <T> T take(T tmpl, long timeout) {
		String found = null;
		if (tmpl != null) {
			found = findOrWaitLeaseForTemplate(getPropertiesForObject(tmpl), timeout, true);
		}
		return (T) xmlToObject(found);
	}

	@Override
	public <T> T takeIfExists(T tmpl) {
		return take(tmpl, 0);
	}

	private String objectToXml(Object obj) {
		StringWriter writer = new StringWriter();
		xStream.marshal(obj, new CompactWriter(writer));
		return writer.toString();
	}

	private Object xmlToObject(String xml) {
		if (xml == null || "".equals(xml)) {
			return null;
		}
		Object result = null;
		try {
			result = xStream.fromXML(xml);
		}
		catch (Exception e) {
			// Not sure if masking exception is the most correct way of dealing with it.
			log.error(
			    "Got exception unmarshalling. Not throwing the exception up, but rather returning null. "
			            + "This is as the cause may be a change in the object which is sent over. "
			            + "The XML was read as\n" + xml, e);
		}
		return result;
	}

	private static class PreprocessedTemplate {
		private Object object;
		private Map<String, String> cachedSet;

		public PreprocessedTemplate(Object object, Map<String, String> cachedSet) {
			this.object = object;
			this.cachedSet = cachedSet;
		}

		public Map<String, String> getCachedSet() {
			return cachedSet;
		}

		public void setCachedSet(Map<String, String> cachedSet) {
			this.cachedSet = cachedSet;
		}

		public Object getObject() {
			return object;
		}

		public void setObject(Object object) {
			this.object = object;
		}
	}

	/**
	 * Create a pre-processed template object that can be used to reduce the amount of work required
	 * to match templates during a take. Applications that take a lot of objects using the same
	 * template instance, a noticeable performance improvement can be had.
	 * 
	 * @param template The object to preprocess
	 * @return A pre-processed object that can be passed to read/take
	 */
	public Object processTemplate(Object template) {
		PreprocessedTemplate toReturn = null;
		if (template != null) {
			toReturn = new PreprocessedTemplate(template, retrievePropertiesFromObject(template));
		}
		return toReturn;
	}

	private Map<String, String> getPropertiesForObject(Object object) {
		if (object instanceof PreprocessedTemplate) {
			return ((PreprocessedTemplate) object).getCachedSet();
		}
		return retrievePropertiesFromObject(object);
	}

	/**
	 * Protected for the benefit of junit test(s)
	 * 
	 * @param examine Non-null object
	 */
	protected Map<String, String> retrievePropertiesFromObject(Object examine) {
		Map<String, String> map = fillMapWithPublicFields(examine);
		addGettersToMap(examine, map);

		if (examine instanceof InternalQuery) {
			map.put(SemiSpace.ADMIN_GROUP_IS_FLAGGED, "true");
		}
		
		if (examine.getClass().isAnnotationPresent(SupressEvents.class)) {
			map.put(SemiSpace.NOEVENTS_IS_FLAGGED, "true");
		}
		
		// Need to rename class entry in order to separate on class elements.
		String className = map.remove("class");
		map.put("class", className.substring("class ".length()));
		return map;
	}

	/**
	 * Add an objects getter names and values in a map. Note that all values are converted to
	 * strings.
	 */
	private void addGettersToMap(Object examine, Map<String, String> map) {
		final Set<String> getters = new HashSet<String>();
		final Method[] methods = examine.getClass().getMethods();
		final Map<String, Method> keyedMethod = new HashMap<String, Method>();
		final Map<String, String> keyedMethodName = new HashMap<String, String>();
		for (Method method : methods) {
			final String name = method.getName();
			final int parameterLength = method.getTypeParameters().length;
			if (parameterLength == 0 && name.startsWith("get")) {
				// Equalize key to [get][set][X]xx
				String normalized = name.substring(3, 4).toLowerCase() + name.substring(4);
				getters.add(normalized);
				keyedMethod.put(name, method);
				keyedMethodName.put(normalized, name);
				// log.info("Got name "+name+" which was normalized to "+normalized);
			}
		}
		for (String name : getters) {
			try {
				Object value = keyedMethod.get(keyedMethodName.get(name)).invoke(examine, null);
				// log.info(">> want to insert "+name+"="+value);
				if (value != null) {
					map.put(name, "" + value);
				}
			}
			catch (IllegalAccessException e) {
				log.error("Could not access method g" + name + ". Got (masked exception) "
				        + e.getMessage());
			}
			catch (InvocationTargetException e) {
				log.error("Could not access method g" + name + ". Got (masked exception) "
				        + e.getMessage());
			}
		}
	}

	/**
	 * Create a map and fill it with the public fields from the object, which is the JavaSpace
	 * manner.
	 */
	private Map<String, String> fillMapWithPublicFields(Object examine) {
		Field[] fields = classFieldMap.get(examine.getClass().getName());
		if (fields == null) {
			fields = examine.getClass().getFields();
			classFieldMap.put(examine.getClass().getName(), fields);
		}
		Map<String, String> map = new HashMap<String, String>();
		for (Field field : fields) {
			try {
				String name = field.getName();
				Object value = field.get(examine);

				if (value != null) {
					map.put(name, "" + value);
				}
			}
			catch (IllegalAccessException e) {
				log.warn("Introspection gave exception - which is not re-thrown.", e);
			}
		}
		return map;
	}

	/**
	 * Preparing for future injection of admin. Note that you must call initialization
	 * <b>yourself</b> after setting the object
	 * <p>
	 * This is tested in junit test (and under terracotta).
	 * </p>
	 */
	public void setAdmin(SemiSpaceAdminInterface admin) {
		this.admin = admin;
	}

	/**
	 * Return admin element
	 */
	public SemiSpaceAdminInterface getAdmin() {
		return admin;
	}

	/**
	 * Need to wrap write in own thread in order to make terracotta pick it up.
	 */
	protected class WrappedInternalWriter implements Runnable {
		private Object entry;

		private long leaseTimeMs;

		private Exception exception;

		private SemiLease lease;

		public Exception getException() {
			return this.exception;
		}

		public SemiLease getLease() {
			return lease;
		}

		protected WrappedInternalWriter(Object entry, long leaseTimeMs) {
			this.entry = entry;
			this.leaseTimeMs = leaseTimeMs;
		}

		@SuppressWarnings("synthetic-access")
		public void run() {
			try {
				lease = writeInternally(entry, leaseTimeMs);
			}
			catch (Exception e) {
				exception = e;
			}
		}
	}

	/**
	 * Harvest old elements from diverse listeners. Used from the periodic harvester and junit
	 * tests.
	 */
	public void harvest() {

		for (ListenerHolder listener : listeners.values()) {
			if (listener.getLiveUntil() < getAdmin().calculateTime()) {
				cancelListener(listener);

			}
		}
		List<Holder> beforeEvict = new ArrayList<Holder>();

		String[] groups = elements.retrieveGroupNames();
		for (String group : groups) {
			int evictSize = beforeEvict.size();
			HolderElement hc = elements.next(group);
			for (Holder elem : hc) {
				if (elem.getLiveUntil() < getAdmin().calculateTime()) {
					beforeEvict.add(elem);
				}
			}
			long afterSize = beforeEvict.size() - evictSize;
			if (afterSize > 0) {
				List<Long> ids = new ArrayList<Long>();
				for (Holder evict : beforeEvict) {
					ids.add(Long.valueOf(evict.getId()));
				}
				if (log.isDebugEnabled()) {
					String moreInfo = "";
					if (ids.size() < 30) {
						Collections.sort(ids);
						moreInfo = "Ids: " + ids;
					}
					log.debug("Testing group " + group + " gave " + afterSize
						+ " element(s) to evict. " + moreInfo);
				}
			}
		}
		for (Holder evict : beforeEvict) {
			cancelElement(Long.valueOf(evict.getId()), false, evict.getClassName(), true);
		}
	}

	/**
	 * Return the number of elements in the space. Notice that this may report old elements that
	 * have not been purged yet.
	 */
	public int numberOfSpaceElements() {
		int size;
		size = elements.size();
		return size;
	}

	/** Need present statistics here due to spring JMX configuration. */
	public int numberOfBlockingRead() {
		return statistics.getBlockingRead();
	}

	/** Need present statistics here due to spring JMX configuration. */
	public int numberOfBlockingTake() {
		return statistics.getBlockingTake();
	}

	/** Need present statistics here due to spring JMX configuration. */
	public int numberOfMissedRead() {
		return statistics.getMissedRead();
	}

	/** Need present statistics here due to spring JMX configuration. */
	public int numberOfMissedTake() {
		return statistics.getMissedTake();
	}

	/** Need present statistics here due to spring JMX configuration. */
	public int numberOfNumberOfListeners() {
		return statistics.getNumberOfListeners();
	}

	/** Need present statistics here due to spring JMX configuration. */
	public int numberOfRead() {
		return statistics.getRead();
	}

	/** Need present statistics here due to spring JMX configuration. */
	public int numberOfTake() {
		return statistics.getTake();
	}

	/** Need present statistics here due to spring JMX configuration. */
	public int numberOfWrite() {
		return statistics.getWrite();
	}

	/**
	 * For the benefit of junit test(s) - defensively copied statistics
	 */
	protected synchronized SemiSpaceStatistics getStatistics() {
		SemiSpaceStatistics stats;
		stats = (SemiSpaceStatistics) xmlToObject(objectToXml(statistics));
		return stats;
	}

	protected boolean cancelListener(ListenerHolder holder) {
		boolean success = false;

		ListenerHolder listener = listeners.remove(Long.valueOf(holder.getId()));
		if (listener != null) {
			statistics.decreaseNumberOfListeners();
			success = true;
		}

		return success;
	}

	protected boolean renewListener(ListenerHolder holder, long duration) {
		boolean success = false;

		ListenerHolder listener = listeners.get(Long.valueOf(holder.getId()));
		if (listener != null) {
			listener.setLiveUntil(duration + getAdmin().calculateTime());
			// Need to re-get due in order to be certain of liveness.
			success = listeners.get(Long.valueOf(holder.getId())) != null;
		}
		return success;
	}

	/**
	 * @param isTake true if reason for the cancellation is a take.
	 */
	protected boolean cancelElement(Long id, boolean isTake, String className, boolean doEvent) {
		boolean success = false;

		Holder elem = elements.removeHolderById(id.longValue(), className);
		if (elem != null) {
			if (elem.getId() != id.longValue()) {
				throw new SemiSpaceInternalException("Sanity problem. Removed " + id.longValue()
				        + " and got back element with id " + elem.getId());
			}
			success = true;
			if (doEvent) {
				SemiEvent semiEvent = null;
				if (isTake) {
					semiEvent = new SemiTakenEvent(elem.getId());
				}
				else {
					semiEvent = new SemiExpirationEvent(elem.getId());
				}
				// log.debug("Notifying about "+(isTake?"take":"expiration")+" of element with id "+semiEvent.getId());
				distributeEvent(new DistributedEvent(elem.getClassName(), semiEvent,
				        elem.getSearchMap()));
			}
		}

		return success;
	}

	/**
	 * @return true if the object actually was renewed. (I.e. it exists and got a new timeout.)
	 */
	protected boolean renewElement(Holder holder, long duration, boolean doEvent) {
		boolean success = false;
		Holder elem = elements.findById(holder.getId(), holder.getClassName());
		if (elem != null) {
			elem.setLiveUntil(duration + getAdmin().calculateTime());
			success = true;
			if (doEvent) {
				distributeEvent(new DistributedEvent(elem.getClassName(), new SemiRenewalEvent(
					elem.getId(), elem.getLiveUntil()), elem.getSearchMap()));
			}
		}

		return success;
	}

	/**
	 * @see HolderContainer#findAllHolderIds
	 */
	public Long[] findAllHolderIds() {
		Long[] result = null;
		result = elements.findAllHolderIds();
		return result;
	}

	/**
	 * Exposing xstream instance in order to allow outside manipulation of aliases and classloader
	 * affiliation.
	 * 
	 * @return The xstream instance used.
	 */
	public XStream getXStream() {
		return xStream;
	}

	private static class ShortestTtlComparator implements Comparator<ListenerHolder>, Serializable {
		public int compare(ListenerHolder o1, ListenerHolder o2) {
			if (o1 == null || o2 == null) {
				throw new SemiSpaceUsageException(
				        "Did not expect any null values for listenerHolder.");
			}
			return (int) (o1.getLiveUntil() - o2.getLiveUntil());
		}
	}
}
