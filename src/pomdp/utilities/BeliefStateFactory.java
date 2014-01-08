package pomdp.utilities;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pomdp.environments.POMDP;
import pomdp.utilities.distance.DistanceMetric;
import pomdp.utilities.distance.L1Distance;

public class BeliefStateFactory{

	protected POMDP m_pPOMDP;
	public int m_cBeliefUpdates = 0;
	protected TreeMap<BeliefState,BeliefState> m_hmCachedBeliefStates;
	protected int m_cDiscretizationLevels;
	protected int m_cBeliefPoints;
	protected boolean m_bCacheBeliefStates;
	protected boolean m_bSparseBeliefStates = true;
	protected BeliefState m_bsInitialState;
	public static double m_dEpsilon = 0.000000001;
	protected static boolean m_bCountBeliefUpdates;
	protected BeliefState m_bsUniformState;
	protected BeliefState[] m_abDeterministic;
	protected RandomGenerator m_rndGenerator = new RandomGenerator( "BeliefStateFactory" );

	protected boolean m_bCacheDeterministicBeliefStates = false;

	public int m_cTimeInTau;
	public long m_cBeliefStateSize;

	public BeliefStateFactory( POMDP pomdp, int cDiscretizationLevels ){
		m_pPOMDP = pomdp;
		m_cDiscretizationLevels = cDiscretizationLevels;
		init();
	}

	private void init(){
		m_hmCachedBeliefStates = new TreeMap<BeliefState,BeliefState>(getBeliefStateComparator(m_dEpsilon));
		m_cBeliefPoints = 0;

		BeliefState.g_cBeliefStateUpdates = 0;
		m_bsInitialState = null;
		m_bCountBeliefUpdates = true;
		m_bsUniformState = null;
		m_cTimeInTau = 0;
		m_abDeterministic = null;
		m_cBeliefStateSize = 0;

		// caching beliefs
		m_bCacheBeliefStates = true;
		//m_bCacheBeliefStates = false;

	}

	public BeliefStateFactory( POMDP pomdp ){
		this( pomdp, -1 );
	}

	protected Comparator<BeliefState> getBeliefStateComparator( double dEpsilon ) {
		return BeliefStateComparator.getInstance( dEpsilon );
	}

	//b_a,o(s') = O(a,s',o)\sum_s tr(s,a,s')b(s)
	protected double nextBeliefValue( BeliefState bs, int iAction, int iEndState, int iObservation ){
		double dProb = 0.0, dO = 0.0, dTr = 0.0, dBelief = 0.0;
		int iStartState = 0;

		Logger.getInstance().log( "BeliefStateFactory", 11, "nextBeliefValue", " s' = " + iEndState );

		dO = m_pPOMDP.O( iAction, iEndState, iObservation );

		//Logger.getInstance().log( "BeliefStateFactory", 11, "nextBeliefValue", " O(a,s',o) = " + dO );

		if( dO == 0.0 )
			return 0.0;

		Collection<Entry<Integer,Double>> colBSNonZero = bs.getNonZeroEntries();
		Collection<Entry<Integer,Double>> colBackwardTransitions = null;//m_pPOMDP.getNonZeroBackwardTransitions( iAction, iEndState );

		if( colBackwardTransitions == null || colBSNonZero.size() < colBackwardTransitions.size() ){
			for( Entry<Integer, Double> e : colBSNonZero ){
				iStartState = e.getKey();
				dBelief = e.getValue();
				dTr = m_pPOMDP.tr( iStartState, iAction, iEndState );
				dProb += dTr * dBelief;
			}
		}
		else{
			for( Entry<Integer, Double> e : colBackwardTransitions ){
				iStartState = e.getKey();
				dTr = e.getValue();
				dBelief = bs.valueAt( iStartState );
				dProb += dTr * dBelief;
			}
		}

		dProb *= dO;
		return dProb;
	}

	//int cOperations = 0;
	//long cTotalTime = 0;

