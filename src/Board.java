import java.util.*;
/** Class: Board.java
 *  @author Yury Park
 *
 *  This Class - The Board class. Represents a 15-puzzle game board (For reference, see https://en.wikipedia.org/wiki/15_puzzle).
 *  Theoretically, this board can be of an arbitrary size N x N, but due to memory constraints, this class assumes
 *  a 3 x 3 or 4 x 4 board.
 */
public class Board {
	private char[] grid;		//N x N board expressed in terms of a 1-D char array (instead of 2-D int[][] array) to save memory.
	private int N;				//The dimension N of the N x N board
	private Board parentBoard;	//Parent of this Board object. Used for more efficient manhattan() method.
	private int hamming, manhattan;	//hamming and manhattan distance, respectively.
	private short indexOfEmptySpot;	//The empty space in this NxN puzzle board, denoted as block no. 0. Used short instead of int to save space.

	/**
	 * 1-arg constructor.
	 * @param blocks
	 */
	public Board(int[][] blocks) {          // construct a board from an N-by-N int[][] array of blocks

		N = blocks.length;
		grid = new char[N * N];	//to save space, we'll use a 1-D char[] array to represent the 2-D grid.

		int index = 0;
		for (int i = 0; i < blocks.length; i++) {
			for (int j = 0; j < blocks[i].length; j++) {
				grid[index] = (char)blocks[i][j];
				if (grid[index] == 0) indexOfEmptySpot = (short)index;	//Remember the index position of the empty space (block no. 0)
				index++;
			}
		}
		init();	//custom method
	}

	/**
	 * 1-arg constructor. Constructs a Board from a 1-D char[] array. Used primarily by the twin() method.
	 * @param grid
	 */
	private Board(char[] grid) {

		N = (int)Math.sqrt(grid.length);	//Since the given grid is 1-D, we must take sqrt of the length to get the dimension.
		this.grid = new char[N * N];

		for (int i = 0; i < this.grid.length; i++) {
			this.grid[i] = grid[i];
			if (this.grid[i] == 0) indexOfEmptySpot = (short)i;	//Remember the index position of the empty space (block no. 0)
		}
		init();	//custom method
	}

	/**
	 * Method: init
	 *         Initialize some data fields
	 */
	private void init() {
		parentBoard = null;
		hamming = -1;
		manhattan = -1;
	}

	/**
	 * Method: dimension
	 * @return the dimension of this board
	 */
	public int dimension() {                // board dimension N x N
		return N;
	}

	/**
	 * Method: hamming
	 * @return the number of blocks (NOTE: block no. 0 -- AKA the empty space -- doesn't count)
	 *         that are not in their correct place on the board.
	 */
	public int hamming() {                  // number of blocks out of place
		if (hamming >= 0) return hamming;	//Base case. If we previously computed this value, then just return the variable.

		int count = 0;	//initialize the return value

		/* Iterate thru each block */
		for (int i = 0; i < N * N; i++) {
			/* All numbers must be in their respective index position.
			 * So index position 0 should contain block no. 1,
			 * index 1 should contain block no. 2, and so on.
			 * We skip over block no. 0 (because, again, it's not an actual block but an empty space) */
			int block = grid[i];
			if (block != 0 && block != correctBlock(i)) count++;	//correctBlock() is a custom method
		}
		return count;
	}

	/**
	 * Method: correctBlock
	 * @param index given index of the board's grid.
	 * @return the correct block that should be in this index position.
	 *         For example, index 0 should contain block 1.
	 */
	private int correctBlock (int index) {
		if (index == grid.length - 1) return 0;	//The only exception is that block 0 should be in the last index position.
		return index + 1; //As for all other blocks, each index position should contain the block no. that's greater than that position by one.
	}

