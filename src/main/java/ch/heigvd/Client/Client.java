package ch.heigvd.Client;

import static ch.heigvd.Client.Functions.Login.login;

import ch.heigvd.Server.Server;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(name = "client", description = "Start the client part of the network game.")
public class Client implements Callable<Integer> {

  public enum Message {
    GUESS,
    RESTART,
    HELP,
    QUIT,
    LOGIN,
    PLAYERS,
    CHALLENGE,
    ACCEPT,
    REFUSE,
    PLAY
  }

  // End of line character
  public static String END_OF_LINE = "\n";

  @CommandLine.Option(
      names = {"-H", "--host"},
      description = "Host to connect to.",
      required = true)
  private String host;

  @CommandLine.Option(
      names = {"-p", "--port"},
      description = "Port to use (default: ${DEFAULT-VALUE}).",
      defaultValue = "42069")
  protected int port;

  @Override
  public Integer call() {
    try (Socket socket = new Socket(host, port);
        Reader reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
        BufferedReader in = new BufferedReader(reader);
        Writer writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
        BufferedWriter out = new BufferedWriter(writer)) {
      System.out.println("[Client] Connected to " + host + ":" + port);
      // checking connection
      String serverResponse = in.readLine(); // stuck here
      Server.Message message = Server.Message.valueOf(serverResponse.split(" ")[0]);
      if (message != Server.Message.OK) {
        System.out.println("[Client] Server is full");
        socket.close();
        return 1;
      }
      // login
      String username = login(socket, in, out);

      if (username == null) {
        System.out.println("[Client] Closing connection and quitting...");
        return 1;
      }

    } catch (Exception e) {
      System.out.println("[Client] Exception: " + e);
      return 1;
    }
    return 0;
  }

  private static void help() {
    System.out.println("Usage:");
    System.out.println("  " + Message.GUESS + " <number> - Submit the number you want to guess.");
    System.out.println("  " + Message.RESTART + " - Restart the game.");
    System.out.println("  " + Message.QUIT + " - Close the connection to the server.");
    System.out.println("  " + Message.HELP + " - Display this help message.");
  }
}
