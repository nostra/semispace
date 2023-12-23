package org.semispace;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;

/**
 * XStreamSerializer is a serializer that uses XStream to serialize and deserialize objects.
 */
public class XStreamSerializer implements SemiSpaceSerializer{
    private final XStream xStream;
    private final Logger log = LoggerFactory.getLogger(XStreamSerializer.class);

    public XStreamSerializer() {
        this(new XStream());
    }

    public XStreamSerializer(XStream xStream) {
        this.xStream = xStream;
    }


    @Override
    public String objectToXml(Object obj) {
        StringWriter writer = new StringWriter();
        xStream.marshal(obj, new CompactWriter(writer));
        return writer.toString();
    }

    @Override
    public Object xmlToObject(String xml) {
        if (xml == null || xml.isEmpty()) {
            return null;
        }
        Object result = null;
        try {
            result = xStream.fromXML(xml);
        } catch (Exception e) {
            // Not sure if masking exception is the most correct way of dealing with it.
            log.error("Got exception unmarshalling. Not throwing the exception up, but rather returning null. "
                    + "This is as the cause may be a change in the object which is sent over. "
                    + "The XML was read as\n" + xml, e);
        }
        return result;
    }

}
