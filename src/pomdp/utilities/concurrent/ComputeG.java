package pomdp.utilities.concurrent;

import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.Logger;
import pomdp.utilities.datastructures.LinkedList;
import pomdp.valuefunction.LinearValueFunctionApproximation;

public class ComputeG extends Task implements Runnable {
	private int m_iAction;
	private int m_cObservations;
	private BeliefState m_bsBelief;
	private LinearValueFunctionApproximation m_vValueFunction;
	private AlphaVector m_avG;
	
	public ComputeG( BeliefState bs, LinearValueFunctionApproximation vValueFunction, int iAction, int cObservations ){
		m_iAction = iAction;
		m_cObservations = cObservations;
		m_bsBelief = bs;
		m_vValueFunction = vValueFunction;
		m_avG = null;
	}
	
	protected AlphaVector G(){
		AlphaVector avMax = null, avG = null, avSum = null, avMaxOriginal = null;;
		int iObservation = 0, iState = 0;
		LinkedList<AlphaVector> vVectors = new LinkedList<AlphaVector>( m_vValueFunction.getVectors() );
		double dMaxValue = Double.NEGATIVE_INFINITY, dValue = 0, dProb = 0.0, dSumProbs = 0.0;

		for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
			dProb = m_bsBelief.probabilityOGivenA( m_iAction, iObservation );
			dSumProbs += dProb;
			if( dProb > 0.0 ){
				dMaxValue = Double.NEGATIVE_INFINITY;
				//argmax_i g^i_a,o \cdot b
				for( AlphaVector avAlpha : vVectors ){
					avG = avAlpha.G( m_iAction, iObservation );
					
					dValue = avG.dotProduct( m_bsBelief );
					if( ( avMax == null ) || ( dValue > dMaxValue ) ){
						dMaxValue = dValue;
						avMax = avG;
						avMaxOriginal = avAlpha;
					}
				}
			}
			else{
				dMaxValue = 0.0;
				avMaxOriginal = m_vValueFunction.getLast(); 
				avMax = avMaxOriginal.G( m_iAction, iObservation );
			}

			if( avSum == null ){
				avSum = avMax.copy(); 
			}
			else if( avMax != null ){
				avSum.accumulate( avMax ); 
			}
			avMax = null;
		}
		
		
		AlphaVector avResult = avSum.addReward( m_iAction );
		avResult.setAction( m_iAction );

		avSum.release();
		return avResult;
	}
	
	public void run() {
		m_avG = G();		
	}

	public void execute() {
		m_avG = G();		
	}

	public AlphaVector getG(){
		return m_avG;
	}

	@Override
	public void copyResults(Task tProcessed) {
		m_avG = ((ComputeG)tProcessed).getG();		
	}

	@Override
	public Element getDOM(Document doc) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
