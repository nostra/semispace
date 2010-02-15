/*
 * ============================================================================
 *
 *  File:     DelayedId.java
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
 *  Created:      Jun 11, 2008
 * ============================================================================ 
 */

package org.semispace.persistence;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayedId implements Delayed  {
    private long delayMs;
    private long id;
    private long originalTime;
    private boolean shallIgnoreTime = false;
    
    protected long getId() {
        return this.id;
    }

    protected void setId(long id) {
        this.id = id;
    }

    public DelayedId(long id, long delayMs) {
        this.id = id;
        this.delayMs= delayMs;
        this.originalTime = System.currentTimeMillis();
        this.shallIgnoreTime = false;
    }

    /**
     * This is a workaround for a bug in java-1.5, which has been fixed in 1.6.
     * The problem is, basically, that when using this in a delay queue, the 
     * comparator is used for later equality comparisons, and not the equals method.
     */
    public DelayedId(long id, int delayMs, boolean b) {
        this.id = id;
        this.delayMs= delayMs;
        this.originalTime = System.currentTimeMillis();
        this.shallIgnoreTime = b;
    }

    public long getDelay(TimeUnit unit) {
        return getDelay(unit, System.currentTimeMillis() );
    }

    private long getDelay(TimeUnit unit, long base ) {
        long timeLeft = delayMs + originalTime - base;
        return unit.convert(timeLeft, TimeUnit.MILLISECONDS);    
    }

    public int compareTo(DelayedId other) {
        final long base = System.currentTimeMillis(); 
        int d = 0;
        if ( !this.shallIgnoreTime && !other.shallIgnoreTime ) {
            d = (int) ( getDelay(TimeUnit.MILLISECONDS, base ) - other.getDelay(TimeUnit.MILLISECONDS, base ));
        }
        if ( d == 0 ) {
            d = (int) (getId() - other.getId() );
        }
        return d;
    }
    
    
    /**
     * Need to override equals due to a difference in java-1.5 and 1.6 with regards to 
     * how an element is removed from delayed queue.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) {
            return false;
        }
        DelayedId other = (DelayedId) obj;
        return this.id == other.id && this.delayMs == other.delayMs;
    }
    /**
     * Must override as equals is already overridden.
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int hashCode = (int)(id ^ (id >>> 32) ^ (delayMs >>> 8 ));
        return hashCode;
    }
     
    /**
     * Only present for debug purposes.
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[id:").append(id);
        sb.append("][delayMs:").append(delayMs);
        sb.append("][originalTime:").append(originalTime).append("]");
        return sb.toString();
    }

    public int compareTo(Delayed obj) {
        return compareTo((DelayedId) obj);
    }
}
