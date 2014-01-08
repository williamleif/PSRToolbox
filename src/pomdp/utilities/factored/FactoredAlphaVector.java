package pomdp.utilities.factored;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.org.apache.bcel.internal.generic.ISTORE;

import pomdp.environments.FactoredPOMDP;
import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.JProf;
import pomdp.utilities.Logger;
import pomdp.utilities.TabularAlphaVector;
import pomdp.utilities.concurrent.Lock;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import pomdp.utilities.factored.AlgebraicDecisionDiagram.VariableTranslator;

public class FactoredAlphaVector extends AlphaVector {

	public AlgebraicDecisionDiagram m_addValues;
	private int m_cStateVariables;
	private FactoredPOMDP m_pPOMDP;
	private static double g_dEpsilon = 0.05;
	
	private boolean m_bAbstracted = true;
	
	public FactoredAlphaVector( BeliefState bsWitness, int iAction, int cStateVariables, FactoredPOMDP pomdp ){
		super( bsWitness, iAction, pomdp );
		m_pPOMDP = pomdp;
		m_addValues = null;
		m_cStateVariables = cStateVariables;
	}

	public FactoredAlphaVector( FactoredPOMDP pomdp ){
		this( null, -1, pomdp.getStateVariablesCount(), pomdp );
	}

	public double valueAt( int iState ){
		if( m_addValues == null )
			return 0.0;
		boolean[] abState = m_pPOMDP.indexToState( iState );
		return m_addValues.valueAt( abState );
	}

	public void setValue( int iState, double dValue ){
		boolean[] abState = m_pPOMDP.indexToState( iState );
		if( m_addValues == null )
			m_addValues = m_pPOMDP.newAlgebraicDecisionDiagram( m_cStateVariables, true );
		m_addValues.addPath( abState, (float)dValue );
	}

	public void accumulate( AlphaVector av ){
		if( av instanceof FactoredAlphaVector ){
			AlgebraicDecisionDiagram addNewValues = m_addValues.sum( ((FactoredAlphaVector)av).m_addValues );
			m_addValues.release();
			m_addValues = addNewValues;
		}
		else
			Logger.getInstance().log( "FactoredAlphaVector", 0, "accumulate",  "received regular alpha vector" );
	}

	public Iterator<Entry<Integer,Double>> getNonZeroEntries() {
		//Logger.getInstance().log( "FactoredAlphaVector", 0, "getNonZeroEntries",  "not implemented" );
		//return null;
		//return m_addValues.getNonZeroEntries();
		throw new NotImplementedException();
	}

	public void finalizeValues() {
		//Logger.getInstance().logln( "before: " + m_addValues.getTreeString() );
		if( m_addValues == null )
			m_addValues = m_pPOMDP.newAlgebraicDecisionDiagram( m_cStateVariables, true );
		m_addValues.finalizePaths( 0 );
		m_addValues.reduceToMin( g_dEpsilon );
		//Logger.getInstance().logln( "after: " + m_addValues.getTreeString() );
	}

	public AlphaVector newAlphaVector() {
		return new FactoredAlphaVector( null, -1, m_cStateVariables, m_pPOMDP );
	}

	public int getNonZeroEntriesCount() {
		Logger.getInstance().log( "FactoredAlphaVector", 0, "getNonZeroEntriesCount",  "not implemented" );
		return 0;
	}
	
	private static int g_cGs = 0;
		
	protected AlphaVector computeG( int iAction, int iObservation ){
		FactoredAlphaVector avG = new FactoredAlphaVector( null, iAction, m_cStateVariables, m_pPOMDP );

		AlgebraicDecisionDiagram addProduct = null, addAbstracted = null;

		if( m_pPOMDP.m_bUseRelevantVariablesOnly ){
			//synchronized(lock){
				addProduct = m_pPOMDP.relevantActionDiagramProduct( m_addValues, iAction, iObservation, false );
			//}
			addAbstracted = m_pPOMDP.existentialAbstraction( addProduct, true, false, iAction,  true );
		}
		else{
			m_addValues.translateVariables( m_pPOMDP.getPostActionVariableExpander() );
			addProduct = m_addValues.product( m_pPOMDP.getCompleteActionDiagram( iAction, iObservation ) );
			m_addValues.translateVariables( m_pPOMDP.getVariableReducer() );
			addAbstracted = m_pPOMDP.existentialAbstraction( addProduct, false );
		}
		addAbstracted.reduceToMin( g_dEpsilon );
		avG.m_addValues = addAbstracted;
		
		g_cGs++;

		return avG;
	}


