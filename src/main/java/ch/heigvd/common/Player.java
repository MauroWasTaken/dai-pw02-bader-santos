package ch.heigvd.common;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a player connected to the server.
 *
 * <p>The Player object stores a username, its current status, a list of incoming challenges and
 * simple statistics tracked by the server.
 */
public class Player {
  public String username;
  public Status status;
  public CopyOnWriteArrayList<Challenge> challenges;
  public int wins, losses, draws, winStreak;

  /** Create a player with zeroed statistics. */
  public Player(String username) {
    this(username, 0, 0, 0, 0);
  }

  /**
   * Create a player with explicit statistics values (used by the client when parsing server list).
   *
   * @param username username identifier
   * @param wins number of wins
   * @param losses number of losses
   * @param draws number of draws
   * @param winStreak current winning streak
   */
  public Player(String username, int wins, int losses, int draws, int winStreak) {
    this.username = username;
    this.wins = wins;
    this.losses = losses;
    this.draws = draws;
    this.winStreak = winStreak;
    this.status = Status.ONLINE;
    this.challenges = new CopyOnWriteArrayList<>();
  }

  /** Return a comma-separated representation of the player used by the server protocol. */
  public String toString() {
    return username
        + Norms.ELEMENT_SEPARATOR
        + wins
        + Norms.ELEMENT_SEPARATOR
        + losses
        + Norms.ELEMENT_SEPARATOR
        + draws
        + Norms.ELEMENT_SEPARATOR
        + winStreak;
  }

  public enum Status {
    ONLINE,
    IN_GAME
  }
}
