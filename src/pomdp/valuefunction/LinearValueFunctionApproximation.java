package pomdp.valuefunction;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import pomdp.algorithms.PolicyStrategy;
import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.Logger;
import pomdp.utilities.RandomGenerator;
import pomdp.utilities.concurrent.DotProduct;
import pomdp.utilities.concurrent.ThreadPool;
import pomdp.utilities.datastructures.LinkedList;

public class LinearValueFunctionApproximation extends PolicyStrategy implements Serializable {
	//protected Vector<AlphaVector> m_vAlphaVectors;
	//protected ArrayList<AlphaVector> m_vAlphaVectorsRead, m_vAlphaVectorsWrite;
	protected LinkedList<AlphaVector> m_vAlphaVectors;
	protected RandomGenerator m_rndGenerator;
	protected int m_cValueFunctionChanges;
	protected double m_dEpsilon;
	protected boolean m_bCacheValues;
	protected double m_dMaxValue;
	private static boolean g_bUseMultithreadInDotProducts = false;
	private boolean m_bEvaluatingPolicy;
	private boolean m_bPruned;
	
	public LinearValueFunctionApproximation( double dEpsilon, boolean bCacheValues ){
		m_vAlphaVectors = new LinkedList<AlphaVector>();
		m_cValueFunctionChanges = 0;
		m_dEpsilon = dEpsilon;
		m_bCacheValues = true;
		m_dMaxValue = 0.0;
		m_bEvaluatingPolicy = false;
		m_bPruned = false;
		m_rndGenerator = new RandomGenerator( "LinearValueFunctionApproximation" );
	}

	public LinearValueFunctionApproximation(){
		this( 1.0, true );
	}

	public LinearValueFunctionApproximation( LinearValueFunctionApproximation vOtherValueFunction ) {
		copy( vOtherValueFunction );
		m_bCacheValues = vOtherValueFunction.m_bCacheValues;
	}
	
	//public void finalize(){
	//	Logger.getInstance().logln( "LinearValueFunctionApproximation finalized" );
	//}
	

	public double valueAt( BeliefState bs ){
		if( m_vAlphaVectors.size() == 0 )
			return Double.NEGATIVE_INFINITY;
		
		double dValue = bs.getMaxValue();
		int iTime = bs.getMaxValueTime(), cValueFunctionChanges = m_cValueFunctionChanges;
		if( ( iTime < cValueFunctionChanges ) || !m_bCacheValues ){
			AlphaVector avMaxAlpha = getMaxAlpha( bs );
			if( avMaxAlpha == null )
				return Double.NEGATIVE_INFINITY;
			dValue = avMaxAlpha.dotProduct( bs );
			if( m_bCacheValues ){
				bs.setMaxValue( dValue, cValueFunctionChanges );
			}
		}
		
		return dValue;
	}

