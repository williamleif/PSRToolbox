package cpsr.environment.simulation;

import cpsr.environment.components.Action;
import cpsr.environment.components.Observation;
import cpsr.planning.PSRPlanningExperiment;

public class RobotNavigationCassPOMDPSim extends CassandraPOMDPSim
{
	public static void main(String args[])
	{
		RobotNavigationCassPOMDPSim cass = new RobotNavigationCassPOMDPSim("/home/williamleif/Models/cit.POMDP", 10);
		PSRPlanningExperiment experiment = new PSRPlanningExperiment("PSRConfigs/cass", "PlanningConfigs/cass", cass);
		experiment.runExperiment();
		experiment.publishResults("/home/williamleif/workspace/PSRToolbox/Results/cit-test");
	}
	
	
	public RobotNavigationCassPOMDPSim(String fileName, long seed) 
	{
		super(fileName, seed);
	}
	
	@Override
	protected void initRun() 
	{
		currentState = pomdpSim.chooseStartState();
		currentReward = 0;
	}

	@Override
	protected void executeAction(Action act) {
		currentReward = pomdpSim.R(currentState, act.getID());
		currentObservation = new Observation(pomdpSim.observe(act.getID(), currentState));
		currentState = pomdpSim.execute(act.getID(), currentState);	
		
		if(pomdpSim.R(currentState, 3) == 1.0)
		{
			currentReward = 1.0;
		}
		
	}
	
	@Override
	protected int getNumberOfActions() 
	{
		return pomdpSim.getActionCount()-1;
	}
	
	@Override
	protected boolean inTerminalState() 
	{
		return currentReward == 1.0;
	}

}