	//pr(o|a,b) = \sum_s b(s) \sum_s' tr(s,a,s')O(a,s',o)
	public double calcNormalizingFactor( BeliefState bs, int iAction, int iObservation ){

		//long lStartTime = JProf.getCurrentThreadCpuTimeSafe(), lEndTime = 0;

		double dProb = 0.0, dO = 0.0, dBelief = 0.0, dTr = 0.0, dSum = 0.0;
		int iStartState = 0, iEndState = 0;
		Iterator<Entry<Integer,Double>> itNonZeroTransitions = null;
		Iterator<Entry<Integer,Double>> itNonZeroBeliefs = bs.getNonZeroEntries().iterator();
		Map.Entry<Integer,Double> eTransition = null, eBelief = null;

		//for( iStartState = 0 ; iStartState < m_cStates ; iStartState++ ){
		while( itNonZeroBeliefs.hasNext() ){
			eBelief = itNonZeroBeliefs.next();
			iStartState = (eBelief.getKey()).intValue();
			dBelief = (eBelief.getValue()).doubleValue();
			//dBelief = bs.valueAt( iStartState );
			dSum = 0.0;
			itNonZeroTransitions = m_pPOMDP.getNonZeroTransitions( iStartState, iAction );
			while( itNonZeroTransitions.hasNext() ){
				eTransition = itNonZeroTransitions.next();
				iEndState = (eTransition.getKey()).intValue();
				dTr = (eTransition.getValue()).doubleValue();
				dO = m_pPOMDP.O( iAction, iEndState, iObservation );
				dSum += dO * dTr;
			}
			dProb += dSum * dBelief;
		}

		/*
		lEndTime = JProf.getCurrentThreadCpuTimeSafe();
		cTotalTime += lEndTime - lStartTime;
		cOperations++;

		if( cOperations == 10 ){
			Logger.getInstance().logln( "calcNormalizingFactor ops = " + cOperations + " avg time " +
					cTotalTime / (double)cOperations );
			cTotalTime = 0;
			cOperations = 0;
		}*/

		//assert 0.0 < dProb && dProb <= 1.0;

		return dProb;
	}

	private static long g_cNext = 0, g_cTime = 0;

	/**
	 * Computes the next belief state given the current belief state, and action and an observation
	 * @param bs - current belief state
	 * @param iAction - action
	 * @param iObservation - observation
	 * @return next belief state
	 */
	public BeliefState nextBeliefState( BeliefState bs, int iAction, int iObservation ){
		try{
			BeliefState bsNext = newBeliefState();

			double dNormalizingFactor = 0.0, dNextValue = 0.0, dSum = 0.0, dBelief = 0.0, dTr = 0.0, dOb = 0.0;
			int iEndState = 0, iStartState = 0;
			int cStates = m_pPOMDP.getStateCount();
			Collection<Entry<Integer, Double>> cNonZeroBeliefs = bs.getNonZeroEntries();
			Iterator<Entry<Integer, Double>> itNonZeroTransitions = null;
			Entry<Integer, Double> eTr = null;

			long lTimeBefore = 0, lTimeAfter = 0;

			if( ExecutionProperties.getReportOperationTime() )
				lTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
			if( m_bCountBeliefUpdates )
				m_cBeliefUpdates++;

			dNormalizingFactor = 0.0;

			if( cNonZeroBeliefs.size() > m_pPOMDP.getStateCount() / 2.0 ){	//sparse beliefs
				for( iEndState = 0 ; iEndState < cStates ; iEndState++ ){
					dNextValue = nextBeliefValue( bs, iAction, iEndState, iObservation );
					bsNext.setValueAt( iEndState, dNextValue );
					dNormalizingFactor += dNextValue;
				}
			}
			else
			{
				for( Entry<Integer, Double> eBelief : cNonZeroBeliefs ){ //dense beliefs
					iStartState = eBelief.getKey();
					dBelief = eBelief.getValue();
					itNonZeroTransitions = m_pPOMDP.getNonZeroTransitions( iStartState, iAction );
					while( itNonZeroTransitions.hasNext() ){
						eTr = itNonZeroTransitions.next();
						iEndState = eTr.getKey();
						dTr = eTr.getValue();
						dOb = m_pPOMDP.O( iAction, iEndState, iObservation );
						if( dOb > 0.0 ){
							dNextValue = bsNext.valueAt( iEndState );
							bsNext.setValueAt( iEndState, dNextValue + dBelief * dTr * dOb );
							dNormalizingFactor += dBelief * dTr * dOb;
						}
					}
				}
			}

			bs.setProbabilityOGivenA( iAction, iObservation, dNormalizingFactor );

			if( dNormalizingFactor == 0.0 ){
				return null;
			}



			Iterator itNonZeroEntries = bsNext.getNonZeroEntries().iterator();
			Map.Entry e = null;
			while( itNonZeroEntries.hasNext() ){
				e = (Entry) itNonZeroEntries.next();
				iEndState = ((Number) e.getKey()).intValue();
				dNextValue = ((Number) e.getValue()).doubleValue();
				bsNext.setValueAt( iEndState, dNextValue / dNormalizingFactor );
				dSum += dNextValue / dNormalizingFactor;
			}

			g_cNext++;
			if( ExecutionProperties.getReportOperationTime() ){
				lTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
				g_cTime += ( lTimeAfter - lTimeBefore ) / 1000;
				m_cBeliefStateSize += bsNext.size();
				/*
				if( g_cNext % 1000 == 0 ){
					Logger.getInstance().log( "BeliefStateFactory", 0, "nextBeliefState" ,"After " + g_cNext + " next BS computations, avg time " + g_cTime / 1000 + ", avg belief state size " + m_cBeliefStateSize / 1000 );
					g_cTime = 0;
					m_cBeliefStateSize = 0;
				}
				 */
			}

			if( m_bCacheBeliefStates ){
				BeliefState bsExisting = m_hmCachedBeliefStates.get( bsNext );
				if( bsExisting == null ){
					//Logger.getInstance().log( "BeliefStateFactory", 0, "nextBeliefState",
					//		"Tau( " + bs.getId() + ", " + iAction + ", " + iObservation + " ) = " + bsNext.toString() );
					cacheBeliefState( bsNext );
					m_cBeliefPoints++;
				}
				else{
					bsNext = bsExisting;
					if( bsNext == null )
						Logger.getInstance().logln( "***" );
				}

				if( bsNext != bs )
					bsNext.addPredecessor( bs, dNormalizingFactor, iAction );
			}
			if( ExecutionProperties.getReportOperationTime() && m_bCountBeliefUpdates ){
				lTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
				m_cTimeInTau += ( lTimeAfter - lTimeBefore ) / 1000;
			}

			if( bsNext == null )
				Logger.getInstance().logln( "****" );

			return bsNext;
		}
		catch( Error err ){
			Runtime rtRuntime = Runtime.getRuntime();
			Logger.getInstance().logln( "|BeliefSpace| " + m_cBeliefPoints + ", " + err +
					" allocated " + ( rtRuntime.totalMemory() - rtRuntime.freeMemory() ) / 1000000 +
					" free " + rtRuntime.freeMemory() / 1000000 +
					" max " + rtRuntime.maxMemory() / 1000000 );

			err.printStackTrace();
			System.exit( 0 );
		}
		return null;
	}

