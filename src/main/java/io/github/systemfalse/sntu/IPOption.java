package io.github.systemfalse.sntu;

import picocli.CommandLine;

public class IPOption {
    @CommandLine.Option(
            names = "-4",
            description = "Use only IPv4"
    )
    private boolean ipv4;

    @CommandLine.Option(
            names = "-6",
            description = "Use only IPv6"
    )
    private boolean ipv6;

    @CommandLine.Option(
            names = "-46",
            description = "Use both IPv4 and IPv6 (default)",
            defaultValue = "true"
    )
    private boolean ipv46;

    public boolean ipv4() {
        return ipv4;
    }

    public boolean ipv6() {
        return ipv6;
    }

    public boolean ipv46() {
        return ipv46;
    }
}
