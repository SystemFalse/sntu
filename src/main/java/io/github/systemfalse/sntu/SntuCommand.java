package io.github.systemfalse.sntu;

import io.github.systemfalse.sntu.util.Styles;
import picocli.CommandLine;

import java.io.PrintStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.fusesource.jansi.Ansi.ansi;

@CommandLine.Command(
        name = "sntu",
        header = "Simple Network Test Utility%n",
        version = "SNTU 0.0.1",
        subcommands = {
                GetIpCommand.class,
                NetInfoCommand.class,
                DeviceListCommand.class
        },
        footer = {
                "Use \"sntu -?, -h, or --help\" for this help message",
                "Use \"sntu COMMAND_NAME --help\" for usage of COMMAND_NAME"
        }
)
public class SntuCommand implements Callable<Integer> {
    @CommandLine.Option(
            names = {"-h", "-?", "--help"},
            usageHelp = true,
            hidden = true
    )
    private boolean help;

    @CommandLine.Option(
            names = {"-V", "--version"},
            versionHelp = true,
            hidden = true
    )
    private boolean version;

    @CommandLine.Option(
            names = "--list-interfaces",
            description = "List all network interfaces and exit"
    )
    private boolean listInterfaces;

    @CommandLine.ArgGroup
    IPOption ipo = new IPOption();

    @CommandLine.Option(
            names = {"-i", "--interactive"},
            description = "Enable interactive mode",
            defaultValue = "false"
    )
    boolean interactive;

    @CommandLine.Option(
            names = "-1",
            description = "Use default interface"
    )
    boolean useDefaultInterface;

    @Override
    public Integer call() throws Exception {
        PrintStream out = Main.OUTPUT.orElse(System.out);

        if (listInterfaces) {
            listInterfaces().forEach(ni -> out.println(ansi().render("%s (@|yellow,bold %s|@)", ni.getDisplayName(), ni.getName())));
        }
        return 0;
    }

    public Stream<NetworkInterface> listInterfaces() throws SocketException {
        return NetworkInterface.networkInterfaces()
                .filter(ni -> ni.inetAddresses().anyMatch(addressFilter()));
    }

    public Predicate<InetAddress> addressFilter() {
        return ia -> ipo.ipv4() && ia instanceof Inet4Address ||
                ipo.ipv6() && ia instanceof Inet6Address || ipo.ipv46();
    }

    public Optional<NetworkInterface> getNetworkInterface(NetworkInterface value)
            throws SocketException {
        Styles styles = Styles.getInstance();
        if (value == null && interactive && !useDefaultInterface) {
            return askInterface();
        } else if (value == null && useDefaultInterface) {
            return listInterfaces().findFirst();
        } else if (value == null) {
            System.out.println(ansi().apply(styles.error("Network interface not specified")));
            return Optional.empty();
        } else {
            return Optional.of(value);
        }
    }

    public Optional<NetworkInterface> askInterface() throws SocketException {
        Styles styles = Styles.getInstance();
        List<NetworkInterface> interfaces = new ArrayList<>();
        listInterfaces().forEach(interfaces::add);
        for (int i = 0; i < interfaces.size(); i++) {
            NetworkInterface ni = interfaces.get(i);
            System.out.println(ansi().a(i + 1).a(": ").apply(styles.interfaceDisplayName(ni)).a(" (")
                    .apply(styles.interfaceName(ni)).a(')'));
        }
        System.out.printf("0: Cancel%n");
        System.out.print(ansi().apply(styles.userPrompt("Select network interface: ")));
        Scanner sc = new Scanner(System.in, System.getProperty("stdin.encoding"));
        int choice = -1;
        do {
            try {
                choice = sc.nextInt();
            } catch (InputMismatchException _) {
                System.out.println(ansi().apply(styles.error("Invalid input")));
                continue;
            }
            if (choice < 0 || choice >= interfaces.size()) {
                System.out.println(ansi().apply(styles.error("Unsupported option")));
            }
        } while (choice < 0 || choice >= interfaces.size());
        if (choice == 0) {
            return Optional.empty();
        }
        return Optional.of(interfaces.get(choice - 1));
    }

    private static CommandLine.Help.ColorScheme colorScheme() {
        return new CommandLine.Help.ColorScheme.Builder()
                .commands(CommandLine.Help.Ansi.Style.bold, CommandLine.Help.Ansi.Style.fg_green)
                .options(CommandLine.Help.Ansi.Style.fg_yellow)
                .parameters(CommandLine.Help.Ansi.Style.italic, CommandLine.Help.Ansi.Style.fg_cyan)
                .optionParams(CommandLine.Help.Ansi.Style.underline)
                .errors(CommandLine.Help.Ansi.Style.bold, CommandLine.Help.Ansi.Style.fg_red)
                .stackTraces(CommandLine.Help.Ansi.Style.italic)
                .applySystemProperties()
                .build();
    }

    public static CommandLine createCommandLine() {
        CommandLine cl = new CommandLine(new SntuCommand());

        cl.setUsageHelpAutoWidth(true);
        cl.setCaseInsensitiveEnumValuesAllowed(true);
        cl.setColorScheme(colorScheme());

        return cl;
    }
}