	protected synchronized void cacheBeliefState( BeliefState bs ){
		m_hmCachedBeliefStates.put( bs, bs );
	}

	public BeliefState getInitialBeliefState(){
		if( m_bsInitialState == null ){
			BeliefState bsInitial = newBeliefState();
			m_cBeliefPoints++;
			int iState = 0, cStates = m_pPOMDP.getStateCount();
			double dSum = 0.0, dValue = 0.0;
			Logger.getInstance().logln(m_pPOMDP.probStartState(0));
			for(iState = 0 ;iState < cStates; iState++){
				dValue = m_pPOMDP.probStartState( iState );
				bsInitial.setValueAt( iState, dValue );
				dSum += dValue;
			}
			if( dSum < 0.99999 || dSum > 1.000001 )
				Logger.getInstance().log( "BeliefStateFactory", 0, "getInitialBeliefState", "Corrupted initial belief state " + m_bsInitialState.toString() );
			cacheBeliefState( bsInitial );
			m_bsInitialState = bsInitial;
			Logger.getInstance().log( "BeliefStateFactory", 11, "getInitialBeliefState", m_bsInitialState.toString() );
		}
		return m_bsInitialState;
	}

	public int getBeliefUpdatesCount(){
		return m_cBeliefUpdates;
	}

	public BeliefState getDeterministicBeliefState( int iState ){
		if( m_bCacheDeterministicBeliefStates ){
			if( m_abDeterministic == null )
				m_abDeterministic = new BeliefState[m_pPOMDP.getStateCount()];
			if( m_abDeterministic[iState] != null )
				return m_abDeterministic[iState];
		}
		BeliefState bs = newBeliefState();
		bs.setValueAt( iState, 1.0 );
		bs.getNonZeroEntries();
		if( m_bCacheDeterministicBeliefStates ){
			BeliefState bsExisting = m_hmCachedBeliefStates.get( bs );
			if( ( bsExisting == null ) && isCachingBeliefStates() ){
				//cacheBeliefState( bs );
				Logger.getInstance().log( "BeliefStateFactory", 11, "getDeterministicBeliefState", bs.toString() );
				m_cBeliefPoints++;
			}
			else if( bsExisting != null ){
				bs = bsExisting;
			}
			m_abDeterministic[iState] = bs;
		}
		return bs;
	}

