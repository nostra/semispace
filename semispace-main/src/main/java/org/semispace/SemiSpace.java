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

import java.io.Serializable;
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A tuple space implementation which can be distributed with terracotta. This is
 * the main class from which the SemiSpace interface is obtained.
 */
public class SemiSpace implements SemiSpaceInterface {

    private static final String ADMIN_GROUP_IS_FLAGGED = "adminGroupIsFlagged";

    private static final Logger log = LoggerFactory.getLogger(SemiSpace.class);

    public static final long ONE_DAY = 86_400_000L;

    private static SemiSpace instance = null;

    private long listenerId = 0;

    private HolderContainer elements = null;

    private transient Map<Long, ListenerHolder> listeners;

    private transient SemiSpaceAdminInterface admin;

    private transient Map<String, Field[]> classFieldMap = new WeakHashMap<String, Field[]>();

    private SemiSpaceStatistics statistics;

    private transient SemiSpaceSerializer serializer;

    private EventDistributor eventDistributor = EventDistributor.getInstance();


    /**
     * Holder for sanity check of stored class. It should not be an inner class.
     */
    private Set<String> checkedClassSet = new HashSet<String>();

    private SemiSpace() {
        elements = HolderContainer.retrieveContainer();
        listeners = new ConcurrentHashMap<>();
        statistics = new SemiSpaceStatistics();
        serializer = resolveSerializer();
        setAdmin(new SemiSpaceAdmin(this, serializer));
    }

    private static SemiSpaceSerializer resolveSerializer() {
        try {
            Class.forName("com.fasterxml.jackson.databind.ObjectMapper", false, SemiSpace.class.getClassLoader());
            return new JacksonSerializer();

        } catch (ClassNotFoundException e) {
            log.warn("Jackson serializer not found. Using insecure XStream instead.");
            return new XStreamSerializer();
        }
    }

    /**
     * @return Return the space
     */
    public static synchronized SemiSpaceInterface retrieveSpace() {
        if (instance == null) {
            instance = new SemiSpace();
        }
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
        Map<String, String> searchProps = retrievePropertiesFromObject(tmpl);
        SemiEventRegistration registration = notify(searchProps, listener, duration);
        return registration;
    }

    /**
     * Basically the same as the notify method demanded by the interface, except that it accepts search properties
     * directly. Used from the web services class. None of the parameters can be null
     *
     * @return Returning null if something went wrong or was wrong, a registration object otherwise.
     */
    public SemiEventRegistration notify(Map<String, String> searchProps, SemiEventListener listener, long duration) {
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
        listenerId++;
        holder = new ListenerHolder(listenerId, listener, duration + admin.calculateTime(),
                searchProps);
        if (listeners.put(Long.valueOf(holder.getId()), holder) != null) {
            throw new SemiSpaceInternalException("Internal assertion error. Listener map already had element with id " + holder.getId());
        }
        statistics.increaseNumberOfListeners();
        SemiLease lease = new ListenerLease(holder, this);
        SemiEventRegistration eventRegistration = new SemiEventRegistration(holder.getId(), lease);
        return eventRegistration;
    }

    /**
     * Distributed notification method.
     */
    protected void notifyListeners(DistributedEvent distributedEvent) {
        final List<SemiEventListener> toNotify = new ArrayList<>();
        ListenerHolder[] listenerArray = listeners.values().toArray(new ListenerHolder[0]);
        Arrays.sort(listenerArray, new ShortestTtlComparator());
        for (ListenerHolder listener : listenerArray) {
            if (listener.getLiveUntil() < admin.calculateTime()) {

                cancelListener(listener);

            } else if (hasSubSet(distributedEvent.getEntrySet(), listener.getSearchMap())) {
                SemiEventListener notifyMe = listener.getListener();
                toNotify.add(notifyMe);
            }
        }
        final SemiEvent event = distributedEvent.getEvent();
        for (SemiEventListener notify : toNotify) {
            try {
                notify.notify(event);
            } catch (ClassCastException ignored) {
                // Sadly enough, I need to ignore this due to type erasure.
            }
        }


        admin.notifyAboutEvent(distributedEvent);
    }

