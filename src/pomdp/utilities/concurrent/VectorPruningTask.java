package pomdp.utilities.concurrent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pomdp.algorithms.PolicyStrategy;
import pomdp.environments.POMDP;
import pomdp.utilities.BeliefState;
import pomdp.utilities.Logger;
import pomdp.valuefunction.LinearValueFunctionApproximation;

public class VectorPruningTask extends Task implements Runnable {

	private PolicyStrategy m_psStrategy;
	private LinearValueFunctionApproximation m_vValueFunction;
	private POMDP m_pPOMDP;
	private int m_cTrials, m_cSteps;
	
	public VectorPruningTask( POMDP pomdp, LinearValueFunctionApproximation vValueFunction ){
		m_pPOMDP = pomdp;
		m_vValueFunction = vValueFunction;
		m_cTrials = 250;
		m_cSteps = 100;
		m_psStrategy = new PolicyWrapper();
		Logger.getInstance().log( "VectorPruningTask", 0, "contructor", "Pruning task initialized" );
	}
	
	
	@Override
	public void copyResults(Task processed) {
		// TODO Auto-generated method stub

	}

	@Override
	public void execute() {
		int iTimeStamp = 0, iSegment = 0, cSegments = 10;
		while( !m_bTerminate ){
			while( ( ( m_vValueFunction.size() < 50 ) || m_vValueFunction.wasPruned() ) && !m_bTerminate ){
				synchronized( this ){
					try {
						wait( 1000 );
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			if( !m_bTerminate ){
				Logger.getInstance().log( "VectorPruningTask", 0, "execute", "Pruning iteration started. |V| = " + m_vValueFunction.size() );
				m_vValueFunction.initHitCounts();
				iTimeStamp = m_vValueFunction.getChangesCount();
				for( iSegment = 0 ; iSegment < cSegments && !m_bTerminate ; iSegment++ ){
					m_pPOMDP.computeAverageDiscountedReward( m_cTrials / cSegments, m_cSteps, m_psStrategy, false, false );
				}
				if( !m_bTerminate && m_vValueFunction.size() > 50 )
					m_vValueFunction.pruneLowHitCountVectors( 0, iTimeStamp );
				Logger.getInstance().log( "VectorPruningTask", 0, "execute", "Pruning iteration done. |V| = " + m_vValueFunction.size() );
			}
		}
		Logger.getInstance().log( "VectorPruningTask", 0, "execute", "Pruning task terminated" );
	}

	@Override
	public Element getDOM(Document doc) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

	private class PolicyWrapper extends PolicyStrategy{

		@Override
		public int getAction(BeliefState bsCurrent) {
			return m_vValueFunction.getBestAction( bsCurrent );
		}

		@Override
		public String getStatus() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double getValue(BeliefState bsCurrent) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public LinearValueFunctionApproximation getValueFunction() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean hasConverged() {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
}
