package io.github.systemfalse.sntu;

import io.github.systemfalse.sntu.util.Styles;
import picocli.CommandLine;

import java.io.PrintStream;
import java.net.*;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.ansi;

@CommandLine.Command(
        name = "net-info",
        description = "Get information about network interface",
        sortOptions = false,
        sortSynopsis = false,
        footer = {
                "@|yellow If interactive mode enabled|@, program will ask to select network interface @|bold only if not specified|@"
        }
)
public class NetInfoCommand implements Callable<Integer> {
    @CommandLine.Option(
            names = {"-h", "-?", "--help"},
            usageHelp = true,
            hidden = true
    )
    private boolean help;

    @CommandLine.ParentCommand
    private SntuCommand parent;

    @CommandLine.Option(
            names = {"-n", "--network-interface"},
            description = "Network interface to use"
    )
    NetworkInterface networkInterface;

    @Override
    public Integer call() throws Exception {
        PrintStream out = Main.OUTPUT.orElse(System.out);
        PrintStream err = Main.ERROR.orElse(System.err);

        Optional<NetworkInterface> anInterface = parent.getNetworkInterface(networkInterface);
        anInterface.ifPresent(ni -> printInfo(out, ni, 0));

        return 0;
    }

    public void printInfo(PrintStream out, NetworkInterface ni, int index) {
        Styles styles = Styles.getInstance();
        String space = " ".repeat(index * 4);
        out.println(ansi().a(space).apply(styles.section("Information:")));
        out.println(ansi().a(space).apply(styles.subsection("Display name: ")).apply(styles.interfaceDisplayName(ni)));
        out.println(ansi().a(space).apply(styles.subsection("Name: ")).apply(styles.interfaceName(ni)));
        try {
            byte[] mac = ni.getHardwareAddress();
            if (mac != null) {
                out.println(ansi().a(space).apply(styles.subsection("MAC address: ")).apply(styles.macAddress(mac)));
            }
        } catch (SocketException e) {
            //ignore
        }
        try {
            out.println(ansi().a(space).apply(styles.subsection("MTU: ")).a(ni.getMTU()));
        } catch (SocketException e) {
            //ignore
        }
        try {
            boolean isp2p = ni.isPointToPoint();
            out.println(ansi().a(space).apply(styles.subsection("Point-to-point: ")).apply(styles.booleanType(isp2p)));
        } catch (SocketException e) {
            //ignore
        }
        try {
            boolean isLoopback = ni.isLoopback();
            out.println(ansi().a(space).apply(styles.subsection("Loopback: ")).apply(styles.booleanType(isLoopback)));
        } catch (SocketException e) {
            //ignore
        }
        {
            boolean isVirtual = ni.isVirtual();
            out.println(ansi().a(space).apply(styles.subsection("Virtual: ")).apply(styles.booleanType(isVirtual)));
        }
        try {
            boolean isUp = ni.isUp();
            out.println(ansi().a(space).apply(styles.subsection("Up: ")).apply(styles.booleanType(isUp)));
        } catch (SocketException e) {
            //ignore
        }
        try {
            boolean supportsMulticast = ni.supportsMulticast();
            out.println(ansi().a(space).apply(styles.subsection("Supports multicast: ")).apply(styles.booleanType(supportsMulticast)));
        } catch (SocketException e) {
            //ignore
        }
        var ias = ni.getInterfaceAddresses();
        if (!ias.isEmpty()) {
            LinkedList<String> masks = new LinkedList<>();
            for (InterfaceAddress ia : ias) {
                if (ia.getAddress() instanceof Inet4Address) {
                    masks.add(ansi().a(space).apply(styles.subsection2("IPv4 mask: ")).apply(styles.subnetMask(ia)).newline().toString());
                } else if (ia.getAddress() instanceof Inet6Address) {
                    masks.add(ansi().a(space).apply(styles.subsection2("IPv6 mask: ")).apply(styles.subnetMask(ia)).newline().toString());
                }
            }
            if (!masks.isEmpty()) {
                out.println(ansi().apply(styles.subsection("Subnet masks:")));
                masks.stream().distinct().forEach(out::print);
            }
        }
        var si = ni.getSubInterfaces();
        if (si.hasMoreElements()) {
            out.println(ansi().apply(styles.subsection("Subinterfaces:")));
            while (si.hasMoreElements()) {
                NetworkInterface subInterface = si.nextElement();
                printInfo(out, subInterface, index + 1);
            }
        }
    }
}