	protected int roundToNearest( double dNumber ){
		int iNumber = (int) dNumber;
		double dDiff = dNumber - iNumber;
		if( dDiff > 0.5 )
			return iNumber + 1;
		return iNumber;
	}

	public void setDiscretizationLevels( int cDiscretizationLevels ){
		m_cDiscretizationLevels = cDiscretizationLevels;
	}

	public int getDiscretizationLevels(){
		return m_cDiscretizationLevels;
	}

	protected double discretize( double dPr, int cDiscretizationLevels ){
		double dScaledPr = dPr * cDiscretizationLevels;
		int iPr = roundToNearest( dScaledPr );
		return iPr / cDiscretizationLevels;
	}


	public BeliefState discretize( BeliefState bs ){
		return discretize( bs, m_cDiscretizationLevels );
	}

	public BeliefState discretize( BeliefState bs, int cDiscretizationLevels ){
		BeliefState bsDiscretized = newBeliefState();
		int iState = 0;
		double dPr = 0.0, dDiscPr = 0.0;
		Iterator it = bs.getNonZeroEntries().iterator();
		Map.Entry e = null;

		while( it.hasNext() ){
			e = (Map.Entry)it.next();
			iState = ((Integer) e.getKey()).intValue();
			dPr = ((Double) e.getValue()).doubleValue();
			dDiscPr = discretize( dPr, cDiscretizationLevels );
			bsDiscretized.setValueAt( iState, dDiscPr );
		}

		//bsDiscretized.normalize(); - no need for normalization as this is not really a belief point - only used for storing
		BeliefState bsExisting = m_hmCachedBeliefStates.get( bsDiscretized );
		if( bsExisting == null ){
			cacheBeliefState( bsDiscretized );
			m_cBeliefPoints++;
		}
		else{
			bsDiscretized = bsExisting;
		}
		return bsDiscretized;
	}

	public int getBeliefStateCount(){
		return m_hmCachedBeliefStates.size();
	}

	public boolean cacheBeliefStates( boolean bCache ){
		boolean bFormerValue = m_bCacheBeliefStates;
		m_bCacheBeliefStates = bCache;
		return bFormerValue;
	}

	public boolean isCachingBeliefStates(){
		return m_bCacheBeliefStates;
	}

	protected String toString( Map m ){
		Iterator it = m.entrySet().iterator();
		Map.Entry e = null;
		String sRetVal = "";

		while( it.hasNext() ){
			e = (Map.Entry)it.next();
			if( e.getKey() != e.getValue() ){
				Logger.getInstance().logln( "Key Value Mismatch" );
			}
			sRetVal += e.getKey();
		}

		return sRetVal;
	}

	public void saveBeliefSpace( String sFileName ) throws IOException, TransformerException, ParserConfigurationException{
		save( sFileName, m_hmCachedBeliefStates.keySet() );
	}

	public void saveBeliefPoints( String sFileName, Vector<BeliefState> vBeliefPoints ) throws IOException, TransformerException, ParserConfigurationException{
		save( sFileName, vBeliefPoints );
	}

	protected void save( String sFileName, Collection<BeliefState> colPoints ) throws IOException, TransformerException, ParserConfigurationException{

		Logger.getInstance().logln( "Saving Belief space to " + sFileName );

		Document docBeliefSpace = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element eBeliefSpace = null, eBeliefState = null;
		Iterator<BeliefState> itBeliefSpace = null;
		BeliefState bsCurrent = null;

		eBeliefSpace = docBeliefSpace.createElement( "BeliefSpace" );
		eBeliefSpace.setAttribute( "size", colPoints.size() + "" );
		docBeliefSpace.appendChild( eBeliefSpace );

		itBeliefSpace = colPoints.iterator();
		while( itBeliefSpace.hasNext() ){
			bsCurrent = itBeliefSpace.next();
			eBeliefState = bsCurrent.getDOM( docBeliefSpace );
			eBeliefSpace.appendChild( eBeliefState );
		}

		// Use a Transformer for output
		TransformerFactory tFactory =
			TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();

		DOMSource source = new DOMSource( docBeliefSpace );
		StreamResult result = new StreamResult( new FileOutputStream( sFileName ) );
		transformer.transform( source, result );
	}

	/**
	 * Loads a set of belief points from the xml file specified by sFileName.
	 * Before loading, clears the belief state factory
	 */
	public Vector<BeliefState> loadBeliefSpace( String sFileName ) throws IOException, TransformerException, ParserConfigurationException, SAXException{
		return load( sFileName );
	}

