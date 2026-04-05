package io.github.systemfalse.sntu.util;

import java.io.IOException;
import java.net.InetAddress;

public class ConnectionException extends IOException {
    private final InetAddress address;

    public ConnectionException(String message, InetAddress address) {
        super(message);
        this.address = address;
    }

    public InetAddress address() {
        return address;
    }
}
