package pomdp.algorithms.pointbased;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import pomdp.algorithms.ValueIteration;
import pomdp.environments.FactoredPOMDP;
import pomdp.environments.POMDP;
import pomdp.environments.FactoredPOMDP.BeliefType;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.HeuristicPolicy;
import pomdp.utilities.JProf;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;
import pomdp.utilities.TabularAlphaVector;
import pomdp.utilities.factored.AlgebraicDecisionDiagramVertex;
import pomdp.utilities.factored.FactoredBeliefState;
import pomdp.utilities.factored.FactoredBeliefStateComparator;
import pomdp.valuefunction.LinearValueFunctionApproximation;
import pomdp.valuefunction.MDPValueFunction;

public class ForwardSearchValueIteration extends ValueIteration {
	protected int m_iLimitedBeliefMDPState;
	protected int m_iLimitedBeliefObservation;
	protected LinearValueFunctionApproximation m_vDetermisticPOMDPValueFunction;
	protected BeliefState m_bsDeterministicPOMDPBeliefState;
	protected HeuristicPolicy m_hpPolicy;
	protected int m_iDepth;
	protected int m_iIteration, m_iInnerIteration;
	protected long m_lLatestADRCheck, m_cTimeInADR, m_lCPUTimeTotal, m_lIterationStartTime;
	protected Pair m_pComputedADRs;
	protected int[] m_aiStartStates;
	protected SortedMap<Double,Integer>[][] m_amNextStates;
	private HeuristicType m_htType;
	public static HeuristicType DEFAULT_HEURISTIC = HeuristicType.MDP;

	public enum HeuristicType{
		MDP, ObservationAwareMDP, DeterministicTransitionsPOMDP, DeterministicObservationsPOMDP, DeterministicPOMDP, LimitedBeliefMDP, HeuristicPolicy
	}

	public ForwardSearchValueIteration( POMDP pomdp ){
		this( pomdp, DEFAULT_HEURISTIC );
	}

	public ForwardSearchValueIteration( POMDP pomdp, HeuristicPolicy hpPolicy ){
		//this( pomdp, HeuristicType.DeterministicObservationsPOMDP );
		//this( pomdp, HeuristicType.LimitedBeliefMDP );
		this( pomdp, HeuristicType.MDP );
		m_hpPolicy = hpPolicy;
	}

	public ForwardSearchValueIteration( POMDP pomdp, HeuristicType htType ){
		super( pomdp );
		m_htType = htType;
		m_iDepth = 0;
		m_iIteration = 0;
		m_iInnerIteration = 0;
		m_lLatestADRCheck = 0;
		m_cTimeInADR = 0;
		m_lCPUTimeTotal = 0;
		m_lIterationStartTime = 0;
		m_pComputedADRs = null;
		m_aiStartStates = null;
		m_vfMDP = null;
		m_bsDeterministicPOMDPBeliefState = null;
		m_vDetermisticPOMDPValueFunction = null;
		m_iLimitedBeliefObservation = -1;
		initHeuristic();
		//m_vLabeledBeliefs = new Vector<BeliefState>();
	}

	private void initHeuristic(){
		long lBefore = JProf.getCurrentThreadCpuTimeSafe(), lAfter = 0;
		if( m_htType == HeuristicType.MDP ){
			m_vfMDP = m_pPOMDP.getMDPValueFunction();
			m_vfMDP.valueIteration( 1000, ExecutionProperties.getEpsilon() );
		}
		lAfter = JProf.getCurrentThreadCpuTimeSafe();
		Logger.getInstance().log( "FSVI", 0, "initHeurisitc", "Initialization time was " + ( lAfter - lBefore ) / 1000000 );
	}