	protected Vector<BeliefState> load( String sFileName ) throws IOException, ParserConfigurationException, SAXException{

		Logger.getInstance().logln( "Loading Belief space from " + sFileName );

		Runtime rtRuntime = Runtime.getRuntime();

		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document docBeliefSpace = builder.parse( new FileInputStream( sFileName ) );
		Element eBeliefSpace = null, eBeliefState = null, eChild = null;
		int iBeliefStateId = 0;
		NodeList nlBeliefStates = null, nlChildren = null;
		int cBeliefStates = 0, iBeliefState = 0, iChild = 0;
		BeliefState bsCurrent = null;
		Vector<BeliefState> vBeliefPoints = new Vector<BeliefState>();
		Map<Integer, BeliefState> mId2BeliefState = new HashMap<Integer, BeliefState>();

		eBeliefSpace = (Element)docBeliefSpace.getChildNodes().item( 0 );
		cBeliefStates = Integer.parseInt( eBeliefSpace.getAttribute( "size" ) );
		m_cBeliefPoints = cBeliefStates;

		nlBeliefStates = eBeliefSpace.getChildNodes();

		for( iBeliefState = 0 ; iBeliefState < cBeliefStates ; iBeliefState++ ){
			eBeliefState = (Element)nlBeliefStates.item( iBeliefState );
			iBeliefStateId = Integer.parseInt( eBeliefState.getAttribute( "Id" ) );
			if( iBeliefStateId >= m_cBeliefPoints )
				m_cBeliefPoints = iBeliefStateId + 1;
			bsCurrent = newBeliefState( iBeliefStateId );

			nlChildren = eBeliefState.getChildNodes();
			for( iChild = 0 ; iChild < nlChildren.getLength() ; iChild++ ){
				eChild = (Element)nlChildren.item( iChild );
				if( eChild.getNodeName().equals( "BeliefValues" ) )
					bsCurrent.loadBeliefValues( eChild );
			}
			vBeliefPoints.add( bsCurrent );

			mId2BeliefState.put( new Integer( iBeliefStateId ), bsCurrent );

			cacheBeliefState( bsCurrent );

			if( ( cBeliefStates > 100 ) && ( iBeliefState > 0 ) && ( iBeliefState % ( cBeliefStates / 10 ) ) == 0 ){
				Logger.getInstance().logln( "Loaded " + iBeliefState + " belief states" +
						" max memory " + rtRuntime.maxMemory() / 1000000 +
						" actual memory " + ( rtRuntime.totalMemory() - rtRuntime.freeMemory() ) / 1000000 +
						" free memory " + rtRuntime.freeMemory() / 1000000 );
			}
		}

		for( iBeliefState = 0 ; iBeliefState < cBeliefStates ; iBeliefState++ ){
			eBeliefState = (Element)nlBeliefStates.item( iBeliefState );
			iBeliefStateId = Integer.parseInt( eBeliefState.getAttribute( "Id" ) );
			bsCurrent = mId2BeliefState.get( new Integer( iBeliefStateId ) );

			nlChildren = eBeliefState.getChildNodes();
			for( iChild = 0 ; iChild < nlChildren.getLength() ; iChild++ ){
				eChild = (Element)nlChildren.item( iChild );
				if( eChild.getNodeName().equals( "Predecessors" ) )
					bsCurrent.loadPredecessors( eChild, mId2BeliefState );
				if( eChild.getNodeName().equals( "Successors" ) )
					bsCurrent.loadSuccessors( eChild, mId2BeliefState );
			}

			mId2BeliefState.put( new Integer( iBeliefStateId ), bsCurrent );
		}

		Logger.getInstance().logln( "Done loading belief space" +
				" max memory " + rtRuntime.maxMemory() / 1000000 +
				" actual memory " + ( rtRuntime.totalMemory() - rtRuntime.freeMemory() ) / 1000000 +
				" free memory " + rtRuntime.freeMemory() / 1000000 );

		return vBeliefPoints;
	}

	public Iterator<BeliefState> getAllBeliefStates() {
		return m_hmCachedBeliefStates.keySet().iterator();
	}

	public void countBeliefUpdates( boolean bCount ){
		BeliefState.countBeliefUpdates( bCount );
		m_bCountBeliefUpdates = bCount;
	}

	public void clearBeliefUpdateCount() {
		BeliefState.clearBeliefStatsUpdate();
		m_cBeliefUpdates = 0;
	}

