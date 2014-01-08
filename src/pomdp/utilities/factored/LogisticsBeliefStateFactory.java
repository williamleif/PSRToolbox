package pomdp.utilities.factored;

import pomdp.environments.Logistics;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;

public class LogisticsBeliefStateFactory extends BeliefStateFactory {
	private Logistics m_pPOMDP;
	private LogisticsBeliefState m_bsInitial;
	
	public LogisticsBeliefStateFactory( Logistics pPOMDP ){
		super( pPOMDP );
		m_pPOMDP = pPOMDP;
		m_bsInitial = new LogisticsBeliefState( m_pPOMDP, this );
	}
	protected BeliefState newBeliefState(){
		return new LogisticsBeliefState( m_pPOMDP, this );
	}
	public BeliefState getInitialBeliefState(){
		return m_bsInitial;
	}

}
