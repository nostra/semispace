/*
 * ============================================================================
 *
 *  File:     SemiSpaceStatistics.java
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
 *  Created:      Mar 22, 2008
 * ============================================================================ 
 */

package org.semispace;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holder for statistical elements
 */
public class SemiSpaceStatistics {
    private AtomicInteger read = new AtomicInteger();
    private AtomicInteger take = new AtomicInteger();
    private AtomicInteger write = new AtomicInteger();
    private AtomicInteger missedTake = new AtomicInteger();
    private AtomicInteger missedRead = new AtomicInteger();
    private AtomicInteger blockingRead = new AtomicInteger();
    private AtomicInteger blockingTake = new AtomicInteger();
    private AtomicInteger numberOfListeners = new AtomicInteger();

    protected SemiSpaceStatistics copy() {
        SemiSpaceStatistics copy = new SemiSpaceStatistics();
        copy.read.set(read.get());
        copy.take.set(take.get());
        copy.write.set(write.get());
        copy.missedTake.set(missedTake.get());
        copy.missedRead.set(missedRead.get());
        copy.blockingRead.set(blockingRead.get());
        copy.blockingTake.set(blockingTake.get());
        copy.numberOfListeners.set(numberOfListeners.get());
        return copy;
    }

    public int getRead() {
        return this.read.get();
    }
    public int getTake() {
        return this.take.get();
    }
    public int getWrite() {
        return this.write.get();
    }
    public int getBlockingRead() {
        return this.blockingRead.get();
    }
    public int getBlockingTake() {
        return this.blockingTake.get();
    }
    public int getNumberOfListeners() {
        return this.numberOfListeners.get();
    }
    public int getMissedTake() {
        return this.missedTake.get();
    }
    public int getMissedRead() {
        return this.missedRead.get();
    }
    protected void increaseWrite() {
        write.incrementAndGet();
    }
    protected void increaseRead() {
        read.incrementAndGet();
    }
    protected void increaseTake() {
        take.incrementAndGet();
    }
    protected void increaseMissedRead() {
        missedRead.incrementAndGet();
    }
    protected void increaseNumberOfListeners() {
        numberOfListeners.incrementAndGet();
    }
    protected void decreaseNumberOfListeners() {
        numberOfListeners.decrementAndGet();
    }
    protected void increaseMissedTake() {
        missedTake.incrementAndGet();
    }
    protected void increaseBlockingRead() {
        blockingRead.incrementAndGet();
    }
    protected void decreaseBlockingRead() {
        blockingRead.decrementAndGet();
    }
    protected void increaseBlockingTake() {
        blockingTake.incrementAndGet();
    }
    protected void decreaseBlockingTake() {
        blockingTake.decrementAndGet();
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[read:"+read+",");
        sb.append("take:"+take+",");
        sb.append("write:"+write+",");
        sb.append("missedRead:"+missedRead+",");
        sb.append("missedTake:"+missedTake+",");
        sb.append("blockingRead:"+blockingRead+",");
        sb.append("blockingTake:"+blockingTake+",");
        sb.append("numberOfListeners:"+numberOfListeners+"]");
        return sb.toString();
    }
}
