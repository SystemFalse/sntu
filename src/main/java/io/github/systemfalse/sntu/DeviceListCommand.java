package io.github.systemfalse.sntu;

import io.github.systemfalse.sntu.util.ConnectionException;
import io.github.systemfalse.sntu.util.Styles;
import picocli.CommandLine;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.fusesource.jansi.Ansi.ansi;

@CommandLine.Command(
        name = "device-list",
        description = "Get list of devices connected to local network",
        sortOptions = false,
        sortSynopsis = false,
        footer = {
                "@|yellow If interactive mode enabled|@, program will ask to select network interface @|bold only if not specified|@"
        }
)
public class DeviceListCommand implements Callable<Integer> {
    @SuppressWarnings("unused")
    @CommandLine.Option(
            names = {"-h", "-?", "--help"},
            usageHelp = true,
            hidden = true
    )
    private boolean help;

    @SuppressWarnings("unused")
    @CommandLine.ParentCommand
    private SntuCommand parent;

    @SuppressWarnings("unused")
    @CommandLine.Option(
            names = {"-n", "--network-interface"},
            description = "Network interface to use"
    )
    private NetworkInterface networkInterface;

    @CommandLine.Option(
            names = {"-t", "--timeout"},
            description = "Maximum time to wait for response (in ms)",
            defaultValue = "3000"
    )
    int timeout;

    @CommandLine.Option(
            names = {"-e", "--errors"},
            description = "Show IO errors during device discovery",
            defaultValue = "false"
    )
    boolean showErrors;

    @Override
    public Integer call() throws Exception {
        Optional<NetworkInterface> anInterface = parent.getNetworkInterface(networkInterface);
        anInterface.ifPresent(this::listDevices);

        return 0;
    }

    private void listDevices(NetworkInterface ni) {
        Styles styles = Styles.getInstance();

        var addresses = ni.getInterfaceAddresses();
        if (parent.ipo.ipv4() || parent.ipo.ipv46()) {
            System.out.println(ansi().a(System.lineSeparator()).apply(styles.section("IPv4 devices:")));
            addresses.stream().filter(address -> address.getAddress() instanceof Inet4Address)
                    .forEach(ia -> {
                LinkedList<String> reachable = new LinkedList<>();
                AtomicBoolean process = new AtomicBoolean(true);
                int deviceBitsLength = 32 - ia.getNetworkPrefixLength();
                long deviceCount = (1L << deviceBitsLength) - 1;
                if (deviceCount > 0xffff) {
                    System.out.println(ansi().apply(styles.info("Too many devices to scan, skipping")));
                    return;
                }
                try (ExecutorService executor = Executors.newWorkStealingPool(); ScheduledExecutorService scheduler =
                        Executors.newSingleThreadScheduledExecutor()) {
                    scheduler.scheduleAtFixedRate(() -> {
                        if (process.get()) {
                            printProcess();
                        }
                    }, 0, 200, TimeUnit.MILLISECONDS);
                    byte[] netAddress = getNetworkAddress(ia);
                    var futures = executor.invokeAll(createV4ReachabilityTest(ni, ia, netAddress));
                    futures.forEach(f -> {
                        try {
                            if (f.get(timeout, TimeUnit.MILLISECONDS) instanceof ConnectionResult(
                                    _, InetAddress address, boolean success, ConnectionException exception)) {
                                if (exception != null && showErrors) {
                                    reachable.add(ansi().apply(styles.ipAddress(exception.address())).a(": ")
                                            .apply(styles.warning(exception.getMessage())).toString());
                                    printReachable(reachable);
                                } else if (success) {
                                    reachable.add(ansi().apply(styles.hostName(address)).a("/")
                                            .apply(styles.ipAddress(address)).toString());
                                    printReachable(reachable);
                                }
                            }
                        } catch (InterruptedException e) {
                            System.out.println(ansi().apply(styles.error(e.getMessage())));
                        } catch (ExecutionException | TimeoutException e) {
                            //ignore
                        }
                    });
                } catch (InterruptedException e) {
                    System.out.println(ansi().apply(styles.error(e.getMessage())));
                } finally {
                    process.set(false);
                    System.out.println(ansi().cursorDownLine(reachable.size() + 1));
                }
            });
        }
    }

    private record ConnectionResult(int deviceId, InetAddress address, boolean success, ConnectionException exception) {}

    private ArrayList<Callable<ConnectionResult>> createV4ReachabilityTest(NetworkInterface ni, InterfaceAddress ia,
                                                                           byte[] netAddress) {
        int deviceBitCount = 32 - ia.getNetworkPrefixLength();
        final int broadcast = (1 << deviceBitCount) - 1;
        ArrayList<Callable<ConnectionResult>> reachable = new ArrayList<>();
        for (int device = 0; device < broadcast; device++) {
            final int deviceId = device;
            reachable.add(() -> {
                InetAddress deviceAddress = InetAddress.getByAddress(getDeviceIPv4Address(netAddress,
                        ia.getNetworkPrefixLength(), deviceId));
                try {
                    return new ConnectionResult(deviceId, deviceAddress, deviceAddress.isReachable(ni, 0, timeout), null);
                } catch (IOException e) {
                    return new ConnectionResult(deviceId, deviceAddress, false, new ConnectionException(
                            e.getMessage(), deviceAddress));
                }
            });
        }
        return reachable;
    }

    private int processState;
    private final char[] processStates = {'|', '/', '-', '\\'};

    private synchronized void printProcess() {
        System.out.print(ansi().a("\rSearching ").a(processStates[processState]).cursorToColumn(1));
        processState = (processState + 1) % processStates.length;
    }

    private synchronized void printReachable(LinkedList<String> reachable) {
        System.out.println();
        reachable.forEach(System.out::println);
        System.out.print(ansi().cursorUpLine(reachable.size() + 1));
    }

    private byte[] getNetworkAddress(InterfaceAddress interfaceAddress) {
        byte[] localAddress = interfaceAddress.getAddress().getAddress();
        ByteBuffer buffer = ByteBuffer.wrap(localAddress);
        if (buffer.capacity() == Integer.BYTES) {
            int mask = (int) (0xffffffffL << (Integer.SIZE - interfaceAddress.getNetworkPrefixLength()));
            buffer.putInt(buffer.getInt(0) & mask);
        } else {
            long maskMost = -1L << (Math.max(0, Long.SIZE - interfaceAddress.getNetworkPrefixLength()));
            long maskLeast = -1L << (2 * Long.SIZE - interfaceAddress.getNetworkPrefixLength());
            buffer.putLong(buffer.getLong(0) & maskMost);
            buffer.putLong(buffer.getLong(1) & maskLeast);
        }
        return buffer.array();
    }

    private byte[] getDeviceIPv4Address(byte[] networkAddress, int maskLength, int deviceId) {
        ByteBuffer buffer = ByteBuffer.allocate(networkAddress.length);
        buffer.put(networkAddress).rewind();
        int mask = (int) (0xffffffffL << (Integer.SIZE - maskLength));
        buffer.putInt(buffer.getInt(0) & mask | deviceId);
        return buffer.array();
    }
}
