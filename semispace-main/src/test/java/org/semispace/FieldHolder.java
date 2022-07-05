/*
 * ============================================================================
 *
 *  File:     FieldHolder.java
 *----------------------------------------------------------------------------
 *
 * No copying allowed without explicit permission.
 *
 *  All rights reserved.
 *
 *  Description:  See javadoc below
 *
 *  Created:      25. des.. 2007
 * ============================================================================
 */

package org.semispace;

public class FieldHolder {
    private String fieldA;
    private String fieldB;

    public String getFieldA() {
        return this.fieldA;
    }

    public void setFieldA(String fieldA) {
        this.fieldA = fieldA;
    }

    public String getFieldB() {
        return this.fieldB;
    }

    public void setFieldB(String fieldB) {
        this.fieldB = fieldB;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FieldHolder)) {
            return false;
        }
        boolean status = true;
        FieldHolder comp = (FieldHolder) obj;
        if (fieldA == null && comp.fieldA != null) {
            status = false;
        } else if (fieldB == null && comp.fieldB != null) {
            status = false;
        } else {
            status = fieldA.equals(comp.fieldA) && fieldB.equals(comp.fieldB);
        }
        return status;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[fieldA:" + fieldA + "]" + "[fieldB:" + fieldB + "]";
    }
}
