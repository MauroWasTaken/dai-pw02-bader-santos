package ch.heigvd.commands;

import ch.heigvd.client.Client;
import ch.heigvd.server.Server;
import picocli.CommandLine;

@CommandLine.Command(
    description = "A small game to experiment with TCP.",
    version = "1.0.0",
    subcommands = {
      Client.class,
      Server.class,
    },
    scope = CommandLine.ScopeType.INHERIT,
    mixinStandardHelpOptions = true)
public class Root {}
