package pomdp.algorithms.gridbased;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import pomdp.environments.POMDP;
import pomdp.utilities.BeliefState;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;

public class VariableResolutionGrid extends FixedResolutionGrid{

	protected static final double LN2 = Math.log( 2 );
	protected int[] m_acLevelInterpolations;
	
	public VariableResolutionGrid( POMDP pomdp ){
		super( pomdp );
		m_acLevelInterpolations = new int[100];
	}
	
	protected void refineGrid( BeliefState bs ){
		BeliefState bsSuccessor = null;
		Map[] amSubSimplex = new Map[m_cStates];
		double[] adLambdas = new double[m_cStates];
		Map mVertice = null;
		int iVertice = 0;
		int iResolution = 0, iNewResolution = 0;
		Iterator itSuccessors = bs.computeSuccessors().iterator();
		Vector vNewVertices = new Vector();
		Pair p = null;
		boolean bAllSuccessorsInGrid = true;
		int iSuccessor = 0;
		
		while( itSuccessors.hasNext() ){
			iSuccessor++;
			bsSuccessor = (BeliefState) itSuccessors.next();
			if( !m_vGridPoints.contains( bsSuccessor ) ){
				bAllSuccessorsInGrid = false;
				iResolution = findMaxResoltionCompleteSubSimplex( bsSuccessor.getNonZeroEntriesMap(), 
						amSubSimplex, adLambdas );
				iNewResolution = iResolution * 2;
				if( iNewResolution > m_iFinestResolution )
					m_iFinestResolution = iNewResolution;
				amSubSimplex = getSubSimplexVertices( bsSuccessor.getNonZeroEntriesMap(), 
									iNewResolution );
				for( iVertice = 0 ; iVertice < amSubSimplex.length ; iVertice++ ){
					mVertice = amSubSimplex[iVertice];
					if( mVertice != null ){
						vNewVertices.add( new Pair( mVertice, new Integer( iNewResolution ) ) );
					}
				}
			}
		}
		if( bAllSuccessorsInGrid ){
			bs.setSuccessorsInGrid();
		}
		else{
			for( iVertice = 0 ; iVertice < vNewVertices.size() ; iVertice++ ){
				p = (Pair)vNewVertices.elementAt( iVertice );
				mVertice = (Map)p.first();
				iNewResolution = ((Integer)p.second()).intValue();
				addGridPoint( mVertice, iNewResolution );
			}
		}
	}

	protected Map[] getSubSimplexVertices( Map mEntries, int iResolution ){
		double[] adDirection = new double[m_cStates];
		int[] aiBase = new int[m_cStates], aiSortedPermutation = null;
		Map[] amVertices = null;
		
		m_cInterpolations++;
		
		computeBaseAndDirection( mEntries, iResolution, aiBase, adDirection );		
		aiSortedPermutation = getSortedPermutation( adDirection );
		amVertices = getSubSimplexVertices( aiBase, aiSortedPermutation, iResolution );
		
		return amVertices;
	}

	protected int findMaxResoltionCompleteSubSimplex( BeliefState bsCurrent ){
		Map[] amVertices = new Map[m_cStates];
		double[] adLambdas = new double[m_cStates];
		int iResolution = findMaxResoltionSubSimplex( bsCurrent.getNonZeroEntriesMap(), amVertices, adLambdas, 0.99999 );
		return iResolution;
		
	}
	
	protected int findMaxResoltionCompleteSubSimplex( Map mBeliefEntries, Map[] amVertices, 
			double[] adLambdas ){
		return findMaxResoltionSubSimplex( mBeliefEntries, amVertices, adLambdas, 1.0 );
		
	}
	
	protected int findMaxResoltionSubSimplex( Map mBeliefEntries, Map[] amVertices, 
									double[] adLambdas, double dLamdaSumThreshold ){
		int iResolution = 1, iLastGoodResolution = 1;
		double dLambdaSum = 1.0;
		
		while( ( dLambdaSum >= dLamdaSumThreshold ) && ( iResolution <= m_iFinestResolution ) ){
			dLambdaSum = getLambdaSum( mBeliefEntries, iResolution, amVertices, adLambdas, dLamdaSumThreshold );
			if( dLambdaSum >= dLamdaSumThreshold )
				iLastGoodResolution = iResolution;
			if( m_bDebug ){
				Logger.getInstance().logln( "findMaxResoltionSubSimplex: M = " + iResolution + ", sum = " + dLambdaSum + " max M = " + m_iFinestResolution );
			}
			iResolution *= 2;
		}
		
		return iLastGoodResolution;
	}
	
