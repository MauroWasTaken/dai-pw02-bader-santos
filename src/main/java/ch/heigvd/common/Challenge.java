package ch.heigvd.common;

import ch.heigvd.server.Server;

/**
 * Represents a challenge issued by a player to another player.
 *
 * <p>A Challenge contains a reference to the challenger (resolved through the server's player
 * registry), the status of the challenge (pending, accepted or refused) and a Game instance when
 * the challenge is accepted.
 */
public class Challenge {
  /** Player who issued the challenge (resolved from Server.players). */
  public volatile Player challenger;

  /** Current status of the challenge. */
  public volatile Status status;

  /** Assigned game when the challenge is accepted. */
  public volatile Game game;

  /**
   * Create a new Challenge for the provided challenger username. The challenger Player instance is
   * resolved using the server's player registry.
   *
   * @param challengerUsername username of the challenger
   */
  public Challenge(String challengerUsername) {
    challenger = Server.getPlayerByUsername(challengerUsername);
    this.status = Status.PENDING;
  }

  /** Challenge states. */
  public enum Status {
    PENDING,
    ACCEPTED,
    REFUSED
  }
}
