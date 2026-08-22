/*
 * Copyright © 2026 Erlend Nossum
 *
 * This file is part of the semispace project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Description:  See javadoc below
 */

package org.semispace.admin;

import org.semispace.SemiEventListener;
import org.semispace.SemiEventRegistration;
import org.semispace.SemiLease;
import org.semispace.SemiSpaceInterface;

import java.util.ArrayList;
import java.util.List;

public class JunitSpace implements SemiSpaceInterface {
    private List elements = new ArrayList();

    public SemiEventRegistration notify(Object tmpl, SemiEventListener listener, long duration) {
        return null;
    }

    public Object read(Object obj, long duration) {
        return readIfExists(obj);
    }

    public Object readIfExists(Object obj) {
        return examineElements(obj, false);
    }

    public Object take(Object obj, long duration) {
        return takeIfExists(obj);
    }

    public Object takeIfExists(Object obj) {
        return examineElements(obj, true);
    }

    private Object examineElements(Object obj, boolean take) {
        for (Object elem : elements) {
            if (elem.getClass().isAssignableFrom(obj.getClass())) {
                if (take) {
                    elements.remove(elem);
                }
                return elem;
            }
        }
        return null;
    }

    public SemiLease write(Object obj, long duration) {
        elements.add(obj);
        return null;
    }

}