	public AlphaVector getMaxAlpha( BeliefState bs ){
		
		
		int cElements = m_vAlphaVectors.size();
		if( cElements == 0 )
			return null;
		
		AlphaVector avMaxAlpha = bs.getMaxAlpha();
		double dMaxValue = bs.getMaxValue(), dValue = 0.0;
		int iBeliefStateLastCheckTime = bs.getMaxAlphaTime();
		int iCurrentTime = m_cValueFunctionChanges;
		

		
		if( !m_vAlphaVectors.contains( avMaxAlpha ) ){
			avMaxAlpha = null;
			dMaxValue = Double.NEGATIVE_INFINITY;
			iBeliefStateLastCheckTime = -1;
		}

		
		if( g_bUseMultithreadInDotProducts  && ExecutionProperties.useMultiThread() ){
			DotProduct[] m_dpTasks = new DotProduct[m_vAlphaVectors.size()];
			int i = 0;
			for( AlphaVector avCurrent : m_vAlphaVectors ){
				if( !m_bCacheValues || avCurrent.getInsertionTime() > iBeliefStateLastCheckTime ){
					m_dpTasks[i] = new DotProduct( avCurrent, bs );
					ThreadPool.getInstance().addTask( m_dpTasks[i] );
					i++;
				}
			}
			while( i > 0 ){
				i--;
				ThreadPool.getInstance().waitForTask( m_dpTasks[i] );
				dValue = m_dpTasks[i].getResult();
				if( dValue > dMaxValue ){
					dMaxValue = dValue;
					avMaxAlpha = m_dpTasks[i].getAlphaVector();
				}
			}
		}
		

		
		int iInsertionTime = Integer.MAX_VALUE;
		Iterator<AlphaVector> itBackward = m_vAlphaVectors.backwardIterator();
		boolean bDone = false;
		while( itBackward.hasNext() && !bDone ){
			AlphaVector avCurrent = itBackward.next();
			if( avCurrent != null ){
				iInsertionTime = avCurrent.getInsertionTime();
				if( m_bCacheValues && ( iBeliefStateLastCheckTime >= iInsertionTime ) )
					bDone = true;
				/*
				if( avCurrent.getId() == 1334 ){
					Logger.getInstance().logln( bs );
					Logger.getInstance().logln( avCurrent );
				}
				*/
				dValue = avCurrent.dotProduct( bs );
				if( ( dValue > dMaxValue ) || ( ( dValue == dMaxValue ) && ( avMaxAlpha != null ) && ( iInsertionTime > avMaxAlpha.getInsertionTime() )  ) ){
					dMaxValue = dValue;
					avMaxAlpha = avCurrent;
				}
			}
		}
		
		if( avMaxAlpha != null ){
			if( m_bCacheValues ){
				bs.setMaxAlpha( avMaxAlpha, iCurrentTime );
				bs.setMaxValue( dMaxValue, iCurrentTime );
			}
			
			avMaxAlpha.incrementHitCount();
		}
		return avMaxAlpha;
	}

	public int getBestAction( BeliefState bs ){
		AlphaVector avMaxAlpha = getMaxAlpha( bs );
		if( avMaxAlpha == null )
			return -1;
		return avMaxAlpha.getAction();
	}

	public Iterator<AlphaVector> iterator(){
		return m_vAlphaVectors.iterator();
	}

	public AlphaVector elementAt(int iElement ){
		return (AlphaVector) m_vAlphaVectors.get( iElement );
	}
	
	public void startEvaluation(){
		m_bEvaluatingPolicy = true;
	}
	public void endEvaluation(){
		m_bEvaluatingPolicy = false;
	}
	
	public boolean addPrunePointwiseDominated( AlphaVector avNew ){
		BeliefState bsWitness = null;
		double dNewValue = 0.0;

		while( m_bEvaluatingPolicy ){
			try {
				wait( 100 );
			} 
			catch (Exception e) { }
		}

		Iterator<AlphaVector> it = m_vAlphaVectors.iterator();
		AlphaVector avExisting = null;
		while( it.hasNext() ){
			avExisting = it.next();
			if( avExisting.equals( avNew ) || avExisting.dominates( avNew ) ){
				return false;
			}
			else if( avNew.dominates( avExisting ) ){
				it.remove();
			}
		}		
		
		m_bPruned = false;
		
		m_cValueFunctionChanges++;
		addVector( avNew );
		if( m_bCacheValues ){		
			avNew.setInsertionTime( m_cValueFunctionChanges );
			bsWitness = avNew.getWitness();
			if( bsWitness != null ){
				dNewValue = avNew.dotProduct( bsWitness );
				bsWitness.setMaxAlpha( avNew, m_cValueFunctionChanges );
				bsWitness.setMaxValue( dNewValue, m_cValueFunctionChanges );
			}
		}
		
		if( avNew.getMaxValue() > m_dMaxValue )
			m_dMaxValue = avNew.getMaxValue();
		
		return true;
	}
	
	private void addVector( AlphaVector avNew ){
		m_vAlphaVectors.add( avNew );
	}
		
