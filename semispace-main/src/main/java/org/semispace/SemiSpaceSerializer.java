package org.semispace;


public interface SemiSpaceSerializer {

    String objectToXml(Object obj);

    Object xmlToObject(String xml);
}