	public void valueIteration( int cMaxSteps, double dEpsilon, double dTargetValue, int maxRunningTime, int numEvaluations) {

		//public void valueIteration( int cMaxSteps, double dEpsilon, double dTargetValue ){
		int iIteration = 0;
		boolean bDone = false;
		Pair pComputedADRs = new Pair();
		double dMaxDelta = 0.0;
		String sMsg = "";

		long lStartTime = System.currentTimeMillis(), lCurrentTime = 0;
		long lCPUTimeBefore = 0, lCPUTimeAfter = 0;
		Runtime rtRuntime = Runtime.getRuntime();

		long cDotProducts = AlphaVector.dotProductCount(), cVnChanges = 0, cStepsWithoutChanges = 0;
		m_cElapsedExecutionTime = 0;
		m_lCPUTimeTotal = 0;

		sMsg = "Starting " + getName() + " target ADR = " + round( dTargetValue, 3 );
		Logger.getInstance().log( "FSVI", 0, "VI", sMsg );

		//initStartStateArray();
		m_pComputedADRs = new Pair();

		for( iIteration = 0 ; ( iIteration < cMaxSteps ) && !bDone ; iIteration++ ){
			lStartTime = System.currentTimeMillis();
			lCPUTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
			AlphaVector.initCurrentDotProductCount();
			cVnChanges = m_vValueFunction.getChangesCount();
			m_iIteration = iIteration;
			m_iInnerIteration = 0;
			m_lLatestADRCheck = lCPUTimeBefore;
			m_cTimeInADR = 0;
			m_lIterationStartTime = lCPUTimeBefore;
			dMaxDelta = improveValueFunction();
			lCPUTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
			lCurrentTime = System.currentTimeMillis();
			m_cElapsedExecutionTime += ( lCurrentTime - lStartTime - m_cTimeInADR );
			m_cCPUExecutionTime += ( lCPUTimeAfter - lCPUTimeBefore - m_cTimeInADR ) / 1000000;
			m_lCPUTimeTotal += lCPUTimeAfter - lCPUTimeBefore - m_cTimeInADR;

			if( m_bTerminate )
				bDone = true;


			if( ExecutionProperties.getReportOperationTime() ){
				try{
					sMsg = "G: - operations " + AlphaVector.getGComputationsCount() + " avg time " + 
					AlphaVector.getAvgGTime();
					Logger.getInstance().log( "FSVI", 0, "VI", sMsg );

					if( m_pPOMDP.isFactored() && ((FactoredPOMDP) m_pPOMDP).getBeliefType() == BeliefType.Factored ){
						sMsg = "Tau: - operations " + FactoredBeliefState.getTauComputationCount() + " avg time " + 
						FactoredBeliefState.getAvgTauTime();
						Logger.getInstance().log( "FSVI", 0, "VI", sMsg );

					}
					else{
						sMsg = "Tau: - operations " + m_pPOMDP.getBeliefStateFactory().getTauComputationCount() + " avg time " + 
						m_pPOMDP.getBeliefStateFactory().getAvgTauTime();
						Logger.getInstance().log( "FSVI", 0, "VI", sMsg );
					}
					sMsg = "dot product - avg time = " + AlphaVector.getCurrentDotProductAvgTime();
					Logger.getInstance().log( "FSVI", 0, "VI", sMsg );
					sMsg = "avg belief state size " + m_pPOMDP.getBeliefStateFactory().getAvgBeliefStateSize();
					Logger.getInstance().log( "FSVI", 0, "VI", sMsg );
					sMsg = "avg alpha vector size " + m_vValueFunction.getAvgAlphaVectorSize();
					Logger.getInstance().log( "FSVI", 0, "VI", sMsg );
					AlphaVector.initCurrentDotProductCount();
				}
				catch( Exception e ){
					Logger.getInstance().logln( e );
				}
			}
			if( ( ( m_lCPUTimeTotal  / 1000000000 ) >= 5 ) && ( iIteration >= 10 ) && ( iIteration % 5 == 0 ) && 
					m_vValueFunction.getChangesCount() > cVnChanges &&
					m_vValueFunction.size() > 5 ){



				cStepsWithoutChanges = 0;
				bDone |= checkADRConvergence( m_pPOMDP, dTargetValue, pComputedADRs );

				sMsg = "FSVI: Iteration " + iIteration + 
				" |Vn| = " + m_vValueFunction.size() +
				" simulated ADR " + round( ((Number) pComputedADRs.first()).doubleValue(), 3 ) +
				" filtered ADR " + round( ((Number) pComputedADRs.second()).doubleValue(), 3 ) +
				" max delta " + round( dMaxDelta, 3 ) +
				" depth " + m_iDepth +
				" V(b0) " + round( m_vValueFunction.valueAt( m_pPOMDP.getBeliefStateFactory().getInitialBeliefState() ), 2 ) +
				" time " + 	( lCurrentTime - lStartTime ) / 1000 +
				" CPU time " + ( lCPUTimeAfter - lCPUTimeBefore - m_cTimeInADR ) / 1000000000 +
				" CPU total " + m_lCPUTimeTotal  / 1000000000 +
				" #backups " + m_cBackups + 
				" V changes " + m_vValueFunction.getChangesCount() +
				" #dot product " + AlphaVector.dotProductCount() + 
				" |BS| " + m_pPOMDP.getBeliefStateFactory().getBeliefStateCount() +
				" memory: " + 
				" total " + rtRuntime.totalMemory() / 1000000 +
				" free " + rtRuntime.freeMemory() / 1000000 +
				" max " + rtRuntime.maxMemory() / 1000000 +
				"";
				Logger.getInstance().log( "FSVI", 0, "VI", sMsg );
			}
			else{
				if( cVnChanges == m_vValueFunction.getChangesCount() ){
					cStepsWithoutChanges++;
					//if( cStepsWithoutChanges == 10 ){
					//	bDone = true;
					//}
				}
				sMsg = "FSVI: Iteration " + iIteration + 
				" |Vn| = " + m_vValueFunction.size() +
				" time " + 	( lCurrentTime - lStartTime ) / 1000 +
				" V changes " + m_vValueFunction.getChangesCount() +
				" max delta " + round( dMaxDelta, 3 ) +
				" depth " + m_iDepth +
				" V(b0) " + round( m_vValueFunction.valueAt( m_pPOMDP.getBeliefStateFactory().getInitialBeliefState() ), 2 ) +
				" CPU time " + ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000000 +
				" CPU total " + m_lCPUTimeTotal  / 1000000000 +
				" #backups " + m_cBackups + 
				" |BS| " + m_pPOMDP.getBeliefStateFactory().getBeliefStateCount() +
				" memory: " + 
				" total " + rtRuntime.totalMemory() / 1000000 +
				" free " + rtRuntime.freeMemory() / 1000000 +
				" max " + rtRuntime.maxMemory() / 1000000 +
				"";
				Logger.getInstance().log( "FSVI", 0, "VI", sMsg );


			}

		}	
		m_bConverged = true;

		m_cDotProducts = AlphaVector.dotProductCount() - cDotProducts;
		m_cElapsedExecutionTime /= 1000;
		m_cCPUExecutionTime /= 1000;

		sMsg = "Finished " + getName() + " - time : " + m_cElapsedExecutionTime + /*" |BS| = " + vBeliefPoints.size() +*/
		" |V| = " + m_vValueFunction.size() + 
		" backups = " + m_cBackups + 
		" GComputations = " + AlphaVector.getGComputationsCount() +
		" Dot products = " + m_cDotProducts;
		Logger.getInstance().log( "FSVI", 0, "VI", sMsg );
	}
	protected double forwardSearch( int iState, BeliefState bsCurrent, int iDepth ){
		double dDelta = 0.0, dNextDelta = 0.0;
		int iNextState = 0, iHeuristicAction = 0, iPOMDPAction = 0, iObservation = 0;
		BeliefState bsNext = null;
		AlphaVector avBackup = null, avMax = null;
		double dPreviousValue = 0.0, dNewValue = 0.0;

		if( m_bTerminate )
			return 0.0;

		if( ( m_pPOMDP.terminalStatesDefined() && isTerminalState( iState ) ) 
				||	( iDepth >= 100 ) ){
			m_iDepth = iDepth;
			Logger.getInstance().logln( "Ended at depth " + iDepth + ". isTerminalState(" + iState + ")=" + isTerminalState( iState ) );
		}
		else{
			iHeuristicAction = getAction( iState, bsCurrent );

			iNextState = selectNextState( iState, iHeuristicAction );
			iObservation = getObservation( iState, iHeuristicAction, iNextState );
			bsNext = bsCurrent.nextBeliefState( iHeuristicAction, iObservation );

			if( bsNext == null || bsNext.equals( bsCurrent ) ){
				m_iDepth = iDepth;
			}
			else{

				double d = bsNext.valueAt( iNextState );
				if( bsNext.valueAt( iNextState ) == 0.0 ){
					bsNext = bsCurrent.nextBeliefState( iHeuristicAction, iObservation );
				}
				dNextDelta = forwardSearch( iNextState, bsNext, iDepth + 1 );
			}
		}

		if( true ){ 
			BeliefState bsDeterministic = getDeterministicBeliefState( iState );
			avBackup = backup( bsDeterministic, iHeuristicAction );
			dPreviousValue = m_vValueFunction.valueAt( bsDeterministic );
			dNewValue = avBackup.dotProduct( bsDeterministic );
			dDelta = dNewValue - dPreviousValue;

			if( dDelta > ExecutionProperties.getEpsilon() ){
				m_vValueFunction.addPrunePointwiseDominated( avBackup );
			}
		}
		avBackup = backup( bsCurrent );

		dPreviousValue = m_vValueFunction.valueAt( bsCurrent );
		dNewValue = avBackup.dotProduct( bsCurrent );
		dDelta = dNewValue - dPreviousValue;
		avMax = m_vValueFunction.getMaxAlpha( bsCurrent );

		if( dDelta > 0.0 ){
			m_vValueFunction.addPrunePointwiseDominated( avBackup );
		}
		else{
			avBackup.release();
		}		

		return Math.max( dDelta, dNextDelta );
	}