	public void initHitCounts(){
		try{
			for( AlphaVector av : m_vAlphaVectors ){
				av.initHitCount();
			}
		}
		catch( Exception e )
		{
			System.err.println( e + " retrying" );
			initHitCounts();
		}
	}
	
	public void pruneLowHitCountVectors( int cMinimalHitCount ){
		pruneLowHitCountVectors( cMinimalHitCount, Integer.MAX_VALUE );
	}
	
	public void pruneLowHitCountVectors( int cMinimalHitCount, int iMaximalTimeStamp ){
		while( m_bEvaluatingPolicy ){
			try {
				wait( 100 );
			} catch (Exception e) {
			}
		}
		int cPruned = 0, cNew = 0;
		LinkedList<AlphaVector> vAlphaVectorsWrite = new LinkedList<AlphaVector>();
		for( AlphaVector av : m_vAlphaVectors ){
			if( av.getInsertionTime() > iMaximalTimeStamp || av.getHitCount() > cMinimalHitCount ){
				vAlphaVectorsWrite.add( av );
			}
			if( av.getInsertionTime() > iMaximalTimeStamp ){
				cNew++;
			}
			if( av.getHitCount() <= cMinimalHitCount ){
				cPruned++;
			}
		}
		if( vAlphaVectorsWrite.size() > 0 ){
			//Logger.getInstance().logln( "Pruned from " + m_vAlphaVectors.size() + " to " + vAlphaVectorsWrite.size() + ". pruned " + cPruned + ", new vectors " + cNew );
			m_bPruned = true;
			m_vAlphaVectors = vAlphaVectorsWrite;
		}
	}
	
	public boolean wasPruned(){
		return m_bPruned;
	}
	
	public void addBounded( AlphaVector avNew, int cMaxVectors ){
		
		addPrunePointwiseDominated( avNew );
		
		if( m_vAlphaVectors.size() > cMaxVectors ){
			int i = m_rndGenerator.nextInt( m_vAlphaVectors.size() );
			m_vAlphaVectors.remove( i );
		}
	}
	
	public void add( AlphaVector avNew, boolean bPruneDominated ){
		AlphaVector avExisting = null;
		BeliefState bsWitness = null;
		boolean bDominated = false;
		double dPreviousValue = 0.0, dNewValue = 0.0;
		
		m_cValueFunctionChanges++;
		if( bPruneDominated ){
			int iVector = 0;
			for( iVector = 0 ; iVector < m_vAlphaVectors.size() && !bDominated ; iVector++ ){
				avExisting = m_vAlphaVectors.get( iVector );
				if( avNew.dominates( avExisting ) ){
					m_vAlphaVectors.remove( avExisting );
				}
				else if( avExisting.dominates( avNew ) ){
					bDominated = true;
				}
			}
		}
		
		if( !bDominated ){
			m_vAlphaVectors.add( avNew );
		
			if( m_bCacheValues ){		
				avNew.setInsertionTime( m_cValueFunctionChanges );
				bsWitness = avNew.getWitness();
				if( bsWitness != null ){
					dNewValue = avNew.dotProduct( bsWitness );
					bsWitness.setMaxAlpha( avNew, m_cValueFunctionChanges );
					bsWitness.setMaxValue( dNewValue, m_cValueFunctionChanges );
				}
			}
	
			if( avNew.getMaxValue() > m_dMaxValue )
				m_dMaxValue = avNew.getMaxValue();
		}
		
	}


	public void clear() {
		for( AlphaVector av : m_vAlphaVectors ){
			av.release();
		}
		m_vAlphaVectors.clear();
		m_cValueFunctionChanges = 0;
		
	}

	public void addAll( LinearValueFunctionApproximation vOtherValueFunction ){
		m_vAlphaVectors.addAll( vOtherValueFunction.m_vAlphaVectors );
	}

	public int size(){
		return m_vAlphaVectors.count();
	}
	
