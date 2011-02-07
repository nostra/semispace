/*
 * ============================================================================
 *
 *  File:     HolderContainer.java
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
 *  Created:      Apr 27, 2008
 * ============================================================================ 
 */

package org.semispace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.semispace.exception.SemiSpaceObjectException;
import org.semispace.exception.SemiSpaceUsageException;
import org.terracotta.annotations.AutolockRead;
import org.terracotta.annotations.AutolockWrite;
import org.terracotta.annotations.InstrumentedClass;
import org.terracotta.annotations.Root;

/**
 * Container for holder elements.
 */
@InstrumentedClass
public class HolderContainer {
	private long idseq = 0;
	private Map<String, HolderElement> heads = null;
	
	@Root
	private static final HolderContainer instance = new HolderContainer();
	
	public static HolderContainer retreiveContainer() {
		return instance;
	}

	public HolderContainer() {
		heads = new HashMap<String, HolderElement>();
	}
	
	@AutolockWrite
	public synchronized long getNextId() {
		idseq++;
		return idseq;
	}

	public synchronized HolderElement next(String className) {
		return heads.get(className);
	}
	
	@AutolockWrite
	public void waitHolder(String className, long timeout) {
		HolderElement e = null;
		synchronized (this) {
			e = heads.get(className);
			if (e == null) {
				e = new HolderElement();
				heads.put(className, e);
			}
        }
		e.waitHolder(timeout);
	}

	@AutolockWrite
	public synchronized Holder removeHolderById(long id, String className) {
		Holder toReturn = null;
		HolderElement head = heads.get(className);
		if (head == null) {
			return null;
		}
		toReturn = head.removeHolderById(id);

		return toReturn;
	}

	@AutolockRead
	public synchronized Holder findById(long id, String className) {
		HolderElement n = heads.get(className);
		if (n == null) {
			return null;
		}
		return n.findById(id);
	}

	@AutolockWrite
	public synchronized Holder addHolder(Holder add) {
		if (add == null) {
			throw new SemiSpaceUsageException("Illegal to add null");
		}
		if (add.getClassName() == null) {
			throw new SemiSpaceObjectException("Need classname in holder with contents "
				+ add.getXml());
		}
		HolderElement head = heads.get(add.getClassName());
		if (head == null) {
			head = HolderElement.createNewCollection(add);
			heads.put(add.getClassName(), head);
		}
		else {
			head.addHolder(add);
		}
		
		return add;
	}
	
	@AutolockWrite
	public synchronized Holder addHolder(String xml, long liveUntil, String className, Map<String, String> map) {
		long holderId = getNextId();
		Holder holder = new Holder(xml, liveUntil, className, holderId, map);
		return addHolder(holder);
	}

	/**
	 * Method presumed called on first object, which is the holder object. Returning count,
	 * excluding holder.
	 */
	@AutolockRead
	public synchronized int size() {
		if (heads == null) {
			return 0;
		}
		int size = 0;
		
		for (HolderElement head : heads.values()) {
			size += head.size();
		}
		return size;
	}

	@AutolockRead
	public synchronized String[] retrieveGroupNames() {
		String[] result = null;
		result = heads.keySet().toArray(new String[0]);
		return result;
	}

	@AutolockRead
	public synchronized Holder readHolderWithId(long id) {
		String[] cnames = retrieveClassNames();
		for (String lookup : cnames) {
			HolderElement next = next(lookup);
			Holder toReturn = next.findById(id);
			if (toReturn != null) {
				return toReturn;
			}
		}
		return null;
	}

	/**
	 * Return all ids present. Notice that this method will be rather network expensive, and is only
	 * intended to be used for persistence purposes.
	 */
	public Long[] findAllHolderIds() {
		List<Long> allIds = new ArrayList<Long>();
		String[] cnames = retrieveClassNames();
		for (String lookup : cnames) {
			HolderElement next = next(lookup);
			synchronized (this) {
				for (Holder elem : next.toArray()) {
					allIds.add(Long.valueOf(elem.getId()));
				}
			}
		}
		return allIds.toArray(new Long[0]);
	}

	@AutolockRead
	private synchronized String[] retrieveClassNames() {
		String[] cnames = null;
		cnames = heads.keySet().toArray(new String[0]);
		return cnames;
	}
}