	/**
	 * Method: manhattan
	 * @return sum of all Manhattan distances between each block and their goal.
	 */
	public int manhattan() {
		if (manhattan >= 0) return manhattan;	//base case. If we already computed this previously, then just return the variable

		/* Another base case. If this board has a parent, then we can use the parent's pre-computed manhattan() distance
		 * to more efficiently compute this manhattan distance. So invoke custom method. */
		if (this.parentBoard != null) return this.calcManhattanEfficient();

		/* End of base cases. Begin computation. */
		int sum = 0;	//Initialize value to be returned

		/* Iterate thru the grid. */
		for (int i = 0; i < N*N; i++) {
			int currBlock = grid[i];
			if (currBlock == 0)
				continue;		//0 is not a block, so continue on to the next iteration

			//where SHOULD the current block be located? the location index should be 1 less than the block number.
			int goalIndex = currBlock - 1;
			sum += getManhattanDistance(i, goalIndex);	//custom method to get the Manhattan distance from current index i to goalindex.
		}
		//end for i
		return sum;
	}

	/**
	 * Method: getManhattanDistance. Helper method invoked by manhattan().
	 * @param currIndex current index where the block is located.
	 * @param goalIndex the index where the block SHOULD be located.
	 * @return the manhattan distance between currIndex and goalIndex.
	 */
	private int getManhattanDistance(int currIndex, int goalIndex) {
		//Base case. return 0 if currIndex and goalIndex are the same.
		if (currIndex == goalIndex) return 0;

		int distance = 0;		//initialize the distance to return.
		int smallerIndex, largerIndex;		//Figure out which index is smaller than the other
		if (currIndex < goalIndex) {
			smallerIndex = currIndex;
			largerIndex = goalIndex;
		}
		else {
			smallerIndex = goalIndex;
			largerIndex = currIndex;
		}

		//Now figure out whether the two indices are in the same row and/or column
		/* For example, for the board below:
		 * 4 0 3
		 * 2 1 8
		 * 5 7 6
		 *
		 * block no. 4 is in row 1 and block no. 2 is in row 2. Block 2 is taking up block 4's goal spot.
		 * So in this case let's say the currIndex = 0, and goalIndex = 3.
		 * If their distance is a multiple of 3, AKA a multiple of the size of each row/column,
		 * then we know they're in the same column.
		 *  */
		int rowOfLargerIndex = getRow(largerIndex);	//getRow is a custom method
		int rowOfSmallerIndex = getRow(smallerIndex);
		if (rowOfSmallerIndex == rowOfLargerIndex) {	//If this is true, they're in the same row
			distance = largerIndex - smallerIndex;		//So just count the column distance between them
		}
		else if ((largerIndex - smallerIndex) % N == 0) {	//If this is true, they're in the same column
			while (smallerIndex < largerIndex) {			//Count the row distance between them
				smallerIndex += N;			//Remember, we're working with a 1-D array (for memory efficiency), so we increment by N
				distance++;					//And we increment manhattan distance by 1
			}
			//end while
		}
		else {	//Else, they're in neither the same row nor column.
			int differenceInRow = rowOfLargerIndex - rowOfSmallerIndex;	//First get their manhattan distance just in terms of rows
			smallerIndex += differenceInRow * N;	//get the smaller index into the same row as the larger index
			distance = differenceInRow + Math.abs(largerIndex - smallerIndex);	//row difference + column difference = total manhattan distance
		}
		//end if/else

		return distance;
	}

	/**
	 * Method: getRow
	 * @param i the index position of a block
	 * @return the row that the block is in. Returns 1 (not 0) for first row, etc.
	 */
	private int getRow(int i) {
		return (i / N) + 1;
	}

	/**
	 * Method: isGoal
	 * @return true if this board, in its current state, has been solved, false otherwise.
	 */
	public boolean isGoal() {
		return (this.hamming() == 0);
	}

