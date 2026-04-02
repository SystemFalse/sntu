module sntu {
    exports io.github.systemfalse.sntu;

    requires info.picocli;
    requires org.fusesource.jansi;
    requires org.apache.commons.io;

    opens io.github.systemfalse.sntu to info.picocli;
}