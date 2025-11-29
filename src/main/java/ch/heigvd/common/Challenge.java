package ch.heigvd.common;

public class Challenge {
  public volatile Player challenger;
  public volatile Status status;
  public volatile Game game;

  public Challenge(String challengerUsername) {
    this.challenger = new Player(challengerUsername);
    this.status = Status.PENDING;
  }

  public enum Status {
    PENDING,
    ACCEPTED,
    REFUSED
  }
}