	/**
	 * Method: twin
	 * @return a Board object that is obtained by exchanging any two adjacent blocks in the same row.
	 */
	public Board twin() {
		//Let's choose 2 adjacent blocks in a row.
		//We want to switch 2 adjacent blocks. Let's choose a "safe" row -- one that does NOT have the empty block (block 0).
		int rowThatHasEmptyBlock = getRow(indexOfEmptySpot);	//This row has the empty block. We don't want this row.
		int safeRow = rowThatHasEmptyBlock + 1;					//Let's choose the next row
		if (safeRow > N) safeRow = rowThatHasEmptyBlock - 1;	//In case the above row is outside the Board's dimension, we'll choose the previous row.

		//choose the first nonzero block in the chosen row
		int indexOf1stBlock = (safeRow - 1) * N;
		//choose the second nonzero block in row.
		int indexOf2ndBlock = (safeRow - 1) * N + 1;

		char[] twinGrid = new char[grid.length];
		System.arraycopy(grid, 0, twinGrid, 0, grid.length);
		exchange(twinGrid, indexOf1stBlock, indexOf2ndBlock);	//exchange is a custom method
		return new Board(twinGrid);
	}

	/**
	 * Method: exchange
	 * @param grid The board's grid on which to perform the exchange
	 * @param i the index position to exchange with j
	 * @param j the index position to exchange with i
	 */
	private void exchange (char[] grid, int i, int j) {
		char temp = grid[i];
		grid[i] = grid[j];
		grid[j] = temp;
	}

	/**
	 * Method: equals
	 */
	@Override
	public boolean equals(Object o) {       // does this board equal o?
		if (this == o) return true;	//if they're strictly the same, return true.
		if (o == null || !(o instanceof Board)) return false;	//check some other base cases
//		if (o == null || !o.getClass().equals(this.getClass())) return false;	//ALT way to check base case

		Board b = (Board)o;	//Once we get thru the base cases above, we can safely cast the Object

		if (this.N != b.N) return false;	//Dimensions must be the same. Otherwise return false

		for (int i = 0; i < grid.length; i++) {
			if (grid[i] != b.grid[i]) return false;	//Every element in the grid must be the same
		}

		return true;
	}

	/**
	 * Method: neighbors
	 * @return a list consisting of a combination of possible board positions after a single move.
	 *         NOTE: disallow duplicate of the previous position. See below comment.
	 */
	public Iterable<Board> neighbors() {
		Stack<Board> neighborsST = new Stack<Board>();
		/*
 		 *	8  1  3       8  1  3       8  1       8  1  3     8  1  3
 		 *	4     2       4  2          4  2  3    4     2     4  2  5
 		 *	7  6  5       7  6  5       7  6  5    7  6  5     7  6
 		 *  previous      current       neighbor   neighbor    neighbor
 		 *  position      position                 (disallow,
 		 *                                          duplicate
 		 *                                          of prev.
 		 *                                          position)
 		 **/
		int e = this.indexOfEmptySpot;

		//See if there's a block to the north of the empty spot
		if (e - N >= 0) {
			Board neighbor = createNeighbor(e, e - N);	//createNeighbor is a custom method that automatically disallows duplicate of previous position.
			if (neighbor != null) neighborsST.push(neighbor);
		}

		//See if there's a block to the south of the empty spot
		if (e + N < this.grid.length) {
			Board neighbor = createNeighbor(e, e + N);
			if (neighbor != null) neighborsST.push(neighbor);
		}

		//See if there's a block to the left of the empty spot
		if (e % N != 0) {
			Board neighbor = createNeighbor(e, e - 1);
			if (neighbor != null) neighborsST.push(neighbor);
		}
		//See if there's a block to the right of the empty spot
		if ((e + 1) % N != 0) {
			Board neighbor = createNeighbor(e, e + 1);
			if (neighbor != null) neighborsST.push(neighbor);
		}
		return neighborsST;
	}

