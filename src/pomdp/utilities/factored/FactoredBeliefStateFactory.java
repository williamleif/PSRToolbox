package pomdp.utilities.factored;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import pomdp.environments.FactoredPOMDP;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateComparator;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.JProf;
import pomdp.utilities.Logger;

public class FactoredBeliefStateFactory extends BeliefStateFactory {

	private int m_cStateVariables;
	private FactoredPOMDP m_pPOMDP;
	protected TreeMap<AlgebraicDecisionDiagram,FactoredBeliefState> m_hmCachedBeliefStates;
	protected FactoredBeliefState m_bsInitialState;
	protected FactoredBeliefState m_bsUniformState;

	private static int g_gIDs = -1;
	
	public FactoredBeliefStateFactory( FactoredPOMDP pomdp ){
		super( pomdp );
		m_pPOMDP = pomdp;
		m_cStateVariables = pomdp.getStateVariablesCount();
		m_hmCachedBeliefStates = new TreeMap<AlgebraicDecisionDiagram,FactoredBeliefState>( new AlgebraicDecisionDiagramComparator() );
	}
		
	protected BeliefState newBeliefState(){
		return new FactoredBeliefState( m_pPOMDP, m_cBeliefPoints );
	}

	protected BeliefState newBeliefState( int id ){
		return new FactoredBeliefState( m_pPOMDP, id );
	}

	
	public double calcNormalizingFactor( BeliefState bs, int iAction, int iObservation ){
		if( bs instanceof FactoredBeliefState ){
			FactoredBeliefState fbs = (FactoredBeliefState)bs;
			double dValue = fbs.getObservationProbability( iAction, iObservation ); 
			return dValue;
		}
		return -1;
		
	}


	public static long g_cNext = 0, g_cTime = 0;
	
	public BeliefState nextBeliefState( BeliefState bs, int iAction, int iObservation ){
		long lTimeBefore = 0, lTimeAfter = 0;
		if( bs instanceof FactoredBeliefState ){
			FactoredBeliefState fbs = (FactoredBeliefState)bs;
			FactoredBeliefState bsNext = null;
			
			if( m_bCountBeliefUpdates )
				m_cBeliefUpdates++;
			
			if( ExecutionProperties.getReportOperationTime() ){
				lTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
			}
			
			AlgebraicDecisionDiagram addProbabilities = fbs.getActionObservationProduct( iAction, iObservation );
			
			//if( Math.abs( addProbabilities.getValueSum() - 1.0 ) > 0.1 )
			//	Logger.getInstance().log( "FactoredBeliefStateFactory", 0, "nextBeliefState", "corrupted belief state - sum = " + addProbabilities.getValueSum() + " bs = " + addProbabilities );
			if( m_bCacheBeliefStates ){
				if( m_hmCachedBeliefStates.containsKey( addProbabilities ) ){
					bsNext = m_hmCachedBeliefStates.get( addProbabilities );
				}
				else{
					bsNext = (FactoredBeliefState) newBeliefState();
					bsNext.m_addProbabilities = addProbabilities;
					/*
					Logger.getInstance().log( "FactoredBeliefStateFactory", 0, "nextBeliefState", 
							"Tau( " + bs.getId() + ", " + iAction + ", " + iObservation + " ) = " + bsNext.getId() + 
							", |B| = " + m_hmCachedBeliefStates.size() );
							*/
					cacheBeliefState( bsNext );
					bsNext.setFactoryPersistence( true );
					m_cBeliefPoints++;
				}
			}
			else{
				bsNext = (FactoredBeliefState) newBeliefState();
				bsNext.m_addProbabilities = addProbabilities.copy();
				bsNext.setFactoryPersistence( false );
				bsNext.setID( -1 );
				g_cNext++;
				if( ExecutionProperties.getReportOperationTime() ){
					m_cBeliefStateSize += bsNext.size();
					/*
					if( g_cNext % 1000 == 0 ){
						Logger.getInstance().log( "FactoredBeliefStateFactory", 0, "nextBeliefState" ,"After " + g_cNext + " next BS computations, avg belief state size " + m_cBeliefStateSize / 1000 );
						m_cBeliefStateSize = 0;
					}
					*/
				}
			}
			return bsNext;
		}
		else{
			Logger.getInstance().logError( "FactoredBeliefStateFactory", "nextBeliefState", "Attempting to compute a factored belief state out of a flat belief state" );
		}
		return null;
	}
		
	protected class AlgebraicDecisionDiagramComparator implements Comparator<AlgebraicDecisionDiagram>{
		public int compare( AlgebraicDecisionDiagram o1, AlgebraicDecisionDiagram o2 ){
			return o1.compareTo( o2 );
		}
	}
	
