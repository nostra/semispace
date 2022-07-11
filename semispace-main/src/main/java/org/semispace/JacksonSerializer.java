package org.semispace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.semispace.exception.SemiSpaceObjectException;

public class JacksonSerializer implements SemiSpaceSerializer {
    private final ObjectMapper mapper;

    public JacksonSerializer() {
        this(new ObjectMapper()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS,false)
        );
    }

    public JacksonSerializer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String objectToXml(Object obj) {
        if ( obj == null ) {
            return null;
        }
        try {
            JacksonObject holder = new JacksonObject();
            holder.className = obj.getClass().getName();
            holder.payload = mapper.writeValueAsString(obj);
            return mapper.writeValueAsString(holder);
        } catch (JsonProcessingException e) {
            throw new SemiSpaceObjectException("Could not process json", e);
        }
    }

    @Override
    public Object xmlToObject(String xml) {
        if (xml == null || xml.isEmpty()) {
            return null;
        }

        try {
            JacksonObject holder = mapper.readValue( xml, JacksonObject.class);
            Class type = Class.forName(holder.className);
            return mapper.readValue(holder.payload, type);
        } catch (JsonProcessingException e) {
            throw new SemiSpaceObjectException("Jackson could not process json", e);
        } catch (ClassNotFoundException e) {
            throw new SemiSpaceObjectException("Class not found, which implies that objects in backend storage are " +
                    "broken, or that distribution uses different versions of application", e);
        }
    }

    private static class JacksonObject {
        public String className;
        public String payload;
    }

}