	public void clearInternalBeliefStateCache(){
		Iterator<BeliefState> itBeliefPoints = m_hmCachedBeliefStates.keySet().iterator();
		BeliefState bs = null;
		while( itBeliefPoints.hasNext() ){
			bs = itBeliefPoints.next();
			bs.clearInternalCache();
		}
	}

	public void computeNeighbors( double dMaxDistance, DistanceMetric dmDistance ){
		Vector<BeliefState> vBeliefStates = new Vector<BeliefState>( m_hmCachedBeliefStates.keySet() );
		computeNeighbors( vBeliefStates, dMaxDistance, dmDistance );
	}

	public static void computeNeighbors( Vector<BeliefState> vBeliefStates, double dMaxDistance, DistanceMetric dmDistance ){
		int cElements = vBeliefStates.size(), iElement1 = 0, iElement2 = 0;
		BeliefState bs1 = null, bs2 = null;
		int cNeighbors = 0;
		double dDistance = 0.0;

		Logger.getInstance().logln( "Started computing belief state neighbors" );

		for( iElement1 = 0 ; iElement1 < cElements ; iElement1++ ){
			bs1 = vBeliefStates.elementAt( iElement1 );
			for( iElement2 = iElement1 + 1 ; iElement2 < cElements ; iElement2++ ){
				bs2 = vBeliefStates.elementAt( iElement2 );
				dDistance = dmDistance.distance( bs1, bs2 );
				if( dDistance < dMaxDistance ){
					bs1.addNeighbor( bs2 );
					bs2.addNeighbor( bs1 );
				}
			}
			if( iElement1 % ( cElements / 10 ) == 0 ){
				Logger.getInstance().logln( "Done " + iElement1 + " belief states" );
			}
		}
		for( iElement1 = 0 ; iElement1 < cElements ; iElement1++ ){
			bs1 = vBeliefStates.elementAt( iElement1 );
			cNeighbors += bs1.getNeighborsCount();
		}
		Logger.getInstance().logln( "Done computing belief state neighbors. avg " + cNeighbors / cElements );
	}

	public BeliefState getUniformBeliefState(){
		if( m_bsUniformState == null ){
			int iState = 0, cStates = m_pPOMDP.getStateCount();
			double dUnifomValue = 1.0 / cStates;
			m_bsUniformState = newBeliefState();
			for( iState = 0 ; iState < cStates ; iState++ )
				m_bsUniformState.setValueAt( iState, dUnifomValue );

			BeliefState bsExisting = m_hmCachedBeliefStates.get( m_bsUniformState );
			if( bsExisting == null ){
				//cacheBeliefState( m_bsUniformState );
				m_cBeliefPoints++;
			}
			else{
				m_bsUniformState = bsExisting;
			}

		}
		return m_bsUniformState;
	}

	public double distance( Collection<BeliefState> vBeliefStates, BeliefState bs ){
		Iterator<BeliefState> it = vBeliefStates.iterator();
		double dDist = 0.0, dMinDist = 10000.0;
		BeliefState bsCurrent = null;
		DistanceMetric dmDistance = L1Distance.getInstance();

		while( it.hasNext() ){
			bsCurrent = (BeliefState) it.next();
			dDist = dmDistance.distance( bs, bsCurrent );
			if( dDist < dMinDist )
				dMinDist = dDist;
		}
		return dMinDist;
	}

	/***
	 * Gets
	 * @param vBeliefStates the set of beliefs to test
	 * @param bs
	 * @return the nearest L1 Belief
	 */
	public BeliefState getNearestL1Belief(Collection<BeliefState> vBeliefStates, BeliefState bs){
		Iterator<BeliefState> it = vBeliefStates.iterator();
		double dDist;
		double dMinDist = Double.POSITIVE_INFINITY;
		BeliefState bsCurrent = null;
		BeliefState bsClosest = null;
		DistanceMetric dmDistance = L1Distance.getInstance();

		while(it.hasNext()){
			bsCurrent = it.next();
			dDist = dmDistance.distance(bs, bsCurrent);
			if(dDist < dMinDist) {
				dMinDist = dDist;
				bsClosest = bsCurrent;
			}
		}
		return bsClosest;
	}