	/**
	 * Method: createNeighbor
	 * @param e the index of the empty spot
	 * @param swapIndex index of the block with which to swap the empty spot
	 * @return the Board object created
	 */
	private Board createNeighbor(int e, int swapIndex) {
		char[] neighborGrid = new char[grid.length];
		System.arraycopy(this.grid, 0, neighborGrid, 0, grid.length);	//Make copy of this grid.
		exchange(neighborGrid, e, swapIndex);							//Use custom method to make the swap
		Board neighbor = new Board(neighborGrid);						//Create the Board object
		if (neighbor.equals(this.parentBoard)) 							//Disallow duplicate of previous board
			return null;
		neighbor.parentBoard = this;									//Set the parent of this neighboring board
		return neighbor;
	}

	/**
	 * Method: calcManhattanEfficient
	 *         Exploit the fact that the difference in Manhattan distance between a parent board and this board
	 *         (AKA the parent's neighbor) is at most 1, based on the direction that the block moves.
	 *         So this method is more efficient than the manhattan() method, provided that this board has a parent.
	 * @return the manhattan distance of the current Board, assuming that it has a parent (neighboring) board.
	 */
	private int calcManhattanEfficient() {
		int ret = parentBoard.manhattan();
		int i = this.indexOfEmptySpot;				//index of block before it moved to a neighboring position
		int j = this.parentBoard.indexOfEmptySpot;	//index of block after moving to a neighboring position

		int blockThatMoved = this.grid[j];			//Get the label no. of the block that actually moved
		int goalIndex = blockThatMoved - 1;			//Get the index position of the correct place the block SHOULD be at.
		                                            //(We can assume that the block that moved is not 0)

		/* Use custom method to get the manhattan distance of this block from its goal index position for the parent
		 * as well as for this board. */
		int parentBoardBlocksManHattanDistance = this.getManhattanDistance(i, goalIndex);
		int currBoardBlocksManhattanDistance = this.getManhattanDistance(j, goalIndex);

		/* This difference should be at most 1 (or -1) */
		int diff = currBoardBlocksManhattanDistance - parentBoardBlocksManHattanDistance;

		this.manhattan = ret + diff;		//save this value to variable before returning
		return manhattan;
	}

	/**
	 * Method: toString
	 */
	public String toString() {              // string representation of this board (in the output format specified below)
		StringBuilder s = new StringBuilder();
	    s.append(N);					//The dimension of this N x N board.
	    for (int i = 0; i < N * N; i++) {
	    	if(i % N == 0) s.append("\n");
	    	s.append(String.format("%2d ", (int)grid[i]));
	    }
	    return s.toString();
	}

	/**
	 * Method: main
	 * @param args
	 */
	public static void main(String[] args) {

		//Testing. Not required.
//		int[][] grid = {{2,1,3},
//				        {4,0,8},
//				        {7,6,5}};
//		Board b = new Board(grid);
//		System.out.printf( "Board:\n%s\n"
//				         + "Dimension:\t\t%s\n"
//				         + "Hamming distance:\t%s\n"
//				         + "Manhattan distance:\t%s\n\n", b, b.dimension(), b.hamming(), b.manhattan());
//
//		for (Board neighbor : b.neighbors()) {
//			System.out.println("Neighbor of original board:");
//			System.out.println(neighbor);
//			System.out.println("Neighbor's neighbors (excluding duplicates of original board): ");
//			for (Board neighborsNeighbor : neighbor.neighbors()) {
//				System.out.println(neighborsNeighbor);
//				System.out.println("Manhattan distance: " + neighborsNeighbor.manhattan());
//			}
//		}
//
//		Board twin = b.twin();
//		System.out.printf( "Twin Board:\n%s\n"
//		         + "Dimension:\t\t%s\n"
//		         + "Hamming distance:\t%s\n"
//		         + "Manhattan distance:\t%s\n\n", twin, twin.dimension(), twin.hamming(), twin.manhattan());
	}

}