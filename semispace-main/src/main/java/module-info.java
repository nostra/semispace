module org.semispace {
    requires static java.desktop;

    requires org.slf4j;
    requires tools.jackson.databind;
    requires transitive tools.jackson.dataformat.xml;

    // --- exports: everything reachable from a public signature -------------
    exports org.semispace;
    exports org.semispace.actor;
    exports org.semispace.admin;
    exports org.semispace.event;

    exports org.semispace.exception;

    opens org.semispace to tools.jackson.databind;
    opens org.semispace.actor to tools.jackson.databind;
    opens org.semispace.admin to tools.jackson.databind;
}