	public BeliefState computeFarthestSuccessorFull(Collection<BeliefState> vBeliefPoints, BeliefState bs){

		int cActions = m_pPOMDP.getActionCount(), cObservations = m_pPOMDP.getObservationCount();
		int iAction = 0, iObservation = 0;
		BeliefState bsMaxDist = null, bsNext = null;
		double dMaxDist = 0.0, dDist = 0.0, dOb = 0.0;


		for( iAction = 0 ; iAction < cActions ; iAction++ ){


			for( iObservation = 0 ; iObservation < cObservations ; iObservation++ ){
				dOb = bs.probabilityOGivenA( iAction, iObservation );
				if( dOb > 0.0 ){
					bsNext = bs.nextBeliefState( iAction, iObservation );
					if( bsNext != null ){
						dDist = distance( vBeliefPoints, bsNext );
						//Logger.getInstance().logln( "Distance " + vBeliefPoints + ", " + bsNext + " = " + dDist );
						if( dDist > dMaxDist ){
							//Logger.getInstance().logln( "New maximal" + dDist );
							dMaxDist = dDist;
							bsMaxDist = bsNext;
						}
					}
				}
			}
		}

		if( dMaxDist == 0.0 )
			return null;
		return bsMaxDist;
		
		
	}
	public BeliefState computeFarthestSuccessor(Collection<BeliefState> vBeliefPoints, BeliefState bs){
		int cActions = m_pPOMDP.getActionCount(), cObservations = m_pPOMDP.getObservationCount();
		int iAction = 0, iObservation = 0;
		BeliefState bsMaxDist = null, bsNext = null;
		double dMaxDist = 0.0, dDist = 0.0, dOb = 0.0;

		for( iAction = 0 ; iAction < cActions ; iAction++ ){

			//Instead of iterating through observations, we sample			
			//for( iObservation = 0 ; iObservation < cObservations ; iObservation++ ){
			iObservation = m_pPOMDP.observe(bs, iAction);

			bsNext = bs.nextBeliefState( iAction, iObservation );
			if( bsNext != null ){
				dDist = distance( vBeliefPoints, bsNext );
				//Logger.getInstance().logln( "Distance " + vBeliefPoints + ", " + bsNext + " = " + dDist );
				if( dDist > dMaxDist ){
					//Logger.getInstance().logln( "New maximal" + dDist );
					dMaxDist = dDist;
					bsMaxDist = bsNext;
				}
			}
		}

		if( dMaxDist == 0.0 )
			return null;
		return bsMaxDist;
	}

	
	
	public BeliefState computeRandomFarthestSuccessor( Vector<BeliefState> vBeliefPoints, BeliefState bs ){
		
		int cObservations = m_pPOMDP.getObservationCount();
		int iAction = 0, iObservation = 0;
		BeliefState bsMaxDist = null, bsNext = null;
		double dMaxDist = 0.0, dDist = 0.0, dOb = 0.0;

		//Logger.getInstance().logln( "expand " + vCurrent + ", " + bs );

		iAction = m_rndGenerator.nextInt( m_pPOMDP.getActionCount() );
		for( iObservation = 0 ; iObservation < cObservations ; iObservation++ ){
			dOb = bs.probabilityOGivenA( iAction, iObservation );
			if( dOb > 0.0 ){
				bsNext = bs.nextBeliefState( iAction, iObservation );
				if( bsNext != null ){
					dDist = distance( vBeliefPoints, bsNext );
					//Logger.getInstance().logln( "Distance " + vBeliefPoints + ", " + bsNext + " = " + dDist );
					if( dDist > dMaxDist ){
						//Logger.getInstance().logln( "New maximal" );
						dMaxDist = dDist;
						bsMaxDist = bsNext;
					}
				}
			}
		}
		if( dMaxDist == 0.0 )
			return null;
		return bsMaxDist;
	}

	public BeliefState computeFarthestSuccessor( Vector vBeliefPoints ){
		BeliefState bsMaxDist = null, bsNext = null;
		double dMaxDist = 0.0, dDist = 0.0;
		BeliefState bsCurrent = null;
		Iterator it = vBeliefPoints.iterator();
		//Logger.getInstance().logln( "expand " + vCurrent + ", " + bs );
		while( it.hasNext() ){
			bsCurrent = (BeliefState) it.next();
			bsNext = computeFarthestSuccessor( vBeliefPoints, bsCurrent );
			if( bsNext != null ){
				dDist = distance( vBeliefPoints, bsNext );
				//Logger.getInstance().logln( "Distance " + vCurrent + ", " + bsNext + " = " + round( dDist, 2 ) );
				if( dDist > dMaxDist ){
					//Logger.getInstance().logln( "New maximal" );
					dMaxDist = dDist;
					bsMaxDist = bsNext;
				}
			}
		}
		if( dMaxDist == 0.0 )
			return null;
		return bsMaxDist;
	}