	public boolean equals( LinearValueFunctionApproximation vOther ){
		return vOther.m_vAlphaVectors.containsAll( m_vAlphaVectors ) && 
			m_vAlphaVectors.containsAll( vOther.m_vAlphaVectors );
	}

	public void copy( LinearValueFunctionApproximation vOtherValueFunction ){
		while( m_bEvaluatingPolicy ){
			try {
				wait( 100 );
			} catch (Exception e) {
			}
		}
		m_vAlphaVectors = new LinkedList<AlphaVector>( vOtherValueFunction.m_vAlphaVectors );
		//m_cValueFunctionChanges = vOtherValueFunction.m_cValueFunctionChanges;
		m_dEpsilon = vOtherValueFunction.m_dEpsilon;
		m_rndGenerator = vOtherValueFunction.m_rndGenerator;
		m_cValueFunctionChanges = vOtherValueFunction.m_cValueFunctionChanges;
		m_dEpsilon = vOtherValueFunction.m_dEpsilon;
		m_bCacheValues = vOtherValueFunction.m_bCacheValues;
		m_dMaxValue = vOtherValueFunction.m_dMaxValue;
		m_bEvaluatingPolicy = vOtherValueFunction.m_bEvaluatingPolicy;
	}

	public void add( AlphaVector avNew ){
		add( avNew, false );
	}

	public void remove( AlphaVector av ){
		m_vAlphaVectors.remove( av );
	}

	public double approximateValueAt( BeliefState bs ){
		if( m_vAlphaVectors.size() == 0 )
			return -1 * Double.MAX_VALUE;
		int iBeliefStateMaxAlphaTime = bs.getApproximateValueTime();
		double dMaxValue = bs.getApproximateValue(), dValue = 0.0;
		AlphaVector avCurrent = null;
		
		if( iBeliefStateMaxAlphaTime < m_cValueFunctionChanges ){
			int iVector = 0;
			for( iVector = 0 ; iVector < m_vAlphaVectors.size() ; iVector++ ){
				avCurrent = m_vAlphaVectors.get( iVector );
				if( avCurrent.getInsertionTime() > iBeliefStateMaxAlphaTime ){
					dValue = avCurrent.approximateDotProduct( bs );
					if( dValue > dMaxValue ){
						dMaxValue = dValue;
					}
				}
			}
			bs.setApproximateValue( dMaxValue, m_cValueFunctionChanges );
		}
		
		return dMaxValue;
	}

	public int getChangesCount() {
		return m_cValueFunctionChanges;
	}
	
	public void setCaching( boolean bCache ){
		m_bCacheValues = bCache;
	}

	public boolean contains( AlphaVector av ){
		return m_vAlphaVectors.contains( av );
	}
	
	public String toString(){
		String sRetVal = "<";
		
		for( AlphaVector av : m_vAlphaVectors ){
			sRetVal += av.toString() + "\n";
		}
		
		return sRetVal;
	}
	
	public double getMaxValue(){
		return m_dMaxValue;
	}

	public AlphaVector getFirst() {
		return m_vAlphaVectors.getFirst();
	}

	public AlphaVector getLast() {
		return m_vAlphaVectors.getLast();
	}
	
	public Element getDOM( Document doc ) throws Exception{
		Element eValueFunction = doc.createElement( "ValueFunction" ), eAlphaVector = null;
		AlphaVector avCurrent = null;
		
		eValueFunction = doc.createElement( "ValueFunction" );
		eValueFunction.setAttribute( "AlphaVectorCount", m_vAlphaVectors.size() + "" );
		eValueFunction.setAttribute( "Epsilon", m_dEpsilon + "" );
		eValueFunction.setAttribute( "CacheValue", m_bCacheValues + "" );
		eValueFunction.setAttribute( "MaxValue", m_dMaxValue + "" );		
		doc.appendChild( eValueFunction );
		
		int iVector = 0;
		for( iVector = 0 ; iVector < m_vAlphaVectors.size() ; iVector++ ){
			avCurrent = m_vAlphaVectors.get( iVector );
			eAlphaVector = avCurrent.getDOM( doc );
			eValueFunction.appendChild( eAlphaVector );
		}
		
		return eValueFunction;
	}
	
