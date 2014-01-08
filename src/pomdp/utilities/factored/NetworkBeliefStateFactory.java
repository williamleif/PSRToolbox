package pomdp.utilities.factored;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import pomdp.environments.NetworkManagement;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;

public class NetworkBeliefStateFactory extends BeliefStateFactory {

	protected NetworkManagement m_pPOMDP;
	protected int m_cMachines;

	
	public NetworkBeliefStateFactory( NetworkManagement pomdp, int cDiscretizationLevels ){
		super( pomdp, cDiscretizationLevels );
		m_cMachines = pomdp.getMachineCount();
		//m_hmCachedBeliefStates = new TreeMap<BeliefState,BeliefState>( new NetworkBeliefStateComparator( m_cMachines ) );
		m_pPOMDP = pomdp;
	}
	
	//pr(LvR) = pr(L,!R) + pr(!L,R) + pr(L,R)
	protected double probabilityNeighborUp( NetworkBeliefState bs, int iMachine ){
		int[] aiNeighbors = m_pPOMDP.getNeighbors( iMachine );
		if( aiNeighbors[0] == -1 )
			aiNeighbors = m_pPOMDP.getNeighbors( iMachine );
		double dProbL = bs.getMachineWorkingProb( aiNeighbors[0] );
		double dProbR = bs.getMachineWorkingProb( aiNeighbors[1] );
		double dProbLnotR = dProbL * ( 1 - dProbR );
		double dProbRnotL = dProbR * ( 1 - dProbL );
		double dProbLandR = dProbL * dProbR;
		return dProbLnotR + dProbRnotL + dProbLandR;		
	}
/*	
	protected NetworkBeliefState newBeliefState(){
		int id = m_cBeliefPoints;
		if( !m_bCacheBeliefStates )
			id = -1;
		
		NetworkBeliefState bs = new NetworkBeliefState( m_cMachines, m_cStates, m_cActions, 
				m_cObservations, id, 
				m_bSparseBeliefStates, m_bCacheBeliefStates );
		
		return bs;
	}
*/
	/*
	int cNBSFOperations = 0;
	long cNBSFTotalTime = 0;
	
	//pr(o|a,b) = \sum_s b(s) \sum_s' tr(s,a,s')O(a,s',o) - X
	//pr(o|a,b) = \sum_s' O(a,s',o) \sum_s tr(s,a,s')b(s) - V - inefficient
	public double calcNormalizingFactor( BeliefState bs, int iAction, int iObservation ){
		
		if( false )
			return super.calcNormalizingFactor(bs, iAction, iObservation);
		
		long lStartTime = JProf.getCurrentThreadCpuTimeSafe(), lEndTime = 0;
		
		double dProb = 0.0, dO = 0.0, dBelief = 0.0, dTr = 0.0, dSum = 0.0;
		int iStartState = 0, iEndState = 0;
		Iterator itNonZeroBackwardTransitions = null;
		Map.Entry eTransition = null;
		
		for( iEndState = 0 ; iEndState < m_cStates ; iEndState++ ){
			dO = m_pPOMDP.O( iAction, iEndState, iObservation );
			if( dO > 0 ){
				itNonZeroBackwardTransitions = m_pPOMDP.getBackwardTransitionNonZeroEntries( iAction, iEndState );
				dSum = 0.0;
				while( itNonZeroBackwardTransitions.hasNext() ){
					eTransition = (Entry)itNonZeroBackwardTransitions.next();
					iStartState = ((Integer)eTransition.getKey()).intValue();
					dTr = ((Double)eTransition.getValue()).doubleValue();
					dBelief = bs.valueAt( iStartState );
					dSum += dTr * dBelief;
				}
			}
			dProb += dO * dSum;
		}
		
		lEndTime = JProf.getCurrentThreadCpuTimeSafe();
		cNBSFTotalTime += lEndTime - lStartTime;
		cNBSFOperations++;
		
		if( cNBSFOperations == 10 ){
			Logger.getInstance().logln( "NBSF: calcNormalizingFactor ops = " + cOperations + " avg time " + 
					cNBSFTotalTime / (double)cNBSFOperations );
			cNBSFTotalTime = 0;
			cNBSFOperations = 0;
		}
		
		
		//assert 0.0 < dProb && dProb <= 1.0;
		
		return dProb;
	}
	*/
	//b_a,o(s') = O(a,s',o)\sum_s tr(s,a,s')b(s)
	protected double nextBeliefValue( BeliefState bs, int iAction, int iEndState, int iObservation ){
		double dProb = 0.0, dO = 0.0, dTr = 0.0, dBelief = 0.0;
		int iStartState = 0;
		
		dO = m_pPOMDP.O( iAction, iEndState, iObservation );
		if( dO == 0.0 )
			return 0.0;
		
		Iterator it = m_pPOMDP.getBackwardTransitionNonZeroEntries( iAction, iEndState );
		Entry e = null;
		
		while( it.hasNext() ){
			e = (Entry) it.next();
			iStartState = ((Integer) e.getKey()).intValue();
			dTr = ((Double) e.getValue()).doubleValue();
			dBelief = bs.valueAt( iStartState );
			dProb += dTr * dBelief;
		}
		dProb *= dO;
		return dProb;
	}
	/*
	public BeliefState nextBeliefState( BeliefState bs, int iAction, int iObservation ){
		long lTimeBefore = JProf.getCurrentThreadCpuTimeSafe(), lTimeAfter = 0;
		NetworkBeliefState nbs = (NetworkBeliefState)bs;
		NetworkBeliefState nbsNext = newBeliefState();
		int iMachine = 0, iNeighbor = 0;
		int[] aiNeighbors = null;
		double dProbMachineWorking = 0.0, dProbMachineDown = 0.0, dPrevProbMachineWorking = 0.0, dPrevProbNeighborDown = 0.0;
		double dPr1 = 0.0, dPr0 = 0.0;
		for( iMachine = 0 ; iMachine < m_cMachines ; iMachine++ ){
			if( iMachine == iAction ){//restart for current machine
				nbsNext.setMachineWorkingProb( iMachine, 1.0 );
			}
			else{
				dPrevProbMachineWorking = nbs.getMachineWorkingProb( iMachine );
				dPrevProbNeighborDown = probabilityNeighborDown( nbs, iMachine );
				dProbMachineWorking = dPrevProbMachineWorking * 
					( 0.95 * ( 1 - dPrevProbNeighborDown ) + 0.66 * dPrevProbNeighborDown );
				dProbMachineDown = 1 - dProbMachineWorking;
				if( iAction - m_cMachines == iMachine ){ //ping for current machine
					if( iObservation == 1 ){
						//pr(U|1)=pr(1|U)pr(U)/pr(1)
						//pr(1) = pr(1|U)pr(U)+pr(1|D)pr(D)
						dPr1 = 0.95 * dProbMachineWorking + 0.05 * dProbMachineDown;
						dProbMachineWorking = 0.95 * dProbMachineWorking / dPr1;
					}
					else{
						//pr(U|0)=pr(0|U)pr(U)/pr(0)
						//pr(0) = pr(0|U)pr(U)+pr(0|D)pr(D)
						dPr0 = 0.95 * dProbMachineDown + 0.05 * dProbMachineWorking;
						dProbMachineWorking = 0.05 * dProbMachineWorking / dPr0;
					}
				}
				else{//not restart for current machine
					nbsNext.setMachineWorkingProb( iMachine, dProbMachineWorking );
				}
			}
		}
		if( m_bCacheBeliefStates ){
			BeliefState bsExisting = (BeliefState) m_hmCachedBeliefStates.get( nbsNext );
			if( bsExisting == null ){
				m_hmCachedBeliefStates.put( nbsNext, nbsNext );
				m_cBeliefPoints++;
			}
			else{
				nbsNext = (NetworkBeliefState)bsExisting;
			}
					
		}
		
		lTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
		m_cTimeInTau += ( lTimeAfter - lTimeBefore ) / 1000;
		m_cBeliefUpdates++;
		
		return nbsNext;
	}

	public BeliefState getDeterministicBeliefState( int iState ){
		if( m_abDeterministic[iState] != null )
			return m_abDeterministic[iState];
		NetworkBeliefState nbs = newBeliefState();
		boolean[] abWorkingMachines = m_pPOMDP.indexToState( iState );
		int iMachine = 0;
		for( iMachine = 0 ; iMachine < m_cMachines ; iMachine++ ){
			if( abWorkingMachines[iMachine] )
				nbs.setMachineWorkingProb( iMachine, 1.0 );
			else
				nbs.setMachineWorkingProb( iMachine, 0.0 );
		}
		
		NetworkBeliefState nbsExisting = (NetworkBeliefState) m_hmCachedBeliefStates.get( nbs );
		if( nbsExisting == null ){
			m_hmCachedBeliefStates.put( nbs, nbs );
			m_cBeliefPoints++;
		}
		else{
			nbs = nbsExisting;
		}
		m_abDeterministic[iState] = nbs;
		return nbs;
	}
	
	public BeliefState getInitialBeliefState(){
		if( m_bsInitialState == null ){
			m_bsInitialState = newBeliefState();
			m_cBeliefPoints++;
			int iMachine = 0;
			double dProbMachineWorkingAtStart = 0.0;
			for( iMachine = 0 ; iMachine < m_cMachines ; iMachine++ ){
				dProbMachineWorkingAtStart = m_pPOMDP.probMachineWorkingAtStart( iMachine );
				((NetworkBeliefState)m_bsInitialState).setMachineWorkingProb( iMachine, dProbMachineWorkingAtStart );
			}
			m_hmCachedBeliefStates.put( m_bsInitialState, m_bsInitialState );
		}
		return m_bsInitialState;
	}

	BeliefState getUniformBeliefState(){	
		if( m_bsUniform == null ){
			m_bsUniform = getInitialBeliefState();
		}
		return m_bsUniform;
	}
	*/
}
