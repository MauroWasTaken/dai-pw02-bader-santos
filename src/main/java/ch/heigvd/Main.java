package ch.heigvd;

import ch.heigvd.commands.Root;
import java.io.File;
import picocli.CommandLine;

public class Main {

  /**
   * Main entry point of the application. This method forwards the provided arguments to the Picocli
   * command system.
   *
   * @param args an array of arguments passed to the application
   */
  public static void main(String[] args) {
    // Define command name - source: https://stackoverflow.com/a/11159435
    String jarFilename =
        new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath())
            .getName();

    // Create root command
    Root root = new Root();

    // Execute command and get exit code
    int exitCode =
        new CommandLine(root)
            .setCommandName(jarFilename)
            .setCaseInsensitiveEnumValuesAllowed(true)
            .execute(args);

    System.exit(exitCode);
  }
}
