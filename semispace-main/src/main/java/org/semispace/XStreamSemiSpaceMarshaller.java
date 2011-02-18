package org.semispace;

import com.thoughtworks.xstream.io.xml.CompactWriter;
import java.io.StringWriter;

import com.thoughtworks.xstream.XStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XStreamSemiSpaceMarshaller implements SemiSpaceMarshaller {
	private static final Logger log = LoggerFactory.getLogger(SemiSpace.class);
	private XStream xstream = null;
	
	public XStreamSemiSpaceMarshaller() {
		xstream = new XStream();
    }
	
	public XStreamSemiSpaceMarshaller(XStream xstream) {
		this.xstream = xstream;
	}
	
	public XStream getXStream() {
		return xstream;
	}
	
	public void setXStream(XStream xstream) {
		this.xstream = xstream;
	}

	@Override
	public String objectToXml(Object aObj) {
		StringWriter writer = new StringWriter();
		xstream.marshal(aObj, new CompactWriter(writer));
		return writer.toString();
	}

	@Override
	public Object xmlToObject(String xml) {
		Object result = null;
		try {
			result = xstream.fromXML(xml);
		}
		catch (Exception e) {
			// Not sure if masking exception is the most correct way of dealing with it.
			log.error(
			    "Got exception unmarshalling. Not throwing the exception up, but rather returning null. "
			            + "This is as the cause may be a change in the object which is sent over. "
			            + "The XML was read as\n" + xml, e);
		}
		return result;
	}
}
