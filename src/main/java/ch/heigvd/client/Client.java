package ch.heigvd.client;

import static ch.heigvd.client.functions.GameFunctions.gameloop;
import static ch.heigvd.client.functions.Login.login;
import static ch.heigvd.client.functions.Matchmaking.*;
import static ch.heigvd.client.functions.UI.*;

import ch.heigvd.common.Game;
import ch.heigvd.common.Norms;
import ch.heigvd.common.Player;
import ch.heigvd.server.Server;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(name = "client", description = "Start the client part of the network game.")
public class Client implements Callable<Integer> {
  public static ArrayList<Player> players = new ArrayList<>();
  public static ArrayList<Player> challenges = new ArrayList<>();
  public static boolean inGame = false;
  public static boolean myTurn = false;
  public static Game game = null;
  public static String message = "";

  public enum Message {
    RESTART,
    HELP,
    QUIT,
    LOGIN,
    PLAYERS_LIST,
    CHALLENGES,
    CHALLENGE,
    ACCEPT,
    REFUSE,
    PLAY
  }

  public static final String[] lobbyOptions = {
    "PLAYERS_LIST", "CHALLENGE", "ACCEPT", "REFUSE", "REFRESH", "HELP", "QUIT"
  };

  @CommandLine.Option(
      names = {"-H", "--host"},
      description = "Host to connect to.",
      defaultValue = "localhost")
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
        BufferedWriter out = new BufferedWriter(writer);
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
      System.out.println("[Client] Connected to " + host + ":" + port);
      // checking connection
      String serverResponse = in.readLine();
      Server.Message message = Server.Message.valueOf(serverResponse.split(" ")[0]);
      if (message != Server.Message.OK) {
        System.out.println("[Client] Server is full");
        socket.close();
        return 1;
      }
      // login
      final String username = login(socket, in, out);
      if (username == null) {
        return 1;
      }

      while (!socket.isClosed()) {
        while (!inGame) {
          fetchData(socket, in, out);
          drawLobby(username);
          String input = consoleReader.readLine();
          if (input == null) {
            break;
          }
          switch (input.split(" ")[0].toUpperCase()) {
            case "PLAYERS_LIST":
              listAllPlayer(socket, in, out, consoleReader);
              break;
            case "CHALLENGE":
              inGame = challengePlayer(socket, in, out, consoleReader, username);
              break;
            case "ACCEPT":
              inGame = acceptChallenge(socket, in, out, consoleReader, challenges);
              break;
            case "REFUSE":
              refuseChallenge(socket, in, out, consoleReader, challenges);
              break;
            case "":
            case "REFRESH":
              break;
            case "QUIT":
              out.write(Client.Message.QUIT + " " + Norms.END_OF_LINE);
              out.flush();
              System.out.println("Quitting...");
              socket.close();
              in.close();
              out.close();
              consoleReader.close();
              return 0;
            default:
              help();
              break;
          }
        }
        // Game loop
        if (inGame) {
          gameloop(socket, in, out, consoleReader, username);
          inGame = false;
        }
      }

    } catch (Exception e) {
      System.out.println("[Client] Exception: " + e);
      return 1;
    }
    return 0;
  }

  public static void fetchData(Socket socket, BufferedReader in, BufferedWriter out) {
    Client.challenges.clear();
    Client.challenges.addAll(getChallenges(socket, in, out));
  }
}
