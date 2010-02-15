/*
 * ============================================================================
 *
 *  File:     Element.java
 *----------------------------------------------------------------------------
 *
 * No copying allowed without explicit permission.
 *
 *  All rights reserved.
 *
 *  Description:  See javadoc below
 *
 *  Created:      27. jan.. 2008
 * ============================================================================ 
 */

package org.semispace.space.tutorial;

/**
 * A simple name / value element.
 */
public class Element {
    private String name;
    private String value;
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getValue() {
        return this.value;
    }
    public void setValue(String value) {
        this.value = value;
    }
}
