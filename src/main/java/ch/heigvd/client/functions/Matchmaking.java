package ch.heigvd.client.functions;

import ch.heigvd.client.Client;
import ch.heigvd.common.Norms;
import ch.heigvd.common.Player;
import ch.heigvd.server.Server;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.Socket;
import java.util.ArrayList;

public class Matchmaking {

  public static ArrayList<Player> getChallenges(
      Socket socket, BufferedReader in, BufferedWriter out) {
    if (!socket.isClosed()) {
      try {
        out.write(Client.Message.CHALLENGES + Norms.END_OF_LINE);
        out.flush();
        String serverResponse = in.readLine();
        String[] serverResponseParts = serverResponse.split(" ", 2);
        Server.Message message = Server.Message.valueOf(serverResponseParts[0]);
        if (message != Server.Message.CHALLENGES) {
          return new ArrayList<>();
        }
        String[] challengersUsernames = serverResponseParts[1].split(Norms.OBJECT_SEPARATOR);
        ArrayList<Player> challengers = new ArrayList<>();
        for (String username : challengersUsernames) {
          if (!username.isEmpty()) {
            challengers.add(new Player(username));
          }
        }
        return challengers;
      } catch (Exception e) {
        return new ArrayList<>();
      }
    }
    return new ArrayList<>();
  }

  public static boolean challengePlayer(
      Socket socket,
      BufferedReader in,
      BufferedWriter out,
      BufferedReader consoleReader,
      String username) {
    if (!socket.isClosed()) {
      try {
        // sending challenge request to server
        System.out.print("Enter the username of the player you want to challenge: ");
        String challengedUsername = consoleReader.readLine().split(" ")[0];

        if (challengedUsername.equals(username)) {
          Client.message += " (╯°□°）╯︵ ┻━┻   — Hold up! You cannot challenge yourself.";
          return false;
        }

        out.write(Client.Message.CHALLENGE + " " + challengedUsername + Norms.END_OF_LINE);
        out.flush();

        // wait for server response
        System.out.println("Challenge sent to " + challengedUsername + ", waiting for response...");

        // read server response
        String serverResponse = in.readLine();
        Server.Message message = Server.Message.valueOf(serverResponse.split(" ")[0]);
        if (message == Server.Message.GAMESTART) {
          Client.myTurn = Integer.parseInt(serverResponse.split(" ")[1]) == 1;
          return true;
        } else if (message == Server.Message.ERROR) {
          switch (Integer.parseInt(serverResponse.split(" ")[1])) {
            case 1 -> Client.message += "Player " + challengedUsername + " does not exist.";
            case 2 -> Client.message += "Player " + challengedUsername + " is not available.";
          }
        } else if (message == Server.Message.REFUSE) {
          Client.message += challengedUsername + " refused your challenge.";
        }
        return false;
      } catch (Exception e) {
        System.out.println("Something went wrong while sending challenge to server.");
      }
    }
    return false;
  }

  public static boolean acceptChallenge(
      Socket socket,
      BufferedReader in,
      BufferedWriter out,
      BufferedReader consoleReader,
      ArrayList<Player> challengers) {
    if (!socket.isClosed()) {
      try {
        System.out.print("Enter the username of the player you want to challenge: ");
        String challengerUsername = consoleReader.readLine().split(" ")[0];
        if (challengerUsername.isEmpty()) {
          Client.message = "No username entered.";
          return false;
        }
        if (challengers.stream().noneMatch(p -> p.username.equals(challengerUsername))) {
          Client.message = "No challenge from " + challengerUsername + " found.";
          return false;
        }
        for (Player challenger : challengers) {
          if (!challenger.username.equals(challengerUsername)) {
            refuseChallenge(socket, in, out, challenger.username, challengers);
          }
        }
        out.write(Client.Message.ACCEPT + " " + challengerUsername + Norms.END_OF_LINE);
        out.flush();
        String serverResponse = in.readLine();
        Server.Message message = Server.Message.valueOf(serverResponse.split(" ")[0]);
        if (message == Server.Message.GAMESTART) {
          Client.myTurn = Integer.parseInt(serverResponse.split(" ")[1]) == 1;
          return true;
        } else if (message == Server.Message.ERROR) {
          Client.message = "Error while accepting challenge.";
        }
      } catch (Exception e) {
        Client.message = "Something went wrong while responding to challenge.";
      }
    }
    return false;
  }

  public static void refuseChallenge(
      Socket socket,
      BufferedReader in,
      BufferedWriter out,
      BufferedReader consoleReader,
      ArrayList<Player> challengers) {
    try {
      System.out.print("Enter the username of the player you want to challenge: ");
      String challengerUsername = consoleReader.readLine().split(" ")[0];
      if (challengerUsername.isEmpty()) {
        Client.message = "No username entered.";
        return;
      }
      if (challengers.stream().noneMatch(p -> p.username.equals(challengerUsername))) {
        Client.message = "No challenge from " + challengerUsername + " found.";
        return;
      }
      refuseChallenge(socket, in, out, challengerUsername, challengers);
    } catch (Exception e) {
      System.out.println("Something went wrong while responding to challenge.");
    }
  }

  public static void refuseChallenge(
      Socket socket,
      BufferedReader in,
      BufferedWriter out,
      String challengerUsername,
      ArrayList<Player> challengers) {
    if (!socket.isClosed()) {
      try {
        out.write(Client.Message.REFUSE + " " + challengerUsername + Norms.END_OF_LINE);
        out.flush();
        challengers.removeIf(p -> p.username.equals(challengerUsername));
      } catch (Exception e) {
        System.out.println("Something went wrong while responding to challenge.");
      }
    }
  }
}
