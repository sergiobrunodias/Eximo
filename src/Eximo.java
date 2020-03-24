import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Eximo {
	private int gamemode;
	private Board board;
	private int currentPlayer;
	private UI gui;
	private int moveState = Constants.NORMAL;
	private int piecesToPlace = 0;
	
	public Eximo(int gamemode, UI gui) {
		this.gamemode = gamemode;
		this.board = new Board();
		currentPlayer = Constants.PLAYER_1;
		this.gui = gui;
	}
	
	public List<Move> findValidMoves() {
		List<Move> validMoves = new ArrayList<Move>();
		
		for(int i = 0; i < board.length(); i++) {
			validMoves.addAll(generateMoves(i));
		} 
		
		return validMoves;
	}
	
	/* Generates the valid jump over moves from a given position */ 
	public List<Move> generateJumpOverMoves(int startPos) {
		List<Move> jumpOverMoves = new ArrayList<Move>();
		if (board.getCell(startPos) != currentPlayer) return jumpOverMoves;
		for (int direction : Constants.FrontDirections) {
			Move jumpOver = checkJumpOver(startPos, direction);
			if(jumpOver != null) jumpOverMoves.add(jumpOver);
		}
		return jumpOverMoves;
	}
	
	public List<Move> generateAllCaptureMoves() {
		List<Move> captureMoves = new ArrayList<Move>();
		
		for(int i = 0; i < board.length(); i++) {
			captureMoves.addAll(generateCaptureMoves(i));
		} 
		
		return captureMoves;
	}
	
	/* Generates the valid capture moves from a given position */ 
	public List<Move> generateCaptureMoves(int startPos) {
		List<Move> captureMoves = new ArrayList<Move>();
		if (board.getCell(startPos) != currentPlayer) return captureMoves;
		for (int direction : Constants.FrontDirections) {
			Move capture = checkCaptureFront(startPos, direction);
			if(capture != null) captureMoves.add(capture);
		}
		Move captureWest = checkCaptureSide(startPos, Constants.WEST);
		if(captureWest != null) captureMoves.add(captureWest);
		Move captureEast = checkCaptureSide(startPos, Constants.EAST);
		if(captureEast != null) captureMoves.add(captureEast);
		
		return captureMoves;
	}
	
	/* Generates all the possible basic moves from a given position in the board */
	public List<Move> generateMoves(int startPos) {
		List<Move> moves = new ArrayList<Move>();
		if (board.getCell(startPos) != currentPlayer) return moves;
		
		for (int direction : Constants.FrontDirections) {
			Move simpleMove = checkSimpleMove(startPos, direction);
			if(simpleMove != null) moves.add(simpleMove);
			Move jumpOver = checkJumpOver(startPos, direction);
			if(jumpOver != null) moves.add(jumpOver);
		}
		return moves;
	}
	
	/* Generating and checking a possible basic movement in a given direction */
	public Move checkSimpleMove(int startPos, int direction) {
		int sign = Utils.getSign(currentPlayer);
		int endPos = startPos + sign * (Constants.LINE_LENGTH + direction);
		Move move = new Move(startPos, endPos);
		if(move.checkBoundaries() && board.getCell(endPos) == Constants.EMPTY_CELL) {
			return move;
		}
		return null;
	}
	
	/* Generating and checking a possible jump over movement in a given direction */
	public Move checkJumpOver(int startPos, int direction) {
		int sign = Utils.getSign(currentPlayer);
		int endPos = startPos + sign * 2 * (Constants.LINE_LENGTH + direction);
		int midPos = (startPos + endPos) / 2;
		Move move = new Move(startPos, endPos);
		if(move.checkBoundaries() && board.getCell(endPos) == Constants.EMPTY_CELL && board.getCell(midPos) == currentPlayer) {
			return move;
		}
		return null;
	}
	
	/* Generating and checking a possible capture movement in a given direction (only sides) */
	public Move checkCaptureSide(int startPos, int direction) {
		int sign = Utils.getSign(currentPlayer);
		int endPos = startPos + sign * 2 * direction;
		Move move = new Move(startPos, endPos);
		if(move.setCaptured() && move.checkBoundaries() && board.getCell(endPos) == Constants.EMPTY_CELL && board.getCell(move.captured) == Utils.otherPlayer(currentPlayer)) {
			return move;
		}
		return null;
	}
	
	/* Generating and checking a possible capture movement in a given direction (only in front) */
	public Move checkCaptureFront(int startPos, int direction) {
		int sign = Utils.getSign(currentPlayer);
		int endPos = startPos + sign * 2 * (Constants.LINE_LENGTH + direction);
		Move move = new Move(startPos, endPos);
		if(move.setCaptured() && move.checkBoundaries() && board.getCell(endPos) == Constants.EMPTY_CELL && board.getCell(move.captured) == Utils.otherPlayer(currentPlayer)) {
			return move;
		}
		return null;
	}
	
	public void nextPlayer() {
		currentPlayer = Utils.otherPlayer(currentPlayer);
	}
	
	public void emptyCell(int position) {
		board.setCell(position, Constants.EMPTY_CELL);
		gui.getBoard().setIconAt(position, Constants.EMPTY_CELL);
	}
	
	public void fillCell(int position) {
		board.setCell(position, currentPlayer);
		gui.getBoard().setIconAt(position, currentPlayer);
	}
	
	public int getCurrentPlayer() {
		return currentPlayer;
	}
	
	public void playerMove(Move move) {
		int startP = move.startPos.toBoardPos();
		int endP = move.endPos.toBoardPos();
		if (board.getCell((endP+startP)/2) == Utils.otherPlayer(currentPlayer))
			move.setCaptured();
		List<Move> captureMoves = generateAllCaptureMoves();
		if (captureMoves.size() != 0) {
			if (!captureMoves.contains(move)) {
				return;
			}
		} else if (!generateMoves(startP).contains(move)) {
			return;
		}
		emptyCell(startP);
		fillCell(endP);
		if(move.isCapture()) { // we're handling a capture move
			emptyCell(move.captured);
			if (generateCaptureMoves(endP).size() != 0) {
				moveState = Constants.CAPTURE;
				return;
			}
		}
		else if (move.isJumpOver()) {
			if (generateJumpOverMoves(endP).size() != 0) {
				moveState = Constants.JUMP_OVER;
				return;
			}
		}
		reachedEndOfBoard(endP);
		if(gameOver()) return;
		System.out.println("Player turn: " + currentPlayer);
		if(gamemode == Constants.PLAYER_VS_BOT) {
			botMove();
		}
	}
	
	public void reachedEndOfBoard(int endPos) {
		if ((currentPlayer == Constants.PLAYER_1 && endPos/Constants.LINE_LENGTH == 7) 
				|| (currentPlayer == Constants.PLAYER_2 && endPos/Constants.LINE_LENGTH == 0)) {
			switch(board.countSafeZoneFreeCells(currentPlayer)) {
				case 0:
					nextPlayer();
					break;
				case 1:
					piecesToPlace = 1;
					break;
				default:
					System.out.println("You earned yourself 2 new pieces!");
					piecesToPlace = 2;
					break;
			}
			emptyCell(endPos);
		}
		else nextPlayer();
			
	}
	
	public void sequentialJumpOver(Move move) {
		int startP = move.startPos.toBoardPos();
		int endP = move.endPos.toBoardPos();
		if (!generateJumpOverMoves(startP).contains(move)) {
			return;
		}
		emptyCell(startP);
		fillCell(endP);
		if (generateJumpOverMoves(endP).size() == 0) {
			moveState = Constants.NORMAL;
			nextPlayer();
		}
	}
	
	public void sequentialCapture(Move move) {
		int startP = move.startPos.toBoardPos();
		int endP = move.endPos.toBoardPos();
		move.setCaptured();
		if (!generateCaptureMoves(startP).contains(move)) {
			return;
		}
		emptyCell(startP);
		fillCell(endP);
		emptyCell(move.captured);
		if (generateCaptureMoves(endP).size() == 0) {
			moveState = Constants.NORMAL;
			nextPlayer();
		}
	}
	
	public void botMove() {
		List<Move> possibleMoves = findValidMoves();
		Random ran = new Random();
		int x = ran.nextInt(possibleMoves.size());
		
		Move randomMove = possibleMoves.get(x); // chooses move
		
		int startP = randomMove.startPos.toBoardPos();
		int endP = randomMove.endPos.toBoardPos();
		
		//if (cells[(endP+startP)/2] == Utils.otherPlayer(currentPlayer))
			//move.checkCapture();
		emptyCell(startP);
		fillCell(endP);
		if(randomMove.isCapture()) { // we're handling a capture move
			emptyCell(randomMove.captured);
		}
		else if (randomMove.isJumpOver()) {
			if (generateJumpOverMoves(endP).size() != 0) {
				moveState = Constants.NORMAL;
				return;
			}
		}
		nextPlayer();
		System.out.println("Player turn: " + currentPlayer);
	}

	public int getMoveState() {
		return moveState;
	}

	public int getPiecesToPlace() {
		return piecesToPlace;
	}

	public boolean gameOver() {
		if(generateAllCaptureMoves().isEmpty() && findValidMoves().isEmpty()) {
			// handles game over
			System.out.println("Game over! Player " + Utils.otherPlayer(currentPlayer) + " wins!");
			return true;
		}
		return false;
	}

	public void addPieceAt(int position) {
		if(Utils.isWithinSafeZone(currentPlayer, position) && board.getCell(position) == Constants.EMPTY_CELL) {
			fillCell(position);
			piecesToPlace--;
		}
		if (piecesToPlace == 0) nextPlayer();
	}
	
	
	
}
