package pomdp.utilities.concurrent;

import java.util.Iterator;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.Pair;
import pomdp.valuefunction.LinearValueFunctionApproximation;

public class ComputeBellmanError extends Task implements Runnable {

	private POMDP m_pPOMDP;
	private LinearValueFunctionApproximation m_vValueFunction;
	private Vector<BeliefState> m_vBeliefs;
	private BeliefState m_bsMaxErrorBelief;
	private double m_dMaxError;
	
	public ComputeBellmanError( POMDP pPOMDP, LinearValueFunctionApproximation vValueFunction ){
		m_vValueFunction = vValueFunction;
		m_pPOMDP = pPOMDP;
		m_vBeliefs = new Vector<BeliefState>();
		m_dMaxError = 0.0;
		m_bsMaxErrorBelief = null;
	}
	
	public void setValueFunction( LinearValueFunctionApproximation vValueFunction ){
		m_vValueFunction = new LinearValueFunctionApproximation( vValueFunction );
	}
	
	public void addBelief( BeliefState bs ){
		m_vBeliefs.add( bs );
	}
	
	public BeliefState getMaxErrorBelief(){
		return m_bsMaxErrorBelief;
	}
	
	public double getMaxError(){
		return m_dMaxError;
	}
	
	public void clearResults(){
		m_dMaxError = 0.0;
		m_bsMaxErrorBelief = null;
	}
	
	@Override
	public void copyResults(Task processed) {
		// TODO Auto-generated method stub

	}

	@Override
	public void execute() {
		long lBefore = System.currentTimeMillis();
		double dError = 0.0;
		for( BeliefState bs : m_vBeliefs ){
			dError = computeBellmanError( bs, m_vValueFunction );
			if( dError > m_dMaxError ){
				m_dMaxError = dError;
				m_bsMaxErrorBelief = bs;
			}
		}
		long lAfter = System.currentTimeMillis();
		//Logger.getInstance().logln( "Finished thread in " + ( lAfter - lBefore ) / 1000.0 );
	}

	@Override
	public Element getDOM(Document doc) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void run() {
		double dError = 0.0;
		for( BeliefState bs : m_vBeliefs ){
			dError = computeBellmanError( bs, m_vValueFunction );
			if( dError > m_dMaxError ){
				m_dMaxError = dError;
				m_bsMaxErrorBelief = bs;
			}
		}
	}
	
	protected double computePotentialActionValue( BeliefState bsCurrent, LinearValueFunctionApproximation vValueFunction, int iAction ){
		double dActionValue = 0.0;
		double dSumProbs = 0.0;
		BeliefState bsNext = null;
		double dNextValue = 0.0, dProb = 1.0;
		Iterator itSuccssessors = bsCurrent.getSortedSuccessors( iAction );
		Pair pEntry = null;
		double dGamma = m_pPOMDP.getDiscountFactor();
		double dImmediateReward = bsCurrent.getActionImmediateReward( iAction );
		if( dImmediateReward == Double.NEGATIVE_INFINITY ){
			dImmediateReward = m_pPOMDP.immediateReward( bsCurrent, iAction );
			bsCurrent.setActionImmediateReward( iAction, dImmediateReward );
		}

		dActionValue = dImmediateReward;
		while( itSuccssessors.hasNext() ){
			pEntry = (Pair) itSuccssessors.next();
			dProb = ((Number) pEntry.second()).doubleValue();
			
			bsNext = (BeliefState) pEntry.first();
			dNextValue = vValueFunction.valueAt( bsNext );
			dSumProbs += dProb;
			dActionValue += dGamma * dProb * dNextValue;
			
		}
		
		bsCurrent.setPotentialActionValue( iAction, dActionValue );
		
		return dActionValue;
	}
	
	public double computeBellmanError( BeliefState bsCurrent, LinearValueFunctionApproximation vValueFunction ){
		double dError = 0.0;
		int iAction = 0, iMaxAction = 0;
		double dActionValue = 0.0, dMaxActionValue = Double.NEGATIVE_INFINITY;
		double dValue = 0.0;
		
		dValue = vValueFunction.valueAt( bsCurrent );
		
		for( iAction = 0 ; iAction < m_pPOMDP.getActionCount() ; iAction++ ){
			dActionValue = computePotentialActionValue( bsCurrent, vValueFunction, iAction );
			dError = dActionValue - dValue;
			bsCurrent.setActionError( iAction, dError );
			if( dActionValue > dMaxActionValue ){
				iMaxAction = iAction;
				dMaxActionValue = dActionValue;
			}
		}	
		dError = dMaxActionValue - dValue;
		
		bsCurrent.setMaxErrorAction( iMaxAction );

		return dError;
	}
}
