package cpsr.environment.simulation;

import java.util.ArrayList;
import java.util.Random;

import cpsr.environment.DataSet;
import cpsr.environment.TrainingDataSet;
import cpsr.environment.components.Action;
import cpsr.environment.components.ActionObservation;
import cpsr.environment.components.Observation;
import cpsr.planning.APSRPlanner;
import cpsr.planning.RandomPlanner;

public abstract class ASimulator implements ISimulator 
{

	//use arbitrary maximum run length limit.
	//this simply to prevent infinite loops in simulator
	//and also just practical since runs should not be any longer than this limit
	//if they are to be used with PSRs.
	public int DEFAULT_RUN_LEN_LIMIT = 100000000;

	protected Random rando;
	protected long seed;
	protected int maxRunLength;

	protected ASimulator(long seed, int maxRunLength)
	{
		if(maxRunLength > 1000000)
		{
			throw new IllegalArgumentException("Cannot have max run length greater than: " + DEFAULT_RUN_LEN_LIMIT);
		}

		this.rando = new Random(seed);
		this.seed = seed;
		this.maxRunLength = maxRunLength;
	}

	protected ASimulator(long seed)
	{
		this.rando = new Random(seed);
		this.maxRunLength = DEFAULT_RUN_LEN_LIMIT;
	}	

	@Override 
	public DataSet simulateTestRuns(int runs, APSRPlanner planner)
	{
		int currRun;
		double currReward;
		Observation currObs;
		Action currAction;

		RandomPlanner randPlanner = null;
		if(planner == null)
			randPlanner = getRandomPlanner(rando.nextInt());

		DataSet testData = new DataSet();
		//TODO: clean 
		testData.newDataBatch(10000000);

		for(currRun = 0; currRun < runs; currRun++)
		{
			currReward = 0.0;
			ArrayList<ActionObservation> runActObs = new ArrayList<ActionObservation>();
			ArrayList<Double> runRewards = new ArrayList<Double>();

			initRun();
			if(planner != null)
				planner.resetToStartState();
			
			int counter = 0;
			while(!inTerminalState()  && counter < maxRunLength)
			{
				if(planner == null)
				{
					currAction = randPlanner.getAction();
				}
				else
				{
					currAction = planner.getAction();
				}

				executeAction(currAction);
				currReward = getCurrentReward();
				currObs = getCurrentObservation();
				currObs.setMaxID(getNumberOfObservations()-1);
				currAction.setMaxID(getNumberOfActions()-1);
				
				runActObs.add(new ActionObservation(currAction, currObs));
				runRewards.add(currReward);
								
				if(planner != null)
					planner.update(new ActionObservation(currAction, currObs));
					
				counter++;
			}
			testData.addRunData(runActObs, runRewards);
		}
		
		return testData;
	}

	@Override
	public void simulateTrainingRuns(int runs, TrainingDataSet trainData) 
	{
		simulateTrainingRuns(runs, null, trainData);
	}

	
	@Override
	public void simulateTrainingRuns(int runs, APSRPlanner planner, TrainingDataSet trainData)
	{
		int currRun;
		double currReward;
		Observation currObs;
		Action currAction;

		RandomPlanner randPlanner = null;
		if(planner == null)
			randPlanner = getRandomPlanner(rando.nextInt());

		for(currRun = 0; currRun < runs; currRun++)
		{
			currReward = 0.0;
			ArrayList<ActionObservation> runActObs = new ArrayList<ActionObservation>();
			ArrayList<Double> runRewards = new ArrayList<Double>();

			initRun();
			if(planner != null)
				planner.resetToStartState();
			
			int counter = 0;
			while(!inTerminalState()  && counter < maxRunLength)
			{
				if(planner == null)
				{
					currAction = randPlanner.getAction();
				}
				else
				{
					currAction = planner.getAction();
				}

				executeAction(currAction);
				currReward = getCurrentReward();
				currObs = getCurrentObservation();
				currObs.setMaxID(getNumberOfObservations()-1);
				currAction.setMaxID(getNumberOfActions()-1);
				
				runActObs.add(new ActionObservation(currAction, currObs));
				runRewards.add(currReward);
				
				trainData.addRunDataForTraining(runActObs);
				
				if(planner != null)
					planner.update(new ActionObservation(currAction, currObs));
					
				counter++;
			}
			trainData.addRunData(runActObs, runRewards);
		}
	}
		

	@Override
	public void setMaxRunLength(int pMaxRunLength) 
	{
		maxRunLength = pMaxRunLength;
	}
	
	@Override
	public RandomPlanner getRandomPlanner(int pSeed)
	{
		return new RandomPlanner(rando.nextInt(),0,getNumberOfActions());
	}

	protected abstract void initRun();

	protected abstract int getNumberOfActions();
	
	protected abstract int getNumberOfObservations();
	
	protected abstract boolean inTerminalState();
	
	protected abstract void executeAction(Action act);

	protected abstract double getCurrentReward();
	
	protected abstract Observation getCurrentObservation();
}