	private boolean isTerminalState( int iState ){
		return m_pPOMDP.isTerminalState( iState );
	}
	private BeliefState getDeterministicBeliefState( int iState ){
		return m_pPOMDP.getBeliefStateFactory().getDeterministicBeliefState( iState );
	}
	private int getAction( int iState, BeliefState bs ){
		if( m_htType == HeuristicType.MDP ){
			if( m_rndGenerator.nextDouble() < 0.9 )
				return m_vfMDP.getAction( iState );
			return m_rndGenerator.nextInt( m_cActions );
		}
		else if( m_htType == HeuristicType.HeuristicPolicy ){
			return m_hpPolicy.getBestAction( iState, bs );
		}
		return -1;
	}
	private int getObservation( int iStartState, int iAction, int iEndState ){
		if( m_htType == HeuristicType.MDP ){
			return m_pPOMDP.observe( iAction, iEndState );
		}
		else if( m_htType == HeuristicType.HeuristicPolicy ){
			int iObservation = m_hpPolicy.getObservation( iStartState, iAction, iEndState );
			if( iObservation == -1 )
				return m_pPOMDP.observe( iAction, iEndState );
			return iObservation;
		}
		return -1;
	}
	private int selectNextState( int iState, int iAction ) {
		if( m_htType == HeuristicType.MDP ){
			return m_pPOMDP.execute( iAction, iState );
		}
		else if( m_htType == HeuristicType.HeuristicPolicy ){
			int iNextState = m_hpPolicy.getNextState( iState, iAction );
			if( iNextState == -1 )
				return m_pPOMDP.execute( iAction, iState );
		}
		return -1;
	}

