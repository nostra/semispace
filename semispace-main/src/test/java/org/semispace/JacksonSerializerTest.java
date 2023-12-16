package org.semispace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.semispace.actor.ActorManifest;

class JacksonSerializerTest {
    private JacksonSerializer jackson = new JacksonSerializer();

    @Test
    void objectToXml() {
        var str = """
                {"className":"org.semispace.actor.ActorManifest","payload":"{\\"holderId\\":8,\\"originatorId\\":6}"}
                """.stripIndent();
        ActorManifest actorManifest = (ActorManifest) jackson.xmlToObject(str);
        assertNotNull( actorManifest );
    }

    @Test
    void xmlToObject() {
        ActorManifest am = new ActorManifest(123L, 456L);
        var str = jackson.objectToXml(am);
        ActorManifest read = (ActorManifest) jackson.xmlToObject(str);
        assertEquals( am.getHolderId(), read.getHolderId());
        assertEquals( am.getOriginatorId(), read.getOriginatorId());
    }
}