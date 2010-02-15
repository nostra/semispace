/*
 * ============================================================================
 *
 *  File:     AlternateHolder.java
 *----------------------------------------------------------------------------
 *
 * No copying allowed without explicit permission.
 *
 *  All rights reserved.
 *
 *  Description:  See javadoc below
 *
 *  Created:      30. des.. 2007
 * ============================================================================ 
 */

package org.semispace;

public class AlternateHolder {
    public String fieldA;
    public String fieldB;
    
    @Override
    public String toString() {
        return "AlternateHolder[fieldA:"+fieldA+"][fieldB:"+fieldB+"]";
    }
}
