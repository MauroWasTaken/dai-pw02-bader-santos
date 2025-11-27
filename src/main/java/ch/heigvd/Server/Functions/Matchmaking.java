package ch.heigvd.Server.Functions;

import ch.heigvd.Common.Challenge;
import ch.heigvd.Common.Norms;
import ch.heigvd.Common.Player;
import ch.heigvd.Server.Server;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class Matchmaking {
  public static void getChallenges(
      Socket socket, BufferedReader in, BufferedWriter out, Player player) {
    try {
      StringBuilder sb = new StringBuilder(Server.Message.CHALLENGES + " ");
      for (Challenge challenge : player.challenges) {
        if (challenge.status == Challenge.Status.PENDING) {
          sb.append(challenge.challenger.username).append(Norms.OBJECT_SEPARATOR);
        }
      }
      out.write(sb.toString() + Norms.END_OF_LINE);
      out.flush();
    } catch (Exception e) {
      System.out.println("[Server] Exception while fetching challenges: " + e);
    }
  }

  public static boolean challengePlayer(
      Socket socket,
      BufferedReader in,
      BufferedWriter out,
      Player challanger,
      CopyOnWriteArrayList<Player> players,
      String username) {
    try {
      boolean found = false;
      Player challenged = null;
      for (Player player : players) {
        if (player.username.equals(username)) {
          found = true;
          challenged = player;
          if (player.status == Player.Status.IN_GAME) {
            out.write(Server.Message.ERROR + " " + 2 + Norms.END_OF_LINE);
            out.flush();
            return false;
          }
        }
      }
      if (!found) {
        String response = Server.Message.ERROR + " " + 1 + Norms.END_OF_LINE;
        out.write(response);
        out.flush();
        return false;
      }
      Challenge challenge = new Challenge(challanger.username);
      challenged.challenges.add(challenge);
      while (challenge.status == Challenge.Status.PENDING) {
        Thread.sleep(100);
      }
      if (challenge.status == Challenge.Status.ACCEPTED) {
        challanger.status = Player.Status.IN_GAME;
        out.write(Server.Message.GAMESTART + Norms.END_OF_LINE); // todo ajouter qui va en premier
        out.flush();
        return true;
      }
      out.write(Server.Message.REFUSE + Norms.END_OF_LINE);
      out.flush();
    } catch (Exception e) {
      System.out.println("[Server] Exception while challenging player: " + e);
      try {
        socket.close();
      } catch (Exception ee) {
        System.out.println("[Server] Exception while closing socket: " + ee);
      }
    }
    return false;
  }

  public static boolean acceptChallenge(
      Socket socket, BufferedReader in, BufferedWriter out, Player player, String username) {
    try {
      for (Challenge challenge : player.challenges) {
        if (challenge.challenger.username.equals(username)
            && challenge.status == Challenge.Status.PENDING) {
          challenge.status = Challenge.Status.ACCEPTED;
          player.status = Player.Status.IN_GAME;
          player.challenges.clear();
          out.write(Server.Message.GAMESTART + Norms.END_OF_LINE); // todo ajouter qui va en premier
          out.flush();
          return true;
        }
      }
    } catch (Exception e) {
      System.out.println("[Server] Exception while accepting challenge: " + e);
      try {
        socket.close();
      } catch (Exception ee) {
        System.out.println("[Server] Exception while closing socket: " + ee);
      }
    }
    return false;
  }

  public static void refuseChallenge(
      Socket socket, BufferedReader in, BufferedWriter out, Player player, String username) {
    try {
      for (Challenge challenge : player.challenges) {
        if (challenge.challenger.username.equals(username)
            && challenge.status == Challenge.Status.PENDING) {
          challenge.status = Challenge.Status.REFUSED;
          out.write(Server.Message.REFUSE + Norms.END_OF_LINE);
          out.flush();
          player.challenges.remove(challenge);
          return;
        }
      }
    } catch (Exception e) {
      System.out.println("[Server] Exception while refusing challenge: " + e);
      try {
        socket.close();
      } catch (Exception ee) {
        System.out.println("[Server] Exception while closing socket: " + ee);
      }
    }
  }
}
