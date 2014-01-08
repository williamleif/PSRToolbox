package pomdp.utilities.factored;

import pomdp.environments.ModifiedRockSample;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;


public class ModifiedRockSampleBeliefStateFactory extends BeliefStateFactory {

	private ModifiedRockSample m_mrsPOMDP;
	
	public ModifiedRockSampleBeliefStateFactory( ModifiedRockSample mrsPOMDP ){
		super( mrsPOMDP );
		m_mrsPOMDP = mrsPOMDP;
	}
	protected BeliefState newBeliefState(){
		return new ModifiedRockSampleBeliefState( m_mrsPOMDP );
	}
	public BeliefState getInitialBeliefState(){
		return new ModifiedRockSampleBeliefState( m_mrsPOMDP );
	}
	
	
	public BeliefState getRandomBeliefState() {
		int iState = 0;
		ModifiedRockSampleBeliefState bs = new ModifiedRockSampleBeliefState( m_mrsPOMDP );
		bs.initRandomValues();
		return bs;
	}

}
