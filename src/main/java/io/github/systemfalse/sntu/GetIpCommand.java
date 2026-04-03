package io.github.systemfalse.sntu;

import io.github.systemfalse.sntu.util.Styles;
import picocli.CommandLine;

import java.net.NetworkInterface;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.ansi;

@CommandLine.Command(
        name = "get-ip",
        description = "Get your IP address",
        optionListHeading = "Options:%n",
        sortOptions = false,
        sortSynopsis = false,
        footer = {
                "@|yellow If interactive mode enabled|@, program will ask to select network interface, ip version @|bold only if not specified|@"
        }
)
public class GetIpCommand implements Callable<Integer> {
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
        Styles styles = Styles.getInstance();

        Optional<NetworkInterface> anInterface = parent.getNetworkInterface(networkInterface);
        if (anInterface.isEmpty()) {
            return 0;
        }
        System.out.println(ansi().apply(styles.section("IPs:")));
        anInterface.get().inetAddresses()
                .filter(parent.addressFilter())
                .forEach(ia -> System.out.println(ansi().apply(styles.ipAddress(ia))));

        return 0;
    }
}
