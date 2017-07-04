import java.util.*;

/** Class: Solver.java
 *  @author Yury Park
 *
 * This is an AI solver for the 15-puzzle game (For reference, see https://en.wikipedia.org/wiki/15_puzzle).
 * Given a text file representing a shuffled game board, this solver figures out whether or not a solution is possible,
 * and if so, efficiently computes the solution requiring the fewest number of moves, and outputs each move to the console.
 */
public class Solver {
	private final boolean debugOn = false;
	private boolean isSolvable;		//Is a given puzzle solvable or not?
	private Node initialBoard;		//The given puzzle
	private Node initialBoardTwin;	//The given puzzle board with two of its blocks swapped. Either the orig. or the "twin" will be solvable, not both.
	private int totalNumOfMovesForSolution;	//self-explanatory
	private Iterable<Board> solutionST;		//Solution key

	/**
	 * Inner class. A private wrapper class for the Board.java object.
	 * @author Yury Park
	 */
	private class Node implements Comparable<Node> {
		private Board board;
		private int distanceSoFar;		//The no. of moves made so far to get to this board
		private int estTotalCost;		//The A* heuristic using Manhattan distance.
		private Node parent;			//Parent Node. Used to construct solution path.
		private boolean isTwin;			//Whether this board is a "twin" of the original or a descendant of a twin board.

		/**
		 * 4-arg constructor.
		 * @param b 			Given board
		 * @param distanceSoFar The moves made so far to get to this board.
		 * @param parent		This board's parent board.
		 * @param isTwin		Whether this board is a twin or a descendant of a twin.
		 */
		Node(Board b, int distanceSoFar, Node parent, boolean isTwin) {
			this.board = b;
			this.distanceSoFar = distanceSoFar;
			this.parent = parent;
			this.isTwin = isTwin;
		}

		/**
		 * Method: compareTo
		 * @param b2 the other Node to compare to
		 */
		@Override
		public int compareTo(Node b2) {
			if (estTotalCost < b2.estTotalCost) return -1;
			if (estTotalCost > b2.estTotalCost) return 1;
			if (board.manhattan() < b2.board.manhattan()) return -1;	//manhattan distance is used as tiebreaker
			if (board.manhattan() > b2.board.manhattan()) return 1;
			return 0;
		}
	}
	//end private class Node implements Comparable<Node>

	/**
	 * 1-arg constructor.
	 * @param initial Given puzzle board.
	 */
	public Solver(Board initial) {
		this.isSolvable = false;										//Initialize this as false
		this.initialBoard = new Node(initial, 0, null, false);			//Initialize wrapper class
		this.initialBoardTwin = new Node(initial.twin(), 0, null, true);//Initialize wrapper class for twin board
		this.solutionST = null;											//Initialize solution path
		this.totalNumOfMovesForSolution = -1;							//Initialize as -1, indicating that it's unsolvable

		if (debugOn) {
			System.out.println("Starting board:");
			System.out.println(initial);
		}

		//MinPQ is a custom PriorityQueue class. Will always remove the "minimum" Node.
		MinPQ<Node> pq = new MinPQ<Node>();
		pq.insert(initialBoard);
		pq.insert(initialBoardTwin);
		solve(pq);		//custom method
	}
	//end public Solver

	/**
	 * Method: solve
	 *         Finds an optimal solution for the given puzzle board, by using the A-Star (A*, AStar) algorithm.
	 * @param pq the given PriorityQueue, containing Nodes, the wrapper class for Board objects.
	 */
	private void solve(MinPQ<Node> pq) {
		Node current = pq.delMin();		//Pop out the "minimum" node.

		/* Keep going until we found the solution */
		while (!current.board.isGoal()) {
			Iterable<Board> neighbors = current.board.neighbors();	//custom method in Board class. Get all of the board's "neighbors."

			/* Go thru each neighboring Board object */
			for (Board neighbor : neighbors) {
				/* Create wrapper class for each Board object. Be sure to update the distanceSoFar attribute in the
				 * parameter as given below. */
				Node neighborNode = new Node(neighbor, current.distanceSoFar + 1, current, current.isTwin);

				/* Update the A* distance heuristic. manhattan() is a custom method in Board class. */
				neighborNode.estTotalCost = neighborNode.distanceSoFar + neighborNode.board.manhattan();

				//We will ignore a neighbor if it equals the previous board state. This optimization is crucial for performance.
				if (current.parent == null || !neighborNode.board.equals(current.parent.board)) pq.insert(neighborNode);
			}
			//end for
			current = pq.delMin();
		}
		//end while

		/* If the twin board found a solution, then the ORIGINAL board is not solvable. */
		if (current.isTwin) {
			this.isSolvable = false;
			this.totalNumOfMovesForSolution = -1;
			this.solutionST = null;
		}
		else {
			this.isSolvable = true;
			this.totalNumOfMovesForSolution = current.estTotalCost;

			/* Construct a solution path by starting from the solution Board all the way down to the original board. */
			Stack<Board> solutionInReverseST = new Stack<Board>();
			while (!current.equals(this.initialBoard)) {
				solutionInReverseST.push(current.board);
				current = current.parent;
			}
			solutionInReverseST.push(current.board);	//Finally, push in the original board to the solution path.

			/* Now pop the Boards back out in reverse order from the Stack, and save it to the solution path. */
			LinkedList<Board> solutionQ = new LinkedList<Board>();
			while (!solutionInReverseST.isEmpty()) {
				solutionQ.add(solutionInReverseST.pop());
			}

			this.solutionST = solutionQ;	//Saved to solution path.
		}
		//end if
	}
	//end private void solve

	/**
	 * Method: isSolvable
	 * @return whether the initial board is solvable
	 */
	public boolean isSolvable() {           // is the initial board solvable?
		return this.isSolvable;
	}

	/**
	 * Method: moves
	 * @return the min. number of moves to solve initial board; -1 if unsolvable.
	 */
	public int moves() {                    // min number of moves to solve initial board; -1 if unsolvable
		return totalNumOfMovesForSolution;
	}

	/**
	 * Method: solution
	 * @return the sequence of boards in a shortest solution; null if unsolvable.
	 */
	public Iterable<Board> solution() {      // sequence of boards in a shortest solution; null if unsolvable
		return this.solutionST;
	}

	/**
	 * Method: main
	 * @param args
	 */
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();	//Optional: for time testing

		// Create initial board from file
		// End user will input something like: puzzle50.txt
		In in = new In(args[0]);
		int N = in.readInt();
		int[][] blocks = new int[N][N];
		for (int i = 0; i < N; i++)
			for (int j = 0; j < N; j++)
				blocks[i][j] = in.readInt();
		Board initial = new Board(blocks);

		// solve the puzzle
		Solver solver = new Solver(initial);

		// print solution to standard output
		if (!solver.isSolvable())
			StdOut.println("No solution possible");
		else {
			StdOut.println("Minimum number of moves = " + solver.moves());
			for (Board board : solver.solution())
				System.out.println(board + "\n");
		}
		System.out.println("Elapsed time (in milliseconds): " + (System.currentTimeMillis() - startTime));
	}
	//end main
}
