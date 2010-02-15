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
        return examineElements( obj, false);
    }

    public Object take(Object obj, long duration) {
        return takeIfExists(obj);
    }

    public Object takeIfExists(Object obj) {
        return examineElements( obj, true);
    }

    private Object examineElements( Object obj, boolean take ) {
        for ( Object elem : elements ) {
            if ( elem.getClass().isAssignableFrom(obj.getClass()) ) {
                if ( take ) {
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
