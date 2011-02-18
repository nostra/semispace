package org.semispace;

public interface SemiSpaceMarshaller {
	public String objectToXml(Object obj);
	public Object xmlToObject(String xml);
}
