/*
 *   Copyright 2013 William Hamilton
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package cpsr.environment.simulation;

import pomdp.environments.POMDP;
import cpsr.environment.components.Action;
import cpsr.environment.components.Observation;
import cpsr.environment.simulation.domains.PacMan;
import cpsr.planning.PSRPlanningExperiment;
import cpsr.planning.RandomPlanner;

/**
 * Wrapper that allows Cassandra style .POMDP file defined domains to be used for simulation,
 * learning and planning.
 * The code uses Guy Shani's POMDP package to parse the .POMDP files.
 * 
 * @author William Hamilton
 */
public class CassandraPOMDPSim extends ASimulator {

	protected POMDP pomdpSim;
	
	protected Observation currentObservation;
	protected double currentReward;
	protected int currentState;
	protected String filename;
	
	
	public static void main(String args[])
	{
			CassandraPOMDPSim cass = new CassandraPOMDPSim("/home/williamleif/Models/mit.POMDP", 10);
			PSRPlanningExperiment experiment = new PSRPlanningExperiment("PSRConfigs/cass", "PlanningConfigs/cass", cass);
			experiment.runExperiment();
			experiment.publishResults("/home/williamleif/workspace/PSRToolbox/Results/mit-test");
	}
	
	/**
	 * Constructs a Cassandra .POMDP simulator from specified file.
	 * 
	 * @param fileName Path to .POMDP file.
	 * @param seed Random seed to use.
	 */
	public CassandraPOMDPSim(String fileName, long seed)
	{
		super(seed);
		pomdpSim = new POMDP();
		this.filename = fileName;
		try
		{
			pomdpSim.load(fileName);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	@Override
	public RandomPlanner getRandomPlanner(int pSeed)
	{
		return new RandomPlanner(pSeed, 0,getNumberOfActions());
	}
	
	@Override
	public String getName()
	{
		return "Cassandra:"+filename;
	}

	@Override
	protected void initRun() 
	{
		currentState = pomdpSim.chooseStartState();
	}

	@Override
	protected int getNumberOfActions() 
	{
		return pomdpSim.getActionCount();
	}

	@Override
	protected int getNumberOfObservations() 
	{
		return pomdpSim.getObservationCount();
	}

	@Override
	protected boolean inTerminalState() 
	{
		return pomdpSim.isTerminalState(currentState);
	}

	@Override
	protected void executeAction(Action act) {
		currentReward = pomdpSim.R(currentState, act.getID());
		currentObservation = new Observation(pomdpSim.observe(act.getID(), currentState));
		currentState = pomdpSim.execute(act.getID(), currentState);	
	}

	@Override
	protected double getCurrentReward() 
	{
//		if(currentReward != -1.0)
//			return currentReward;
//		else
//			return 0.0;
		
		return currentReward;
	}

	@Override
	protected Observation getCurrentObservation()
	{
		return currentObservation;
	}

}