	protected double getLambdaSum( Map mBeliefEntries, int iResolution, Map[] amValidVertices, 
									double[] adValidLambdas, double dLamdaSumThreshold ){
		double[] adDirection = new double[m_cStates];
		int[] aiBase = new int[m_cStates], aiSortedPermutation = null;
		Map[] amVertices = null;
		double[] adLambdas = null;
		Map mVertice = null;
		int iVertice = 0;
		double dSum = 0.0;
		
		m_cInterpolations++;
		
		computeBaseAndDirection( mBeliefEntries, iResolution, aiBase, adDirection );		
		aiSortedPermutation = getSortedPermutation( adDirection );
		amVertices = getSubSimplexVertices( aiBase, aiSortedPermutation, iResolution );
		adLambdas = computeLambdas( aiSortedPermutation, adDirection );
		
		for( iVertice = 0 ; iVertice < m_cStates ; iVertice++ ){
			if( adLambdas[iVertice] > 0.00000001 ){
				mVertice = amVertices[iVertice];
				if( m_mGridPointValues.containsKey( mVertice ) ){
					dSum += adLambdas[iVertice];
				}
				if( m_bDebug ){
					Logger.getInstance().logln( "for bs 198 - point " + toString( mVertice ) + " exsits? " + m_mGridPointValues.containsKey( mVertice ) + " M = " + iResolution );
				}
			}
		}
		
		if( dSum >= ( dLamdaSumThreshold - EPSILON ) ){
			copy( amVertices, amValidVertices );
			copy( adLambdas, adValidLambdas );
		}
		
		return dSum;
	}

	protected void copy( Map[] source, Map[] target ){
		int i = 0;
		for( i = 0 ; i < source.length ; i++ ){
			target[i] = source[i];
		}		
	}

	protected void copy( double[] source, double[] target ){
		int i = 0;
		for( i = 0 ; i < source.length ; i++ ){
			target[i] = source[i];
		}		
	}
	
	protected double getUnknownGridPointValue( Map mVerticeEntries ){
		return interpolateValue( mVerticeEntries, 1 );
	}

	protected double interpolateValue( BeliefState bs ){
		return interpolateValue( bs, 0.5 );		
	}
	
	protected int log2( int a ){
		return (int)( Math.log( a ) / LN2 );
	}
	
	protected double interpolateValue( BeliefState bs, double dMinLambdaSum ){
		Map[] amSubSimplex = new Map[m_cStates];
		double[] adLambdas = new double[m_cStates];
		
		int iResolution = findMaxResoltionSubSimplex( bs.getNonZeroEntriesMap(), amSubSimplex, adLambdas, dMinLambdaSum );
		double dValue = computeValueGivenSubSimplex( adLambdas, amSubSimplex );	
		
		m_acLevelInterpolations[log2( iResolution )]++;
		
		if( m_bDebug ){
			Logger.getInstance().logln( "interpolateValue " + m_cInterpolations + ") bs = " + bs + " , " + iResolution );
			for( int iVertice = 0 ; iVertice < m_cStates ; iVertice++ )
				if( adLambdas[iVertice] > 0.00000001 )
					Logger.getInstance().logln( "v" + iVertice + " = " + toString( amSubSimplex[iVertice] ) + 
							" , lambda = " + round( adLambdas[iVertice], 4 ) + 
							" , value = " + round( getGridPointValue( amSubSimplex[iVertice] ), 4 ) );
			Logger.getInstance().logln( "Total value is " + round( dValue, 4 ) );
		}		

		
		return dValue;
	}
	
	protected void refineGrid(){
		double dMaxError = MIN_INF; 
		double dSumError = 0.0;
		Iterator itBSIterator = m_vGridPoints.iterator();
		BeliefState bsCurrent = null, bsMaxError = null;
		double dUpperValue = 0.0, dLowerValue = 0.0, dError = 0.0;
		
		Logger.getInstance().logln( "VRRG: Refining grid" );
		
		if( m_iFinestResolution == 0 )
			super.refineGrid();
		else{
			while( itBSIterator.hasNext() ){
				bsCurrent = (BeliefState)itBSIterator.next();
				if( !bsCurrent.allSuccessorsInGrid() ){
					dUpperValue = getGridPointValue( bsCurrent.getNonZeroEntriesMap() );
					dLowerValue = m_vValueFunction.valueAt( bsCurrent );
					dError = dUpperValue - dLowerValue;
					if( dError > dMaxError ){
						dMaxError = dError;
						bsMaxError = bsCurrent;
					}
					dSumError += dError;
				}
			}
			
			Logger.getInstance().logln( "Max error point is " + bsMaxError );
		
			refineGrid( bsMaxError );
		}
	}
	
	protected String getInterpolationLevels(){
		int iResolution = 1, iPower = 0;
		String sResult = "[";
		while( m_acLevelInterpolations[iPower] > 0 ){
			sResult += iResolution + "=" + m_acLevelInterpolations[iPower] + ", ";
			iResolution *= 2;
			iPower++;
		}
		sResult = sResult.substring( 0, sResult.length() - 2 ) + "]";
		return sResult;
	}
}
