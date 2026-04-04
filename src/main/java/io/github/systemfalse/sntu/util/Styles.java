package io.github.systemfalse.sntu.util;

import org.fusesource.jansi.Ansi;

import java.net.*;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Styles {
    public static final ScopedValue<Styles> STYLES = ScopedValue.newInstance();

    //network properties
    public static final String DISPLAY_NAME = "display-name";
    public static final String INTERFACE_NAME = "interface-name";
    public static final String MAC_ADDRESS = "mac-address";
    public static final String IP_ADDRESS = "ip-address";
    public static final String SUBNET_MASK = "subnet-mask";

    //constants
    public static final String CONSTANT_TRUE = "true";
    public static final String CONSTANT_FALSE = "false";

    //text blocks
    public static final String SECTION = "section";
    public static final String SUBSECTION = "subsection";
    public static final String SUBSECTION2 = "subsection2";
    public static final String USER_PROMPT = "user-prompt";
    public static final String ERROR = "error";
    public static final String WARNING = "warning";
    public static final String INFO = "info";

    private final Map<String, Ansi.Consumer> styles;

    public Styles() {
        styles = new HashMap<>();
        initStyles();
    }

    private void initStyles() {
        styles.put(DISPLAY_NAME, a -> a.fgBlue().a(Ansi.Attribute.ITALIC));
        styles.put(INTERFACE_NAME, Ansi::fgBlue);
        styles.put(MAC_ADDRESS, Ansi::fgMagenta);
        styles.put(IP_ADDRESS, a -> a.fgBlue().a(Ansi.Attribute.UNDERLINE));
        styles.put(SUBNET_MASK, Ansi::fgCyan);

        styles.put(CONSTANT_TRUE, Ansi::fgGreen);
        styles.put(CONSTANT_FALSE, Ansi::fgRed);

        styles.put(SECTION, a -> a.fgYellow().bold());
        styles.put(SUBSECTION, a -> a.a("  ").fgYellow());
        styles.put(SUBSECTION2, a -> a.a("    ").fgYellow());
        styles.put(USER_PROMPT, Ansi::fgBrightYellow);
        styles.put(ERROR, Ansi::fgRed);
        styles.put(WARNING, a -> a.fg(220));
        styles.put(INFO, a -> a.fg(111));
    }

    public Ansi applyStyle(Ansi ansi, String key) {
        return ansi.apply(styles.get(key));
    }

    public Ansi.Consumer interfaceDisplayName(NetworkInterface networkInterface) {
        return ansi -> applyStyle(ansi, DISPLAY_NAME).a(networkInterface.getDisplayName()).reset();
    }

    public Ansi.Consumer interfaceName(NetworkInterface networkInterface) {
        return ansi -> applyStyle(ansi, INTERFACE_NAME).a(networkInterface.getName()).reset();
    }

    public Ansi.Consumer macAddress(byte[] macAddress) {
        String macString = IntStream.range(0, macAddress.length)
                .map(i -> macAddress[i] & 0xff)
                .mapToObj(Integer::toHexString)
                .map(s -> s.length() == 1 ? "0" + s : s)
                .collect(Collectors.joining("-"));
        return ansi -> applyStyle(ansi, MAC_ADDRESS).a(macString).reset();
    }

    public Ansi.Consumer ipAddress(InetAddress ipAddress) {
        return ansi -> applyStyle(ansi, IP_ADDRESS).a(ipAddress.getHostAddress()).reset();
    }

    public Ansi.Consumer booleanType(boolean bool) {
        return ansi -> applyStyle(ansi, bool ? CONSTANT_TRUE : CONSTANT_FALSE).a(bool).reset();
    }

    public Ansi.Consumer section(String section) {
        return ansi -> applyStyle(ansi, SECTION).a(section).reset();
    }

    public Ansi.Consumer subsection(String subsection) {
        return ansi -> applyStyle(ansi, SUBSECTION).a(subsection).reset();
    }

    public Ansi.Consumer subsection2(String subsection2) {
        return ansi -> applyStyle(ansi, SUBSECTION2).a(subsection2).reset();
    }

    public Ansi.Consumer subnetMask(InterfaceAddress address) {
        int mask = address.getNetworkPrefixLength();
        if (address.getAddress() instanceof Inet4Address) {
            return ansi -> applyStyle(ansi, SUBNET_MASK).a(maskToString(mask, 32)).reset();
        } else if (address.getAddress() instanceof Inet6Address) {
            return ansi -> applyStyle(ansi, SUBNET_MASK).a(maskToString(mask, 128)).reset();
        } else return _ -> {};
    }

    public Ansi.Consumer userPrompt(String prompt) {
        return ansi -> applyStyle(ansi, USER_PROMPT).a(prompt).reset();
    }

    public Ansi.Consumer error(String text) {
        return ansi -> applyStyle(ansi, ERROR).a(text).reset();
    }

    public Ansi.Consumer warning(String text) {
        return ansi -> applyStyle(ansi, WARNING).a(text).reset();
    }

    public Ansi.Consumer info(String text) {
        return ansi -> applyStyle(ansi, INFO).a(text).reset();
    }

    private static String maskToString(int mask, int addressSize) {
        if (addressSize == Integer.SIZE) {
            //IPv4
            switch (mask) {
                case 8 -> {
                    return "255.0.0.0";
                }
                case 16 -> {
                    return "255.255.0.0";
                }
                case 24 -> {
                    return "255.255.255.0";
                }
            }
        } else {
            //IPv6
            switch (mask) {
                case 32 -> {
                    return "ffff:ffff::";
                }
                case 64 -> {
                    return "ffff:ffff:ffff:ffff::";
                }
                case 96 -> {
                    return "ffff:ffff:ffff:ffff:ffff:ffff::";
                }
            }
        }
        BitSet bitMask = new BitSet(addressSize);
        bitMask.set(0, mask);
        byte[] byteAddress = new byte[addressSize / Byte.SIZE];
        byte[] byteMask = bitMask.toByteArray();
        System.arraycopy(byteMask, 0, byteAddress, 0, byteMask.length);
        try {
            return InetAddress.getByAddress(byteAddress).getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static Styles getInstance() {
        return STYLES.isBound() ? STYLES.get() : new Styles();
    }
}