	private void removeNextState( int iState, int iAction, int iNextState ){
		m_amNextStates[iState][iAction].remove( m_amNextStates[iState][iAction].lastKey() );
	}

	protected int getNextState( int iAction, int iState ){
		int iNextState = -1;
		double dTr = 0.0, dValue = 0.0;

		if( m_amNextStates[iState][iAction] == null )
			m_amNextStates[iState][iAction] = new TreeMap<Double,Integer>();
		if( m_amNextStates[iState][iAction].isEmpty() ){
			Iterator itNonZero = m_pPOMDP.getNonZeroTransitions( iState, iAction );
			Map.Entry e = null;
			String sDescription = "";
			while( itNonZero.hasNext() ){
				e = (Entry) itNonZero.next();
				iNextState = ((Number)e.getKey()).intValue();
				dTr = ((Number)e.getValue()).doubleValue();
				dValue = m_vfMDP.getValue( iNextState );
				sDescription += "V(" + iNextState + ") = " + round( dValue, 3 ) + ", ";
				m_amNextStates[iState][iAction].put( dValue, iNextState );
			}
		}
		iNextState = m_amNextStates[iState][iAction].get( m_amNextStates[iState][iAction].lastKey() );
		return iNextState;
	}

	protected void initStartStateArray(){
		int cStates = m_pPOMDP.getStartStateCount(), iState = 0;
		Iterator<Entry<Integer,Double>> itStartStates = m_pPOMDP.getStartStates();
		Entry<Integer,Double> e = null;
		m_aiStartStates = new int[cStates];
		for( iState = 0 ; iState < cStates ; iState++ ){
			e = itStartStates.next();
			m_aiStartStates[iState] = e.getKey();
		}
		if( m_amNextStates == null ){
			m_amNextStates = new SortedMap[m_cStates][m_cActions];
		}
	}

	protected int chooseStartState(){
		int cStates = m_pPOMDP.getStartStateCount(), iState = 0, iMaxValueState = -1;
		double dValue = 0.0, dMaxValue = MIN_INF;
		for( iState = 0 ; iState < cStates ; iState++ ){
			if( m_aiStartStates[iState] != -1 ){
				dValue = m_vfMDP.getValue( iState );
				if( dValue > dMaxValue ){
					dMaxValue = dValue;
					iMaxValueState = iState;
				}
			}
		}
		if( iMaxValueState == -1 ){
			initStartStateArray();
			return chooseStartState();
		}
		iState = m_aiStartStates[iMaxValueState];
		m_aiStartStates[iMaxValueState] = -1;
		return iState;
	}


	protected double improveValueFunction(){
		int iInitialState = -1;
		do{
			iInitialState = m_pPOMDP.chooseStartState();
		}while( m_pPOMDP.isTerminalState( iInitialState ) );
		BeliefState bsInitial = m_pPOMDP.getBeliefStateFactory().getInitialBeliefState();
		Logger.getInstance().logln( "Starting at state " + m_pPOMDP.getStateName( iInitialState ) );
		m_iDepth = 0;
		Logger.getInstance().logln( "Begin improve" );
		double dDelta = forwardSearch( iInitialState, bsInitial, 0 );
		Logger.getInstance().logln( "End improve, |V| = " + 
				m_vValueFunction.size() + ", delta = " + dDelta );
		return dDelta;
	}

}