	public BeliefState getInitialBeliefState(){
		if( m_bsInitialState == null ){
			FactoredBeliefState bsInitialState = (FactoredBeliefState) newBeliefState();
			m_cBeliefPoints++;
			int iState = 0, cStates = (int)Math.pow( 2, m_cStateVariables );
			double dSum = 0.0, dPr = 0.0;
			for( iState = 0 ; iState < cStates ; iState++ ){
				dPr =  m_pPOMDP.probStartState( iState );
				if( dPr > 0.0 )
					bsInitialState.setValueAt( iState, dPr );
				dSum += dPr;
			}
			bsInitialState.m_addProbabilities.finalizePaths( 0 );
			bsInitialState.m_addProbabilities.reduce();
			m_bsInitialState = bsInitialState;
			cacheBeliefState( m_bsInitialState );
			m_bsInitialState.setFactoryPersistence( true );
			if( dSum < 0.99 || dSum > 1.001 ){
				Logger.getInstance().log( "FactoredBeliefStateFactory", 0, "getInitialBeliefState", "Probability sum is " + dSum );
			}
		}
		return m_bsInitialState;
	}
	
	public BeliefState getUniformBeliefState(){	
		if( m_bsUniformState == null ){
			int cStates = m_pPOMDP.getStateCount();
			double dUnifomValue = 1.0 / cStates;
			m_bsUniformState = (FactoredBeliefState) newBeliefState();
			m_bsUniformState.m_addProbabilities = m_pPOMDP.newAlgebraicDecisionDiagram( m_pPOMDP.getStateVariablesCount(), true );
			m_bsUniformState.m_addProbabilities.finalizePaths( (float)dUnifomValue );

			FactoredBeliefState bsExisting = m_hmCachedBeliefStates.get( m_bsUniformState.m_addProbabilities );
			if( bsExisting == null ){
				cacheBeliefState( m_bsUniformState );
				m_bsUniformState.setFactoryPersistence( true );
				m_cBeliefPoints++;
			}
			else{
				m_bsUniformState = bsExisting;
			}
		
		}
		return m_bsUniformState;
	}

	public BeliefState getDeterministicBeliefState( int iState ){
		if( m_bCacheDeterministicBeliefStates ){
			if( m_abDeterministic == null )
				m_abDeterministic = new BeliefState[m_pPOMDP.getStateCount()];
			if( m_abDeterministic[iState] != null )
				return m_abDeterministic[iState];
		}
		FactoredBeliefState bs = (FactoredBeliefState)newBeliefState();
		bs.setValueAt( iState, 1.0 );
		bs.m_addProbabilities.finalizePaths( 0 );
		bs.m_addProbabilities.reduce();
		if( m_bCacheDeterministicBeliefStates ){
			FactoredBeliefState bsExisting =  m_hmCachedBeliefStates.get( bs.m_addProbabilities );
			if( ( bsExisting == null ) && isCachingBeliefStates() ){
				//Logger.getInstance().log( "FactoredBeliefStateFactory", 11, "getDeterministicBeliefState", bs.toString() );
				cacheBeliefState( bs );
				bs.setFactoryPersistence( true );
				m_cBeliefPoints++;
			}
			else if( bsExisting != null ){
				bs = bsExisting;
			}
			m_abDeterministic[iState] = bs;
		}
		return bs;
	}

	public int getBeliefStateCount(){
		return m_hmCachedBeliefStates.size();
	}
	
	public int countEntries() {
		int cEntries = 0;
		for( BeliefState bs : m_hmCachedBeliefStates.values() ){
			cEntries += bs.countEntries();
		}
		return cEntries;
	}
	
	protected void cacheBeliefState( BeliefState bs ){
		FactoredBeliefState fbs = (FactoredBeliefState)bs;
		m_hmCachedBeliefStates.put( fbs.m_addProbabilities, fbs );
	}
	
	protected Comparator<BeliefState> getBeliefStateComparator( double dEpsilon ) {
		return FactoredBeliefStateComparator.getInstance( dEpsilon );
	}

	public void saveBeliefSpace( String sFileName ) throws IOException, TransformerException, ParserConfigurationException{
		Collection<FactoredBeliefState> colOriginal = m_hmCachedBeliefStates.values();
		Vector<BeliefState> vRevised = new Vector<BeliefState>();
		for( FactoredBeliefState fbs : colOriginal )
			vRevised.add( fbs );
		save( sFileName, vRevised );
	}
}
