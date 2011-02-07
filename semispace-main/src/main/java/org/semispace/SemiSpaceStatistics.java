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

import org.terracotta.annotations.InstrumentedClass;

/**
 * Holder for statistical elements
 */
@InstrumentedClass
public class SemiSpaceStatistics {
    private int read = 0;
    private int take = 0;
    private int write = 0;
    private int missedTake = 0;
    private int missedRead = 0;
    private int blockingRead = 0;
    private int blockingTake = 0;
    private int numberOfListeners = 0;
    
    public int getRead() {
        return this.read;
    }
    public int getTake() {
        return this.take;
    }
    public int getWrite() {
        return this.write;
    }
    public int getBlockingRead() {
        return this.blockingRead;
    }
    public int getBlockingTake() {
        return this.blockingTake;
    }
    public int getNumberOfListeners() {
        return this.numberOfListeners;
    }
    public int getMissedTake() {
        return this.missedTake;
    }
    public int getMissedRead() {
        return this.missedRead;
    }
    protected void increaseWrite() {
        write++;
    }
    protected void increaseRead() {
        read++;
    }
    protected void increaseTake() {
        take++;
    }
    protected void increaseMissedRead() {
        missedRead++;
    }
    protected void increaseNumberOfListeners() {
        numberOfListeners++;
    }
    protected void decreaseNumberOfListeners() {
        numberOfListeners--;
    }
    protected void increaseMissedTake() {
        missedTake++;
    }
    protected void increaseBlockingRead() {
        blockingRead++;
    }
    protected void decreaseBlockingRead() {
        blockingRead--;
    }
    protected void increaseBlockingTake() {
        blockingTake++;
    }
    protected void decreaseBlockingTake() {
        blockingTake--;
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
