package pomdp.algorithms.online;

import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.algorithms.pointbased.HeuristicSearchValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.Pair;
import pomdp.valuefunction.DicretizedUpperBound;
import pomdp.valuefunction.LinearValueFunctionApproximation;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class RealTimeDynamicProgramming extends ValueIteration {

	protected int m_cDiscretizationLevels;
	protected DicretizedUpperBound m_dUpperBound;
	
	public RealTimeDynamicProgramming( POMDP pomdp ){
		this( pomdp, 10 );
	}

	public RealTimeDynamicProgramming( POMDP pomdp, int cDiscretizationLevels ){
		super( pomdp );
		m_cDiscretizationLevels = cDiscretizationLevels;
		m_pPOMDP.getBeliefStateFactory().setDiscretizationLevels( cDiscretizationLevels );
		m_dUpperBound = new DicretizedUpperBound( m_pPOMDP, cDiscretizationLevels, m_vfMDP );
	}
		
	protected int explore( BeliefState bsCurrent, double dEpsilon, int iTime, double dDiscount, Vector<BeliefState> vObservedBeliefStates ){
		int iStartState = m_pPOMDP.chooseStartState();
		return explore( bsCurrent, iStartState, dEpsilon, iTime, dDiscount, vObservedBeliefStates );
	}
	protected int explore( BeliefState bsCurrent, int iCurrentState, double dEpsilon, int iTime, double dDiscount, Vector<BeliefState> vObservedBeliefStates ){
		int iAction = 0, iObservation = 0, iNextState = 0;
		BeliefState bsNext = null;
		int iMaxDepth = 0;

		if( m_pPOMDP.isTerminalState( iCurrentState ) )
			return iTime;
		
		if( !vObservedBeliefStates.contains( bsCurrent ) )
			vObservedBeliefStates.add( bsCurrent );
		

		if( iTime > 100  )
			return iTime;
			
		iAction = m_dUpperBound.getAction( bsCurrent );
		
		iNextState = m_pPOMDP.execute( iAction, iCurrentState );
		iObservation = m_pPOMDP.observe( iAction, iNextState );

		bsNext = bsCurrent.nextBeliefState( iAction, iObservation );
		
		iMaxDepth = explore( bsNext, iNextState, dEpsilon, iTime + 1, dDiscount * m_dGamma, vObservedBeliefStates );

		m_dUpperBound.updateValue( bsCurrent );
					
		return iMaxDepth;
	}

	public String getName(){
		return "RTDP";
	}

	@Override
	public void valueIteration(int maxSteps, double epsilon, double targetValue) {
	}

	@Override
	public void valueIteration(int maxSteps, double epsilon,
			double targetValue, int maxRunningTime, int numEvaluations) {
		throw new NotImplementedException();		
		
	}
}
