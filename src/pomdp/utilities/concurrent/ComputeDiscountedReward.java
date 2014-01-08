package pomdp.utilities.concurrent;

import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import pomdp.algorithms.PolicyStrategy;
import pomdp.algorithms.pointbased.HeuristicSearchValueIteration.ValueFunctionEntry;
import pomdp.environments.POMDP;
import pomdp.environments.POMDP.RewardType;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.Logger;
import pomdp.utilities.RandomGenerator;
import pomdp.valuefunction.LinearValueFunctionApproximation;

public class ComputeDiscountedReward extends Task {

	private int m_cMaxStepsToGoal;
	private double m_dSumDiscountedReward;
	private int m_cTests;
	private LinearValueFunctionApproximation m_vValueFunction;
	
	public ComputeDiscountedReward( POMDP pomdp, int cMaxStepsToTheGoal, 
			LinearValueFunctionApproximation vValueFunction, int cTests ){
		m_pPOMDP = pomdp;
		m_vValueFunction = vValueFunction;
		m_cMaxStepsToGoal = cMaxStepsToTheGoal;
		m_dSumDiscountedReward = 0.0;
		m_cTests = cTests;
	}
	
	public ComputeDiscountedReward( Element eTask, POMDP pomdp ) throws Exception{
		m_pPOMDP = pomdp;
		setId( Integer.parseInt( eTask.getAttribute( "ID" ) ) );
		m_dSumDiscountedReward = Double.parseDouble( eTask.getAttribute( "DiscountedReward" ) );
		m_cMaxStepsToGoal = Integer.parseInt( eTask.getAttribute( "MaxSteps" ) );
		NodeList nl = eTask.getChildNodes();
		if( nl.getLength() > 0 ){
			m_vValueFunction = new LinearValueFunctionApproximation();
			m_vValueFunction.parseDOM( (Element)nl.item( 0 ), m_pPOMDP );
		}
	}

	
	public Element getDOM( Document doc ) throws Exception {
		Element eTask = doc.createElement( "ComputeDiscountedReward" ), eValueFunction = null;
		eTask.setAttribute( "ID", getId() + "" );
		eTask.setAttribute( "DiscountedReward", m_dSumDiscountedReward +"" );
		eTask.setAttribute( "MaxSteps", m_cMaxStepsToGoal +"" );
		if( m_vValueFunction != null ){
			eValueFunction = m_vValueFunction.getDOM( doc );
			eTask.appendChild( eValueFunction );
		}
		return eTask;
	}

	
	public void execute() {
		int iTest = 0;
		double dDiscountedReward = 0.0;
		for( iTest = 0 ; iTest < m_cTests ; iTest++ ){
			dDiscountedReward = computeDiscountedReward();
			m_dSumDiscountedReward += dDiscountedReward;
			if( iTest == 10 )
				if( m_dSumDiscountedReward == 0.0 )
					return;
			if( iTest > 0 && iTest % 10 == 0 )
				Logger.getInstance().log( "." );
		}
	}

	public double computeDiscountedReward(){
		double dDiscountedReward = 0.0, dCurrentReward = 0.0, dDiscountFactor = 1.0;
		int iStep = 0, iAction = 0, iObservation = 0;
		int iState = m_pPOMDP.chooseStartState(), iNextState = 0;
		BeliefState bsCurrentBelief = null, bsNext = null;
		AlphaVector avMax = null;
		boolean bDone = false;
		int cRewards = 0;
		int cSameStates = 0;

		//if( m_pPOMDP.getBeliefStateFactory() == null )
		//	BeliefStateFactory.getInstance( m_pPOMDP );
		bsCurrentBelief = m_pPOMDP.getBeliefStateFactory().getInitialBeliefState().copy();
		
		for( iStep = 0 ; ( iStep < m_cMaxStepsToGoal ) && !bDone ; iStep++ ){
			
			avMax = m_vValueFunction.getMaxAlpha( bsCurrentBelief );
			iAction = avMax.getAction();
			
			iNextState = m_pPOMDP.execute( iAction, iState );
			iObservation = m_pPOMDP.observe( iAction, iNextState );
			
			if( m_pPOMDP.getRewardType() == RewardType.StateAction )
				dCurrentReward = m_pPOMDP.R( iState, iAction ); //R(s,a)
			else if( m_pPOMDP.getRewardType() == RewardType.StateActionState )
				dCurrentReward = m_pPOMDP.R( iState, iAction, iNextState ); //R(s,a,s')
			else if( m_pPOMDP.getRewardType() == RewardType.State )
				dCurrentReward = m_pPOMDP.R( iState ); //R(s)
			dDiscountedReward += dCurrentReward * dDiscountFactor;
			dDiscountFactor *= m_pPOMDP.getDiscountFactor();
			
			if( dCurrentReward > 0 )
				cRewards++;

			bDone = m_pPOMDP.endADR( iNextState, dCurrentReward );
			
			bsNext = bsCurrentBelief.nextBeliefState( iAction, iObservation );
			 
			if( bsNext == null ){
				return dDiscountedReward;
			}
			
			if( iState != iNextState )
				cSameStates = 0;
			else
				cSameStates++;

			iState = iNextState;
			
			//if( bsCurrentBelief.equals( bsNext ) )
			//	bDone = true;
			
			bsCurrentBelief = bsNext;			
		}	
		
		return dDiscountedReward;
	}
	
	public double getDiscountedReward(){
		return m_dSumDiscountedReward;
	}

	
	public void copyResults( Task tProcessed ) {
		m_dSumDiscountedReward = ((ComputeDiscountedReward)tProcessed).getDiscountedReward();		
	}
}
