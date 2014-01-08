package cpsr.environment.simulation.domains;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cpsr.environment.components.Action;
import cpsr.environment.components.Observation;
import cpsr.environment.simulation.ASimulator;
import cpsr.planning.PSRPlanningExperiment;

public class PacMan extends ASimulator {
	
	protected static final char[][] INIT_GAME_MAP = {
		{'x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x'},
		{'x',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ','x'},
		{'x',' ','x','x',' ','x','x','x',' ','x',' ','x','x','x',' ','x','x',' ','x'},
		{'x','o',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ','o','x'},
		{'x',' ','x','x',' ','x',' ','x','x','x','x','x',' ','x',' ','x','x',' ','x'},
		{'x',' ',' ',' ',' ','x',' ',' ',' ','x',' ',' ',' ','x',' ',' ',' ',' ','x'},
		{'x','x','x','x',' ','x','x','x',' ','x',' ','x','x','x',' ','x','x','x','x'},
		{'x','x','x','x',' ','x',' ',' ',' ',' ',' ',' ',' ','x',' ','x','x','x','x'},
		{'x','x','x','x',' ','x',' ','x',' ',' ',' ','x',' ','x',' ','x','x','x','x'},
		{'<',' ',' ',' ',' ','x',' ','x',' ',' ',' ','x',' ','x',' ',' ',' ',' ','>'},
		{'x','x','x','x',' ','x',' ','x','x','x','x','x',' ','x',' ','x','x','x','x'},
		{'x','x','x','x',' ','x',' ',' ',' ',' ',' ',' ',' ','x',' ','x','x','x','x'},
		{'x','x','x','x',' ','x',' ','x','x','x','x','x',' ','x',' ','x','x','x','x'},
		{'x',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ','x'},
		{'x',' ','x','x',' ','x','x','x',' ','x',' ','x','x','x',' ','x','x',' ','x'},
		{'x','o',' ','x',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ','x',' ','o','x'},
		{'x','x',' ','x',' ','x',' ','x','x','x','x','x',' ','x',' ','x',' ','x','x'},
		{'x',' ',' ',' ',' ','x',' ',' ',' ','x',' ',' ',' ','x',' ',' ',' ',' ','x'},
		{'x',' ','x','x','x','x','x','x',' ','x',' ','x','x','x','x','x','x',' ','x'},
		{'x',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ','x'},
		{'x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x'},
	};
	protected static final int[][] INIT_GHOST_POSES = {{8,9},{8,10},{9,9},{9,10}};
	protected static final int[] INIT_PACMAN_POS ={13,9};
	protected static final int NORTH=0, EAST=1, WEST=2, SOUTH=3;
	protected static final double CHASE_PROB = 0.75, DEFENSIVE_SLIP=0.25;
	protected static final int NUM_ACTS = 4;
	protected static final int NUM_OBS = (int)Math.pow(2,16);
	
	protected boolean inTerminalState;
	protected double currImmediateReward;
	protected int foodLeft;
	protected int powerPillCounter;

	protected char[][] gameMap;
	protected int[][] ghostPoses;
	protected int[] pacManPos, ghostDirs;
	
	public static void main(String args[])
	{
		PacMan pacman = new PacMan(10);
		PSRPlanningExperiment experiment = new PSRPlanningExperiment(args[0], args[1],pacman);
		experiment.runExperiment();
		experiment.publishResults(args[2]);
	}

	public PacMan(long seed)
	{
		super(seed);
	}
	
	public PacMan(long seed, int maxRunLength)
	{
		super(seed, maxRunLength);
	}
	
	@Override
	public String getName()
	{
		return "PacMan";
	}
	
	@Override
	protected Observation getCurrentObservation()
	{
		boolean[] lObsInfo = new boolean[16];
		int lYPos = pacManPos[0];
		int lXPos = pacManPos[1];

		/*check for walls*/

		//north
		if(gameMap[lYPos-1][lXPos] == 'x')
		{
			lObsInfo[0] = true;
		}
		//east
		if(gameMap[lYPos][lXPos+1] == 'x')
		{
			lObsInfo[1] = true;
		}
		//south
		if(gameMap[lYPos+1][lXPos] == 'x')
		{
			lObsInfo[2] = true;
		}
		//west
		if(gameMap[lYPos][lXPos-1] == 'x')
		{
			lObsInfo[3] = true;
		}

		/*check for food smell*/
		int lFoodManhattanDis = computeFoodManhattanDist();

		if(lFoodManhattanDis <= 2)
		{
			lObsInfo[4] = true;
		}
		else if(lFoodManhattanDis <= 3)
		{
			lObsInfo[5] = true;
		}
		else if(lFoodManhattanDis <= 4)
		{
			lObsInfo[6] = true;
		}

		/*Check see ghosts or food*/
		//check north
		int[] tempLoc = pacManPos.clone();
		tempLoc[0]--;
		while(tempLoc[0] > 0 && tempLoc[1] < gameMap[0].length
				&& gameMap[tempLoc[0]][tempLoc[1]] != 'x')
		{

			for(int lGhost = 0; lGhost < ghostPoses.length; lGhost++)
			{
				if(tempLoc[0] == ghostPoses[lGhost][0] && tempLoc[1] == ghostPoses[lGhost][1])
				{
					lObsInfo[11] = true;
					break;
				}
			}
			if(lObsInfo[11] == true)
				break;
			if(gameMap[tempLoc[0]][tempLoc[1]] == '.')
			{
				lObsInfo[7] = true;
			}
			tempLoc[0]--;

		}

		//check east
		tempLoc = pacManPos.clone();
		tempLoc[1]++;
		while(tempLoc[1] < gameMap[0].length -1
				&& gameMap[tempLoc[0]][tempLoc[1]] != 'x')
		{
			for(int lGhost = 0; lGhost < ghostPoses.length; lGhost++)
			{
				if(tempLoc[0] == ghostPoses[lGhost][0] && tempLoc[1] == ghostPoses[lGhost][1])
				{
					lObsInfo[12] = true;
					break;
				}
			}
			if(lObsInfo[12] == true)
				break;
			if(gameMap[tempLoc[0]][tempLoc[1]] == '.')
			{
				lObsInfo[8] = true;
			}
			tempLoc[1]++;

		}

		//check south
		tempLoc = pacManPos.clone();

		tempLoc[0]++;
		while(tempLoc[0] < gameMap.length - 1
				&& gameMap[tempLoc[0]][tempLoc[1]] != 'x')		
		{
			for(int lGhost = 0; lGhost < ghostPoses.length; lGhost++)
			{
				if(tempLoc[0] == ghostPoses[lGhost][0] && tempLoc[1] == ghostPoses[lGhost][1])
				{
					lObsInfo[13] = true;
					break;
				}
			}
			if(lObsInfo[13] == true)
				break;
			if(gameMap[tempLoc[0]][tempLoc[1]] == '.')
			{
				lObsInfo[9] = true;
			}
			tempLoc[0]++;
		}

		//check west
		tempLoc = pacManPos.clone();
		tempLoc[1]--;
		while( tempLoc[1] > 0
				&& gameMap[tempLoc[0]][tempLoc[1]] != 'x')		
		{
			for(int lGhost = 0; lGhost < ghostPoses.length; lGhost++)
			{
				if(tempLoc[0] == ghostPoses[lGhost][0] && tempLoc[1] == ghostPoses[lGhost][1])
				{
					lObsInfo[14] = true;
					break;
				}
			}
			if(lObsInfo[14] == true)
				break;
			if(gameMap[tempLoc[0]][tempLoc[1]] == '.')
			{
				lObsInfo[10] = true;
			}
			tempLoc[1]--;
		}

		lObsInfo[15] = powerPillCounter >= 0;

		return new Observation(computeIntFromBinary(lObsInfo));
	}

	@Override
	protected void initRun()
	{
		gameMap = new char[INIT_GAME_MAP.length][INIT_GAME_MAP[0].length];
		for(int i = 0; i < INIT_GAME_MAP.length; i++)
			for(int j = 0; j < INIT_GAME_MAP[0].length; j++)
				gameMap[i][j] = INIT_GAME_MAP[i][j];
		
		ghostPoses = new int[INIT_GHOST_POSES.length][INIT_GHOST_POSES[0].length];
		for(int i = 0; i < INIT_GHOST_POSES.length; i++)
			for(int j = 0; j < INIT_GHOST_POSES[0].length; j++)
				ghostPoses[i][j] = INIT_GHOST_POSES[i][j];
		
		ghostDirs = new int[INIT_GHOST_POSES.length];
		for(int i = 0; i < ghostDirs.length ; i++)
			ghostDirs[i] = -1;
		
		pacManPos = INIT_PACMAN_POS.clone();
		inTerminalState = false;
		foodLeft = 0;
		powerPillCounter = 0;
		placeFood();
	}
	

	@Override
	protected int getNumberOfActions() 
	{
		return NUM_ACTS;
	}

	@Override
	protected int getNumberOfObservations() 
	{
		return NUM_OBS;
	}

	@Override
	protected boolean inTerminalState() 
	{
		return inTerminalState;
	}

	@Override
	protected void executeAction(Action act) 
	{
		moveGhosts();
		currImmediateReward = movePacman(act.getID());
	}

	@Override
	protected double getCurrentReward() 
	{
		return currImmediateReward;
	}


	protected void placeFood()
	{
		foodLeft = 0;
		for(int lYPos = 1; lYPos < gameMap.length-1; lYPos++)
		{
			for(int lXPos = 1; lXPos < gameMap[lYPos].length; lXPos++)
			{
				if(gameMap[lYPos][lXPos] == ' ' && 
						!(lYPos > 6 && lYPos < 12 && lXPos > 5 && lXPos <13) &&
						rando.nextDouble() < 0.5 &&
						!(lYPos == pacManPos[0] && lXPos == pacManPos[1]))
				{
					gameMap[lYPos][lXPos] = '.';
					foodLeft++;
				}

			}
		}
	}


	protected int computeIntFromBinary(boolean[] pBinary)
	{
		int lResult = 0;

		for(int i = 0; i < pBinary.length; i++)
		{
			if(pBinary[i])
				lResult += (int)Math.pow(2, i);
		}

		return lResult;
	}

	protected int computeFoodManhattanDist()
	{
		int lMinDist = 5;

		for(int lYDiff = -4; lYDiff < 4; lYDiff++)
		{
			for(int lXDiff = -4; lXDiff < 4; lXDiff++)
			{
				int lYPos = pacManPos[0]+lYDiff;
				int lXPos = pacManPos[1]+lXDiff;

				if(lYPos > 0 && lYPos < gameMap.length -1 &&
						lXPos > 0 && lXPos < gameMap[0].length -1)
				{

					if(gameMap[lYPos][lXPos] == '.' &&
							Math.abs(lYDiff)+Math.abs(lXDiff) < lMinDist)
					{
						lMinDist = Math.abs(lYDiff)+Math.abs(lXDiff);
					}
				}
			}
		}
		return lMinDist;
	}

	protected double movePacman(int pAct)
	{
		List<Integer> lValidMoves = getValidMovements(pacManPos[1],pacManPos[0]);

		if(powerPillCounter >= 0)
			powerPillCounter--;

		if(!lValidMoves.contains(pAct))
		{
			return computeNewStateInformation() - 10;
		}
		else
		{
			makeMove(pAct, pacManPos);
		}

		return computeNewStateInformation();
	}

	protected double computeNewStateInformation()
	{
		double lReward = -1;

		for(int lGhost = 0; lGhost < ghostPoses.length; lGhost++)
		{
			if(ghostPoses[lGhost][0] == pacManPos[0] &&
					ghostPoses[lGhost][1] == pacManPos[1])
			{
				if(powerPillCounter >= 0)
				{
					lReward = lReward+25;
					resetGhost(lGhost);
				}
				else
				{
					lReward = lReward - 50;
					inTerminalState = true;
				}
			}
		}

		if(gameMap[pacManPos[0]][pacManPos[1]] == '.')
		{
			gameMap[pacManPos[0]][pacManPos[1]] = ' ';
			lReward = lReward + 10;
			
			foodLeft--;
			if(foodLeft == 0)
			{
				inTerminalState = true;
				lReward+=100;
			}
		}
		else if(gameMap[pacManPos[0]][pacManPos[1]] == 'o')
		{
			gameMap[pacManPos[0]][pacManPos[1]] = ' ';
			powerPillCounter = 15;
		}

		return lReward;
	}

	protected void moveGhosts()
	{
		for(int lGhost = 0; lGhost < ghostPoses.length; lGhost++)
		{
			if((Math.abs(ghostPoses[lGhost][0]-pacManPos[0]) + 
					Math.abs(ghostPoses[lGhost][1]-pacManPos[1]) <= 5))
			{
				if(powerPillCounter < 0)
				{
					ghostDirs[lGhost] = moveGhostAggressive(lGhost);
				}
				else
				{
					ghostDirs[lGhost] = moveGhostDefensive(lGhost);
				}
			}
			else
			{
				ghostDirs[lGhost] = moveGhostRandom(lGhost);
			}
		}
	}

	protected int moveGhostAggressive(int pGhost)
	{

		int bestDist = Integer.MAX_VALUE;
		int bestDir = -1;

		if(rando.nextDouble() < CHASE_PROB)
		{
			List<Integer> validMoves = getValidMovements(ghostPoses[pGhost][1], ghostPoses[pGhost][0]);
			for(int dir = 0; dir < 4; dir++)
			{
				if(!validMoves.contains(dir) || dir == oppositeDir(ghostDirs[pGhost]))
					continue;

				int dist = directionalDistance(pacManPos,ghostPoses[pGhost], dir);
				if(dist <= bestDist)
				{
					bestDir = dir;
				}
			}
		}

		if(bestDir != -1)
		{
			makeMove(bestDir, ghostPoses[pGhost]);
		}
		else
		{
			moveGhostRandom(pGhost);
		}
		
		return bestDir;
	}
	
	protected int oppositeDir(int dir)
	{
		switch(dir)
		{
		case NORTH:
			return SOUTH;
		case EAST:
			return WEST;
		case SOUTH:
			return NORTH;
		case WEST:
			return EAST;
		default:
			return -1;
		}
	}
	
	protected int moveGhostDefensive(int pGhost)
	{

		int bestDist = Integer.MIN_VALUE;
		int bestDir = -1;

		if(rando.nextDouble() > DEFENSIVE_SLIP)
		{
			List<Integer> validMoves = getValidMovements(ghostPoses[pGhost][1], ghostPoses[pGhost][0]);
			for(int dir = 0; dir < 4; dir++)
			{
				if(!validMoves.contains(dir) || dir == oppositeDir(ghostDirs[pGhost]))
					continue;

				int dist = directionalDistance(pacManPos,ghostPoses[pGhost], dir);
				if(dist >= bestDist)
				{
					bestDir = dir;
				}
			}
		}

		if(bestDir != -1)
		{
			makeMove(bestDir, ghostPoses[pGhost]);
		}
		
		return bestDir;
	}
	

	protected int moveGhostRandom(int pGhost)
	{
		List<Integer> lValidMoves = getValidMovements(ghostPoses[pGhost][1], ghostPoses[pGhost][0]);

		int lMove = -1;
		
		do
		{
			lMove = lValidMoves.get(rando.nextInt(lValidMoves.size()));
		}while(lMove == oppositeDir(ghostDirs[pGhost]));

		makeMove(lMove, ghostPoses[pGhost]);
		
		return lMove;
	}


	protected void resetGhost(int pGhost)
	{
		ghostPoses[pGhost] = INIT_GHOST_POSES[pGhost];
	}

	protected List<Integer> getValidMovements(int pXPos, int pYPos)
	{
		List<Integer> lValidMoves = new ArrayList<Integer>();

		if(gameMap[pYPos+1][pXPos] != 'x')
		{
			lValidMoves.add(SOUTH);
		}
		if(gameMap[pYPos-1][pXPos] != 'x')
		{
			lValidMoves.add(NORTH);
		}
		if(gameMap[pYPos][pXPos-1] != 'x')
		{
			lValidMoves.add(WEST);
		}
		if(gameMap[pYPos][pXPos+1] != 'x')
		{
			lValidMoves.add(EAST);
		}

		return lValidMoves;
	}

	protected void makeMove(int pMove, int[] pPos)
	{
		if(pMove == NORTH)
		{
			pPos[0]--;
		}
		else if(pMove == EAST)
		{
			pPos[1]++;
		}
		else if(pMove == SOUTH)
		{
			pPos[0]++;
		}
		else if(pMove == WEST)
		{
			pPos[1]--;
		}

		if(gameMap[pPos[0]][pPos[1]] == '<')
		{
			pPos[1] = gameMap[0].length-2;
		}
		else if(gameMap[pPos[0]][pPos[1]] == '>')
		{
			pPos[1] = 1;
		}
	}

	protected int directionalDistance(int[] lhs, int[] rhs, int dir)
	{
		if(dir == NORTH)
		{
			return lhs[0]-rhs[0];
		}
		else if(dir == EAST)
		{
			return rhs[1]-lhs[1];
		}
		else if(dir == SOUTH)
		{
			return rhs[0]-lhs[0];
		}
		else
		{
			return lhs[1]-rhs[1];
		}

	}


	

}
