package io.github.systemfalse.sntu;

import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;

public class Main {
    static void main(String[] args) {
        AnsiConsole.systemInstall();

        CommandLine cl = SntuCommand.createCommandLine();

        int exitCode = cl.execute(args);

        AnsiConsole.systemUninstall();

        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}