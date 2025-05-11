package org.semispace;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface SemiSpaceSerializer {

    String objectToXml(Object obj);

    Object xmlToObject(String xml);
}
