/*
 * Copyright 2010 Erlend Nossum
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
 */

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

package org.semispace.comet.demo;

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
    public boolean equals( Object obj ) {
        if ( ! (obj instanceof FieldHolder) ) {
            return false;
        }
        boolean status = true;
        FieldHolder comp = (FieldHolder) obj;
        if ( fieldA == null && comp.fieldA != null  ) {
            status = false;
        } else if ( fieldB == null && comp.fieldB != null  ) {
            status = false;
        } else {
            status = fieldA.equals( comp.fieldA ) && fieldB.equals( comp.fieldB );
        }
        return status;
    }

    @Override
    public String toString() {
        return getClass().getName()+"[fieldA:"+fieldA+"]"+"[fieldB:"+fieldB+"]";
    }
}