package cpsr.environment.simulation.domains;

import java.util.ArrayList;

import cpsr.environment.components.Action;
import cpsr.environment.components.Observation;
import cpsr.environment.exceptions.EnvironmentException;
import cpsr.environment.simulation.ASimulator;
import cpsr.planning.PSRPlanningExperiment;
import cpsr.planning.RandomPlanner;

public class Follow extends ASimulator
{
	private static int NUM_ACTS = 5;
	private static int NUM_OBS = 5;
	private static double TERMINAL_REWARD = -20.0;

	
	private int[] aPersPos, aAgentPos;
	private int aPersonID, aCurrDist;
	private ArrayList<Integer> aPersMoveProbs;
	
	public static void main(String args[])
	{
		Follow follow = new Follow(10);
		PSRPlanningExperiment experiment = new PSRPlanningExperiment(args[0], args[1],follow);
		experiment.runExperiment();
		experiment.publishResults(args[2]);
//		ModelQualityExperiment experiment = new ModelQualityExperiment(args[0], args[1], follow);
//		System.out.println("Likilihood: " + experiment.runExperiment());
//		System.out.println("Runtime: " + experiment.getRuntime());
	}

	public Follow(long seed)
	{
		super(seed);
	}
	
	public Follow(long seed, int maxRunLength)
	{
		super(seed, maxRunLength);
	}
	
	@Override
	public String getName()
	{
		return "Follow";
	}


	@Override
	protected void initRun()
	{
		aPersMoveProbs = new ArrayList<Integer>();
		aPersonID = rando.nextInt(2)+1;
		aCurrDist = 0;

		aAgentPos = new int[2];
		aPersPos = new int[2];
		aAgentPos[0] = 0;
		aPersPos[0] = 0;
		aAgentPos[1] = 0;
		aAgentPos[1] = 0;

		if(aPersonID == 1)
		{
			//no move prob
			for(int i = 0; i < 30; i++)aPersMoveProbs.add(0);
			//north move prob
			for(int i = 0; i < 40; i++)aPersMoveProbs.add(1);
			//east move prob
			for(int i = 0; i < 20; i++)aPersMoveProbs.add(2);
			//south move pob
			for(int i = 0; i < 5; i++)aPersMoveProbs.add(3);
			//west move prob
			for(int i = 0; i < 5; i++)aPersMoveProbs.add(4);
		}
		else
		{
			//no move prob
			for(int i = 0; i < 10; i++)aPersMoveProbs.add(0);
			//north move prob
			for(int i = 0; i < 5; i++)aPersMoveProbs.add(1);
			//east move prob
			for(int i = 0; i < 80; i++)aPersMoveProbs.add(2);
			//south move pob
			for(int i = 0; i < 3; i++)aPersMoveProbs.add(3);
			//west move prob
			for(int i = 0; i < 2; i++)aPersMoveProbs.add(4);
		}
	}

	/**
	 * Moves the person random according to identity.
	 */
	private void movePerson()
	{
		makeMove(aPersPos, aPersMoveProbs.get(rando.nextInt(100)));
	}

	/**
	 * Moves the agent according to specified action.
	 * @param pAction The action to take.
	 */
	private void moveAgent(int pAction)
	{
		if(pAction < 0 || pAction > 4)
		{
			throw new EnvironmentException("Illegal action ID for follow. Must be in range [0,4]");
		}
		makeMove(aAgentPos, pAction);
	}

	/**
	 * Alters position array according to action.
	 * @param pPos The position array.
	 * @param pAction The action to take.
	 */
	private void makeMove(int[] pPos, int pAction)
	{
		switch(pAction)
		{
		case 1:
			pPos[1]++;
			break;
		case 2:
			pPos[0]++;
			break;
		case 3:
			pPos[1]--;
			break;
		case 4:
			pPos[0]--;
			break;
		}
	}

	/**
	 * Recomputes current distance from agent to person.
	 */
	private void calcCurrDist()
	{
		aCurrDist = Math.abs(aPersPos[0]-aAgentPos[0])+Math.abs(aPersPos[1]-aAgentPos[1]);
	}

	@Override
	protected double getCurrentReward()
	{

		if(aCurrDist == 0)
		{
			return 1.0;
		}
		else if(aCurrDist == 1)
		{
			return 0.0;
		}
		else if(aCurrDist  == 2)
		{
			return -1.0;
		}
		else
		{
			return -20.0;
		}
	}


	@Override
	protected Observation getCurrentObservation()
	{
		Observation lCurrObs;

		if(rando.nextDouble() < 0.8 && aCurrDist <= 1)
		{
			if(aCurrDist == 0)
			{
				lCurrObs = new Observation(0);
			}
			else if(aAgentPos[0] == aPersPos[0])
			{
				if(aAgentPos[1] - aPersPos[1] < 0)
				{
					lCurrObs = new Observation(1);
				}
				else
				{
					lCurrObs = new Observation(3);
				}
			}
			else
			{
				if(aAgentPos[0] - aPersPos[0] < 0)
				{
					lCurrObs = new Observation(4);
				}
				else
				{
					lCurrObs = new Observation(2);
				}
			}
		}
		else
		{
			lCurrObs = new Observation(5);
		}

		return lCurrObs;
	}
	
	@Override
	public RandomPlanner getRandomPlanner(int pSeed)
	{
		return new RandomPlanner(pSeed, 0, NUM_ACTS);
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
		return getCurrentReward() == TERMINAL_REWARD;
	}

	@Override
	protected void executeAction(Action act) 
	{
		movePerson();
		moveAgent(act.getID());
		calcCurrDist();		
	}


}