    /**
     * Notice that the lease time is the time in milliseconds the element is wants to live, <b>not</b> the system time
     * plus the time to live.
     *
     * @return Either the resulting lease or null if an error
     */
    @Override
    public SemiLease write(final Object entry, final long leaseTimeMs) {
        if (entry == null) {
            return null;
        }

        WrappedInternalWriter write = new WrappedInternalWriter(entry, leaseTimeMs);

        Future<?> future = admin.getThreadPool().submit(write);
        Exception exception = null;
        try {
            future.get(10, TimeUnit.SECONDS);
        } catch (CancellationException e) {
            log.error("Got exception", e);
            exception = e;
        } catch (InterruptedException e) {
            log.error("Got exception", e);
            exception = e;
            e.notifyAll();
        } catch (ExecutionException e) {
            log.error("Got exception", e);
            exception = e;
        } catch (TimeoutException e) {
            log.error("Not expected to run into a timeout writing an entry", e);
            exception = e;
        }

        if (write.getException() != null || exception != null) {
            String error = " Writing object (of type " + entry.getClass().getName()
                    + ") to space gave exception. XML version: " + serializer.objectToXml(entry);
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
        String xml = serializer.objectToXml(entry);
        Map<String, String> searchMap = retrievePropertiesFromObject(entry);
        return writeToElements(entryClassName, leaseTimeMs, xml, searchMap);
    }

    /**
     * This method is public for the benefit of the web services, which shortcuts the writing process.
     * All values are expected to be non-null and valid upon entry.
     */
    public SemiLease writeToElements(String entryClassName, long leaseTimeMs, String xml, Map<String, String> searchMap) {
        if (!checkedClassSet.contains(entryClassName)) {
            checkedClassSet.add(entryClassName);
            if (xml.contains("<outer-class>")) {
                log.warn("It seems that " + entryClassName + " is an inner class. This is DISCOURAGED as it WILL serialize the outer " +
                        "class as well. If you did not intend this, note that what you store MAY be significantly larger than you " +
                        "expected. This warning is printed once for each class type.");
            }
        }
        // TODO Error here when leasetime is Max long: FIX
        // Need to add holder within lock. This indicates that HolderContainer has some thread safety issues
        Holder holder = elements.addHolder(xml, admin.calculateTime() + leaseTimeMs, entryClassName, searchMap);

        SemiLease lease = new ElementLease(holder, this);
        statistics.increaseWrite();

        SemiAvailabilityEvent semiEvent = new SemiAvailabilityEvent(holder.getId());

        distributeEvent(new DistributedEvent(holder.getClassName(), semiEvent,
                holder.getSearchMap()));

        return lease;
    }

    private void distributeEvent(final DistributedEvent distributedEvent) {
        final Runnable distRunnable = () -> eventDistributor.distributeEvent(distributedEvent);
        if (!getAdmin().getThreadPool().isShutdown()) {
            try {
                admin.getThreadPool().execute(distRunnable);
            } catch (RejectedExecutionException e) {
                log.error("Could not schedule notification", e);
            }
        } else {
            log.warn("Thread pool is shut down, not relaying event");
        }
    }


    @Override
    public <T> T read(T tmpl, long timeout) {
        String found = null;
        if (tmpl != null) {
            found = findOrWaitLeaseForTemplate(getPropertiesForObject(tmpl), timeout, false);
        }
        return (T) serializer.xmlToObject(found);
    }

    /**
     * Public for the benefit of the webservices interface.
     *
     * @param timeout          how long to wait in milliseconds. If timeout is zero or negative, query once.
     * @param isToTakeTheLease true if the element shall be marked as taken.
     * @return XML version of data, if found, or null
     */
    public String findOrWaitLeaseForTemplate(Map<String, String> templateSet, long timeout, boolean isToTakeTheLease) {
        final long until = admin.calculateTime() + timeout;
        long systime = admin.calculateTime();
        String className = templateSet.get("class");
        if (templateSet.get(SemiSpace.ADMIN_GROUP_IS_FLAGGED) != null) {
            className = InternalQuery.class.getName();
        }
        String found = null;
        long subtract = 0;
        do {
            final long duration = timeout - subtract;
            if (isToTakeTheLease) {
                statistics.increaseBlockingTake();
            } else {
                statistics.increaseBlockingRead();
            }

            found = findLeaseForTemplate(templateSet, isToTakeTheLease);

            if (found == null && duration > 0) {
                Thread.yield(); // Need to yield to avoid rare deadlock
                elements.waitHolder(className, duration);
            }
            if (isToTakeTheLease) {
                statistics.decreaseBlockingTake();
            } else {
                statistics.decreaseBlockingRead();
            }

            final long now = getAdmin().calculateTime();
            subtract += now - systime;
            systime = now;
        } while (found == null && systime < until);
        return found;
    }

    @Override
    public <T> T readIfExists(T tmpl) {
        return read(tmpl, 0);
    }

    /**
     * @return Xml version of found object
     */
    private String findLeaseForTemplate(Map<String, String> templateSet, boolean isToTakeTheLease) {
        // Read all elements until element is found. Side effect is to generate eviction list.
        if (templateSet.get("class") == null) {
            throw new SemiSpaceObjectException("Did not expect classname to be null");
        }
        String className = templateSet.get("class");
        if (templateSet.get(SemiSpace.ADMIN_GROUP_IS_FLAGGED) != null) {
            className = InternalQuery.class.getName();
        }

        HolderElement next = elements.next(className);
        Holder found = null;
        List<Holder> toEvict = new ArrayList<>();
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
            if (!cancelElement(Long.valueOf(evict.getId()), false, evict.getClassName())) {
                log
                        .debug("Element with id "
                                + evict.getId()
                                + " should exist in most cases. This time, it is probably missing as it belongs to a timed out query.");
            }
        }
        boolean needToRetake = false;

        if (found != null) {
            if (isToTakeTheLease && !cancelElement(Long.valueOf(found.getId()), isToTakeTheLease, found.getClassName())) {
                log.info("Element with id " + found.getId() + " ceased to exist during take. "
                        + "This is not an error; Just an indication of a busy space. ");
                found = null;
                needToRetake = true;
            }
        }

        if (needToRetake) {
            // As element ceased to exist during take, I need to try again. The chances of this is rather slim.
            // Nevertheless, this is needed as the query might have zero in timeout.
            return findLeaseForTemplate(templateSet, isToTakeTheLease);

        } else if (found != null) {
            if (isToTakeTheLease) {
                statistics.increaseTake();
            } else {
                statistics.increaseRead();
            }
        } else {
            if (isToTakeTheLease) {
                statistics.increaseMissedTake();
            } else {
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
        Holder result = null;
        result = elements.readHolderWithId(hId);
        return result;
    }

    private boolean hasSubSet(Set<Entry<String, String>> containerEntrySet, Map<String, String> templateSubSet) {
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
        return (T) serializer.xmlToObject(found);
    }

    @Override
    public <T> T takeIfExists(T tmpl) {
        return take(tmpl, 0);
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
     * Create a pre-processed template object that can be used to reduce the amount of
     * work required to match templates during a take.  Applications that take a lot of
     * objects using the same template instance, a noticeable performance improvement
     * can be had.
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
        // TODO : Delete (probably)
        /*
        if (!map.getOrDefault("class", "").startsWith("class ")) {
            // Workaround to make jackson serializer compatible with xstream same.
            map.put("class", "class "+examine.getClass().toString());
        }*/

        if (examine instanceof InternalQuery) {
            map.put(SemiSpace.ADMIN_GROUP_IS_FLAGGED, "true");
        }
        // Need to rename class entry in order to separate on class elements.
        String className = map.remove("class");
        map.put("class", className.substring("class ".length()));
        return map;
    }

    /**
     * Add an objects getter names and values in a map. Note that all values are converted to strings.
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
                //log.info("Got name "+name+" which was normalized to "+normalized);
            }
        }
        for (String name : getters) {
            try {
                Object value = keyedMethod.get(keyedMethodName.get(name)).invoke(examine, null);
                //log.info(">> want to insert "+name+"="+value);
                if (value != null) {
                    map.put(name, "" + value);
                }
            } catch (IllegalAccessException e) {
                log.error("Could not access method g" + name + ". Got (masked exception) " + e.getMessage());
            } catch (InvocationTargetException e) {
                log.error("Could not access method g" + name + ". Got (masked exception) " + e.getMessage());
            }
        }
    }

    /**
     * Create a map and fill it with the public fields from the object, which
     * is the JavaSpace manner.
     */
    private Map<String, String> fillMapWithPublicFields(Object examine) {
        Field[] fields = classFieldMap.computeIfAbsent(examine.getClass().getName(), k -> examine.getClass().getFields());
        Map<String, String> map = new HashMap<>();
        for (Field field : fields) {
            try {
                String name = field.getName();
                Object value = field.get(examine);

                if (value != null) {
                    map.put(name, "" + value);
                }
            } catch (IllegalAccessException e) {
                log.warn("Introspection gave exception - which is not re-thrown.", e);
            }
        }
        return map;
    }

    /**
     * Preparing for future injection of admin. Note that you
     * must call initialization <b>yourself</b> after setting the
     * object
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
        return this.admin;
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

        @Override
        @SuppressWarnings("synthetic-access")
        public void run() {
            try {
                lease = writeInternally(entry, leaseTimeMs);
            } catch (Exception e) {
                log.debug("Got exception writing object.", e);
                exception = e;
            }
        }
    }

    /**
     * Harvest old elements from diverse listeners. Used from
     * the periodic harvester and junit tests.
     */
    public void harvest() {

        for (ListenerHolder listener : listeners.values()) {
            if (listener.getLiveUntil() < admin.calculateTime()) {
                cancelListener(listener);

            }
        }
        List<Holder> beforeEvict = new ArrayList<>();

        String[] groups = elements.retrieveGroupNames();
        for (String group : groups) {
            int evictSize = beforeEvict.size();
            HolderElement hc = elements.next(group);
            for (Holder elem : hc) {
                if (elem.getLiveUntil() < admin.calculateTime()) {
                    beforeEvict.add(elem);
                }
            }
            long afterSize = beforeEvict.size() - evictSize;
            if (afterSize > 0) {
                List<Long> ids = new ArrayList<>();
                for (Holder evict : beforeEvict) {
                    ids.add(Long.valueOf(evict.getId()));
                }
                String moreInfo = "";
                if (ids.size() < 30) {
                    Collections.sort(ids);
                    moreInfo = "Ids: " + ids;
                }
                log.debug("Testing group " + group + " gave " + afterSize + " element(s) to evict. " + moreInfo);
            }
        }
        for (Holder evict : beforeEvict) {
            cancelElement(Long.valueOf(evict.getId()), false, evict.getClassName());
        }
    }

    /**
     * Return the number of elements in the space. Notice that this may report old elements that have not been purged
     * yet.
     */
    public int numberOfSpaceElements() {
        int size;
        size = elements.size();
        return size;
    }

    /**
     * Need present statistics here due to spring JMX configuration.
     */
    public int numberOfBlockingRead() {
        return statistics.getBlockingRead();
    }

    /**
     * Need present statistics here due to spring JMX configuration.
     */
    public int numberOfBlockingTake() {
        return statistics.getBlockingTake();
    }

    /**
     * Need present statistics here due to spring JMX configuration.
     */
    public int numberOfMissedRead() {
        return statistics.getMissedRead();
    }

    /**
     * Need present statistics here due to spring JMX configuration.
     */
    public int numberOfMissedTake() {
        return statistics.getMissedTake();
    }

    /**
     * Need present statistics here due to spring JMX configuration.
     */
    public int numberOfNumberOfListeners() {
        return statistics.getNumberOfListeners();
    }

    /**
     * Need present statistics here due to spring JMX configuration.
     */
    public int numberOfRead() {
        return statistics.getRead();
    }

    /**
     * Need present statistics here due to spring JMX configuration.
     */
    public int numberOfTake() {
        return statistics.getTake();
    }

    /**
     * Need present statistics here due to spring JMX configuration.
     */
    public int numberOfWrite() {
        return statistics.getWrite();
    }

    /**
     * For the benefit of junit test(s) - defensively copied statistics
     */
    protected SemiSpaceStatistics getStatistics() {
        // Defensively copied statistics
        return statistics.copy();
    }

    /**
     * @return true if listener was removed
     */
    protected boolean cancelListener(ListenerHolder holder) {
        if (listeners.remove(Long.valueOf(holder.getId())) != null) {
            statistics.decreaseNumberOfListeners();
            return true;
        }

        return false;
    }

    protected boolean renewListener(ListenerHolder holder, long duration) {
        boolean success = false;

        ListenerHolder listener = listeners.get(Long.valueOf(holder.getId()));
        if (listener != null) {
            listener.setLiveUntil(duration + admin.calculateTime());
            // Need to re-get due in order to be certain of liveness.
            success = listeners.get(Long.valueOf(holder.getId())) != null;
        }
        return success;
    }

    /**
     * @param isTake true if reason for the cancellation is a take.
     */
    protected boolean cancelElement(Long id, boolean isTake, String className) {
        boolean success = false;

        Holder elem = elements.removeHolderById(id.longValue(), className);
        if (elem != null) {
            if (elem.getId() != id.longValue()) {
                throw new SemiSpaceInternalException("Sanity problem. Removed " + id.longValue() + " and got back element with id " + elem.getId());
            }
            success = true;
            SemiEvent semiEvent = null;
            if (isTake) {
                semiEvent = new SemiTakenEvent(elem.getId());
            } else {
                semiEvent = new SemiExpirationEvent(elem.getId());
            }
            //log.debug("Notifying about "+(isTake?"take":"expiration")+" of element with id "+semiEvent.getId());
            //notifyListeners(new EventDistributor(elem.getClassName(), semiEvent, elem.getSearchMap()));
            distributeEvent(new DistributedEvent(elem.getClassName(), semiEvent,
                    elem.getSearchMap()));

        }

        return success;
    }

    /**
     * @return true if the object actually was renewed. (I.e. it exists and got a new timeout.)
     */
    protected boolean renewElement(Holder holder, long duration) {
        boolean success = false;
        Holder elem = elements.findById(holder.getId(), holder.getClassName());
        if (elem != null) {
            elem.setLiveUntil(duration + admin.calculateTime());
            success = true;
            distributeEvent(new DistributedEvent(elem.getClassName(), new SemiRenewalEvent(
                    elem.getId(), elem.getLiveUntil()), elem.getSearchMap()));
        }

        return success;
    }

    /**
     * @see HolderContainer#findAllHolderIds
     */
    public Long[] findAllHolderIds() {
        return elements.findAllHolderIds();
    }

    /**
     * Exposing xstream instance in order to allow outside manipulation of aliases and classloader affiliation.
     *
     * @return The xstream instance used.
     */
    public SemiSpaceSerializer getXStream() {
        return serializer;
    }

    private static class ShortestTtlComparator implements Comparator<ListenerHolder>, Serializable {
        @Override
        public int compare(ListenerHolder o1, ListenerHolder o2) {
            if (o1 == null || o2 == null) {
                throw new SemiSpaceUsageException("Did not expect any null values for listenerHolder.");
            }
            return (int) (o1.getLiveUntil() - o2.getLiveUntil());
        }
    }
}
