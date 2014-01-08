package pomdp.utilities;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import pomdp.algorithms.PolicyStrategy;
import pomdp.algorithms.ValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.datastructures.Heap;
import pomdp.utilities.datastructures.PriorityQueueElement;
import pomdp.valuefunction.LinearValueFunctionApproximation;
import pomdp.valuefunction.MDPValueFunction;

public class ValueFunctionEvaluation {
	
	private LinearValueFunctionApproximation m_vLowerBound;
	private MDPValueFunction m_vUpperBound;
	private Heap m_hLeaves;
	private double m_dRMax, m_dRMin, m_dDiscount;
	private POMDP m_pPOMDP;
	private int m_iMaxDepth;
	private double m_dMaxProbability;
	private double m_dProbFiniteState;
	
	public ValueFunctionEvaluation( POMDP pomdp, LinearValueFunctionApproximation vLowerBound, MDPValueFunction vUpperBound ){
		m_vLowerBound = vLowerBound;
		m_vUpperBound = vUpperBound;
		m_pPOMDP = pomdp;
		m_hLeaves = null;
		m_dRMax = m_pPOMDP.getMaxR();
		m_dRMin = m_pPOMDP.getMinR();
		m_dDiscount = m_pPOMDP.getDiscountFactor();
		m_iMaxDepth = 0;
		m_dMaxProbability = 0.0;
	}
	
	private void expand(){
		PolicyTreeNode nCurrent = (PolicyTreeNode) m_hLeaves.extractMax(), nNew = null;
		BeliefState bsCurrent = nCurrent.getBelief(), bsNext = null;
		int iAction = m_vLowerBound.getBestAction( bsCurrent ), iObservation = -1;
		double dProbability = nCurrent.getProbability(), dPrOGivenBsAndA = 0.0;
		double dPreviousReward = nCurrent.getDiscountedReward(), dImmediateReward = 0.0, dNewReward = 0.0;
		int iDepth = nCurrent.getDepth();
		Map<BeliefState,Double> mNext = new TreeMap<BeliefState, Double>( BeliefStateComparator.getInstance() );
		for( iObservation = 0 ; iObservation < m_pPOMDP.getObservationCount() ; iObservation++ ){
			dPrOGivenBsAndA = bsCurrent.probabilityOGivenA( iAction, iObservation );
			if( dPrOGivenBsAndA > 0.0 ){
				bsNext = bsCurrent.nextBeliefState( iAction, iObservation );
				if( mNext.containsKey( bsNext ) ){
					mNext.put( bsNext, mNext.get( bsNext ) + dPrOGivenBsAndA );
				}
				else{
					mNext.put( bsNext, dPrOGivenBsAndA );
				}
			}
		}
		for( Entry<BeliefState, Double> e : mNext.entrySet() ){
			dPrOGivenBsAndA = e.getValue();
			bsNext = e.getKey();
			dImmediateReward = m_pPOMDP.R( bsCurrent, iAction, bsNext );
			if( dImmediateReward > 0.0 )
				dNewReward = dPreviousReward + Math.pow( m_dDiscount, iDepth ) * dImmediateReward;
			else
				dNewReward = dPreviousReward;
			nNew = new PolicyTreeNode( bsNext, dProbability * dPrOGivenBsAndA, dNewReward, iDepth + 1 );
			m_hLeaves.insert( nNew );
		}
	}
	
