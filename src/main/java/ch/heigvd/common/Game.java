package ch.heigvd.common;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Game {
  public String[] board = new String[9];
  public Player player1;
  public Player player2;
  public AtomicBoolean isPlayer1Turn = new AtomicBoolean(true);
  public AtomicBoolean isOver = new AtomicBoolean(false);
  public Player winner = null;
  public final String player1Symbol = "X";
  public final String player2Symbol = "O";
  public AtomicInteger lastMove = new AtomicInteger(-1);
  public AtomicBoolean hasDisconnect = new AtomicBoolean(false);

  public Game() {
    for (int i = 0; i < 9; i++) {
      board[i] = " ";
    }
  }

  public Game(Player player1, Player player2) {
    this.player1 = player1;
    this.player2 = player2;
    this.isPlayer1Turn.set(Math.random() < 0.5);
    for (int i = 0; i < 9; i++) {
      board[i] = " ";
    }
  }

  public boolean makeMove(int position, String username) {
    Player player = username.equals(player1.username) ? player1 : player2;
    if (isOver.get() || position < 0 || position > 8 || !board[position].equals(" ")) {
      return false;
    }
    if ((isPlayer1Turn.get() && player.equals(player1))
        || (!isPlayer1Turn.get() && player.equals(player2))) {
      board[position] = isPlayer1Turn.get() ? player1Symbol : player2Symbol;
      checkWin();
      lastMove.set(position);
      isPlayer1Turn.set(!isPlayer1Turn.get());
      return true;
    }
    return false;
  }

  private void checkWin() {
    String[][] winConditions = {
      {board[0], board[1], board[2]},
      {board[3], board[4], board[5]},
      {board[6], board[7], board[8]},
      {board[0], board[3], board[6]},
      {board[1], board[4], board[7]},
      {board[2], board[5], board[8]},
      {board[0], board[4], board[8]},
      {board[2], board[4], board[6]}
    };
    for (String[] condition : winConditions) {
      if (condition[0].equals(player1Symbol)
          && condition[1].equals(player1Symbol)
          && condition[2].equals(player1Symbol)) {
        isOver.set(true);
        winner = player1;
        return;
      }
      if (condition[0].equals(player2Symbol)
          && condition[1].equals(player2Symbol)
          && condition[2].equals(player2Symbol)) {
        isOver.set(true);
        winner = player2;
        return;
      }
    }
    if (isBoardFull()) {
      isOver.set(true);
    }
  }

  private boolean isBoardFull() {
    for (String cell : board) {
      if (cell.equals(" ")) {
        return false;
      }
    }
    return true;
  }
}
