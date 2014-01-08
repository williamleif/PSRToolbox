package pomdp.utilities.factored;

import java.io.IOException;
import java.util.Comparator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import pomdp.environments.FactoredPOMDP;
import pomdp.environments.POMDP;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.JProf;
import pomdp.utilities.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class IndependenBeliefStateFactory extends BeliefStateFactory {

	private FactoredPOMDP m_pPOMDP;
	
	public IndependenBeliefStateFactory( FactoredPOMDP pomdp ){
		super( pomdp );
		m_pPOMDP = pomdp;
	}

	protected BeliefState newBeliefState(){
		return new IndepandantBeliefState( m_pPOMDP, m_cBeliefPoints );
	}

	protected BeliefState newBeliefState( int id ){
		return new IndepandantBeliefState( m_pPOMDP, id );
	}

	
	public double calcNormalizingFactor( BeliefState bs, int iAction, int iObservation ){
		throw new NotImplementedException();
	}


	public static long g_cNext = 0, g_cTime = 0;
	
			
	public BeliefState getInitialBeliefState(){
		if( m_bsInitialState == null ){
			IndepandantBeliefState bs = (IndepandantBeliefState) newBeliefState();
			m_cBeliefPoints++;
			int iComponent = 0, cComponentVariables = 0, cComponentValues = 0, iValue = 0;
			int cComponents = m_pPOMDP.getIndependentComponentsCount();
			double[] adProbabilities = null;
			for( iComponent = 0 ; iComponent < cComponents ; iComponent++ ){
				cComponentVariables = m_pPOMDP.getIndependentComponentVariables( iComponent ).length;
				cComponentValues = (int) Math.pow( 2, cComponentVariables );
				adProbabilities = new double[cComponentValues];
				for( iValue = 0 ; iValue < cComponentValues ; iValue++ ){
					adProbabilities[iValue] =  m_pPOMDP.getInitialComponentValueProbability( iComponent, iValue );
					bs.setComponentProbability( iComponent, adProbabilities );
				}
			}
			m_bsInitialState = bs;
			m_bsInitialState.setFactoryPersistence( false );
		}
		return m_bsInitialState;
	}
	
	public BeliefState getUniformBeliefState(){	
		if( m_bsUniformState == null ){
			IndepandantBeliefState bs = (IndepandantBeliefState) newBeliefState();
			int iComponent = 0, cComponents = m_pPOMDP.getIndependentComponentsCount();
			for( iComponent = 0 ; iComponent < cComponents ; iComponent++ ){
				bs.setUniformProbability( iComponent );
			}
			m_bsUniformState = bs;
			m_bsUniformState.setFactoryPersistence( false );
		}
		return m_bsUniformState;
	}

	public BeliefState getDeterministicBeliefState( int iState ){
		IndepandantBeliefState bs = (IndepandantBeliefState)newBeliefState();
		bs.setDeterministicState( iState );
		return bs;
	}
}