	public double[] computeValues(){
		Iterator<PriorityQueueElement> it = m_hLeaves.iterator();
		PolicyTreeNode nCurrent = null;
		BeliefState bsCurrent = null;
		double dProbability = 0.0, dDiscountedReward = 0.0, dLowerBound = 0.0, dUpperBound = 0.0;
		double dMaxRForever = m_dRMax / ( 1 - m_dDiscount );
		double dMinRForever = m_dRMin / ( 1 - m_dDiscount );
		m_dProbFiniteState = 0.0;
		while( it.hasNext() ){
			nCurrent = (PolicyTreeNode) it.next();
			dProbability = nCurrent.getProbability();
			dDiscountedReward += dProbability * nCurrent.getDiscountedReward();
			bsCurrent = nCurrent.getBelief();
			if( bsCurrent.isDeterministic() && m_pPOMDP.isTerminalState( bsCurrent.getDeterministicIndex() ) ){
				m_dProbFiniteState += dProbability;
			}
			//dLowerBound += dProbability * Math.pow( m_dDiscount, nCurrent.getDepth() + 1 ) * dMinRForever;
			//dUpperBound += dProbability * Math.pow( m_dDiscount, nCurrent.getDepth() + 1 ) * dMaxRForever;
			dLowerBound += dProbability * Math.pow( m_dDiscount, nCurrent.getDepth() ) * nCurrent.getLowerBound();
			dUpperBound += dProbability * Math.pow( m_dDiscount, nCurrent.getDepth() ) * nCurrent.getUpperBound();
			if( nCurrent.getDepth() + 1 > m_iMaxDepth )
				m_iMaxDepth = nCurrent.getDepth() + 1;
			if( dProbability > m_dMaxProbability )
				m_dMaxProbability = dProbability;
		}
		return new double[]{ dLowerBound, dDiscountedReward, dUpperBound };
	}
	
	public double[] evaluate(){
		double[] aEvaluation = new double[3];//low, evaluation, high
		m_hLeaves = new Heap();
		PolicyTreeNode nInitial = null;
		BeliefState bsInitial = m_pPOMDP.getBeliefStateFactory().getInitialBeliefState();
		m_iMaxDepth = 0;

		nInitial = new PolicyTreeNode( bsInitial, 1.0, 0.0, 0 );
		m_hLeaves.insert( nInitial );
		
		for( int i = 0 ; i < 10000 ; i++ ){
			expand();
			if( i % 1000 == 0 ){
				aEvaluation = computeValues();
				Logger.getInstance().logln( i + ") LB " + aEvaluation[0] + ", DR " + aEvaluation[1] + ", UB " + aEvaluation[2] + ", d " + m_iMaxDepth + ", pr " + m_dMaxProbability + ", |H| " + m_hLeaves.size() + " prEnd " + m_dProbFiniteState );
				m_dMaxProbability = 0.0;
			}
		}
		
		return computeValues();
	}
	
	private class PolicyTreeNode extends PriorityQueueElement{
		private BeliefState m_bsCurrent;
		private double m_dProbability;
		private int m_iDepth, m_iAction;
		private double m_dDiscountedReward;
		private double m_dLowerBound, m_dUpperBound;
		
		public PolicyTreeNode( BeliefState bsCurrent, double dProbability, double dDiscountedReward, int iDepth ){
			m_dProbability = dProbability;
			m_bsCurrent = bsCurrent;
			m_iDepth = iDepth;
			m_iAction = m_vLowerBound.getBestAction( bsCurrent );
			m_dDiscountedReward = dDiscountedReward;
			m_dLowerBound = m_vLowerBound.valueAt( bsCurrent );
			m_dUpperBound = m_vUpperBound.getValue( bsCurrent );
		}		
		public int getDepth() {
			return m_iDepth;
		}
		public double getDiscountedReward() {
			return m_dDiscountedReward;
		}
		public BeliefState getBelief(){
			return m_bsCurrent;
		}
		public double getProbability(){
			return m_dProbability;
		}
		public int getAction(){
			return m_iAction;
		}
		public double getPriority(){
			//return m_dProbability * Math.pow( m_dDiscount, m_iDepth ) * ( m_dUpperBound - m_dLowerBound );
			//return m_dProbability * Math.pow( m_dDiscount, m_iDepth ) * m_dLowerBound;
			return m_dProbability * m_dLowerBound;
		}
		public double getLowerBound(){
			return m_dLowerBound;
		}
		public double getUpperBound(){
			return m_dUpperBound;
		}
	}
}
