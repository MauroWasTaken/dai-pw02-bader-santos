package ch.heigvd.Server.Functions;

import ch.heigvd.Common.Challenge;
import ch.heigvd.Common.Norms;
import ch.heigvd.Common.Player;
import ch.heigvd.Server.Server;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class containing all matchmaking-related functions. This class centralizes the logic that manages
 * interactions between players.
 */
public class Matchmaking {

  /**
   * Sends the list of pending challenges to the player.
   *
   * The message format is: CHALLENGES challengerName1;challengerName2;
   *
   * @param socket client socket
   * @param in input stream associated with the client
   * @param out output stream associated with the client
   * @param player player whose pending challenges must be retrieved
   */
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

  /**
   * Sends a challenge to a specific player.
   *
   * This function performs the following steps: - checks that the target player exists - ensures
   * the target player is not already in a game - adds a challenge to the target player's list -
   * waits for the target player to accept or refuse
   *
   * Error codes sent: ERROR 1 : player not found ERROR 2 : target player already in game
   *
   * @param socket sender's socket
   * @param in sender's input stream
   * @param out sender's output stream
   * @param challanger player sending the challenge
   * @param players list of all connected players
   * @param username username of the targeted player
   * @return true if the challenge is accepted and the game can start, false otherwise
   */
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
        out.write(Server.Message.ERROR + " " + 1 + Norms.END_OF_LINE);
        out.flush();
        return false;
      }

      Challenge challenge = new Challenge(challanger.username);
      challenged.challenges.add(challenge);

      // Wait until the target player accepts or refuses
      while (challenge.status == Challenge.Status.PENDING) {
        Thread.sleep(100);
      }

      if (challenge.status == Challenge.Status.ACCEPTED) {
        challanger.status = Player.Status.IN_GAME;
        out.write(Server.Message.GAMESTART + Norms.END_OF_LINE);
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

  /**
   * Accepts a challenge sent by another player.
   *
   * This method: - finds the pending challenge corresponding to the given username - sets its
   * status to ACCEPTED - sets the player status to IN_GAME
   *
   * @param socket socket of the player accepting the challenge
   * @param in input stream
   * @param out output stream
   * @param player player accepting the challenge
   * @param username challenger username
   * @return true if the challenge was accepted, false otherwise
   */
  public static boolean acceptChallenge(
      Socket socket, BufferedReader in, BufferedWriter out, Player player, String username) {

    try {
      for (Challenge challenge : player.challenges) {
        if (challenge.challenger.username.equals(username)
            && challenge.status == Challenge.Status.PENDING) {

          challenge.status = Challenge.Status.ACCEPTED;
          player.status = Player.Status.IN_GAME;
          player.challenges.clear();

          out.write(Server.Message.GAMESTART + Norms.END_OF_LINE);
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

  /**
   * Refuses a challenge sent by another player.
   *
   * This method: - locates the challenge sent by the specified username - sets its status to
   * REFUSED - removes the challenge from the player's list - notifies the client
   *
   * @param socket socket of the player refusing the challenge
   * @param in input stream
   * @param out output stream
   * @param player player refusing the challenge
   * @param username challenger username
   */
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