	public void save( String sFileName ) throws Exception{
		Document docValueFunction = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element eValueFunction = getDOM( docValueFunction );
		
		// Use a Transformer for output
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		
		DOMSource source = new DOMSource( eValueFunction );
		StreamResult result = new StreamResult( new FileOutputStream( sFileName ) );
		transformer.transform( source, result );
	}
	
	public void parseDOM( Element eValueFunction, POMDP pomdp ) throws Exception{
		Element eVector = null;
		NodeList nlVectors = null;
		int cVectors = 0, iVector = 0;
		AlphaVector avNew = null;
		
		cVectors = Integer.parseInt( eValueFunction.getAttribute( "AlphaVectorCount" ) );
		nlVectors = eValueFunction.getChildNodes();
		
		m_dEpsilon = Double.parseDouble( eValueFunction.getAttribute( "Epsilon" ) );
		m_bCacheValues = Boolean.parseBoolean( eValueFunction.getAttribute( "CacheValue" ) );
		m_dMaxValue = Double.parseDouble( eValueFunction.getAttribute( "MaxValue" ) );

		for( iVector = 0 ; iVector < cVectors ; iVector++ ){
			eVector = (Element)nlVectors.item( iVector );
			avNew = AlphaVector.parseDOM( eVector, pomdp );
			m_vAlphaVectors.add( avNew );
		}
	}
	
	public void load( String sFileName, POMDP pomdp ) throws Exception{
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document docValueFunction = builder.parse( new FileInputStream( sFileName ) );
		Element eValueFunction = (Element)docValueFunction.getChildNodes().item( 0 );
		
		parseDOM( eValueFunction, pomdp );
	}

	
	public void removeFirst() {
		m_vAlphaVectors.remove( 0 );
	}

	public Collection<AlphaVector> getVectors() {
		return m_vAlphaVectors;
	}

	public void setVectors( Vector<AlphaVector> v ) {
		m_vAlphaVectors = new LinkedList<AlphaVector>( v );		
	}

	public int countEntries() {
		AlphaVector avCurrent = null;
		int cEntries = 0;
		
		int iVector = 0;
		for( iVector = 0 ; iVector < m_vAlphaVectors.size() ; iVector++ ){
			avCurrent = m_vAlphaVectors.get( iVector );
			cEntries += avCurrent.countEntries();
		}
		return cEntries;
	}

	public double getAvgAlphaVectorSize() {
		double cNodes = 0;
		for( AlphaVector av : m_vAlphaVectors )
			cNodes += av.countEntries();
		return cNodes / m_vAlphaVectors.size();
	}

	public void pruneTrials( POMDP pPOMDP, int cTrials, int cSteps, PolicyStrategy ps ){
		initHitCounts();
		double dSimulatedADR = pPOMDP.computeAverageDiscountedReward( cTrials, cSteps, ps );
		int cBefore = m_vAlphaVectors.size();
		pruneLowHitCountVectors( 0 );
		Logger.getInstance().logln( "Pruned the lower bound from " + cBefore + " to " + m_vAlphaVectors.size() );
	}

	/* PolicyStrategy Stuff	 */
	
	public int getAction(BeliefState bsCurrent)
	{
		return getBestAction(bsCurrent);
	}
	public double getValue(BeliefState bsCurrent)
	{
		return valueAt(bsCurrent);
	}
	public boolean hasConverged() 
	{
		return false;
	}
	
	public String getStatus()
	{
		return "";
	}

	public LinearValueFunctionApproximation getValueFunction()
	{
		return this;
	}
	
	
	public void valueAtWithAction(BeliefState bs, int action)
	{
		
		
		
	}
	
	
}
