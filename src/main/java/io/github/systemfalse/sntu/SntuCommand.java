package io.github.systemfalse.sntu;

import picocli.CommandLine;

import java.io.PrintStream;
import java.net.*;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.fusesource.jansi.Ansi.ansi;

@CommandLine.Command(
        name = "sntu",
        header = "Simple Network Test Utility%n",
        version = "SNTU 0.0.1",
        subcommands = {
                GetIpCommand.class
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
