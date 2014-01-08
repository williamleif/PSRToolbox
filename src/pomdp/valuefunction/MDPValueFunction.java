package pomdp.valuefunction;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

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
import pomdp.utilities.TabularAlphaVector;
import pomdp.utilities.concurrent.MakeQVector;
import pomdp.utilities.concurrent.ThreadPool;
import pomdp.utilities.datastructures.DoubleVector;
import pomdp.utilities.datastructures.IntVector;
import pomdp.utilities.datastructures.LongVector;


public class MDPValueFunction extends PolicyStrategy {
	protected LinearValueFunctionApproximation m_vValueFunction;
	protected POMDP m_pPOMDP;
	protected int m_cObservations;
	protected int m_cStates;
	protected int m_cActions;
	protected double m_dGamma;
	protected DoubleVector m_adValues;
	protected IntVector m_ivBestActions;
	protected AlphaVector m_avBestActions;
	protected double m_dExplorationRate;
	protected boolean m_bConverged;
	protected boolean m_bLoaded;
	protected RandomGenerator m_rndGenerator;
	
	//private static MDPValueFunction g_vMDP = null;
	
	private boolean m_bFullQFunction = false;
	
	public static boolean PERSIST_FUNCTION = false;
	
	/*
	public static MDPValueFunction getInstance(){
		return g_vMDP;
	}
	
	public static MDPValueFunction getInstance( POMDP pomdp, double dExplorationRate ){
		if( g_vMDP == null )
			g_vMDP = new MDPValueFunction( pomdp, dExplorationRate );
		return g_vMDP;
	}
	*/
	
	public MDPValueFunction( POMDP pomdp, double dExplorationRate ){
		m_pPOMDP = pomdp;
		m_vValueFunction = new LinearValueFunctionApproximation( 0.0001, false );
		m_cStates = m_pPOMDP.getStateCount();
		m_cActions = m_pPOMDP.getActionCount();
		m_cObservations = m_pPOMDP.getObservationCount();
		m_dGamma = m_pPOMDP.getDiscountFactor();
		//m_adQValues = new double[m_cStates][m_cActions];
		//m_adValues = new double[m_cStates];
		m_adValues = new DoubleVector( m_cStates );
		m_ivBestActions = null;
		m_avBestActions = null;
		//m_vValues = new Vector<Double>();//newAlphaVector();
		m_dExplorationRate = dExplorationRate;
		m_bConverged = false;
		m_bLoaded = false;
		//m_aPQStates = null;
		//m_pqStates = null;
		//m_vPredStates = null;
		m_rndGenerator = new RandomGenerator( "MDPVI", 0 );
	}
	
	protected double computeStateActionValue( int iState, int iAction ){
		int iEndState = 0;
		double dValue = 0.0, dTr = 0.0, dSumValues = 0.0, dQValue = 0.0;
		Iterator itNonZeroTransitions = null;
		Entry e = null;
		
		dSumValues = 0.0;
		double dSumTr = 0.0;
		itNonZeroTransitions = getNonZeroTransitions( iState, iAction );
		while( itNonZeroTransitions.hasNext() ){
			e = (Entry) itNonZeroTransitions.next();
			iEndState = ((Number) e.getKey()).intValue();

			dTr = ((Number) e.getValue()).doubleValue();
			dSumTr += dTr;
			dValue = getValue( iEndState );
			dSumValues += dTr * dValue;		

		}
		dQValue = R( iState, iAction ) + m_dGamma * dSumValues;

		return dQValue;
	}
	
	protected double updateState( int iStartState ){
		int iAction = 0, iMaxAction = -1;
		double dMaxQValue = 0.0, dQValue = 0.0, dDelta = 0.0;
		
		dMaxQValue = Double.NEGATIVE_INFINITY;
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			dQValue = computeStateActionValue( iStartState, iAction );
			if( dQValue > dMaxQValue ){
				iMaxAction = iAction;
				dMaxQValue = dQValue;
			}
		}
				
