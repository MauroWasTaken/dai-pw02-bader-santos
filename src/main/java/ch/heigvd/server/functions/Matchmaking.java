package ch.heigvd.server.functions;

import ch.heigvd.common.Challenge;
import ch.heigvd.common.Game;
import ch.heigvd.common.Norms;
import ch.heigvd.common.Player;
import ch.heigvd.server.Server;
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
   * <p>The message format is: CHALLENGES challengerName1;challengerName2;
   *
   * @param socket client socket
   * @param in input stream associated with the client
   * @param out output stream associated with the client
   * @param player player whose pending challenges must be retrieved
   */
  public static void getChallenges(BufferedWriter out, Player player) {
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
   * <p>This function performs the following steps: - checks that the target player exists - ensures
   * the target player is not already in a game - adds a challenge to the target player's list -
   * waits for the target player to accept or refuse
   *
   * <p>Error codes sent: ERROR 1 : player not found ERROR 2 : target player already in game
   *
   * @param socket sender's socket
   * @param in sender's input stream
   * @param out sender's output stream
   * @param challanger player sending the challenge
   * @param players list of all connected players
   * @param username username of the targeted player
   * @return true if the challenge is accepted and the game can start, false otherwise
   */
  public static Game challengePlayer(
      Socket socket,
      BufferedWriter out,
      Player challenger,
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
            return null;
          }
        }
      }
      if (!found) {
        String response = Server.Message.ERROR + " " + 1 + Norms.END_OF_LINE;
        out.write(response);
        out.flush();
        return null;
      }
      Challenge challenge = new Challenge(challenger.username);
      challenged.challenges.add(challenge);
      while (challenge.status == Challenge.Status.PENDING) {
        Thread.sleep(100);
      }
      if (challenge.status == Challenge.Status.ACCEPTED) {
        challenger.status = Player.Status.IN_GAME;
        int firstPlayer = challenge.game.isPlayer1Turn.get() ? 2 : 1;
        out.write(Server.Message.GAMESTART + " " + firstPlayer + Norms.END_OF_LINE);
        out.flush();
        return challenge.game;
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
    return null;
  }

  /**
   * Accepts a challenge sent by another player.
   *
   * <p>This method: - finds the pending challenge corresponding to the given username - sets its
   * status to ACCEPTED - sets the player status to IN_GAME
   *
   * @param socket socket of the player accepting the challenge
   * @param in input stream
   * @param out output stream
   * @param player player accepting the challenge
   * @param username challenger username
   * @return true if the challenge was accepted, false otherwise
   */
  public static Game acceptChallenge(
      Socket socket, BufferedReader in, BufferedWriter out, Player player, String username) {
    try {
      for (Challenge challenge : player.challenges) {
        if (challenge.challenger.username.equals(username)
            && challenge.status == Challenge.Status.PENDING) {
          challenge.game = new ch.heigvd.common.Game(player, challenge.challenger);
          challenge.status = Challenge.Status.ACCEPTED;
          player.status = Player.Status.IN_GAME;
          player.challenges.clear();
          int firstPlayer = challenge.game.isPlayer1Turn.get() ? 1 : 2;
          out.write(Server.Message.GAMESTART + " " + firstPlayer + Norms.END_OF_LINE);
          out.flush();
          return challenge.game;
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
    return null;
  }

  /**
   * Refuses a challenge sent by another player.
   *
   * <p>This method: - locates the challenge sent by the specified username - sets its status to
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

  public static void sendListPlayers(
      Socket socket, BufferedWriter out, CopyOnWriteArrayList<Player> players) {
    try {
      for (Player p : players) {
        out.write(p.toString());
        out.write(Norms.ELEMENT_SEPARATOR);
      }
      out.write(Norms.END_OF_LINE);
      out.flush();
    } catch (Exception e) {
      System.out.println("[Server] Exception while sending list player: " + e);
      try {
        socket.close();
      } catch (Exception ee) {
        System.out.println("[Server] Exception while closing socket: " + ee);
      }
    }
  }
}
