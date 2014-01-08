package cpsr.environment.simulation.domains;

import java.util.List;

import cpsr.planning.PSRPlanningExperiment;
import cpsr.environment.ModelQualityExperiment;
import cpsr.environment.components.Action;
import cpsr.environment.components.Observation;

public class HarderPacMan extends PacMan
{

	protected static final char[][] INIT_GAME_MAP = {
		{'x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x'},
		{'x',' ',' ','*',' ',' ',' ',' ',' ','*',' ',' ',' ',' ',' ','*',' ',' ','x'},
		{'x',' ','x','x',' ','x','x','x',' ','x',' ','x','x','x',' ','x','x',' ','x'},
		{'x','o',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ','o','x'},
		{'x',' ','x','x',' ','x',' ','x','x','x','x','x',' ','x',' ','x','x',' ','x'},
		{'x',' ',' ',' ',' ','x',' ',' ',' ','x',' ',' ',' ','x',' ',' ',' ',' ','x'},
		{'x','x','x','x',' ','x','x','x',' ','x',' ','x','x','x',' ','x','x','x','x'},
		{'x','x','x','x',' ','x',' ',' ',' ',' ',' ',' ',' ','x',' ','x','x','x','x'},
		{'x','x','x','x',' ','x',' ','x',' ','*',' ','x',' ','x',' ','x','x','x','x'},
		{'<',' ',' ',' ',' ','x',' ','x',' ',' ',' ','x',' ','x',' ',' ',' ',' ','>'},
		{'x','x','x','x',' ','x',' ','x','x','x','x','x',' ','x',' ','x','x','x','x'},
		{'x','x','x','x',' ','x',' ',' ',' ',' ',' ',' ',' ','x',' ','x','x','x','x'},
		{'x','x','x','x',' ','x',' ','x','x','x','x','x',' ','x',' ','x','x','x','x'},
		{'x',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ','x'},
		{'x',' ','x','x',' ','x','x','x',' ','x',' ','x','x','x',' ','x','x',' ','x'},
		{'x','o',' ','x',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ','x',' ','o','x'},
		{'x','x',' ','x',' ','x',' ','x','x','x','x','x',' ','x',' ','x',' ','x','x'},
		{'x',' ','*',' ',' ','x',' ',' ',' ','x',' ',' ',' ','x',' ',' ','*',' ','x'},
		{'x',' ','x','x','x','x','x','x',' ','x',' ','x','x','x','x','x','x',' ','x'},
		{'x',' ',' ',' ',' ',' ',' ',' ',' ','*',' ',' ',' ',' ',' ',' ',' ',' ','x'},
		{'x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x'},
	};
	
	private int NUM_FOOD = 7;
	
	public static void main(String args[])
	{
		HarderPacMan pacman = new HarderPacMan(10);
		PSRPlanningExperiment experiment = new PSRPlanningExperiment(args[0], args[1],pacman);
		experiment.runExperiment();
		
//		ModelQualityExperiment experiment = new ModelQualityExperiment(args[0], args[1],pacman);
//		experiment.runExperiment(20);
		
		experiment.publishResults(args[2]);
	}

	public HarderPacMan(long seed)
	{
		super(seed);
	}
	
	public HarderPacMan(long seed, int maxRunLength)
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
					lObsInfo[4] = true;
					break;
				}
			}
			if(lObsInfo[4] == true)
				break;
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
					lObsInfo[5] = true;
				}
			}
			if(lObsInfo[5] == true)
				break;
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
					lObsInfo[6] = true;
					break;
				}
			}
			if(lObsInfo[6] == true)
				break;
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
					lObsInfo[7] = true;
					break;
				}
			}
			if(lObsInfo[7] == true)
				break;
			tempLoc[1]--;
		}

		lObsInfo[8] = powerPillCounter >= 0;

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
		foodLeft = NUM_FOOD;
		powerPillCounter = 0;
	}
	

	@Override
	protected int getNumberOfActions() 
	{
		return NUM_ACTS;
	}

	@Override
	protected int getNumberOfObservations() 
	{
		return (int)Math.pow(2, 9);
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

		if(gameMap[pacManPos[0]][pacManPos[1]] == '*')
		{
			gameMap[pacManPos[0]][pacManPos[1]] = ' ';
			lReward = lReward + 100;
			
			foodLeft--;
			if(foodLeft == 0)
			{
				inTerminalState = true;
				lReward+=1000;
			}
		}
		else if(gameMap[pacManPos[0]][pacManPos[1]] == 'o')
		{
			gameMap[pacManPos[0]][pacManPos[1]] = ' ';
			powerPillCounter = 15;
		}

		return lReward;
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

	
}
