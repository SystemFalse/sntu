package io.github.systemfalse.sntu;

import picocli.CommandLine;

import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;

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
        PrintStream out = Main.OUTPUT.orElse(System.out);
        PrintStream err = Main.ERROR.orElse(System.err);

        NetworkInterface anInterface;
        if (networkInterface == null && parent.interactive) {
            out.println("Select network interface:");
            List<NetworkInterface> interfaces = new ArrayList<>();
            parent.listInterfaces().forEach(interfaces::add);
            for (int i = 0; i < interfaces.size(); i++) {
                NetworkInterface ni = interfaces.get(i);
                out.printf("%d: %s (%s)%n", i + 1, ni.getDisplayName(), ni.getName());
            }
            out.printf("%n0: Cancel%n");
            Scanner sc = new Scanner(System.in, System.getProperty("stdin.encoding"));
            int choice = -1;
            do {
                try {
                    choice = sc.nextInt();
                } catch (InputMismatchException _) {
                    err.println("Invalid input");
                }
                if (choice < 0 || choice >= interfaces.size()) {
                    err.println("Unsupported option");
                }
            } while (choice < 0 || choice >= interfaces.size());
            if (choice == 0) {
                return 0;
            }
            anInterface = interfaces.get(choice - 1);
        } else if (networkInterface == null) {
            err.println("Network interface not specified");
            return 3;
        } else {
            anInterface = networkInterface;
        }
        out.printf("IPs:%n");
        anInterface.inetAddresses()
                .filter(parent.addressFilter())
                .map(InetAddress::getHostAddress)
                .forEach(out::println);

        return 0;
    }
}