		dDelta = diff( dMaxQValue, getValue( iStartState )  );
		setValue( iStartState, dMaxQValue );
		m_ivBestActions.set( iStartState, iMaxAction );
		return dDelta;
	}
	
	public double getQValue( int iState, int iAction ){
		int iEndState = 0;
		Iterator itNonZero = null;
		double dValue = R( iState, iAction ), dTr = 0.0, dNextValue = 0.0;
		Map.Entry e = null;
		
		itNonZero = getNonZeroTransitions( iState, iAction );
		while( itNonZero.hasNext() ){
			e = (Map.Entry)itNonZero.next();
			iEndState = ((Integer) e.getKey()).intValue();
			dTr = ((Double) e.getValue()).doubleValue();
			//dValue += m_dGamma * dTr * m_adValues[iEndState];
			dNextValue = getValue( iEndState );
			dValue += m_dGamma * dTr * dNextValue;
		}
		return dValue;
	}

	public double getQValue( BeliefState bs, int iAction ){
		int iStartState = 0;
		Iterator itStates = bs.getNonZeroEntries().iterator();
		double dPr = 0.0, dValue = 0.0, dSum = 0.0;
		Map.Entry e1 = null;
		
		while( itStates.hasNext() ){
			e1 = (Map.Entry)itStates.next();
			iStartState = ((Integer) e1.getKey()).intValue();
			dPr = ((Double) e1.getValue()).doubleValue();
			dValue = getQValue( iStartState, iAction );
			dSum += dPr * dValue;
		}
		return dSum;
	}
	/*
	public double getQValue( BeliefState bs, int iAction ){
		int iState = 0;
		Iterator itStates = bs.getNonZeroEntries();
		double dPr = 0.0, dValue = 0.0, dSum = 0.0;
		Map.Entry e = null;
		
		while( itStates.hasNext() ){
			e = (Map.Entry)itStates.next();
			iState = ((Integer) e.getKey()).intValue();
			dPr = ((Double) e.getValue()).doubleValue();
			dValue = m_adQValues[iState][iAction];
			dSum += dPr * dValue;
		}
		return dSum;
	}
	*/
	protected double getMaxImmediateReward( int iState ){
		double dMaxReward = 0.0;
		int iAction = 0;
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			if( R( iState, iAction ) > dMaxReward ){
				dMaxReward = R( iState, iAction );
			}
		}
		return dMaxReward;
	}
	
	public void makeVectors(){
		makeVectors( true );
	}
	
	protected void makeVectors( boolean bFullQFunction ){
		int iAction = 0, iMaxAction = 0;
		AlphaVector av = null;
		m_vValueFunction.clear();
		double dQValue = 0.0, dMaxQValue = 0.0;
		BeliefState bsUniform = m_pPOMDP.getBeliefStateFactory().getUniformBeliefState();
		
		if( bFullQFunction ){
		/*
			for( int iState : getValidStates() ){
				Logger.getInstance().log( iState + " - " );
				for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
					dQValue = getQValue( iState, iAction ) ;
					Logger.getInstance().log( dQValue + ", " );
				}
				Logger.getInstance().logln();
			}
			
			*/
			
			Logger.getInstance().logFull( "MDPVF", 0, "makeVectors", "Started creating Q function vectors" );
			MakeQVector[] m_aTasks = new MakeQVector[m_cActions];
			if( ExecutionProperties.useMultiThread() ){
				for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
					m_aTasks[iAction] = new MakeQVector( this, iAction );
					ThreadPool.getInstance().addTask( m_aTasks[iAction] );
				}
			}
			for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
				if( ExecutionProperties.useMultiThread() ){
					ThreadPool.getInstance().waitForTask( m_aTasks[iAction] );
					av = m_aTasks[iAction].getResult();
				}
				else{
					av = newAlphaVector();
							
					for( int iState : getValidStates() ){
						dQValue = getQValue( iState, iAction ) ;
						if( dQValue != 0.0 )
							av.setValue( iState, dQValue );
					}
					av.finalizeValues();
					av.setWitness( bsUniform );
				}
				av.setAction( iAction );
				m_vValueFunction.add( av, false );
				Logger.getInstance().logFull( "MDPVF", 0, "makeVectors", "Done creating for action " + iAction );
			}
		}
		else{
			Logger.getInstance().logFull( "MDPVF", 0, "makeVectors", "Started creating policy mapping" );
			m_avBestActions = newAlphaVector();
			if( m_ivBestActions != null ){
				for( int iState : getValidStates() ){
					iAction = m_ivBestActions.elementAt( iState );
					m_avBestActions.setValue( iState, iAction );
					//Logger.getInstance().logln( iState + ", " + getStateName( iState ) + " => " + iAction );
					if( (iState > 0 ) && ( iState % 10000 == 0 ) )
						Logger.getInstance().logFull( "MDPVF", 0, "makeVectors", "Done computing " + iState + " states" );
				}				
			}
			else{
				//m_ivBestActions = new IntVector( m_cStates );
				for( int iState : getValidStates() ){
					dMaxQValue = Double.NEGATIVE_INFINITY;
					iMaxAction = -1;
					for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
						dQValue = getQValue( iState, iAction ) ;
						if( dQValue > dMaxQValue ){
							dMaxQValue = dQValue;
							iMaxAction = iAction;
						}
					}
					//m_ivBestActions.set( iState, iMaxAction );
					m_avBestActions.setValue( iState, iMaxAction );
					if( (iState > 0 ) && ( iState % 10000 == 0 ) )
						Logger.getInstance().logFull( "MDPVF", 0, "makeVectors", "Done computing " + iState + " states" );
				}
			}
			//m_adValues.clear();
			//m_adValues = null;
			m_avBestActions.finalizeValues();
			Logger.getInstance().logFull( "MDPVF", 0, "makeVectors", "Done creating policy, |S| = " + m_cStates + ", vertex count = " + m_avBestActions.countEntries() );
			//Logger.getInstance().logFull( "MDPVF", 0, "makeVectors", "Done creating policy" );
		}
		//improveVectors( 100 );
	}
	
	protected String getStateName( int iState ) {
		return m_pPOMDP.getStateName( iState );
	}

	protected void improveVectors( int cMaxIterations ){
		int iIteration = 0 , iObservation = 0, iAction = 0, iNextAction = 0, iNextState = 0;
		LinearValueFunctionApproximation vNextValueFunction = null;
		double dReward = 0.0, dTr = 0.0, dObservation = 0.0, dValue = 0.0; 
		double dNextValue = 0.0, dMaxActionValue = 0.0, dObservationValue = 0.0;
		double dNewValue = 0.0;
		AlphaVector avNext = null, avCurrent = null;
		Iterator itNonZeroStates = null;
		Map.Entry e = null;
		
		Logger.getInstance().logln( "Started improving the vectors" );
		
		for( iIteration = 0 ;iIteration < cMaxIterations ; iIteration++ ){
			vNextValueFunction = new LinearValueFunctionApproximation( 0.0001, false );
			for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
				/*
				avNext = new SparseAlphaVector( null, m_cStates, m_cActions, m_cObservations, 
						m_fTransition, m_fObservation, iAction );
						*/
				
				avNext = new TabularAlphaVector( null, iAction, m_pPOMDP );
						
				avNext.setAction( iAction );
				dNextValue = 0.0;
	 			for( int iState : getValidStates() ){
					dReward = R( iState, iAction );
					dObservationValue = 0.0;
					for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
						dMaxActionValue = Double.MAX_VALUE * -1.0;
						for( iNextAction = 0 ; iNextAction < m_cActions ; iNextAction++ ){
							dNextValue = 0.0;
							itNonZeroStates = getNonZeroTransitions( iState, iAction );
							while( itNonZeroStates.hasNext() ){
								e = (Entry) itNonZeroStates.next();
								iNextState = ((Number) e.getKey()).intValue();
								dTr = ((Number) e.getValue()).doubleValue();
								dObservation = O( iNextAction, iNextState, iObservation );
								if( dObservation > 0.0 ){
									avCurrent = (AlphaVector) m_vValueFunction.elementAt( iNextAction );
									dValue = avCurrent.valueAt( iNextState );
									dNextValue += dValue * dObservation * dTr;
								}
							}
							if( dNextValue > dMaxActionValue ){
								dMaxActionValue = dNextValue;
							}
						} 
						dObservationValue += dMaxActionValue;
					}
					dNewValue = dReward + m_dGamma * dObservationValue;
					avNext.setValue( iState, dNewValue );
				}
	 			vNextValueFunction.add( avNext );
			}
			m_vValueFunction.clear();
			m_vValueFunction = vNextValueFunction;
			Logger.getInstance().logln( "After " + iIteration + " iterations: V = " + m_vValueFunction );
		}
	}
	
	public LinearValueFunctionApproximation getValueFunction(){
		return m_vValueFunction;
	}


	public double computeValueFunction( int cMaxIterations, double dEpsilon, boolean bFixedPolicy ){
		int iIteration = 0, iMaxState = 0, iMinState = 0;
		double dDelta = 0.0, dMaxDelta = 1000.0, dMaxValue = 0.0, dMinValue = 0.0, dValue = 0.0, dMaxValueForAllStates = Double.NEGATIVE_INFINITY;
		int iAction = 0, iMaxAction = 0;
		
		m_ivBestActions = new IntVector( m_cStates );
		
		for( int iState : getValidStates() ){
			dMaxValue = Double.NEGATIVE_INFINITY;
			if( bFixedPolicy ){
				iMaxAction = (int) m_avBestActions.valueAt( iState ); 
				dMaxValue = R( iState, iAction );
			}
			else{
				for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
					dValue = R( iState, iAction );
					if( dValue > dMaxValue ){
						dMaxValue = dValue;
						iMaxAction = iAction;
						if( dValue > dMaxValueForAllStates )
							dMaxValueForAllStates = dValue;
					}
				}
			}
			setValue( iState, dMaxValue );
		}
		
		Logger.getInstance().logFull( "MDPVI", 0, "ComputeV", "Starting to compute value function" );
			
		for( iIteration = 0 ; ( iIteration < cMaxIterations ) && ( dMaxDelta > dEpsilon ) ; iIteration++ ){
			dMaxDelta = 0.0;
			iMaxState = -1;
			iMinState = -1;
			dMaxValue = -1000.0;
			dMinValue = 1000.0;

			Logger.getInstance().logFull( "MDPVI", 0, "ComputeV", "Start iteration " + iIteration );
			for( int iState : getValidStates() ){
				if(!m_pPOMDP.isTerminalState(iState )){
					if( bFixedPolicy ){
						iMaxAction = (int) m_avBestActions.valueAt( iState ); 
						dMaxValue = computeStateActionValue( iState, iMaxAction );
						dValue = getValue( iState );
						dDelta = dMaxValue - dValue;
						setValue( iState, dMaxValue );
					}
					else{

						dDelta = updateState( iState );
						
						
					}	
					/*
					if( m_adValues.elementAt( iState ) == 0.0 ){
						Logger.getInstance().logln( "V(" + iState + ") = " + m_adValues.elementAt( iState ) );
						for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
							for( int i = 0 ; i < m_cStates ; i++ ){
								if( tr( iState, iAction, i ) > 0.0 )
									if( m_adValues.elementAt( i ) > 0.0 )
										dDelta = updateState( iState );
							}
						}
					}
					 */
				}
				if( ( iState % 100000 == 0 ) && ( iState > 0 ) )
					Logger.getInstance().logFull( "MDPVI", 0, "ComputeV", "Iteration " + iIteration + " done " + iState + " states" );
				if( dDelta > dMaxDelta )
					dMaxDelta = dDelta;
				dValue = getValue( iState );
				if( dValue > dMaxValue ){
					dMaxValue = dValue;
					iMaxState = iState;
				}
				else if( dValue < dMinValue ){
					dMinValue = dValue;
					iMinState = iState;
				}
			}

			
			//if( ( iIteration % 100 == 0 ) || ( dMaxDelta <= dEpsilon ) )
				Logger.getInstance().logFull( "MDPVF", 0, "computeValueFunction", "After " + iIteration + 
						" iterations, delta = " + dMaxDelta +
						" min " + iMinState + " = " + dMinValue +
						" max " + iMaxState + " = " + dMaxValue );
		}

		
		return dMaxDelta;
	}	

	
	/**
	 * Attempts to load the value function from a file. If the value function file is not found it computes the value function and saves it to a file.
	 */
	public synchronized void valueIteration( int cMaxIterations, double dEpsilon ){
		int cMDPBackups = 0, iStartState = 0;
		double dMaxDelta = Double.MAX_VALUE;
		String sPath = ExecutionProperties.getPath();
		String sFileName = sPath + m_pPOMDP.getName() + "QMDP.xml";
		
		if( PERSIST_FUNCTION && !m_bLoaded && !sFileName.equals( "" ) ){
			try{
				Logger.getInstance().logFull( "MDPVF", 0, "VI", "Started loading QMDP value function" );
				load( sFileName );
				Logger.getInstance().logFull( "MDPVF", 0, "VI", "QMDP value function loaded successfully" );
				m_bLoaded = true;
				m_bConverged = true;
				return; 
			}
			catch( Exception e ){
				Logger.getInstance().logln( "Unable to load QMDP value function: " + e );
				//e.printStackTrace();
			}
			catch( Error e ){
				Logger.getInstance().logln( "Unable to load QMDP value function: " + e );
			}
		}
		
		if( !m_bLoaded ){
			Logger.getInstance().logln( "Starting MDP value iteration" );
			try{
				dMaxDelta = computeValueFunction( cMaxIterations, dEpsilon, false );
				m_bConverged = true;
			}
			catch( Error e ){
				Logger.getInstance().logln( "Error in computeVN: " + e );
				throw e;
			}
			
			//makeVectors( m_bFullQFunction );
			Logger.getInstance().logFull( "MDPVF", 0, "VI", "MDP value iteration done - iterations " + cMDPBackups + " delta " + dMaxDelta );

			if( PERSIST_FUNCTION && !sFileName.equals( "" ) ){
				try{
					m_bLoaded = true;
					save( sFileName );
				}
				catch( Exception e ){
					Logger.getInstance().logln( "Unable to save QMDP value function: " + e );
				}
				catch( Error e ){
					Logger.getInstance().logln( "Unable to save QMDP value function: " + e );
				}
			}
			m_bLoaded = true;
		}		
	}
		
	protected String getQTable(){
		String sRetVal = "";
		int iState = 0, iAction = 0;
		for( iState=  0 ; iState < m_cStates ; iState++ ){
			for( iAction= 0 ; iAction < m_cActions ; iAction++ ){
				sRetVal += "Q( " + iState + ", " + iAction + " ) = " + getQValue( iState, iAction ) + "\n";
			}
		}
		return sRetVal;
	}
	
	/**
	 * Returns the best action for this belief state
	 */
	public int getBestAction( BeliefState bs ){
		AlphaVector avMaxAlpha = getMaxAlpha( bs );
		return avMaxAlpha.getAction();
	}
	
	/**
	 * Returns the maximal alpha vector for this belief state
	 */
	public AlphaVector getMaxAlpha( BeliefState bs ){
		return m_vValueFunction.getMaxAlpha( bs );
	}
	
	protected double diff( double d1, double d2 ){
		if( d1 > d2 )
			return d1 - d2;
		return d2 - d1;
	}
	
	public int getAction( BeliefState bsCurrent ){
		//return getBestAction( bsCurrent );
		
		double dRand = m_rndGenerator.nextDouble();
		if( dRand > m_dExplorationRate ){
			return getBestAction( bsCurrent );
		}
		else{
			return m_rndGenerator.nextInt( m_cActions );
		}
		
	}

	public double getValue(BeliefState bsCurrent) {
		AlphaVector avMaxAlpha = getMaxAlpha( bsCurrent );
		return avMaxAlpha.dotProduct( bsCurrent );
	}

	public boolean hasConverged() {
		return m_bConverged;
	}

	public String getStatus() {
		return "N/A";
	}
	
	/**
	 * Loads the value function from an XML file.
	 */
	public void load( String sFileName ) throws Exception{
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document docValueFunction = builder.parse( new FileInputStream( sFileName ) );
		Element eMDPFunction = null, eState = null, eADD = null;
		int iState = 0, iAction = 0;
		double dVValue = 0.0;
		NodeList nlStates = null;
		int cStates = 0, cActions = 0, iStateItem = 0;
		String sType = "";
		
		eMDPFunction = (Element)docValueFunction.getChildNodes().item( 0 );
		cStates = Integer.parseInt( eMDPFunction.getAttribute( "StateCount" ) );
		cActions = Integer.parseInt( eMDPFunction.getAttribute( "ActionCount" ) );
		
		if( ( cStates != m_cStates ) || ( cActions != m_cActions ) )
			throw new Exception( "Unmatching state or action count. Expected <" + 
					m_cStates + "," + m_cActions + "> found <" + cStates + "," + cActions + ">" );
		
		sType = eMDPFunction.getAttribute( "type" );
		nlStates = eMDPFunction.getChildNodes();
		
		if( sType.equals( "ValueFunction" ) ){
			for( iStateItem = 0 ; iStateItem < nlStates.getLength() ; iStateItem++ ){
				eState = (Element)nlStates.item( iStateItem );
				dVValue = Double.parseDouble( eState.getAttribute( "Value" ) );
				iState = Integer.parseInt( eState.getAttribute( "Id" ) );
				setValue( iState, dVValue );
			}	
			makeVectors( m_bFullQFunction );
		}
		else if( sType.equals( "Policy" ) ){
			m_avBestActions = newAlphaVector();
			eADD = (Element) eMDPFunction.getFirstChild();
			if( eADD.getNodeName().equals( "ADD" ) ){
				m_avBestActions.parseValuesXML( eADD ); 
			}
			else{
				//m_ivBestActions = new IntVector( m_cStates );
				for( iStateItem = 0 ; iStateItem < nlStates.getLength() ; iStateItem++ ){
					eState = (Element)nlStates.item( iStateItem );
					iAction = Integer.parseInt( eState.getAttribute( "Action" ) );
					iState = Integer.parseInt( eState.getAttribute( "Id" ) );
					m_avBestActions.setValue( iState, iAction );
					//m_ivBestActions.set( iState, iAction );
				}
				m_avBestActions.finalizeValues();
			}
			if( m_bFullQFunction ){
				computeValueFunction( 1000, 0.0001, true );
				makeVectors( m_bFullQFunction );
			}
			//Logger.getInstance().logFull( "MDPVF", 0, "load", "Done loading best actions" );
			Logger.getInstance().logFull( "MDPVF", 0, "load", "Done loading best actions, |S| = " + m_cStates + ", vertex count = " + m_avBestActions.countEntries() );
		}
		else{
			throw new Exception( "Wrong file format" );
		}
	}
	
	/**
	 * Saves the value function to a file in an XML format.
	 */
	protected void save( String sFileName ) throws Exception{
		Document docValueFunction = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element eMDPFunction = null, eState = null;
		int iAction = 0;
		double dVValue = 0.0;
		
		eMDPFunction = docValueFunction.createElement( "MDPFunction" );
		
		if( m_bFullQFunction ){
			eMDPFunction.setAttribute( "type", "ValueFunction" );
			eMDPFunction.setAttribute( "StateCount", m_cStates + "" );
			eMDPFunction.setAttribute( "ActionCount", m_cActions + "" );
			docValueFunction.appendChild( eMDPFunction );
			for( int iState : getValidStates() ){
				eState = docValueFunction.createElement( "State" );
				dVValue = getValue( iState );
				eState.setAttribute( "Id", iState + "" );
				eState.setAttribute( "Value", dVValue + "" );
				eMDPFunction.appendChild( eState );
			}
		}
		else{
			eMDPFunction.setAttribute( "type", "Policy" );
			eMDPFunction.setAttribute( "StateCount", m_cStates + "" );
			eMDPFunction.setAttribute( "ActionCount", m_cActions + "" );
			docValueFunction.appendChild( eMDPFunction );
			/*
			for( int iState : getValidStates() ){
				eState = docValueFunction.createElement( "State" );
				iAction = (int)m_avBestActions.valueAt( iState );
				//iAction = (int)m_ivBestActions.elementAt( iState );
				eState.setAttribute( "Id", iState + "" );
				eState.setAttribute( "Action", iAction + "" );
				eMDPFunction.appendChild( eState );
			}
			*/
			m_avBestActions.getDOM( eMDPFunction, docValueFunction );
		}
		
	   // Use a Transformer for output
	   TransformerFactory tFactory =
	       TransformerFactory.newInstance();
	   Transformer transformer = tFactory.newTransformer();

	   DOMSource source = new DOMSource( docValueFunction );
	   FileOutputStream fos = new FileOutputStream( sFileName );
	   StreamResult result = new StreamResult( fos );
	   transformer.transform( source, result );
	}

	public int getAction( int iState ){
		int iAction = 0, iMaxAction = 0;
		double dMaxQValue = Double.MAX_VALUE * -1, dQValue = 0.0;
		
		if( m_bExploring && m_dExplorationRate > 0.0 ){
			if( m_rndGenerator.nextDouble() < m_dExplorationRate )
				return m_rndGenerator.nextInt( m_cActions );
		}
		
		if( m_avBestActions == null ){
			for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
				dQValue = getQValue( iState, iAction );
				if( dQValue > dMaxQValue ){
					dMaxQValue = dQValue;
					iMaxAction = iAction;
				}
			}
		}
		else{
			//iMaxAction = m_ivBestActions.elementAt( iState );
			iMaxAction = (int)m_avBestActions.valueAt( iState );
		}
		return iMaxAction;
	}

	public double getValue( int iState ){
		//return m_vValues.elementAt( iState );
		//return m_adValues[iState];
		return m_adValues.elementAt( iState );
	}

	public void setValue( int iState, double dValue ){
		//return m_vValues.elementAt( iState );
		//m_adValues[iState] = dValue;
		m_adValues.set( iState, dValue );
	}

	public int countEntries() {
		return m_vValueFunction.countEntries();
	}

	public void persistQValues( boolean bPersist ) {
		if( ( bPersist == true ) && ( m_bFullQFunction == false ) && ( m_bConverged == true ) ){
			m_bFullQFunction = true;
			makeVectors( m_bFullQFunction );
		}
		m_bFullQFunction = bPersist;
	}
	public boolean persistQValues() {
		return m_bFullQFunction;
	}
	
	protected double tr( int iS1, int iAction, int iS2 ){
		return m_pPOMDP.tr( iS1, iAction, iS2 );
	}
	protected double O( int iAction, int iState, int iObservation ){
		return m_pPOMDP.O( iAction, iState, iObservation );
	}
	protected double R( int iState, int iAction ){
		return m_pPOMDP.R( iState, iAction );
	}
	protected Iterator<Entry<Integer,Double>> getNonZeroTransitions( int iState, int iAction ){
		return m_pPOMDP.getNonZeroTransitions( iState, iAction );
	}
	public Collection<Integer> getValidStates(){
		return m_pPOMDP.getValidStates();
	}
	public AlphaVector newAlphaVector(){
		return m_pPOMDP.newAlphaVector();
	}

	public Number[] importanceSampling(int iAction, int iStartState) {
		int iNextState = 0, cSuccessors = 0, iStateIdx = 0;;
		double dValue = 0.0, dMinValue = 0.0, dSumValues = 0.0, dRand = 0.0, dTr = 0.0;
		Iterator<Entry<Integer, Double>> itNonZero = m_pPOMDP.getNonZeroTransitions( iStartState, iAction );
		Vector<Double> vValues = new Vector<Double>();
		Entry<Integer, Double> eTr = null;
		while( itNonZero.hasNext() ){
			eTr = itNonZero.next();
			iNextState = eTr.getKey();
			dTr = eTr.getValue();
			dValue = ( getValue( iNextState ) + m_pPOMDP.R( iStartState, iAction, iNextState ) ) * dTr + 0.1;
			vValues.add( dValue );
			if( dValue < dMinValue )
				dMinValue = dValue;
			dSumValues += dValue;
			cSuccessors++;
		}
		if( dMinValue < 0.0 )
			dMinValue -= 1.0;// to allow all transitions to be possible
		dSumValues -= cSuccessors * dMinValue;
		if( dSumValues == 0.0 ){
			dRand = m_rndGenerator.nextDouble( 1.0 );		
		}
		else{
			dRand = m_rndGenerator.nextDouble( dSumValues );
		}
		itNonZero = m_pPOMDP.getNonZeroTransitions( iStartState, iAction );
		iStateIdx = 0;
		while( itNonZero.hasNext() ){
			eTr = itNonZero.next();
			iNextState = eTr.getKey();
			dTr = eTr.getValue();
			if( dSumValues == 0.0 ){
				dValue = eTr.getValue();
			}
			else{
				dValue = vValues.elementAt( iStateIdx ) - dMinValue;
				iStateIdx++;
			}
			dRand -= dValue;
			if( dRand <= 0.0 ){
				if( dSumValues == 0.0 )					
					return new Number[]{ iNextState, dValue };
				else
					return new Number[]{ iNextState, dValue / dSumValues };
			}
		}
		return null;
	}

	public Number[] importanceSampling( int iAction, BeliefState bsCurrent ) {
		int iObservation = 0, cSuccessors = 0, iStateIdx = 0;;
		double dValue = 0.0, dMinValue = 0.0, dSumValues = 0.0, dRand = 0.0, dPr = 0.0;
		double[] adValues = new double[m_cObservations];
		BeliefState bsNext = null;
		
		for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
			dPr = bsCurrent.probabilityOGivenA( iAction, iObservation );
			if( dPr > 0 ){
				bsNext = bsCurrent.nextBeliefState( iAction, iObservation );
				//dValue = ( getValue( bsNext ) + m_pPOMDP.R( bsCurrent, iAction, bsNext ) ) * dPr + 0.1;
				dValue = dPr;
				if( dValue < dMinValue )
					dMinValue = dValue;
				dSumValues += dValue;
				cSuccessors++;
			}
			else{
				dValue = 0.0;
			}
			adValues[iObservation] = dValue;
		}
		if( dMinValue < 0.0 )
			dMinValue -= 1.0;// to allow all transitions to be possible
		dSumValues -= cSuccessors * dMinValue;
		dRand = m_rndGenerator.nextDouble( dSumValues );
		iStateIdx = 0;
		for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
			dValue = adValues[iObservation];
			dRand -= dValue;
			if( dRand <= 0.0 ){
				return new Number[]{ iObservation, dValue / dSumValues };
			}
		}
		return null;
	}

	public Number[] importanceSamplingForStartState() {
		int iStartState = 0, cStates = 0;
		double dValue = 0.0, dSumValues = 0.0, dRand = 0.0, dPr = 0.0, dMinValue = 0.0;
		Iterator<Entry<Integer, Double>> itStartState = m_pPOMDP.getBeliefStateFactory().getInitialBeliefState().getNonZeroEntries().iterator();
		Entry<Integer, Double> ePr = null;
		while( itStartState.hasNext() ){
			ePr = itStartState.next();
			if( ePr != null ){
				iStartState = ePr.getKey();
				dPr = ePr.getValue();
				dValue = getValue( iStartState ) * dPr;
				if( dValue < dMinValue )
					dMinValue = dValue;
				dSumValues += dValue;
				cStates++;
			}
		}
		if( dMinValue < 0.0 ){
			dMinValue -= 0.01;
			dSumValues -= cStates * dMinValue;
		}
		if( dSumValues == 0.0 ){
			dRand = m_rndGenerator.nextDouble( 1.0 );		
		}
		else{
			dRand = m_rndGenerator.nextDouble( dSumValues );
		}
		itStartState = m_pPOMDP.getBeliefStateFactory().getInitialBeliefState().getNonZeroEntries().iterator();
		while( itStartState.hasNext() ){
			ePr = itStartState.next();
			if( ePr != null ){
				iStartState = ePr.getKey();
				dPr = ePr.getValue();
				if( dSumValues == 0.0 ){
					dValue = ePr.getValue();
				}
				else{
					dValue = getValue( iStartState ) * dPr - dMinValue;
				}
				dRand -= dValue;
				if( dRand <= 0.0 ){
					if( dSumValues == 0.0 )					
						return new Number[]{ iStartState, dValue };
					else
						return new Number[]{ iStartState, dValue / dSumValues };
				}
			}
		}
		return null;
	}

}
