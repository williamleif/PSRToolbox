/*
 * Created on May 5, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package pomdp.utilities;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import pomdp.utilities.datastructures.PriorityQueueElement;

/**
 * @author shanigu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public abstract class BeliefState extends PriorityQueueElement implements Serializable{

	private static final long serialVersionUID = 8715715835544313266L;
	protected double[][] m_aCachedObservationProbabilities;
	protected int m_cStates;
	private BeliefState m_bsDiscretized;
	private Vector<BeliefState> m_vPredecessors;
	protected Map<BeliefState, Pair<Double, Integer>> m_mProbCurrentGivenPred;
	private Vector<BeliefState> m_vNeighbors;
	protected Map<Integer,Pair<BeliefState, Double>>[] m_amSuccessors;
	private Vector<BeliefState> m_vAllSuccessors;
	private SortedSet[] m_sSortedSuccessors;
	private int m_cVisits;
	protected int m_cActions;
	protected int m_cObservations;
	protected int m_iID;
	protected boolean m_bDeterministic;
	protected int m_iDeterministicIndex;
	protected boolean m_bCacheBeliefStates;
	public static int g_cBeliefStateUpdates = 0;
	protected double m_dComputedValue;
	protected double m_dLastMaxValue;
	protected int m_iLastMaxValueTime;
	protected AlphaVector m_avLastMaxAlpha;
	protected int m_iLastMaxAlphaTime;
	protected double m_dLastApproximateValue;
	protected int m_iLastApproximateValueTime;
	protected AlphaVector m_avLastApproximateAlpha;
	protected int m_iLastApproximateAlphaTime;
	protected boolean m_bPersistedBeliefState;//refers to bs that belong to B in Perseus or PBVI
	private static boolean m_bCountBeliefUpdates = true;
	protected double m_dImmediateReward;
	protected double[] m_adActionImmediateReward;
	protected int m_iGridResolution;
	protected boolean m_bAllSuccessorsInGrid;
	protected int m_cGridInterpolations;
	protected int m_iMaxErrorAction;
	protected double[] m_adActionError;
	protected double[] m_adPotentialActionValue;
	private int m_cBackups;
	private boolean m_bPersistedInFactory;
	protected BeliefStateFactory m_bsFactory;
	protected int m_iMaxBeliefState;
	protected double m_dMaxBelief;

	public static int g_cBS = 0, g_cLiveBS = 0;

	private static long g_cBeliefStates = 0;
	private long m_iSerialNumber;

	private boolean binned = false;
	

	public BeliefState( int cStates, int cActions, int cObservations, int id, boolean bCacheBeliefStates, BeliefStateFactory bsFactory ){
		super();
		m_bCacheBeliefStates = bCacheBeliefStates;
		m_cStates = cStates;
		m_cActions = cActions;
		m_cObservations = cObservations;
		m_bsFactory = bsFactory;

		m_iMaxBeliefState = -1;
		m_dMaxBelief = 0.0;

		m_cBackups = 0;

		if( m_bCacheBeliefStates ){
			m_amSuccessors = new TreeMap[m_cActions];
			m_sSortedSuccessors = new TreeSet[m_cActions];
			for( int iAction = 0 ; iAction < m_cActions ; iAction++ ){
				m_amSuccessors[iAction] = new TreeMap( );
				m_sSortedSuccessors[iAction] = new TreeSet( new ReversePairComparator() );
			}
		}
		else{
			m_amSuccessors = null;
		}
		m_aCachedObservationProbabilities = new double[m_cActions][m_cObservations];
		for( int i = 0 ; i< m_cActions ; i++ ){
			for( int j = 0 ; j< m_cObservations ; j++ ){
				m_aCachedObservationProbabilities[i][j] = -1.0;
			}
		}

		m_vPredecessors = new Vector<BeliefState>();
		m_vNeighbors = new Vector<BeliefState>();
		m_cVisits = 0;
		m_bsDiscretized = null;

		m_iID = id;
		m_bDeterministic = false;
		m_iDeterministicIndex = -1;
		m_dComputedValue = 0;
		m_mProbCurrentGivenPred = new TreeMap<BeliefState, Pair<Double, Integer>>( getComparator() );

		m_dLastMaxValue = 0.0;
		m_iLastMaxValueTime = -1;
		m_iLastMaxAlphaTime = -1;
		m_avLastMaxAlpha = null;

		m_dLastApproximateValue = 0.0;
		m_iLastApproximateValueTime = -1;
		m_iLastApproximateAlphaTime = -1;
		m_avLastApproximateAlpha = null;

		m_bPersistedBeliefState = false;
		m_adActionImmediateReward = new double[m_cActions];
		m_adPotentialActionValue = new double[m_cActions];
		for( int iAction = 0 ; iAction < m_cActions ; iAction++ ){
			m_adActionImmediateReward[iAction] = Double.NEGATIVE_INFINITY;
			m_adPotentialActionValue[iAction] = Double.NEGATIVE_INFINITY;
		}
		m_dImmediateReward = Double.NEGATIVE_INFINITY;

		m_iGridResolution = 0;

		m_vAllSuccessors = new Vector<BeliefState>();
		m_bAllSuccessorsInGrid = false;
		m_cGridInterpolations = 0;

		m_iMaxErrorAction = -1;
		m_adActionError = new double[m_cActions];

		m_bPersistedInFactory = false;

		g_cBS++;
		g_cLiveBS++;

		m_iSerialNumber = g_cBeliefStates++;
	}

	public BeliefStateFactory getBeliefStateFactory(){
		return m_bsFactory;
	}

	protected Comparator<BeliefState> getComparator() {
		return BeliefStateComparator.getInstance();
	}

	public abstract double valueAt( int iState );

	public abstract void setValueAt( int iState, double dValue );

	public int getMostLikelyState(){
		return m_iMaxBeliefState;
	}

	/**
	 * Returns an Iterator over the non-zero entries of the belief state
	 * @return
	 */
	public abstract Collection<Entry<Integer,Double>> getNonZeroEntries();

	public abstract Iterator<Entry<Integer, Double>> getDominatingNonZeroEntries();

	public abstract int getNonZeroEntriesCount();

	public synchronized void addSuccessor( int iAction, int iObservation, BeliefState bsSuccessor ){
		Integer iKey = new Integer( iObservation );
		double dProb = probabilityOGivenA( iAction, iObservation );
		Pair<BeliefState, Double> pEntry = new Pair<BeliefState, Double>( bsSuccessor, new Double( dProb ) );
		m_amSuccessors[iAction].put( iKey, pEntry );
		if( !m_vAllSuccessors.contains( bsSuccessor ) )
			m_vAllSuccessors.add( bsSuccessor );
	}

	public synchronized BeliefState nextBeliefState( int iAction, int iObservation ){
		BeliefState bsNext = null;
		m_cVisits++;
		if( m_bCountBeliefUpdates )
			g_cBeliefStateUpdates++;

		if( m_bCacheBeliefStates && getBeliefStateFactory().isCachingBeliefStates() ){

			Integer iKey = new Integer( iObservation );
			Pair pEntry = m_amSuccessors[iAction].get( iKey );
			if( pEntry == null ){
				bsNext = getBeliefStateFactory().nextBeliefState( this, iAction, iObservation );
				if( ( bsNext != null ) && ( getBeliefStateFactory().isCachingBeliefStates() ) )
					addSuccessor( iAction, iObservation, bsNext );
			}
			else{
				bsNext = (BeliefState) pEntry.m_first;
			}
		}
		else{
			bsNext = getBeliefStateFactory().nextBeliefState( this, iAction, iObservation );
		}
		return bsNext;
	}

	public double probabilityOGivenA( int iAction, int iObservation ){
		if( m_aCachedObservationProbabilities == null )
			return getBeliefStateFactory().calcNormalizingFactor( this, iAction, iObservation );
		double dValue = m_aCachedObservationProbabilities[iAction][iObservation];
		if( dValue < 0.0 ){
			dValue = getBeliefStateFactory().calcNormalizingFactor( this, iAction, iObservation );
			m_aCachedObservationProbabilities[iAction][iObservation] = dValue;
		}
		return dValue;
	}


	
	
	public void setProbabilityOGivenA( int iAction, int iObservation, double dValue ){
		if( m_aCachedObservationProbabilities != null )
			m_aCachedObservationProbabilities[iAction][iObservation] = dValue;
	}

	public String toString(){
		return toString( 0.01 );
	}

	public int hashCode(){
		int iState = 0, iShortValue = 0, iFullValue = 0;
		double dValue = 0.0;

		Iterator it = getNonZeroEntries().iterator();
		Map.Entry e = null;
		while( it.hasNext() ){
			e= (Map.Entry)it.next();
			iState = ((Integer) e.getKey()).intValue();
			dValue  = ((Double) e.getValue()).doubleValue();
			iShortValue = (int)( dValue * 10000 );
			iFullValue += iShortValue * iState;
		}

		return iFullValue;
	}

	private double diff( double d1, double d2 ){
		double d = d1 - d2;
		if( d < 0 )
			d *= -1;
		return d;
	}

	private boolean equals( BeliefState bsOther ){
		return BeliefStateComparator.getInstance().compare( this, bsOther ) == 0;
	}

	public boolean equals( Object oOther ){
		if( oOther == this )
			return true;
		if( oOther instanceof BeliefState ){
			BeliefState bs = (BeliefState)oOther;
			return equals( bs );
		}
		return false;
	}

	public double distance( BeliefState bsOther ){
		int iState = 0;
		double dSum = 0.0;
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			dSum += diff( valueAt( iState ), bsOther.valueAt( iState ) );
		}
		return dSum;
	}

	public Iterator getSuccessors( int iAction ){
		return m_amSuccessors[iAction].values().iterator();
	}

	public Iterator getSortedSuccessors( int iAction ){
		if( m_sSortedSuccessors[iAction].size() == 0 ){
			computeAllSuccessors( iAction );
		}

		return m_sSortedSuccessors[iAction].iterator();
	}

	public synchronized void addPredecessor( BeliefState bs, double dProb, int iAction ){
		if( m_bCacheBeliefStates && !m_vPredecessors.contains( bs ) ){
			m_vPredecessors.add( bs );
			m_mProbCurrentGivenPred.put( bs, new Pair<Double, Integer>( new Double( dProb ), new Integer( iAction ) ) );
		}
	}

	public Vector<BeliefState> getPredecessors(){
		return m_vPredecessors;
	}

	public int countPredecessors(){
		return m_vPredecessors.size();
	}

	public abstract double[] toArray();

	public BeliefState discretize(){
		if( m_bsDiscretized == null ){
			m_bsDiscretized = getBeliefStateFactory().discretize( this );
		}
		return m_bsDiscretized;
	}

	public boolean validate(){
		double dSum = 0.0, dValue = 0.0;
		int iState = 0;
		boolean bValid = true;

		for( iState = 0 ; iState < m_cStates ; iState++ ){
			dValue = valueAt( iState );
			if( dValue < 0.0 || dValue > 1.0 )
				bValid = false;
			dSum += dValue;
		}
		return bValid || ( dSum != 1.0 );
	}

	public void normalize(){
		double dSum = 0.0, dValue = 0.0, dNormalizedSum = 0.0;
		int iState = 0;

		for( iState = 0 ; iState < m_cStates ; iState++ ){
			dSum += valueAt( iState );
		}
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			dValue = valueAt( iState ) / dSum;
			setValueAt( iState, dValue );
			dNormalizedSum += dValue;
		}
	}

	public boolean isDeterministic(){
		return m_bDeterministic;
	}

	public int getDeterministicIndex(){
		return m_iDeterministicIndex;
	}

	public String toString( double dMin ){
		String sVector = "bs" + m_iID + "[", sValue = "";
		int iState = 0, iEndState = 0;
		double dValue = -1.0, dNextValue = -1.0, dSum = 0.0;
		int cEntries = 0;

		for( iState = 0 ; iState < m_cStates ; iState++ ){
			dValue = valueAt( iState );
			dSum += dValue;
			if( dValue >= dMin ){
				cEntries++;

				sValue = dValue + "";
				if( sValue.length() > 8 )
					sValue = sValue.substring( 0, 8 );

				if( iState < m_cStates - 1 ){
					iEndState = iState;
					do{
						iEndState++;
						dNextValue = valueAt( iEndState );
					}while( ( iEndState < m_cStates - 1 ) && ( dValue == dNextValue ) );
					if( dValue != dNextValue )
						iEndState--;
					if( iEndState == iState )
						sVector += iState + "=" + sValue + ",";
					else{
						sVector += "(" + iState + "-" + iEndState + ")=" + sValue + ",";
						dSum += dValue * ( iEndState - iState );
						iState = iEndState;
					}
				}
				else{
					sVector += iState + "=" + sValue + ",";
				}
			}
		}
		if( cEntries > 0 )
			sVector = sVector.substring( 0, sVector.length() - 1 ) + "]";
		else
			sVector += "]";

		//if( diff( dSum, 1.0 ) > 0.001 )
		//	Logger.getInstance().logError( "BS", "toString", "Sum of probabilities is " + dSum + ", bs = " + sVector );

		return sVector;
	}

	public int countStates(){
		return m_cStates;
	}

	public double getComputedValue() {
		return m_dComputedValue;
	}

	public void setComputedValue( double computedValue ){
		m_dComputedValue = computedValue;
	}

	public int countSuccessors() {
		int cSuccessors = 0;
		for( int iAction = 0 ; iAction < m_cActions ; iAction++ ){
			cSuccessors += m_amSuccessors[iAction].size();
		}
		return cSuccessors;
	}

	public int getId() {
		return m_iID;
	}

	protected Element getPreds( Document docBeliefSpace ){
		Element ePreds = null, ePred = null;
		Iterator<BeliefState> itPreds = m_vPredecessors.iterator();
		BeliefState bsPred = null;
		double dProb = 0.0;

		ePreds = docBeliefSpace.createElement( "Predecessors" );
		ePreds.setAttribute( "size", m_vPredecessors.size() + "" );

		while( itPreds.hasNext() ){
			bsPred = itPreds.next();
			ePred = docBeliefSpace.createElement( "Predecessor" );
			ePred.setAttribute( "Id", bsPred.getId() + "" );
			dProb = getProbCurrentGivenPredecessor( bsPred );
			ePred.setAttribute( "Probability", dProb + "" );
			ePreds.appendChild( ePred );
		}

		return ePreds;
	}

	public Pair<Double, Integer> getCurrentGivenPredecessorDetails( BeliefState bsPredecessor ){
		return m_mProbCurrentGivenPred.get( bsPredecessor );
	}

	public double getProbCurrentGivenPredecessor( BeliefState bsPredecessor ){
		Pair<Double, Integer> p = getCurrentGivenPredecessorDetails( bsPredecessor );
		if( p == null )
			return 0.0;
		Double dProb = p.m_first;
		if( dProb == null )
			return 0.0;
		return dProb.doubleValue();
	}

	public int getActionCurrentGivenPredecessor( BeliefState bsPredecessor ){
		Pair<Double, Integer> p = getCurrentGivenPredecessorDetails( bsPredecessor );
		if( p == null )
			return -1;
		Integer iAction = p.m_second;
		if( iAction == null )
			return -1;
		return iAction.intValue();
	}

	protected Element getSuccessors( Document docBeliefSpace ){
		Element eSuccessors = null, eSuccessor = null;
		Iterator itSuccessors = null;
		BeliefState bsSuccessor = null;
		int iAction = 0;
		Map.Entry e = null;
		Integer iObservation = null;
		int cSuccessors = 0;
		Pair pEntry = null;

		eSuccessors = docBeliefSpace.createElement( "Successors" );

		for( iAction = 0 ;iAction < m_cActions ; iAction++ ){
			if( m_amSuccessors[iAction] != null ){
				itSuccessors = m_amSuccessors[iAction].entrySet().iterator();
				while( itSuccessors.hasNext() ){
					e = (Entry)itSuccessors.next();
					pEntry = (Pair) e.getValue();
					bsSuccessor = (BeliefState)pEntry.first();

					if( bsSuccessor == null )
						Logger.getInstance().logln( getId() + "," + iAction + "," + e );

					iObservation = (Integer)e.getKey();
					eSuccessor = docBeliefSpace.createElement( "Successor" );
					eSuccessor.setAttribute( "Action", iAction + "" );
					eSuccessor.setAttribute( "Observation", iObservation + "" );
					eSuccessor.setAttribute( "Id", bsSuccessor.getId() + "" );
					eSuccessors.appendChild( eSuccessor );
					cSuccessors++;
				}
			}
		}
		eSuccessors.setAttribute( "size", cSuccessors + "" );

		return eSuccessors;
	}

	protected Element getBeliefValues( Document docBeliefSpace ){
		Element eBeliefValues = null, eStateBelief = null;
		Iterator itBeliefStateEntries = getNonZeroEntries().iterator();
		double dValue = 0.0;
		int iState = 0;
		Map.Entry e = null;
		int cBeliefs = 0;

		eBeliefValues = docBeliefSpace.createElement( "BeliefValues" );

		while( itBeliefStateEntries.hasNext() ){
			e = (Entry) itBeliefStateEntries.next();
			iState = ((Number) e.getKey()).intValue();
			dValue = ((Number) e.getValue()).doubleValue();
			eStateBelief = docBeliefSpace.createElement( "StateBelief" );
			eStateBelief.setAttribute( "Id", iState + "" );
			eStateBelief.setAttribute( "Belief", dValue + "" );
			eBeliefValues.appendChild( eStateBelief );
			cBeliefs++;
		}
		eBeliefValues.setAttribute( "size", cBeliefs + "" );

		return eBeliefValues;
	}

	public Element getDOM( Document docBeliefSpace ){
		Element eBeliefState = null, eBeliefValues = null, eSuccessors = null, ePreds = null;

		eBeliefState = docBeliefSpace.createElement( "BeliefState" );
		eBeliefState.setAttribute( "Id", getId() + "" );

		eBeliefValues = getBeliefValues( docBeliefSpace );
		eBeliefState.appendChild( eBeliefValues );
		if( m_amSuccessors != null ){
			eSuccessors = getSuccessors( docBeliefSpace );
			eBeliefState.appendChild( eSuccessors );
		}

		if( m_vPredecessors.size() != 0 ){
			ePreds = getPreds( docBeliefSpace );
			eBeliefState.appendChild( ePreds );
		}
		return eBeliefState;
	}

	public void loadBeliefValues( Element eBeliefValues ){
		Element eStateBelief = null;
		int iState = 0, iStateItem = 0;
		double dValue = 0.0;
		NodeList nlStates = null;
		nlStates = eBeliefValues.getChildNodes();
		for( iStateItem = 0 ; iStateItem < nlStates.getLength() ; iStateItem++ ){
			eStateBelief = (Element)nlStates.item( iStateItem );
			iState = Integer.parseInt( eStateBelief.getAttribute( "Id" ) );
			dValue = Double.parseDouble( eStateBelief.getAttribute( "Belief" ) );
			setValueAt( iState, dValue );
		}

		m_bPersistedBeliefState = true;
	}

	public void loadPredecessors( Element ePredecessors, Map mId2BeliefState ){
		NodeList nlPreds = ePredecessors.getChildNodes();
		Element ePred = null;
		String sId = "", sProb = "", sAction = "";
		int iPred = 0 , cPreds = nlPreds.getLength();
		Integer iId = null;
		BeliefState bsPred = null;
		double dProb = 0.0;
		int iAction = -1;

		for( iPred = 0 ; iPred < cPreds ; iPred++ ){
			ePred = (Element) nlPreds.item( iPred );
			sId = ePred.getAttribute( "Id" );
			iId = new Integer( sId );
			sProb = ePred.getAttribute( "Probability" );
			if( sProb.equals( "" ) )
				dProb = 0.0;
			else
				dProb = Double.parseDouble( sProb );
			sAction = ePred.getAttribute( "Action" );
			if( sAction.equals( "" ) )
				iAction = -1;
			else
				iAction = Integer.parseInt( sAction );
			bsPred = (BeliefState) mId2BeliefState.get( iId );
			if( bsPred != null )
				addPredecessor( bsPred, dProb, iAction );
		}
	}

	public void loadSuccessors( Element eSuccessors, Map mId2BeliefState ){
		NodeList nlSuccessors = eSuccessors.getChildNodes();
		Element eSuccessor = null;
		String sId = "", sAction = "", sObservation = "";
		int iSuccessor = 0 , cSuccessors = nlSuccessors.getLength();
		Integer iId = null;
		int iAction = 0, iObservation = 0;
		BeliefState bsSuccessor = null;

		for( iSuccessor = 0 ; iSuccessor < cSuccessors ; iSuccessor++ ){
			eSuccessor = (Element) nlSuccessors.item( iSuccessor );
			sId = eSuccessor.getAttribute( "Id" );
			sAction = eSuccessor.getAttribute( "Action" );
			sObservation = eSuccessor.getAttribute( "Observation" );
			iId = new Integer( sId );
			iAction = Integer.parseInt( sAction );
			iObservation = Integer.parseInt( sObservation );
			bsSuccessor = (BeliefState) mId2BeliefState.get( iId );
			if( bsSuccessor != null )
				addSuccessor( iAction, iObservation, bsSuccessor );
		}
	}

	public static void countBeliefUpdates( boolean bCount ){
		m_bCountBeliefUpdates = bCount;
	}

	public static void clearBeliefStatsUpdate(){
		g_cBeliefStateUpdates = 0;
	}

	public void addNeighbor( BeliefState bsNeighbor ){
		m_vNeighbors.add( bsNeighbor );
	}

	public Iterator<BeliefState> getNeighbors(){
		return m_vNeighbors.iterator();
	}

	public int getNeighborsCount() {
		return m_vNeighbors.size();
	}

	public void setMaxValue( double dMaxValue, int iTime ){
		m_dLastMaxValue = dMaxValue;
		m_iLastMaxValueTime = iTime;

		if( m_iLastMaxValueTime >= m_iLastApproximateValueTime ){
			m_dLastApproximateValue = m_dLastMaxValue;
			m_iLastApproximateValueTime = m_iLastMaxValueTime;
		}
	}

	public void setApproximateValue( double dApproximateValue, int iTime ){
		m_dLastApproximateValue = dApproximateValue;
		m_iLastApproximateValueTime = iTime;
	}

	public double getMaxValue(){
		return m_dLastMaxValue;
	}
	public int getMaxValueTime(){
		return m_iLastMaxValueTime;
	}

	public double getApproximateValue(){
		return m_dLastApproximateValue;
	}
	public int getApproximateValueTime(){
		return m_iLastApproximateValueTime;
	}

	public void setMaxAlpha( AlphaVector avMax, int iTime ){
		m_avLastMaxAlpha = avMax;
		m_iLastMaxAlphaTime = iTime;
		/*
		BeliefState bsWitness = avMax.getWitness();
		if( bsWitness != null && bsWitness != this && persistedInFactory() ){
			double dWitnessValue = avMax.dotProduct( bsWitness );
			double dValue = avMax.dotProduct( this );
			if( dValue > dWitnessValue + 0.0001 ){
				avMax.setWitness( this );
			}
		}
		 */
	}

	public AlphaVector getMaxAlpha(){
		return m_avLastMaxAlpha;
	}
	public int getMaxAlphaTime(){
		return m_iLastMaxAlphaTime;
	}

	public void clearInternalCache() {
		m_dComputedValue = 0.0;
		m_dLastMaxValue = 0.0;
		m_iLastMaxValueTime = -1;
		m_avLastMaxAlpha = null;
		m_iLastMaxAlphaTime = -1;
	}

	public double getImmediateReward(){
		return m_dImmediateReward;
	}

	public double getActionImmediateReward( int iAction ){
		return m_adActionImmediateReward[iAction];
	}

	public void setImmediateReward( double dReward ){
		m_dImmediateReward = dReward;
	}

	public void setActionImmediateReward( int iAction, double dReward ){
		m_adActionImmediateReward[iAction] = dReward;
	}

	public void computeAllSuccessors( int iAction ){
		int iObservation = 0;
		BeliefState bsNext = null;
		double dProb = 0.0, dSumProbs = 0.0;
		Pair<BeliefState, Double> pEntry = null;
		int cEntries = 0;

		for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
			dProb = probabilityOGivenA( iAction, iObservation );
			if( dProb > 0.0 ){
				bsNext = nextBeliefState( iAction, iObservation );
				pEntry = new Pair<BeliefState, Double>( bsNext, new Double( dProb ) );
				cEntries++;
				m_sSortedSuccessors[iAction].add( pEntry );
				addSuccessor( iAction, iObservation, bsNext );
				if( m_sSortedSuccessors[iAction].size() != cEntries )
					Logger.getInstance().logln( "Real entries " + m_sSortedSuccessors[iAction].size() + ", intended " + cEntries );
				dSumProbs += dProb;
			}
		}
	}

	private class ReversePairComparator implements Comparator{

		public int compare( Object o1, Object o2 ){
			if( o1 instanceof Pair && o2 instanceof Pair ){
				Pair p1 = (Pair)o1;
				Pair p2 = (Pair)o2;
				Double d1 = (Double) p1.second();
				Double d2 = (Double) p2.second();
				if( d1.doubleValue() <= d2.doubleValue() )
					return 1;
				else
					return -1;
			}
			return 0;
		}

	}

	public int getGridResolution() {
		return m_iGridResolution;
	}

	public void setGridResolution(int iGridResolution) {
		m_iGridResolution = iGridResolution;
	}

	public Vector<BeliefState> computeSuccessors() {
		int iAction = 0, iObservation = 0;
		BeliefState bsSuccessor = null;
		double dProb = 0.0;

		m_vAllSuccessors = new Vector<BeliefState>();
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
				dProb = probabilityOGivenA( iAction, iObservation );
				if( dProb > 0 ){
					bsSuccessor = nextBeliefState( iAction, iObservation );
					addSuccessor( iAction, iObservation, bsSuccessor );
					if( !m_vAllSuccessors.contains( bsSuccessor ) )
						m_vAllSuccessors.add( bsSuccessor );
				}
			}
		}
		return m_vAllSuccessors;
	}

	public Vector<BeliefState> getSuccessors() {
		return m_vAllSuccessors;
	}

	public void setSuccessorsInGrid() {
		m_bAllSuccessorsInGrid = true;

	}

	public boolean allSuccessorsInGrid() {
		return m_bAllSuccessorsInGrid;
	}

	public int getGridInterpolations(){
		return m_cGridInterpolations;
	}

	public void clearGridInterpolations(){
		m_cGridInterpolations = 0;
	}

	public void incrementGridInterploations(){
		m_cGridInterpolations++;
	}

	public void setMaxErrorAction( int iAction ){
		m_iMaxErrorAction = iAction;
	}
	public int getMaxErrorAction(){
		return m_iMaxErrorAction;
	}
	public void setActionError( int iAction, double dError ){
		if( ( m_adActionError[iAction] > dError ) && ( iAction == m_iMaxErrorAction ) ){
			int i = 0;
			m_adActionError[iAction] = dError;
			for( i = 0 ; i < m_cActions ; i++ ){
				if( m_adActionError[i] > m_adActionError[m_iMaxErrorAction] ){
					m_iMaxErrorAction = i;
				}
			}
		}
		else{
			m_adActionError[iAction] = dError;
			if( ( m_iMaxErrorAction == -1 ) || ( dError > m_adActionError[m_iMaxErrorAction] ) )
				m_iMaxErrorAction = iAction;
		}
	}
	public double getActionError( int iAction ){
		return m_adActionError[iAction];
	}

	public void setPotentialActionValue( int iAction, double dNewValue ){
		m_adPotentialActionValue[iAction] = dNewValue;
	}
	public void incrementPotentialActionValue( int iAction, double dDeltaValue ){
		m_adPotentialActionValue[iAction] += dDeltaValue;
	}
	public double getPotentialActionValue( int iAction ){
		return m_adPotentialActionValue[iAction];
	}

	public int countBackups() {
		return m_cBackups;
	}

	public void addBackup(){
		m_cBackups++;
	}

	public void clearBackups(){
		m_cBackups = 0;
	}

	public abstract int countEntries();

	public void release() {
	}
	public void setFactoryPersistence( boolean bPersisted ){
		m_bPersistedInFactory = bPersisted;
	}
	public boolean persistedInFactory(){
		return m_bPersistedInFactory;
	}
	public abstract long size();
	public long getSerialNumber(){
		return m_iSerialNumber;
	}
	public BeliefState copy(){
		return this;
	}
	public void finalizeBeliefs(){

	}

	public Map<Integer, Double> getNonZeroEntriesMap() {
		TreeMap<Integer, Double> mNonZero = new TreeMap<Integer, Double>();
		Iterator<Entry<Integer,Double>> itNonZero = getNonZeroEntries().iterator();
		Entry<Integer,Double> e = null;
		while( itNonZero.hasNext() ){
			e = itNonZero.next();
			mNonZero.put( e.getKey(), e.getValue() );
		}
		return mNonZero;
	}

	public abstract void clearZeroEntries();

	public int getFirstNonZeroIndex() {
		return getNonZeroEntries().iterator().next().getKey();
	}

	public boolean isTerminalBelief() {
		Collection<Entry<Integer, Double>> cNonZero = getNonZeroEntries();
		for( Entry<Integer, Double> e : cNonZero ){
			if( !m_bsFactory.getPOMDP().isTerminalState( e.getKey() ) )
				return false;
		}
		return true;
	}
	
	public boolean isBinned()
	{
		return binned;
	}
	
	public void setBinned()
	{
		binned = true;
	}
	
	
	public double getEntropy()
	{
		double entropy = 0;
		int iState;
		for(iState = 0 ; iState < m_cStates; iState++){
			entropy += valueAt(iState) * (Math.log(valueAt(iState)) / Math.log(2));
		}
		return (-1) * entropy;
		
		
	}
}
