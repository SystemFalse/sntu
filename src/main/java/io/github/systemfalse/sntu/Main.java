package io.github.systemfalse.sntu;

import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;

import java.io.PrintStream;

public class Main {
    public static final ScopedValue<PrintStream> OUTPUT = ScopedValue.newInstance();
    public static final ScopedValue<PrintStream> ERROR = ScopedValue.newInstance();

    static void main(String[] args) {
        AnsiConsole.systemInstall();

        CommandLine cl = SntuCommand.createCommandLine();

        int exitCode = ScopedValue.where(OUTPUT, AnsiConsole.out())
                .where(ERROR, AnsiConsole.err())
                .call(() -> cl.execute(args));

        AnsiConsole.systemUninstall();
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}