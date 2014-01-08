package pomdp.algorithms.gridbased;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefMapComparator;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.JProf;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;
import pomdp.utilities.sort.BubbleSort;
import pomdp.utilities.sort.SortStrategy;
import pomdp.valuefunction.LinearValueFunctionApproximation;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class FixedResolutionGrid extends ValueIteration {

	protected Map m_mGridPointValues;
	protected Vector m_vGridPoints;
	protected int m_iFinestResolution;
	protected int m_cInterpolations;
	protected boolean m_bUseUpperBound;
	protected int m_iMaximalCountPerIteration;
	
	public static final double EPSILON = 0.000001;
	
	public FixedResolutionGrid( POMDP pomdp ){
		super( pomdp );
		m_iFinestResolution = 0;
		m_mGridPointValues = new TreeMap( BeliefMapComparator.getInstance() );
		m_vGridPoints = new Vector();
		m_cInterpolations = 0;
		m_bUseUpperBound = true;
		m_iMaximalCountPerIteration = 0;
	}
	
	protected int resolutionToIndex( int iResolution ){
		int iLog = 0;
		while( iResolution > 1 ){
			iResolution /= 2;
			iLog++;
		}
		return iLog;
	}
	
	protected void initGrid(){
		Map mEntries = null;
		BeliefState bs = null;
		int iState = 0;
		
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			bs = m_pPOMDP.getBeliefStateFactory().getDeterministicBeliefState( iState );
			mEntries = bs.getNonZeroEntriesMap();
			addGridPoint( mEntries, bs, 1 );
		}
		m_iFinestResolution = 1;
		Logger.getInstance().logln( "Done initializing the grid |V^| = " + m_vGridPoints.size() );
	}
	
	protected void refineGrid(){
		int iResolution = ( m_iFinestResolution == 0 ) ? 1 : m_iFinestResolution * 2;
		Vector vEntries = computeResolutionBeliefPoints( iResolution );
		Iterator itEntries = vEntries.iterator();
		Map mEntries = null;
		BeliefState bs = null;
		
		while( itEntries.hasNext() ){
			mEntries = (Map) itEntries.next();
			bs = m_pPOMDP.getBeliefStateFactory().getBeliefState( mEntries );
			addGridPoint( mEntries, bs, iResolution );
		}
		Logger.getInstance().logln( "Done refining the grid |V^| = " + m_vGridPoints.size() );
	}
	
	protected void addGridPoint( Map mBeliefEntries, int iResolution ){
		BeliefState bs = m_pPOMDP.getBeliefStateFactory().getBeliefState( mBeliefEntries );
		addGridPoint( mBeliefEntries, bs, iResolution );
	}
	
	protected void addGridPoint( Map mBeliefEntries, BeliefState bs, int iResolution ){
		double dValue = getMaxReward() / ( 1.0 - m_dGamma );
				
		if( m_mGridPointValues.containsKey( mBeliefEntries ) )
			return;
		
		if( iResolution > 1 ){
			dValue = interpolateValue( mBeliefEntries, 1 );
		}
		if( iResolution > m_iFinestResolution ){
			m_iFinestResolution = iResolution;
		}
		
		//Logger.getInstance().logln( "Adding grid point " + bs + " v = " + dValue );
		
		Pair pEntry = new Pair( bs, new Double( dValue ) );
		m_mGridPointValues.put( mBeliefEntries, pEntry );
		bs.setGridResolution( iResolution );
		if( !m_vGridPoints.contains( bs ) )
			m_vGridPoints.add( bs );
	}
	
	protected double beliefValueAt( Map mEntries, int iState ){
		Double dValue = (Double) mEntries.get( new Integer( iState ) );
		if( dValue == null )
			return 0.0;
		return dValue.doubleValue();
	}
	
	//x(s) = M \sum_{i=s..|S|}b(i)
	//v(s) = (int)x(s)
	//d(s) = x(s) - v(s)
	protected void computeBaseAndDirection( Map mEntries, int iResolution, int[] aiBase, double[] adDirection ){
		int iState = 0;
		double dSum = 0;
		
		for( iState = m_cStates - 1 ; iState >= 0 ; iState-- ){
			dSum += beliefValueAt( mEntries, iState );
			if( dSum > 0.999999 )
				dSum = 1.0;
			aiBase[iState] = (int)( iResolution * dSum );
			adDirection[iState] = iResolution * dSum - aiBase[iState];
		}
	}
	
	protected double interpolateValue( BeliefState bs ){
		return interpolateValue( bs, m_iFinestResolution );
	}
	
	protected double interpolateValue( BeliefState bs, int iResolution ){
		Map mEntries = new TreeMap( bs.getNonZeroEntriesMap() );
		return interpolateValue( mEntries, iResolution );
	}
	
	protected double computeValueGivenSubSimplex( double[] adLambdas, Map[] amVertices ){
		int iVertice = 0;
		double dValue = 0.0, dSum = 0.0;
		Map mEntries = null;

		for( iVertice = 0 ; iVertice < m_cStates ; iVertice++ ){
			if( adLambdas[iVertice] > EPSILON ){
				mEntries = amVertices[iVertice];
				
				if( mEntries == null )
					Logger.getInstance().logln( "v" + iVertice + " = " + toString( amVertices[iVertice] ) + " , lambda = " + round( adLambdas[iVertice], 4 ) );
				
				dValue = getGridPointValue( mEntries );
				dSum += adLambdas[iVertice] * dValue;
			}
		}
		
		return dSum;
	}
	
	protected double interpolateValue( Map mEntries ){
		return interpolateValue( mEntries, m_iFinestResolution );
	}
	
	protected double interpolateValue( Map mEntries, int iResolution ){
		double[] adDirection = new double[m_cStates];
		int[] aiBase = new int[m_cStates], aiSortedPermutation = null;
		Map[] amVertices = null;
		double[] adLambdas = null;
		int iVertice = 0;
		BeliefState bsVertice = null;
		double dValue = 0.0, dSum = 0.0;
		
		m_cInterpolations++;
		
		computeBaseAndDirection( mEntries, iResolution, aiBase, adDirection );		
		aiSortedPermutation = getSortedPermutation( adDirection );
		amVertices = getSubSimplexVertices( aiBase, aiSortedPermutation, iResolution );
		adLambdas = computeLambdas( aiSortedPermutation, adDirection );
		dValue = computeValueGivenSubSimplex( adLambdas, amVertices );
				
		if( m_bDebug ){
			Logger.getInstance().logln( "interpolateValue " + m_cInterpolations + ") bs = " + toString( mEntries ) + " , " + iResolution );
			//Logger.getInstance().logln( "v = " + toString( aiBase ) );
			//Logger.getInstance().logln( "d = " + toString( adDirection ) );
			//Logger.getInstance().logln( "p = " + toString( aiSortedPermutation ) );
			for( iVertice = 0 ; iVertice < m_cStates ; iVertice++ )
				if( adLambdas[iVertice] > 0 )
					Logger.getInstance().logln( "v" + iVertice + " = " + toString( amVertices[iVertice] ) + 
							" , lambda = " + round( adLambdas[iVertice], 4 ) + 
							" , value = " + round( getGridPointValue( amVertices[iVertice] ), 4 ) );
			Logger.getInstance().logln( "Total value is " + round( dValue, 4 ) );
		}		
		
		return dValue;
	}

	protected double getUnknownGridPointValue( Map mVerticeEntries ){
		Logger.getInstance().logln( "getUnknownGridPointValue: BUGBUG bs doen't exist in grid - " + toString( mVerticeEntries ) );
		return 0.0;
	}
	
	protected double getGridPointValue( Map mVerticeEntries ){
		Pair pEntry = (Pair)m_mGridPointValues.get( mVerticeEntries );
		if( pEntry != null ){
			BeliefState bs = (BeliefState)pEntry.first();
			bs.incrementGridInterploations();
			if( bs.getGridInterpolations() > m_iMaximalCountPerIteration )
				m_iMaximalCountPerIteration = bs.getGridInterpolations();
			return ((Double)pEntry.second()).doubleValue();
		}
		return getUnknownGridPointValue( mVerticeEntries );
	}
	
	//lambda(i) = d(p(i-1))-d(p(i)), 1 <= i < |S|
	//lambda(0) = 1 - \sum_i lambda(i)
	protected double[] computeLambdas( int[] aiSortedPermutation, double[] adDirection ){
		double[] adLambdas = new double[m_cStates];
		int iLambda = 0;
		double dSum = 0.0;
		
		for( iLambda = 1 ; iLambda < m_cStates ; iLambda++ ){
			adLambdas[iLambda] = adDirection[aiSortedPermutation[iLambda - 1]] - adDirection[aiSortedPermutation[iLambda]];
			dSum += adLambdas[iLambda];
		}
		adLambdas[0] = 1 - dSum;
		
		return adLambdas;
	}

	protected String toString( Map mEntries ){
		if( mEntries == null )
			return "null";
		String sResult = "[";
		Iterator it = mEntries.entrySet().iterator();
		Entry e = null;
		
		while( it.hasNext() ){
			e = (Entry) it.next();
			if( e.getValue() instanceof Double ){
				double d = ((Double)e.getValue()).doubleValue();
				if( d > ExecutionProperties.getEpsilon() )
					sResult += e.getKey() + "=" + round( d, 4 ) + ",";
			}
			else{
				sResult += e.getKey() + "=" + e.getValue() + ",";
			}
		}
		sResult += "]";
		return sResult;
	}
	
	protected String toString( int[] array ){
		String sResult = "[";
		int i = 0;
		for( i = 0 ; i< array.length ; i++ )
			sResult += array[i] + ",";
		sResult += "]";
		return sResult;
	}
	
	protected String toString( double[] array ){
		String sResult = "[";
		int i = 0;
		for( i = 0 ; i< array.length ; i++ )
			sResult += round( array[i], 4 ) + ",";
		sResult += "]";
		return sResult;
	}

	protected Map[] getSubSimplexVertices( int[] aiV, int[] aiSortedPermutation, int iResolution ){
		int iBeliefState = 0, iState = 0;
		Map[] amVertices = new Map[m_cStates];
		double[][] adEntries = new double[m_cStates][m_cStates];
		double dNewValue = 0.0, dSumValues = 0.0;
		boolean bIllegalPoints = false;
		
		for( iBeliefState = 0 ; iBeliefState < m_cStates + 1 ; iBeliefState++ ){
			if( !bIllegalPoints ){
				dSumValues = 0.0;
				for( iState = 0 ; iState < m_cStates; iState++ ){
					if( iBeliefState == 0 ){
						adEntries[iBeliefState][iState] = aiV[iState];
					}
					else{
						if( iBeliefState < m_cStates ){
							adEntries[iBeliefState][iState] = adEntries[iBeliefState - 1][iState];
							if( iState == aiSortedPermutation[iBeliefState - 1] ){
								adEntries[iBeliefState][iState]++;
								if( ( adEntries[iBeliefState][iState] > iResolution ) ||
									( ( iState > 1 ) && ( adEntries[iBeliefState][iState] > adEntries[iBeliefState][iState - 1] ) ) )
									bIllegalPoints = true;
							}
						}
						if( iState < m_cStates - 1 )
							dNewValue = ( adEntries[iBeliefState - 1][iState] - 
										adEntries[iBeliefState - 1][iState + 1] ) / iResolution;
						else
							dNewValue = adEntries[iBeliefState - 1][iState] / iResolution;
						
						dSumValues += dNewValue;
						if( !bIllegalPoints && ( dNewValue > 1.0001 || dNewValue < 0.0 || dSumValues > 1.00001 ) ){
							Logger.getInstance().logln( "Found an illegal point " + toString( adEntries[iBeliefState - 1] ) );
						}
						adEntries[iBeliefState - 1][iState] = dNewValue;
					}
					
				}
				 
				if( iBeliefState > 0 ){	
					amVertices[iBeliefState - 1] = arrayToMap( adEntries[iBeliefState - 1] );
				}
			}
			else{
				amVertices[iBeliefState - 1] = null;
			}
			
		}
		
		return amVertices;
	}

	protected Map arrayToMap( double[] array ){
		Map m = new TreeMap();
		int iState = 0;
		double dValue = 0.0;
		
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			dValue = array[iState];
			if( dValue > 0 ){
				m.put( new Integer( iState ), new Double( dValue ) );
			}
		}
		return m;
	}

	protected void swap( int[] aiArray, int i, int j ){
		int iAux = aiArray[i];
		aiArray[i] = aiArray[j];
		aiArray[j] = iAux;
	}
	
	protected void swap( double[] aiArray, int i, int j ){
		double dAux = aiArray[i];
		aiArray[i] = aiArray[j];
		aiArray[j] = dAux;
	}
	
	protected int[] getSortedPermutation( double[] adDirection ){
		double[] adCopy = new double[m_cStates];
		int[] aiLocations = new int[m_cStates];
		int iState = 0, iIteration = 0;
		int iNonZeroEntry = 0, iZeroEntry = m_cStates - 1;
		boolean bDone = false;
		
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			if( adDirection[iState] == 0.0 ){
				adCopy[iZeroEntry] = 0.0;
				aiLocations[iZeroEntry] = iState;
				iZeroEntry--;
			}
			else{
				adCopy[iNonZeroEntry] = adDirection[iState];
				aiLocations[iNonZeroEntry] = iState;
				iNonZeroEntry++;
			}
		}

		while( !bDone ){
			bDone = true;
			for( iState = 0 ; iState < iNonZeroEntry - iIteration - 1 ; iState++ ){
				if( adCopy[iState] < adCopy[iState + 1] ){
					swap( adCopy, iState, iState + 1 );
					swap( aiLocations, iState, iState + 1 );
					bDone = false;
				}
			}
			iIteration++;
		}
		
		return aiLocations;
	}
	protected Vector computeResolutionBeliefPoints( int iResolution ){
		return computeResolutionBeliefPoints( 0, iResolution, 1.0 );
	}
	
	protected Vector computeResolutionBeliefPoints( int iState, int iResolution, double dRemainder ){
		Vector vResult = new Vector();
		Vector vRemainder = null;
		int iFraction = 0;
		Iterator itRemainder = null;
		Map mRest = null;
		
		
		if( dRemainder == 0 ){
			vResult.add( new TreeMap() );
		}
		else if( iState == m_cStates - 1 ){
			mRest = new TreeMap();
			mRest.put( new Integer( iState ), new Double( dRemainder ) );
			vResult.add( mRest );
		}
		else{
			iFraction = 0;
			while( (double)iFraction / iResolution <= dRemainder ){
				vRemainder = computeResolutionBeliefPoints( iState + 1, iResolution, dRemainder - (double)iFraction / iResolution );
				itRemainder = vRemainder.iterator();
				while( itRemainder.hasNext() ){
					mRest = (Map)itRemainder.next();
					if( iFraction > 0 ){
						mRest.put( new Integer( iState ), new Double( (double)iFraction / iResolution ) );
					}
					vResult.add( mRest );
				}
				iFraction++;
			}
		}
		return vResult;
	}

	protected double computeQValue( BeliefState bs, int iAction ){
		double dQValue = 0.0, dR = 0.0, dPr = 0.0, dVNext = 0.0;
		BeliefState bsNext = null;
		int iObservation = 0;
		
		dR = m_pPOMDP.immediateReward( bs, iAction );
		dQValue = dR;
		for( iObservation = 0 ;iObservation < m_cObservations ; iObservation++ ){
			dPr = bs.probabilityOGivenA( iAction, iObservation );
			if( dPr > 0 ){
				bsNext = bs.nextBeliefState( iAction, iObservation );
				dVNext = interpolateValue( bsNext );
				dQValue += m_dGamma * dVNext * dPr;		
			}
		}
			
		return dQValue;
	}
	
	protected int applyH( BeliefState bsCurrent ){
		return applyH( bsCurrent, false );
	}
	protected int applyH( BeliefState bsCurrent, boolean bStoreResults ){
		double dQValue = 0.0, dMaxQValue = MIN_INF;
		int iAction = 0, iMaxAction = -1;
		
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			dQValue = computeQValue( bsCurrent, iAction );
			
			if( m_bDebug )
				Logger.getInstance().logln( "Q( " + bsCurrent + ", " + iAction + " ) = " + round( dQValue, 4 ) );
			
			if( dQValue > dMaxQValue ){
				dMaxQValue = dQValue;
				iMaxAction = iAction;
			}
		}
		
		if( bStoreResults ){
			//double dPreviousValue = getGridPointValue( bsCurrent.getNonZeroEntriesMap(), m_iFinestResolution );
			//Logger.getInstance().logln( "Update " + bsCurrent + " from " + dPreviousValue + " to " + dMaxQValue );
			setGridPointValue( bsCurrent, dMaxQValue );
		}
		
		if( m_bDebug )
			Logger.getInstance().logln( "Qmax( " + bsCurrent + ", " + iMaxAction + " ) = " + round( dMaxQValue, 4 ) );
		
		return iMaxAction;
	}

	protected void setGridPointValue( BeliefState bsCurrent, double dValue ){
		Pair pEntry = (Pair)m_mGridPointValues.get( bsCurrent.getNonZeroEntriesMap() );
		if( pEntry != null )
			pEntry.setValue( new Double( dValue ) );
		else
			Logger.getInstance().logln( "setGridPointValue: BUGBUG - setGridPointValue: no such grid point" );
	}
	
	boolean m_bDebug = false;

	protected void computeValueFunction( int cMaxIterations ){
		int iIteration = 0, iPoint = 0;
		BeliefState bsCurrent = null, bsMaxError = null, bsMinError = null;
		Iterator itBSIterator = null;
		AlphaVector avNew = null, avOld = null, avBackup = null;
		double dMaxError = MIN_INF, dUpperValue = 0.0, dLowerValue = 0.0;
		double dError = 0.0, dMinError = MAX_INF, dSumError = 0.0;
		double dUpperMaxDelta = 0.0, dPreviousUpperValue = 0.0, dNewUpperValue = 0.0, dUpperDelta = 0.0;
		double dLowerMaxDelta = 0.0, dPreviousLowerValue = 0.0, dNewLowerValue = 0.0, dLowerDelta = 0.0;
		double dAverageLowerBound = 0.0, dAverageUpperBound = 0.0;
		boolean bDone = false;
		Runtime rtRuntime = Runtime.getRuntime();
		LinearValueFunctionApproximation vNewValueFunction = null;
		boolean bUpdateLowerBound = true;
		
		for( iIteration = 0 ; iIteration < cMaxIterations && !bDone ; iIteration++ ){
			iPoint = 0;
			dMaxError = MIN_INF; 
			dMinError = MAX_INF; 
			dSumError = 0.0;
			dUpperMaxDelta = 0.0;
			dLowerMaxDelta = 0.0;
			dAverageUpperBound = 0.0;
			dAverageLowerBound = 0.0;
			if( bUpdateLowerBound )
				vNewValueFunction = new LinearValueFunctionApproximation( ExecutionProperties.getEpsilon(), true );
			
			if( bUpdateLowerBound ){
				itBSIterator = getLowerBoundPointsIterator();
				while( itBSIterator.hasNext() ){				
					bsCurrent = (BeliefState)itBSIterator.next();
					//Logger.getInstance().logln( bsCurrent );
					avOld = m_vValueFunction.getMaxAlpha( bsCurrent );
					//Logger.getInstance().logln( "old = " + avOld );
					dPreviousLowerValue = avOld.dotProduct( bsCurrent );
					avNew = vNewValueFunction.getMaxAlpha( bsCurrent );
					//Logger.getInstance().logln( "new = " + avNew );
					if( avNew != null )
						dNewLowerValue = avNew.dotProduct( bsCurrent );
					else
						dNewLowerValue = MIN_INF;
					if( dNewLowerValue < dPreviousLowerValue ){
						avBackup = backup( bsCurrent );
						//Logger.getInstance().logln( "backup = " + avBackup );
						dNewLowerValue = avBackup.dotProduct( bsCurrent );						
						if( dNewLowerValue > dPreviousLowerValue ){
							vNewValueFunction.add( avBackup, false );
							dLowerDelta = Math.abs( dPreviousLowerValue - dNewLowerValue );
							if( dLowerDelta > dLowerMaxDelta )
								dLowerMaxDelta = dLowerDelta;
							dAverageLowerBound += dNewLowerValue;
						}
						else{
							vNewValueFunction.add( avOld, false );
							dAverageLowerBound += dPreviousLowerValue;
						}
					}
				}
				m_vValueFunction = vNewValueFunction;
				
				if( ( m_vValueFunction.size() > 5 ) && ( dLowerMaxDelta < 0.05 ) ){
					bUpdateLowerBound = false;
					Logger.getInstance().logln( "Done updating lower bound" );
				}		
			}
			
			itBSIterator = getUpperBoundPointsIterator();
			while( itBSIterator.hasNext() ){				
				bsCurrent = (BeliefState)itBSIterator.next();
				dPreviousUpperValue = getGridPointValue( bsCurrent.getNonZeroEntriesMap() );
				applyH( bsCurrent, true );
				dNewUpperValue = getGridPointValue( bsCurrent.getNonZeroEntriesMap() );
				dUpperDelta = Math.abs( dPreviousUpperValue - dNewUpperValue );
				if( dUpperDelta > dUpperMaxDelta )
					dUpperMaxDelta = dUpperDelta;
				
				if( bUpdateLowerBound ){
					dNewLowerValue = vNewValueFunction.valueAt( bsCurrent );
				}
				else{
					dNewLowerValue = dPreviousLowerValue;
				}
				dError = dNewUpperValue - dNewLowerValue;
				
				if( dError > dMaxError ){
					dMaxError = dError;
					bsMaxError = bsCurrent;
				}
				if( dError < dMinError ){
					dMinError = dError;
					bsMinError = bsCurrent;
				}
				
				dSumError += dError;
				dAverageUpperBound += dNewUpperValue;
				
				iPoint++;
			}
			
			if( dUpperMaxDelta < 0.01 )
				bDone = true;
		
			if( true || iIteration % 10 == 9 ){
				rtRuntime.gc();
				Logger.getInstance().logln( "Done iteration "  + iIteration + " M " + m_iFinestResolution +
						" |V_| " + m_vValueFunction.size() +
						" max err " + round( dMaxError, 3 ) +
						" on " + bsMaxError +
						" min err " + round( dMinError, 3 ) +
						" on " + bsMinError +
						" avg err " + round( dSumError / m_vGridPoints.size(), 3 ) +
						" upper delta " + round( dUpperMaxDelta, 3 ) +
						" lower delta " + round( dLowerMaxDelta, 3 ) +
						" interpolations " + getInterpolationLevels() +
						" avg lower " + round( dAverageLowerBound / m_vGridPoints.size(), 3 ) +
						" avg upper " + round( dAverageUpperBound / m_vGridPoints.size(), 3 ) +
						" allocated " + ( rtRuntime.totalMemory() - rtRuntime.freeMemory() ) / 1000000 +
						" free " + rtRuntime.freeMemory() / 1000000 +
						" max " + rtRuntime.maxMemory() / 1000000 );
				System.out.flush();
			}
		}
				
	}
	
	protected Iterator getUpperBoundPointsIterator() {
		return m_vGridPoints.iterator();
	}

	protected Iterator getLowerBoundPointsIterator() {
		return randomPermutation( m_vGridPoints );
	}

	protected String getInterpolationLevels(){
		return m_iFinestResolution + "";
	}

	protected void prioritizedValueIteration( int cMaxIterations ){
		int iIteration = 0, iBeliefState = 0;
		BeliefState bsCurrent = null, bsMaxError = null, bsMinError = null;
		Iterator itBSIterator = null;
		AlphaVector avNew = null;
		double dError = 0.0, dMinError = MAX_INF, dMaxError = MIN_INF, dSumError = 0.0;
		double dUpperMaxDelta = 0.0, dPreviousUpperValue = 0.0, dNewUpperValue = 0.0, dUpperDelta = 0.0;
		double dLowerMaxDelta = 0.0, dPreviousLowerValue = 0.0, dNewLowerValue = 0.0, dLowerDelta = 0.0;
		double dAverageLowerBound = 0.0, dAverageUpperBound = 0.0;
		boolean bDone = false;
		Runtime rtRuntime = Runtime.getRuntime();
		BeliefState[] aoSortedByUsage = null;
		LinearValueFunctionApproximation vNewValueFunction = null;
		boolean bUpdateLowerBound = true;
		
		m_iMaximalCountPerIteration = 0;
		itBSIterator = m_vGridPoints.iterator();
		while( itBSIterator.hasNext() ){
			bsCurrent = (BeliefState)itBSIterator.next();
			bsCurrent.clearGridInterpolations();
		}

		itBSIterator = m_vGridPoints.iterator();
		while( itBSIterator.hasNext() ){
			bsCurrent = (BeliefState)itBSIterator.next();
			applyH( bsCurrent, true );
			avNew = backup( bsCurrent );
			m_vValueFunction.add( avNew, false );
		}

		SortStrategy sorter = new BubbleSort();
		aoSortedByUsage = (BeliefState[]) sorter.sort( m_vGridPoints, new GridInterpolationComparator() );
		for( iBeliefState = aoSortedByUsage.length - 1 ; iBeliefState >= 0 ; iBeliefState-- ){
			bsCurrent = (BeliefState) aoSortedByUsage[iBeliefState];	
			Logger.getInstance().log( bsCurrent.getGridInterpolations() + ", " );
		}
		Logger.getInstance().logln();
		
		for( iIteration = 0 ; iIteration < cMaxIterations && !bDone ; iIteration++ ){	
						
			dUpperMaxDelta = 0.0;
			dLowerMaxDelta = 0.0;
			dAverageUpperBound = 0.0;
			dAverageLowerBound = 0.0;
			dMaxError = MIN_INF; 
			dMinError = MAX_INF; 
			dSumError = 0.0;
			if( bUpdateLowerBound )
				vNewValueFunction = new LinearValueFunctionApproximation( ExecutionProperties.getEpsilon(), true );
			for( iBeliefState = aoSortedByUsage.length - 1 ; iBeliefState >= 0 ; iBeliefState-- ){
				bsCurrent = (BeliefState) aoSortedByUsage[iBeliefState];	
				dPreviousUpperValue = getGridPointValue( bsCurrent.getNonZeroEntriesMap() );
				dPreviousLowerValue = m_vValueFunction.valueAt( bsCurrent );
				applyH( bsCurrent, true );
				if( bUpdateLowerBound ){
					avNew = backup( bsCurrent );
					vNewValueFunction.add( avNew, false );
					dNewLowerValue = avNew.dotProduct( bsCurrent );
					dLowerDelta = Math.abs( dPreviousLowerValue - dNewLowerValue );
					if( dLowerDelta > dLowerMaxDelta )
						dLowerMaxDelta = dLowerDelta;
					dAverageLowerBound += dNewLowerValue;
				}
				else{
					dNewLowerValue = dPreviousLowerValue;
				}
				dNewUpperValue = getGridPointValue( bsCurrent.getNonZeroEntriesMap() );
				dUpperDelta = Math.abs( dPreviousUpperValue - dNewUpperValue );
				if( dUpperDelta > dUpperMaxDelta )
					dUpperMaxDelta = dUpperDelta;
				
				dError = dNewUpperValue - dNewLowerValue;
				
				if( dError > dMaxError ){
					dMaxError = dError;
					bsMaxError = bsCurrent;
				}
				if( dError < dMinError ){
					dMinError = dError;
					bsMinError = bsCurrent;
				}
				
				dSumError += dError;
				dAverageUpperBound += dNewUpperValue;
			}
			
			if( bUpdateLowerBound ){
				m_vValueFunction = vNewValueFunction;
				if( dLowerMaxDelta < 0.01 ){
					bUpdateLowerBound = false;
					Logger.getInstance().logln( "Done updating lower bound" );
				}
			}
						
			if( dUpperMaxDelta < 0.01 )
				bDone = true;
		
			if( true || iIteration % 10 == 9 ){
				rtRuntime.gc();
				Logger.getInstance().logln( "Done iteration "  + iIteration + " M " + m_iFinestResolution +
						" |V_| " + m_vValueFunction.size() +
						" max err " + round( dMaxError, 3 ) +
						" on " + bsMaxError +
						" min err " + round( dMinError, 3 ) +
						" on " + bsMinError +
						" avg err " + round( dSumError / m_vGridPoints.size(), 3 ) +
						" upper delta " + round( dUpperMaxDelta, 3 ) +
						" lower delta " + round( dLowerMaxDelta, 3 ) +
						" interpolations " + getInterpolationLevels() +
						" avg lower " + round( dAverageLowerBound / m_vGridPoints.size(), 3 ) +
						" avg upper " + round( dAverageUpperBound / m_vGridPoints.size(), 3 ) +
						" allocated " + ( rtRuntime.totalMemory() - rtRuntime.freeMemory() ) / 1000000 +
						" free " + rtRuntime.freeMemory() / 1000000 +
						" max " + rtRuntime.maxMemory() / 1000000 );
				System.out.flush();
			}
		}			
	}
	public void printGridValues(){
		Iterator itGridPoint = m_mGridPointValues.entrySet().iterator();
		Entry e = null;
		
		while( itGridPoint.hasNext() ){
			e = (Entry)itGridPoint.next();
			Logger.getInstance().logln( toString( (Map)e.getKey() ) + " = " + ((Pair)e.getValue()).second() );
		}
	}
	
	public void valueIteration( int cMaxSteps, double dEpsilon, double dTargetValue ){
		int iResolution = 0, iMaxPower = 1000, iPower = 0;
		long lStartTime = System.currentTimeMillis(), lCurrentTime = 0;
		long cDotProducts = AlphaVector.dotProductCount();
		int iIteration = 0;
		Runtime rtRuntime = Runtime.getRuntime();
		boolean bDone = false;
		Pair pComputedADRs = new Pair();
		double dMaxDelta = 0.0;
		double dLowerBoundADR = 0.0;
		
		m_cElapsedExecutionTime = 0;
		m_cCPUExecutionTime = 0;
		
		long lCPUTimeBefore = 0, lCPUTimeAfter = 0, lCPUTimeTotal = 0;
		
		Logger.getInstance().logln( "Starting Fixed Resolution Grid Value Iteration" );
		
		for( iPower = 0 ; iPower < iMaxPower ; iPower++ ){
			iResolution = (int)Math.pow( 2, iPower );		
			lStartTime = System.currentTimeMillis();
			lCPUTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
			if( iResolution == 1 )
				initGrid();
			else
				refineGrid();
			computeValueFunction( 50 );
			lCurrentTime = System.currentTimeMillis();
			lCPUTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
			m_cElapsedExecutionTime += ( lCurrentTime - lStartTime );
			m_cCPUExecutionTime += ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000;
			lCPUTimeTotal += lCPUTimeAfter - lCPUTimeBefore;
			rtRuntime.gc();
			
			if( true ){
			
				bDone = checkADRConvergence( m_pPOMDP, dTargetValue, pComputedADRs ) || bDone;
				
				m_bUseUpperBound = false;
				dLowerBoundADR = m_pPOMDP.computeAverageDiscountedReward( 500, 160, this );
				m_bUseUpperBound = true;
				
				Logger.getInstance().logln( "Resolution " + iResolution + 
						" |V^| = " + m_vGridPoints.size() + 
						" |V_| = " + m_vValueFunction.size() + 
						" interpolations " + m_cInterpolations +
						" lower bound ADR " + round( dLowerBoundADR, 3 ) +
						" simulated ADR " + round( ((Number) pComputedADRs.first()).doubleValue(), 3 ) +
						" filtered ADR " + round( ((Number) pComputedADRs.second()).doubleValue(), 3 ) +
						" Time " + ( lCurrentTime - lStartTime ) / 1000 +
						" CPU time " + ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000000 +
						" CPU total " + lCPUTimeTotal  / 1000000000 +
						" #backups " + m_cBackups + 
						" V changes " + m_vValueFunction.getChangesCount() +
						" max delta " + round( dMaxDelta, 3 ) +
						" |BS| " + m_pPOMDP.getBeliefStateFactory().getBeliefStateCount() +
						" allocated " + ( rtRuntime.totalMemory() - rtRuntime.freeMemory() ) / 1000000 +
						" free " + rtRuntime.freeMemory() / 1000000 +
						" max " + rtRuntime.maxMemory() / 1000000 );
			}
		}	
	}
	
	//int cCalls = 0;
	
	public int getAction( BeliefState bsCurrent ){
		//cCalls++;
		if( m_bUseUpperBound ){
			//if( cCalls == 2 )
				//m_bDebug = true;
			int iBestAction = applyH( bsCurrent );
			//m_bDebug = false;
			return iBestAction;
		}
		else{
			return super.getAction( bsCurrent );
		}
	}

	@Override
	public void valueIteration(int maxSteps, double epsilon,
			double targetValue, int maxRunningTime, int numEvaluations) {
		throw new NotImplementedException();		
		
	}

}
