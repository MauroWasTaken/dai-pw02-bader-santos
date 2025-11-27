package ch.heigvd.Common;

import java.util.ArrayList;

public class Player {
  public String username;
  public Status status;
  public ArrayList<Challenge> challenges;
  public int wins, losses, draws, winStreak;

  public Player(String username) {
    this(username, 0, 0, 0, 0);
  }

  public Player(String username, int wins, int losses, int draws, int winStreak) {
    this.username = username;
    this.wins = wins;
    this.losses = losses;
    this.draws = draws;
    this.winStreak = winStreak;
    this.status = Status.ONLINE;
    this.challenges = new ArrayList<>();
  }

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