	public AlphaVector copy(){
		FactoredAlphaVector avCopy = new FactoredAlphaVector( m_bsWitness, m_iAction, m_cStateVariables, m_pPOMDP );
		avCopy.m_addValues = m_addValues.copy();
		return avCopy;
	}
	
	private static long g_cDotProduct = 0;
	private static long g_lTimeInDotProduct = 0;
	
	public double dotProductMax( BeliefState bs, double dMaxValue ){
		double dValue = 0.0;
		if( bs.isDeterministic() ){
			dValue = valueAt( bs.getDeterministicIndex() );
		}
		else if( bs instanceof FactoredBeliefState ){
			//dValue = m_addValues.innerProduct( ((FactoredBeliefState)bs).m_addProbabilities, dMaxValue );
		}
		if( dValue > dMaxValue )
			return dValue;
		return dMaxValue;
	}			
	
	public double dotProduct( BeliefState bs ){
		if( bs.isDeterministic() ){
			return valueAt( bs.getDeterministicIndex() );
		}
		s_cDotProducts++;
		s_cCurrentDotProducts++;
		long lBefore = 0, lAfter = 0;
		double dValue = 0.0;
		if( ExecutionProperties.getReportOperationTime() ){
			lBefore = JProf.getCurrentThreadCpuTimeSafe();
		}
		if( bs instanceof FactoredBeliefState ){
			FactoredBeliefState fbs = (FactoredBeliefState)bs;
			long lTimeBefore = 0, lTimeAfter = 0;
			if( ExecutionProperties.getReportOperationTime() )
				lTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
			
			if( !m_bAbstracted ){
				fbs.m_addProbabilities.translateVariables( m_pPOMDP.getPostActionVariableExpander() );
			}
			
			dValue = m_addValues.innerProduct( fbs.m_addProbabilities );	
						
			if( !m_bAbstracted )
				fbs.m_addProbabilities.translateVariables( m_pPOMDP.getVariableReducer() );
		}
		else if( bs instanceof IndepandantBeliefState ){
			IndepandantBeliefState ibs = (IndepandantBeliefState)bs;
			dValue = ibs.innerProduct( this );
			
		}
		else if( bs instanceof ModifiedRockSampleBeliefState ){
			ModifiedRockSampleBeliefState mbs = (ModifiedRockSampleBeliefState)bs;
			double[] adVariableProbabilities = mbs.getVariableProbabilities();
			dValue = m_addValues.innerProduct( adVariableProbabilities );
		}
		else if( bs instanceof PathProbabilityEstimator ){
			PathProbabilityEstimator pbe = (PathProbabilityEstimator)bs;
			dValue = m_addValues.innerProduct( pbe );
		}
		else{
			Iterator<Entry<Integer, Double>> itNonZero = bs.getNonZeroEntries().iterator();
			Entry<Integer, Double> e = null;
			while( itNonZero.hasNext() ){
				e = itNonZero.next();
				dValue += valueAt( e.getKey() ) * e.getValue();
			}
		}
		if( ExecutionProperties.getReportOperationTime() ){
			lAfter = JProf.getCurrentThreadCpuTimeSafe();
			s_cCurrentTimeInDotProduct += ( lAfter - lBefore ) / 1000;
			s_cTotalTimeInDotProduct += ( lAfter - lBefore ) / 1000;
			/*
			if( s_cCurrentDotProducts == ( TIME_INTERVAL * 1000 ) ){
				String sMsg = "After " + s_cDotProducts + " dot product - avg time = " + s_cCurrentTimeInDotProduct / ( TIME_INTERVAL * 1000 );
				s_cCurrentTimeInDotProduct = 0;
				s_cCurrentDotProducts = 0;
				Logger.getInstance().log( "FactoredAlphaVector", 0, "dotProduct", sMsg );
			}
			*/
		}
			
		return dValue;
	}
	