	protected BeliefState newBeliefState(){
		return newBeliefState( m_cBeliefPoints );
	}

	protected BeliefState newBeliefState( int id ){
		if( !m_bCacheBeliefStates )
			id = -1;

		BeliefState bs = new TabularBeliefState( m_pPOMDP.getStateCount(), m_pPOMDP.getActionCount(),
				m_pPOMDP.getObservationCount(), id,
				m_bSparseBeliefStates, m_bCacheBeliefStates, this );

		return bs;
	}

	public BeliefState newBeliefState( double[] adBelief ){
		BeliefState bs = newBeliefState( -1 );
		int iState = 0;
		for( iState = 0 ; iState < m_pPOMDP.getStateCount() ; iState++ ){
			bs.setValueAt( iState, adBelief[iState] );
		}
		bs.finalizeBeliefs();

		return bs;
	}

	public BeliefState getBeliefState( Map mEntries ){
		BeliefState bs = newBeliefState();
		Iterator itEntries = mEntries.entrySet().iterator();
		Entry eEntry = null;
		Integer iState = null;
		Double dBelief = null;
		double dSumBeliefs = 0.0;

		while( itEntries.hasNext() ){
			eEntry = (Entry) itEntries.next();
			iState = (Integer) eEntry.getKey();
			dBelief = (Double) eEntry.getValue();
			bs.setValueAt( iState.intValue(), dBelief.doubleValue() );
			dSumBeliefs += dBelief.doubleValue();
		}

		if( dSumBeliefs < 0.9999 || dSumBeliefs > 1.0001 )
			Logger.getInstance().logln( "getBeliefState BUGBUG invalid sum(bs(s)) = " + dSumBeliefs );

		if( m_bCacheBeliefStates ){
			BeliefState bsExisting = m_hmCachedBeliefStates.get( bs );
			if( bsExisting == null ){
				cacheBeliefState( bs );
				m_cBeliefPoints++;
			}
			else{
				bs = bsExisting;
			}

		}

		return bs;
	}

	public BeliefState getBeliefState( double[] adEntries ){
		BeliefState bs = newBeliefState();
		int iState = 0, cStates = m_pPOMDP.getStateCount();
		double dSum = 0.0;

		for( iState = 0 ; iState < cStates ; iState++ ){
			bs.setValueAt( iState, adEntries[iState] );
			dSum += adEntries[iState];
		}

		if( dSum < 0.99 || dSum > 1.01 ){
			Logger.getInstance().logln( "getBeliefState: BUGBUG for bs " + bs + " sum is " + dSum );
			return null;
		}

		if( m_bCacheBeliefStates ){
			BeliefState bsExisting = m_hmCachedBeliefStates.get( bs );
			if( bsExisting == null ){
				cacheBeliefState( bs );
				m_cBeliefPoints++;
			}
			else{
				bs = bsExisting;
			}

		}

		return bs;
	}

	public long getTauComputationCount(){
		return m_cBeliefUpdates;
	}

	public double getAvgTauTime(){
		return ( ( m_cTimeInTau * 1.0 ) / m_cBeliefUpdates );
	}

	public double getAvgBeliefStateSize(){
		return ( ( m_cBeliefStateSize * 1.0 ) / m_cBeliefUpdates );
	}

	public int countEntries() {
		int cEntries = 0;
		for( BeliefState bs : m_hmCachedBeliefStates.keySet() ){
			cEntries += bs.countEntries();
		}
		return cEntries;
	}

	public BeliefState parseDOM( Element eBelief ){
		int id = Integer.parseInt( eBelief.getAttribute( "Id" ) );
		BeliefState bs = newBeliefState( id );
		bs.loadBeliefValues( (Element)eBelief.getFirstChild() );
		return bs;
	}

	public void clear() {
		init();
	}

	public POMDP getPOMDP() {
		return m_pPOMDP;
	}

	public BeliefState getRandomBeliefState() {
		int iState = 0;
		BeliefState bs = newBeliefState();
		double[] adValues = new double[m_pPOMDP.getStateCount()];
		double dSum = 0.0;
		for( iState = 0 ; iState < m_pPOMDP.getStateCount() ; iState++ ){
			adValues[iState] =  m_rndGenerator.nextDouble();
			dSum += adValues[iState];
		}
		for( iState = 0 ; iState < m_pPOMDP.getStateCount() ; iState++ ){
			bs.setValueAt( iState, adValues[iState] / dSum );
		}
		bs.finalizeBeliefs();
		return bs;
	}
}
