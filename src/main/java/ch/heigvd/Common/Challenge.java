package ch.heigvd.Common;

public class Challenge {
  public volatile Player challenger;
  public volatile Status status;

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