	public AlphaVector addReward( int iAction ){
		FactoredAlphaVector avResult = (FactoredAlphaVector) newAlphaVector();
		avResult.m_addValues = m_addValues.copy();
		avResult.m_addValues.product( (float)m_pPOMDP.getDiscountFactor() );
		AlgebraicDecisionDiagram addSum = avResult.m_addValues.sum( m_pPOMDP.getReward( iAction ) );
		avResult.m_addValues.release();
		avResult.m_addValues = addSum;
		avResult.finalizeValues();
		return avResult;
	}
	
	public void setAllValues( double dValue ){
		if( m_addValues == null )
			m_addValues = m_pPOMDP.newAlgebraicDecisionDiagram( m_cStateVariables, true );
		m_addValues.finalizePaths( (float)dValue );
	}
	
	public boolean dominates( AlphaVector avOther ){
		if( avOther instanceof FactoredAlphaVector ){
			return m_addValues.dominates( ((FactoredAlphaVector)avOther).m_addValues );
		}
		return super.dominates( avOther );
	}
	
	public boolean equals( AlphaVector avOther ){
		if( avOther instanceof FactoredAlphaVector ){
			return m_addValues.equals( ((FactoredAlphaVector)avOther).m_addValues );
		}
		return super.equals( avOther );
	}

	public long countLocalEntries(){
		//m_addValues.approve();
		return m_addValues.getVertexCount();
	}
	public void release(){
		m_addValues.release();
		m_addValues = null;
		super.release();
	}

	public void parseValuesXML( Element eFunction ){
		m_addValues = m_pPOMDP.newAlgebraicDecisionDiagram( m_cStateVariables, true );
		m_addValues.parseXML( (Element)eFunction.getFirstChild() );
	}
	public Element getDOM( Document doc ){
		Element eVector = doc.createElement( "AlphaVector" );
		eVector.setAttribute( "Id", m_iID + "" );
		eVector.setAttribute( "Action", m_iAction + "" );
		eVector.setAttribute( "Type", "Factored" );
		eVector.appendChild( m_addValues.getDOM( doc ) );
		return eVector;
	}
	
	public long size() {
		return m_addValues.getVertexCount();
	}

	
	public void setSize( int cStates ) {
		//no need to do anything here???
		//Perhaps I should set here the number of variables?
	}
	
	public void detach() {
		super.detach();
		m_pPOMDP = null;
	}

	public void attach( POMDP pomdp ) {
		m_pPOMDP = (FactoredPOMDP) pomdp;
		m_cStates = m_pPOMDP.getStateCount();
		m_cActions = m_pPOMDP.getActionCount();
		m_cObservations = m_pPOMDP.getObservationCount();
		m_cStateVariables = m_pPOMDP.getStateVariablesCount();
		m_aCachedG = new AlphaVector[m_cActions][m_cObservations];
	}

	public void translate( VariableTranslator vt ){
		m_addValues.translateVariables( vt );
	}

	public void assumeWorstCase( Vector<Integer> vSpecifiedVariables ){
		Vector<Integer> vUnspecifiedVariables = new Vector<Integer>();
		int iStateVariable = 0;
		for( iStateVariable = 0 ; iStateVariable < m_cStateVariables ; iStateVariable++ ){
			if( !vSpecifiedVariables.contains( iStateVariable ) )
				vUnspecifiedVariables.add( iStateVariable );
		}
		m_addValues.setUnspecifiedVariablesToWorstCase( vUnspecifiedVariables );
	}

	public void combine( FactoredAlphaVector favNew ) {
		m_addValues = m_addValues.max( favNew.m_addValues );
	}

}
