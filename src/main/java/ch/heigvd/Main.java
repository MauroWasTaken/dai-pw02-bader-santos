package ch.heigvd;

import ch.heigvd.commands.Root;
import java.io.File;
import picocli.CommandLine;

/**
 * Entry point launcher for the application.
 *
 * <p>This class sets up Picocli and forwards command line arguments to the configured root command.
 * The command name is set to the running JAR filename so help messages show a useful program name.
 */
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
