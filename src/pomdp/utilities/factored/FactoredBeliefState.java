package pomdp.utilities.factored;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.w3c.dom.Element;

import pomdp.environments.FactoredPOMDP;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateComparator;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.JProf;
import pomdp.utilities.Logger;
import pomdp.utilities.datastructures.StaticMap;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class FactoredBeliefState extends BeliefState{

	protected AlgebraicDecisionDiagram m_addProbabilities;
	protected FactoredPOMDP m_pPOMDP;
	protected int m_cStateVariables;
	private Map<String,AlgebraicDecisionDiagram> m_mCachedProducts;
	private StaticMap m_mNonZeroEntries;
	
	private static boolean m_bIndependent = false;
		
	private final static double m_dEpsilon = 0.00001;
	
	public FactoredBeliefState( FactoredPOMDP pPOMDP, int id ){
		super( pPOMDP.getStateCount(), pPOMDP.getActionCount(), pPOMDP.getObservationCount(), id, pPOMDP.getBeliefStateFactory().isCachingBeliefStates(), pPOMDP.getBeliefStateFactory() );
		m_pPOMDP = pPOMDP;
		m_cStateVariables = pPOMDP.getStateVariablesCount();
		m_addProbabilities = null;
		m_mCachedProducts = new HashMap<String,AlgebraicDecisionDiagram>();
	}

	public double valueAt( int iState ){
		boolean[] abState = m_pPOMDP.indexToState( iState );
		return m_addProbabilities.valueAt( abState );
	}

	public void setValueAt( int iState, double dValue ){
		boolean[] abState = m_pPOMDP.indexToState( iState );
		if( m_addProbabilities == null )
			m_addProbabilities = m_pPOMDP.newAlgebraicDecisionDiagram( m_pPOMDP.getStateVariablesCount(), true );
		m_addProbabilities.addPath( abState, (float)dValue );
	}

	public void finalizeBeliefs(){
		m_addProbabilities.finalizePaths( 0.0 );
		m_addProbabilities.reduce();
	}
	
	public double sumProbs() {
		return m_addProbabilities.getValueSum();
	}
	
	protected Comparator<BeliefState> getComparator() {
		return FactoredBeliefStateComparator.getInstance();
	}

	public Collection<Entry<Integer,Double>> getNonZeroEntries(){
		if( m_mNonZeroEntries == null ){
			double[] adValues = new double[m_cStates];
			int iState = 0;
			double dProb = 0.0;
			for( iState = 0 ; iState < m_cStates ; iState++ ){
				dProb = valueAt( iState );
				adValues[iState] = dProb;
			}
			m_mNonZeroEntries = new StaticMap( adValues, 0.0 );
		}
		return m_mNonZeroEntries;
	}
	
	protected static int g_cTau = 0;
	protected static long g_lCurrentTimeInTau = 0;
	protected static long g_lTotalTimeInTau = 0;
	
	public static int getTauComputationCount(){
		return g_cTau;
	}
	
	public static double getAvgTauTime(){
		return g_lTotalTimeInTau / (double)g_cTau;
	}
	
	private String getKey( int iAction, int iObservation ){
		return new String( iAction + "," + iObservation );
	}
	
	public AlgebraicDecisionDiagram getActionObservationProduct( int iAction, int iObservation ){
		String sKey = getKey( iAction, iObservation );
		AlgebraicDecisionDiagram addProduct = m_mCachedProducts.get( sKey ), addAbstracted = null;
		long lTimeBefore = 0, lTimeAfter = 0;
		if( addProduct == null ){
			if( ExecutionProperties.getReportOperationTime() )
				lTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
			if( m_pPOMDP.m_bUseRelevantVariablesOnly ){
				addProduct = m_pPOMDP.relevantActionDiagramProduct( m_addProbabilities, iAction, iObservation, true );
								
				addAbstracted = m_pPOMDP.existentialAbstraction( addProduct, true, true, iAction,  true );
				if( m_pPOMDP.g_bUseIrrelelvant ){
					addProduct = m_pPOMDP.irrelevantActionDiagramProduct( addAbstracted, iAction, true );
					addAbstracted = m_pPOMDP.existentialAbstraction( addProduct, false, true, iAction,  true );					
				}
			}
			else{
				m_addProbabilities.translateVariables( m_pPOMDP.getPreActionVariableExpander() );
				addProduct = m_addProbabilities.product( m_pPOMDP.getCompleteActionDiagram( iAction, iObservation ) );
				m_addProbabilities.translateVariables( m_pPOMDP.getVariableReducer() );
				addAbstracted = m_pPOMDP.existentialAbstraction( addProduct, true );
			}
			double dPrOGivenAandB = addAbstracted.getValueSum();
			if( dPrOGivenAandB < 0.0 || dPrOGivenAandB > 1.01 ){
				//addProduct = m_pPOMDP.relevantActionDiagramProduct( m_addProbabilities, iAction, iObservation, true );			
				Logger.getInstance().logln( Thread.currentThread().getName() + " sum out of range, a = " + iAction + ", o = " + iObservation + ", b = " + getSerialNumber() );
				Logger.getInstance().logError( "FactoredBeliefState", "getActionObservationProduct", "Sum of probabilities for belief state out of range, sum = " + dPrOGivenAandB );
			}
			
			if( dPrOGivenAandB != 0.0 ){
				//long iSizeBefore = addAbstracted.getVertexCount();
				addAbstracted.reduceToMin( m_dEpsilon );
				//long iSizeAfter = addAbstracted.getVertexCount();
				double d = addAbstracted.getValueSum();
				//if( iSizeBefore != iSizeAfter ){
					//Logger.getInstance().logln( "*" );
				//}
				addAbstracted.product( 1 / d );
				addAbstracted.reduce();
				
				if( m_bIndependent ){
					addAbstracted = imposeVariableIndependence( addAbstracted );
				}
				
				
				if( ExecutionProperties.getDebug() ){
					double dSum = addAbstracted.getValueSum();
					if( Math.abs( dSum - 1.0 ) > 0.001 ){
						//Logger.getInstance().logln( addProduct.getTreeString() );
						//Logger.getInstance().logln( addAbstracted.getTreeString() );
						Logger.getInstance().logError( "FactoredBeliefState", "getActionObservationProduct", "corrupted belief ADD, sum = " + addAbstracted.getValueSum() + " ADD = " + addAbstracted );
					}
				}
			}

			m_mCachedProducts.put( sKey, addAbstracted );
			m_aCachedObservationProbabilities[iAction][iObservation] = dPrOGivenAandB;
			
			addProduct.release();
			addProduct = addAbstracted;
			g_cTau++;
			if( ExecutionProperties.getReportOperationTime() ){
				lTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
				//if( iAction != m_cActions - 1 ){
					g_lCurrentTimeInTau += ( lTimeAfter - lTimeBefore ) / 1000;
					g_lTotalTimeInTau += ( lTimeAfter - lTimeBefore ) / 1000;
					/*
					if( g_cTau % 1000 == 0 ){
						Logger.getInstance().log( "FactoredBeliefState", 0, "getActionObservationProduct", "After " + g_cTau + " next BS computations, avg time " + g_lCurrentTimeInTau / 1000 );
						g_lCurrentTimeInTau = 0;
					}
					*/
				//}
			}

		}
		else{
			m_mCachedProducts.remove( sKey );
		}
		return addProduct;
	}
	
	private AlgebraicDecisionDiagram imposeVariableIndependence( AlgebraicDecisionDiagram add ){
		AlgebraicDecisionDiagram addProduct = null, addComponent = null;
		int iComponent = 0;
		for( iComponent = 0 ; iComponent < m_pPOMDP.getIndependentComponentsCount() ; iComponent++ ){
			addComponent = m_pPOMDP.existentialAbstraction( add, iComponent, false );
			if( addProduct == null )
				addProduct = addComponent;
			else
				addProduct = addProduct.product( addComponent );
		}
		return addProduct;
	}

	public void addSuccessor( int iAction, int iObservation, BeliefState bsSuccessor ){
		super.addSuccessor( iAction, iObservation, bsSuccessor );
		String sKey = iAction + "," + iObservation;
		//CompactAlgebraicDecisionDiagram addProduct = m_mCachedProducts.get( sKey );
		//addProduct.clearAll();
		AlgebraicDecisionDiagram addCached = m_mCachedProducts.remove( sKey );
	}

	public int countEntries(){
		int cEntries = 0;
		for( AlgebraicDecisionDiagram addProduct : m_mCachedProducts.values() ){
			//addProduct.approve();
			cEntries += addProduct.getVertexCount();
		}
		//m_addProbabilities.approve();
		cEntries +=  m_addProbabilities.getVertexCount();
		return cEntries;
	}

	public double getObservationProbability( int iAction, int iObservation ){
		double d = m_aCachedObservationProbabilities[iAction][iObservation];
		if( d < 0.0 ){
			getActionObservationProduct( iAction, iObservation );
			d = m_aCachedObservationProbabilities[iAction][iObservation];
		}
		
		return d;
	}
	public void setID( int id ) {
		m_iID = id;
	}
	public void loadBeliefValues( Element eBeliefValues ){
		super.loadBeliefValues( eBeliefValues );
		finalizeBeliefs();
	}
	
	public AlgebraicDecisionDiagram getProbabilitiesADD(){
		return m_addProbabilities;
	}
	
	public String getADDString(){
		return m_addProbabilities.toString();
	}
	public long size(){
		return m_addProbabilities.getVertexCount();
	}
	public BeliefState copy(){
		FactoredBeliefState fbsCopy = new FactoredBeliefState( m_pPOMDP, m_iID );
		fbsCopy.m_addProbabilities = m_addProbabilities.copy();
		fbsCopy.m_mCachedProducts = new TreeMap<String,AlgebraicDecisionDiagram>( m_mCachedProducts );
		fbsCopy.m_aCachedObservationProbabilities = m_aCachedObservationProbabilities.clone();
		return fbsCopy;
	}

	@Override
	public void clearZeroEntries() {
		m_mNonZeroEntries = null;
	}

	@Override
	public Iterator<Entry<Integer, Double>> getDominatingNonZeroEntries() {
		return null;
	}

	@Override
	public int getNonZeroEntriesCount() {
		return 0;
	}

	@Override
	public double[] toArray() {
		return null;
	}
}
