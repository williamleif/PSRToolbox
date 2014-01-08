package pomdp.utilities.factored;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import pomdp.utilities.ArrayComparator;
import pomdp.utilities.ArrayVector;
import pomdp.utilities.ArrayVectorFactory;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;
import pomdp.utilities.datastructures.DoubleVector;
import pomdp.utilities.datastructures.DoubleVectorFactory;
import pomdp.utilities.datastructures.LongArrayVector;
import pomdp.utilities.datastructures.LongArrayVectorFactory;
import pomdp.utilities.datastructures.LongVector;
import pomdp.utilities.datastructures.LongVectorFactory;
import pomdp.utilities.factored.AlgebraicDecisionDiagram.AbstractionFilter;

/**
 * Implements an ADD (Algebraic Decision Diagram) using an array based data structure.
 * Appears to be more efficient than using objects for graph nodes.
 * @author shanigu
 *
 */
public class CompactAlgebraicDecisionDiagram implements AlgebraicDecisionDiagram, Serializable {
	protected long m_iRoot;
	private LongArrayVector m_vVertices;
	private Map<Double,Long> m_mLeaves;
	private ArrayVector<LongVector> m_vParents;
	private DoubleVector m_vValueSum;
	private int m_cVariables;
	private double m_dMaxValue, m_dValueSum;
	private long m_iID;
	private long m_cMaxVertexes = -1;
	private static long VALUE_OFFSET = 1000;
	private boolean m_bReduced;
	private static BinaryOperator m_boSum = null, m_boProduct = null;
	private boolean m_bMaintainValueSum;
	
	private static int VERTEX_ENTRIES = 3;
	private static int LEAF_ENTRIES = 2;
	private static int VARIABLE_ID = 0;
	private static int VALUE_ENTRY = 1;
	//private static int MANTISSA_ENTRY = 2;
	private static int FALSE_CHILD = 1;
	private static int TRUE_CHILD = 2;

	private static int MANTISSA_FACTOR = 1000;
	
	private static LongVectorFactory g_ivFactory = new LongVectorFactory();
	private static DoubleVectorFactory g_dvFactory = new DoubleVectorFactory();
	private static LongArrayVectorFactory g_avFactory = new LongArrayVectorFactory();
	private static ArrayVectorFactory<LongVector> g_vvFactory = new ArrayVectorFactory<LongVector>();
	
	public static long m_cADDs = 0, g_cLiveAdds = 0, g_cFinalized = 0;
	
	public CompactAlgebraicDecisionDiagram( int cVariables, boolean bMaintainValueSum ){
		m_iRoot = -1;
		m_cVariables = cVariables;
		m_dMaxValue = Double.NEGATIVE_INFINITY;
		m_dValueSum = Double.NEGATIVE_INFINITY;
		m_iID = m_cADDs++;
		m_bReduced = false;
		g_cLiveAdds++;
		m_cMaxVertexes = (long)Math.pow( 2, cVariables + 1 );
		m_bMaintainValueSum = true;//bMaintainValueSum;
		initDataStructures();
	}
	private void initDataStructures(){
		m_vVertices = g_avFactory.getLargeVector();
		m_mLeaves = new TreeMap<Double,Long>();
		m_vParents = g_vvFactory.getLargeVector();
		if( m_bMaintainValueSum )
			m_vValueSum = g_dvFactory.getLargeVector();
	}
	
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#finalize()
	 */
	
	public void release(){
		m_vVertices.release();
		m_vParents.release();
		m_vValueSum.release();
		m_vVertices = null;
		m_vParents = null;
		m_vValueSum = null;
	}
	public CompactAlgebraicDecisionDiagram( CompactAlgebraicDecisionDiagram addOther ){
		this( addOther.m_cVariables, addOther.m_bMaintainValueSum );
		m_iRoot = copyVertices( addOther );
	}
	
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#getId()
	 */
	public long getId(){
		return m_iID;
	}

	private long getValueVertex( double dValue ){
		double dRound = round( dValue );
		Long idx = m_mLeaves.get( dRound );
		if( idx == null ){
			long[] aValueData = new long[LEAF_ENTRIES];
			aValueData[VARIABLE_ID] = Integer.MAX_VALUE / 2;
			aValueData[VALUE_ENTRY] = Double.doubleToLongBits( dRound );
			
			m_vVertices.add( aValueData );
			m_vParents.add( g_ivFactory.getSmallVector() );
			if( m_bMaintainValueSum )
				m_vValueSum.add( dValue );
			m_mLeaves.put( dRound, m_vVertices.size() - 1 );
			return m_vVertices.size() - 1;
		}
		return idx;
	}
	
	private void setLeafValue( long iLeaf, double dValue ){
		long[] aValueData = m_vVertices.elementAt( iLeaf );
		double dRound = round( dValue );
		aValueData[VALUE_ENTRY] = Double.doubleToLongBits( dRound );
	}

	private double round( double d ){
		int iMantissa = 0;
		double dRound = d;
		if( dRound != 0.0 ){
			while( Math.abs( dRound ) < 1.0 ){
				dRound *= MANTISSA_FACTOR;
				iMantissa++;
			}
			dRound = Math.round( dRound * VALUE_OFFSET );
			dRound /= VALUE_OFFSET;
			while( iMantissa > 0 ){
				dRound /= MANTISSA_FACTOR;
				iMantissa--;
			}
		}
		return dRound;
	}
	
	private long[] getExponentMantissa( double d ){
		long[] aRes = { 0, 0 };
		if( d != 0.0 ){
			while( Math.abs( d ) < 1.0 ){
				d *= MANTISSA_FACTOR;
				aRes[1]++;
			}
			aRes[0] = (long)( d * VALUE_OFFSET );
		}
		return aRes;
	}

	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#product(pomdp.CompactAlgebraicDecisionDiagram)
	 */
	public AlgebraicDecisionDiagram product( AlgebraicDecisionDiagram add ){
		CompactAlgebraicDecisionDiagram addOther = (CompactAlgebraicDecisionDiagram)add;
		int cMaxVariables = Math.max( m_cVariables, addOther.m_cVariables );
				
		CompactAlgebraicDecisionDiagram addProduct = new CompactAlgebraicDecisionDiagram( cMaxVariables, true );
		
		addProduct.applyOperator( m_iRoot, this, addOther.m_iRoot, addOther, getProduct() );
		addProduct.reduce();
				
		return addProduct;
	}
	
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#sum(pomdp.CompactAlgebraicDecisionDiagram)
	 */
	public CompactAlgebraicDecisionDiagram sum( AlgebraicDecisionDiagram add ){
		CompactAlgebraicDecisionDiagram addOther = (CompactAlgebraicDecisionDiagram)add;
		int cMaxVariables = Math.max( m_cVariables, addOther.m_cVariables );
		CompactAlgebraicDecisionDiagram addSum = new CompactAlgebraicDecisionDiagram( cMaxVariables, true );
/*
		if( m_vSum.contains( addOther.getId() ) )
			System.out.prlongln( "Already summed " + getId() + " with " + addOther.getId() );
		m_vSum.add( addOther.getId() );
*/
		
		addSum.applyOperator( m_iRoot, this, addOther.m_iRoot, addOther, getSum() );
		addSum.reduce();
		return addSum;
	}
	
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#sum(pomdp.CompactAlgebraicDecisionDiagram)
	 */
	public CompactAlgebraicDecisionDiagram max( AlgebraicDecisionDiagram add ){
		CompactAlgebraicDecisionDiagram addOther = (CompactAlgebraicDecisionDiagram)add;
		int cMaxVariables = Math.max( m_cVariables, addOther.m_cVariables );
		CompactAlgebraicDecisionDiagram addMax = new CompactAlgebraicDecisionDiagram( cMaxVariables, true );
	
		addMax.applyOperator( m_iRoot, this, addOther.m_iRoot, addOther, new Max() );
		addMax.reduce();
		return addMax;
	}
	
	private static long m_cTouchedVertexes = 0;
	private void applyOperator( long iRoot1, CompactAlgebraicDecisionDiagram add1, 
								long iRoot2, CompactAlgebraicDecisionDiagram add2, 
								BinaryOperator op ){
		//initDataStructures();
		m_cTouchedVertexes = 0;
		m_iRoot = applyOperator( iRoot1, add1, iRoot2, add2, new TreeMap<long[],Long>( ArrayComparator.getLongComparator() ), op );
	}
	
	private boolean validVertexIndex( long iVertexIndex ){
		if( ( iVertexIndex > m_vVertices.size() ) || ( iVertexIndex < 0 ) ||
				( m_vVertices.elementAt( iVertexIndex ) == null ) )
			return false;
		return true;
	}
	
	private boolean isLeaf( long iVertexIndex ){
		long[] aVertex =  m_vVertices.elementAt( iVertexIndex );
		if( aVertex.length == LEAF_ENTRIES )
			return true;
		return false;
	}
	
	private double getValue( long iVertexIndex ){
		long[] aiValueVertex = m_vVertices.elementAt( iVertexIndex );
		return getValue( aiValueVertex );
	}
	
	private double getValue( long[] aiVertexEntries ){
		long iValue = aiVertexEntries[VALUE_ENTRY];
		double dValue = Double.longBitsToDouble( iValue );
		return dValue;
	}
	
	private void setValueSum( long iVertexIndex, AbstractionFilter aFilter ){
		double dFalseValue = 0.0, dTrueValue = 0.0;
		int iVariableId = getVariableId( iVertexIndex ), iChildVariableId = 0;
		long iTrueChild = getTrueChild( iVertexIndex ), iFalseChild = getFalseChild( iVertexIndex );
		int cLeaves  = 0; 
		int cOccurences = -1, cLevels = 0;
		
		if( aFilter == null ){
			cLeaves = (int)Math.pow( 2, m_cVariables - iVariableId - 1 );
		}
		else{
			cLevels = aFilter.countVariablesBetween( iVariableId, aFilter.getLastVariableId() );
			cLeaves  = (int)Math.pow( 2, cLevels - 1 );
		}
		if( isLeaf( iFalseChild ) ){
			dFalseValue = cLeaves * getValue( iFalseChild );
		}
		else{
			iChildVariableId = getVariableId( iFalseChild );
			if( aFilter == null ){
				cLevels = iChildVariableId - iVariableId;
			}
			else{
				cLevels = aFilter.countVariablesBetween( iVariableId, iChildVariableId );
			}
			cOccurences = (int)Math.pow( 2, cLevels - 1 );
			dFalseValue = getValueSum( iFalseChild ) * cOccurences;
		}
		if( isLeaf( iTrueChild ) ){
			dTrueValue = cLeaves * getValue( iTrueChild );
		}
		else{
			iChildVariableId = getVariableId( iTrueChild );
			if( aFilter == null ){
				cLevels = iChildVariableId - iVariableId;
			}
			else{
				cLevels = aFilter.countVariablesBetween( iVariableId, iChildVariableId );
			}
			cOccurences = (int)Math.pow( 2, cLevels - 1 );
			dTrueValue = getValueSum( iTrueChild ) * cOccurences;
		}
		setValueSum( iVertexIndex, dTrueValue + dFalseValue, aFilter );
	}
	
	private void setValueSum( long iVertexIndex, double dValueSum, AbstractionFilter aFilter ){
		if( iVertexIndex == m_iRoot ){
			int iRootId = getVariableId( m_iRoot );
			if( iRootId > m_cVariables )
				iRootId = m_cVariables - 1;
			int cVariablesBetween = iRootId;
			if( aFilter != null )
				cVariablesBetween = aFilter.countVariablesBetween( aFilter.getFirstVariableId(), iRootId );
			m_dValueSum = Math.pow( 2.0, cVariablesBetween ) * dValueSum;
		}
		
		m_vValueSum.set( iVertexIndex, dValueSum );
	}
	
	private double getValueSum( long iVertexIndex ){
		double dValue = m_vValueSum.elementAt( iVertexIndex );
		return dValue;
	}
	
	private int getVariableId( long iVertexIndex ){
		long[] aVertexData = m_vVertices.elementAt( iVertexIndex );
		int iVariableId = (int)aVertexData[VARIABLE_ID];
		return iVariableId;
	}
	
	private void setFalseChild( long iVertexIndex, long iChildIndex ){
		setChild( iVertexIndex, iChildIndex, FALSE_CHILD );
	}
	
	private void setTrueChild( long iVertexIndex, long iChildIndex ){
		setChild( iVertexIndex, iChildIndex, TRUE_CHILD );
	}
	
	private void setChild( long iVertexIndex, long iChildIndex, int iChildType ){
		m_vVertices.elementAt( iVertexIndex )[iChildType] = iChildIndex;
		addParent( iChildIndex, iVertexIndex );
	}
	
	private void setChildren( long iVertexIndex, long iFalseChild, long iTrueChild ){
		long[] aVertexData = m_vVertices.elementAt( iVertexIndex );
		aVertexData[FALSE_CHILD] = iFalseChild;
		aVertexData[TRUE_CHILD] = iTrueChild;
		addParent( iFalseChild, iVertexIndex );
		addParent( iTrueChild, iVertexIndex );
	}
	
	private long getFalseChild( long iVertexIndex ){
		long iFalseVariableIndex = m_vVertices.elementAt( iVertexIndex )[FALSE_CHILD];
		return iFalseVariableIndex;
	}
	
	private long getTrueChild( long iVertexIndex ){
		long iTrueVariableIndex = m_vVertices.elementAt( iVertexIndex )[TRUE_CHILD];
		return iTrueVariableIndex;
	}
	private boolean hasFalseChild( long iVertexIndex ){
		long iFalseVariableIndex = m_vVertices.elementAt( iVertexIndex )[FALSE_CHILD];
		if( iFalseVariableIndex == -1 )
			return false;
		return true;
	}
	
	private boolean hasTrueChild( long iVertexIndex ){
		long iTrueVariableIndex = m_vVertices.elementAt( iVertexIndex )[TRUE_CHILD];
		if( iTrueVariableIndex == -1 )
			return false;
		return true;
	}
	
	private long applyOperator( long iCurrent1, long iCurrent2, Map<long[],Long> mExisting,
			BinaryOperator op ){
		return applyOperator( iCurrent1, this, iCurrent2, this, mExisting, op );
	}
	
	private long applyOperator( long iCurrent1, CompactAlgebraicDecisionDiagram add1, 
			long iCurrent2, CompactAlgebraicDecisionDiagram add2, 
			Map<long[],Long> mExisting,
			BinaryOperator op ){
		if( !add1.validVertexIndex( iCurrent1 ) || !add2.validVertexIndex( iCurrent2 ) )
			Logger.getInstance().log( "CADD", 0, "applyOperator", "Invalid index" );
		
		long iVariableId = 0, iVariableId1 = add1.getVariableId( iCurrent1 ), iVariableId2 = add2.getVariableId( iCurrent2 );
		long iFalse1 = -1, iFalse2 = -1, iTrue1 = -1, iTrue2 = -1;
		long iFalse = -1, iTrue = -1;
		long[] aiKey = null;
		double dValue = 0;
		long iNew = -1;
		boolean bLeaf1 = add1.isLeaf( iCurrent1 );
		boolean bLeaf2 = add2.isLeaf( iCurrent2 );
		double dValue1 = add1.getValue( iCurrent1 );
		double dValue2 = add2.getValue( iCurrent2 );
		
		m_cTouchedVertexes++;
		
		if( bLeaf1 && bLeaf2 ){
			dValue = op.compute( dValue1, dValue2 );
			iNew = getValueVertex( dValue );
		}
		else if( ( op instanceof Product ) && bLeaf1 && ( dValue1 == 0.0 ) ){
			iNew = getValueVertex( 0 );
		}
		else if( ( op instanceof Product ) && bLeaf2 && ( dValue2 == 0.0 ) ){
			iNew = getValueVertex( 0 );
		}
		else{
			aiKey = makeKey( iCurrent1, iCurrent2 );
			if( mExisting.containsKey( aiKey ) ){
				return mExisting.get( aiKey );
			}
			iVariableId = Math.min( iVariableId1, iVariableId2 );
			if( iVariableId == iVariableId1 ){
				iFalse1 = add1.getFalseChild( iCurrent1 );
				iTrue1 = add1.getTrueChild( iCurrent1 );
			}
			else{
				iFalse1 = iCurrent1;
				iTrue1 = iCurrent1;
			}
			if( iVariableId == iVariableId2 ){
				iFalse2 = add2.getFalseChild( iCurrent2 );
				iTrue2 = add2.getTrueChild( iCurrent2 );
			}
			else{
				iFalse2 = iCurrent2;
				iTrue2 = iCurrent2;
			}
			
			iFalse = applyOperator( iFalse1, add1, iFalse2, add2, mExisting, op );
			iTrue = applyOperator( iTrue1, add1, iTrue2, add2, mExisting, op );
			
			if( iFalse == iTrue )
				iNew = iTrue;
			else{
				iNew = addVertex( iVariableId, iFalse, iTrue );
					
				/*
				if( getId() == 386 && iNew == 12 ){
					
					System.out.prlongln( add1.getTreeString(iCurrent1,0) );
					System.out.prlongln( add2.getTreeString(iCurrent2,0) );
					System.out.prlongln( getTreeString(iNew,0) );
				}
				*/

			}
			
			mExisting.put( aiKey, iNew );
		}
		//Logger.getInstance().log( "ADD", 0, "product", "v1 = " + v1 + " v2 = " + v2 + " product = " + vNew.getTreeString( 0 ) );
		return iNew;
	}
	
	private long[] makeKey( long iVertex ){
		return new long[]{ iVertex };
	}
	
	private long[] makeKey( long iVertex1, long iVertex2 ){
		return new long[]{ iVertex1, iVertex2 };
	}

	private long[] makeKey( long iParent, long iTrueChild, long iFalseChild ){
		return new long[]{ iParent, iTrueChild, iFalseChild };
	}
	
	private String makeStringKey( AlgebraicDecisionDiagramVertex v ){
		if( v.isLeaf() )
			return "v" + v.getVertexId();
		return v.getVertexId() + "";
	}
	
	private String makeStringKey( AlgebraicDecisionDiagramVertex v1, AlgebraicDecisionDiagramVertex v2 ){
		return makeStringKey( v1 ) + "," + makeStringKey( v2 );
	}

	private String makeStringKey( AlgebraicDecisionDiagramVertex vParent, 
							AlgebraicDecisionDiagramVertex vTrueChild, 
							AlgebraicDecisionDiagramVertex vFalseChild ){
		return vParent.getVertexId() + "," + makeStringKey( vTrueChild ) + "," + makeStringKey( vFalseChild );
	}

	private void fillReachableVertices( long iCurrent, ArrayVector<LongVector> vVariables, LongVector vLeaves ){
		if( isLeaf( iCurrent ) ){
			if( !vLeaves.contains( iCurrent ) )
				vLeaves.add( iCurrent );
			return;
		}
		long iVariableId = getVariableId( iCurrent );
		LongVector v = vVariables.elementAt( iVariableId );
		if( v == null ){
			v = g_ivFactory.getSmallVector();
			vVariables.set( iVariableId, v );
		}
		else if( v.contains( iCurrent ) )
			return;
		fillReachableVertices( getFalseChild( iCurrent ), vVariables, vLeaves );
		fillReachableVertices( getTrueChild( iCurrent ), vVariables, vLeaves );
		if( !v.contains( iCurrent ) )
			v.add( iCurrent );	
	}

	public void reduce(){
		reduce( null );
	}
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#reduce()
	 */
	public void reduce( AbstractionFilter aFilter ){
		if( m_bReduced && aFilter == null )
			return;
		ArrayVector<LongVector> vVariables = g_vvFactory.getSmallVector();
		vVariables.setSize( m_cVariables );
		LongVector vVariable = null; 
		long iCurrent = -1, iFalseChild = -1, iTrueChild = -1;
		Map<long[],Long> mProcessedVertices = new TreeMap<long[],Long>( ArrayComparator.getLongComparator() );
		LongVector vNewIds = g_ivFactory.getLargeVector();
		long iVariable = 0, iVertex = 0, iVariableId = 0, iVertexId = 0;
		long[] aiKey = null;
		
		for( iVariableId = 0 ; iVariableId < m_cVariables ; iVariableId++ )
			vVariables.add( g_ivFactory.getSmallVector() );
		fillReachableVertices( m_iRoot, vVariables, vNewIds );

		
		for( iVariable = vVariables.size() - 1 ; iVariable >= 0 ; iVariable-- ){
			vVariable = vVariables.elementAt( iVariable );
			mProcessedVertices.clear(); //only vertexes on the same level can be swapped
			if( vVariable != null ){
				for( iVertex = 0 ; iVertex < vVariable.size() ; iVertex++ ){
					iCurrent = vVariable.elementAt( iVertex );					
					iFalseChild = getFalseChild( iCurrent );
					iTrueChild = getTrueChild( iCurrent );

					if( sameVertex( iTrueChild, iFalseChild ) ){
						//System.out.prlongln( "Identical, Replacing " + vCurrent + " with " + vTrueChild );
						replaceVertex( iCurrent, iTrueChild );
					}
					else{
						aiKey = makeKey( iFalseChild, iTrueChild );
					
						if( mProcessedVertices.containsKey( aiKey ) ){
							replaceVertex( iCurrent, mProcessedVertices.get( aiKey ) );
						}
						else{
							mProcessedVertices.put( aiKey, iCurrent );
							vNewIds.add( iCurrent );
							if( m_bMaintainValueSum )
								setValueSum( iCurrent, aFilter );
						}
					}
				}
			}
		}
		if( m_bMaintainValueSum ){
			if( isLeaf( m_iRoot ) )
				setValueSum( m_iRoot, getValue( m_iRoot ), aFilter );
			else
				setValueSum( m_iRoot, aFilter );
		}
		
		vVariables.release();
		
		LongArrayVector vFormerVertices = m_vVertices;
		Map<Double,Long> mFormerValues = m_mLeaves;
		DoubleVector vValueSum = m_vValueSum;
		m_vParents.release();
		initDataStructures();
		m_vVertices.setSize( vNewIds.size() );
		if( m_bMaintainValueSum )
			m_vValueSum.setSize( vNewIds.size() );
		long[] aVertexData = null, aNewVertexData = null;
		long iPreviousId = 0, iNewId = 0;
		for( iNewId = 0 ; iNewId < vNewIds.size() ; iNewId++ ){
			iPreviousId = vNewIds.elementAt( iNewId );
			aVertexData = vFormerVertices.elementAt( iPreviousId );
			aNewVertexData = new long[aVertexData.length];
			if( aVertexData.length == VERTEX_ENTRIES ){
				aNewVertexData[VARIABLE_ID] = aVertexData[VARIABLE_ID];
				aNewVertexData[FALSE_CHILD] = vNewIds.indexOf( aVertexData[FALSE_CHILD] );
				aNewVertexData[TRUE_CHILD] = vNewIds.indexOf( aVertexData[TRUE_CHILD] );
			}
			else if( aVertexData.length == LEAF_ENTRIES ){
				double dValue = Double.longBitsToDouble( aVertexData[VALUE_ENTRY] );
				aNewVertexData[VARIABLE_ID] = aVertexData[VARIABLE_ID];
				aNewVertexData[VALUE_ENTRY] = aVertexData[VALUE_ENTRY];
				m_mLeaves.put( dValue, iNewId );
			}
			if( m_bMaintainValueSum )
				m_vValueSum.set( iNewId, vValueSum.elementAt( iPreviousId ) );
			m_vVertices.set( iNewId, aNewVertexData );
			m_vParents.add( g_ivFactory.getSmallVector() );
		}
		long iNewRoot = vNewIds.indexOf( m_iRoot );
		m_iRoot = iNewRoot;

		vFormerVertices.release();
		if( m_bMaintainValueSum )
			vValueSum.release();
		vNewIds.release();
		
		recomputeParents();

		m_bReduced = true;
	}
	
	private void recomputeParents(){
		long iVertex = 0, cVertexes = m_vVertices.size(), iParent = 0, cParents = 0;
		ArrayVector<LongVector> avParents = g_vvFactory.getLargeVector();
		LongVector lvOld = null, lvNew = null;
		
		if( m_vVertices.size() < Integer.MAX_VALUE ){
			computeParents( m_iRoot, new boolean[(int)m_vVertices.size()] );
		}
		else
			computeParents( m_iRoot );		
		
		avParents.setSize( cVertexes );
		
		for( iVertex = 0 ; iVertex < m_vParents.size() ; iVertex++ ){
			lvOld = m_vParents.elementAt( iVertex );
			cParents = lvOld.size();
			lvNew = g_ivFactory.getSmallVector();
			lvNew.setSize( cParents );
			for( iParent = 0 ; iParent < cParents ; iParent++ ){
				lvNew.add( lvOld.elementAt( iParent ) );
			}
			avParents.add( lvNew );
		}
		m_vParents = avParents;
		
	}
	
	private void computeParents( long iCurrent, boolean[] abProcessed ){
		if( isLeaf( iCurrent ) || abProcessed[(int)iCurrent] )
			return;
		long iFalseChild = getFalseChild( iCurrent );
		long iTrueChild = getTrueChild( iCurrent );
		abProcessed[(int)iCurrent] = true;
		addParent( iFalseChild, iCurrent );
		addParent( iTrueChild, iCurrent );
		computeParents( iFalseChild, abProcessed );
		computeParents( iTrueChild, abProcessed );
	}
	
	private void computeParents( long iCurrent ){
		if( isLeaf( iCurrent ) )
			return;
		long iFalseChild = getFalseChild( iCurrent );
		long iTrueChild = getTrueChild( iCurrent );
		addParent( iFalseChild, iCurrent );
		addParent( iTrueChild, iCurrent );
		computeParents( iFalseChild );
		computeParents( iTrueChild );
	}
	
	private void addParent( long iChild, long iParent ){
		LongVector vParents = getParents( iChild );
		if( !vParents.contains( iParent ) )
			vParents.add( iParent );
	}
	
	private void removeParent( long iChild, long iParent ){
		LongVector vParents = getParents( iChild );
		if( vParents.contains( iParent ) )
			vParents.removeElement( iParent );
	}
	
	private LongVector getParents( long iChild ){
		return m_vParents.elementAt( iChild );
	}
	
	private void replaceChild( long iParent, long iPreviousChild, long iNewChild ){
		long[] aVertexData = null;
		aVertexData = m_vVertices.elementAt( iParent );
		if( aVertexData[FALSE_CHILD] == iPreviousChild )
			aVertexData[FALSE_CHILD] = iNewChild;
		if( aVertexData[TRUE_CHILD] == iPreviousChild )
			aVertexData[TRUE_CHILD] = iNewChild;
	}
	
	private void replaceVertex( long iCurrent, long iNew ){
		LongVector vParents = getParents( iCurrent ); 
		long iParent = 0, iParentIndex = 0;
		for( iParentIndex = 0 ; iParentIndex < vParents.size() ; iParentIndex++ ){
			iParent = vParents.elementAt( iParentIndex );
			replaceChild( iParent, iCurrent, iNew );
		}
		if( !isLeaf( iCurrent ) ){
			long iFalseChild = getFalseChild( iCurrent );
			long iTrueChild = getTrueChild( iCurrent );
			removeParent( iFalseChild, iCurrent );
			removeParent( iTrueChild, iCurrent );
		}
		if( m_iRoot == iCurrent )
			m_iRoot = iNew;
	}

	private boolean sameVertex( long iVertex1, long iVertex2 ) {
		if( iVertex1 == iVertex2 )
			return true;
		else if( isLeaf( iVertex1 ) && isLeaf( iVertex2 ) ){
			double dV1 = getValue( iVertex1 );
			double dV2 = getValue( iVertex2 );
			if( dV1 == dV2 )
				return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#toString()
	 */
	public String toString(){
		return toString( m_iRoot );
	}
	
	private String toString( long iCurrent ){
		if( isLeaf( iCurrent ) )
			return "(" + getValue( iCurrent ) + ")";
		String sData = "(id=" + iCurrent + ",var=" + getVariableId( iCurrent );
		sData += toString( getFalseChild( iCurrent ) );
		sData += toString( getTrueChild( iCurrent ) );
		sData += ")";
		return sData;
	}
	
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#getTreeString()
	 */
	public String getTreeString(){
		return getTreeString( m_iRoot, 0 );
	}
	
	private String getTreeString( long iCurrent, long iDepth ){
		String sOffset = "";
		for( long i = 0 ; i < iDepth ; i++ )
			sOffset += "  ";
		if( isLeaf( iCurrent ) )
			return sOffset + "(" + getValue( iCurrent ) + ")";
		String sTree = sOffset + "(id=" + iCurrent + ",var=" + getVariableId( iCurrent );
		if( m_bMaintainValueSum )
			sTree += ", sum= " + getValueSum( iCurrent ) + "\n";
		else
			sTree += "\n";
		//String sTree = sOffset + "(var=" + getVariableId( iCurrent ) + "\n";
		sTree += getTreeString( getFalseChild( iCurrent ), iDepth + 1 ) + "\n";
		sTree += getTreeString( getTrueChild( iCurrent ), iDepth + 1 ) + "\n";
		sTree += sOffset + ")";
		return sTree;
	}
	
	private long copyVertices( CompactAlgebraicDecisionDiagram dOriginal ){
		int i = 0;
		for( i = 0; i < dOriginal.m_vVertices.size(); i++ ){
			long[] aVertexData = dOriginal.m_vVertices.elementAt( i );
			m_vVertices.add( aVertexData.clone() );
		}
		for( Entry<Double,Long> e : dOriginal.m_mLeaves.entrySet() ){
			m_mLeaves.put( e.getKey(), e.getValue() );
		}
		long iElement = 0;
		double dValue = 0.0;
		for( iElement = 0 ; iElement < dOriginal.m_vValueSum.size() ; iElement++ ){
			dValue = dOriginal.m_vValueSum.elementAt( iElement );
			m_vValueSum.add( dValue );
		}
		for( i = 0; i < dOriginal.m_vParents.size(); i++ ){
			LongVector vParents = dOriginal.m_vParents.elementAt( i );
			m_vParents.add( g_ivFactory.getSmallVector() );
			m_vParents.elementAt( m_vParents.size() - 1 ).addAll( vParents );
		}
		m_dMaxValue = dOriginal.m_dMaxValue;
		m_iRoot = dOriginal.m_iRoot;
		return m_iRoot;
	}

	private long addVertex( long iVariableId ){
		return addVertex( iVariableId, -1, -1 );
	}
	
	private long addVertex( long iVariableId, long iFalseChild, long iTrueChild ){
		if( ( iTrueChild != -1 ) && ( iTrueChild == iFalseChild ) )
			return iFalseChild;
		long[] aVertexData = new long[VERTEX_ENTRIES];
		long iNewId = m_vVertices.size();
		aVertexData[VARIABLE_ID] = iVariableId;
		aVertexData[FALSE_CHILD] = iFalseChild;
		aVertexData[TRUE_CHILD] = iTrueChild;
		m_vVertices.add( aVertexData );
		m_vParents.add( g_ivFactory.getSmallVector() );
		if( m_bMaintainValueSum )
			m_vValueSum.add( Double.MAX_VALUE * -1 );
		if( iFalseChild != -1 )
			addParent( iFalseChild, iNewId );
		if( iTrueChild != -1 )
			addParent( iTrueChild, iNewId );
		return iNewId;
	}
	
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#addPath(boolean[], double)
	 */
	public void addPath( boolean[] abPath, double dValue ){
		addPath( abPath, dValue, 0 );
	}
	
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#finalizePaths(double)
	 */
	public void finalizePaths( double dDefaultValue ){
		long iDefaultValueIndex = getValueVertex( dDefaultValue );
		if( m_iRoot == -1 )
			m_iRoot = iDefaultValueIndex;
		for( int i = 0 ; i <  m_vVertices.size() ; i++ ){
			long[] aVertexData = m_vVertices.elementAt( i );
			if( aVertexData.length == VERTEX_ENTRIES ){
				if( aVertexData[FALSE_CHILD] == -1 )
					aVertexData[FALSE_CHILD] = iDefaultValueIndex;
				if( aVertexData[TRUE_CHILD] == -1 )
					aVertexData[TRUE_CHILD] = iDefaultValueIndex;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#addPath(boolean[], double, long)
	 */
	public void addPath( boolean[] abPath, double dValue, int iFirstVariable ){
		if( abPath == null ){
			m_iRoot = getValueVertex( dValue );
			return;
		}
	
		long iCurrent = -1;
		int iVar = 0;
		if( m_iRoot == -1 ){
			m_iRoot = addVertex( iFirstVariable );
		}
		iCurrent = m_iRoot;
		for( iVar = 0 ; iVar < abPath.length - 1 ; iVar++ ){ 
			if( abPath[iVar] ){ //goto the True child
				if( !hasTrueChild( iCurrent ) ){
					setTrueChild( iCurrent, addVertex( iFirstVariable + iVar + 1 ) );
				}
				iCurrent = getTrueChild( iCurrent );
			}
			else{ //goto the False child
				if( !hasFalseChild( iCurrent ) ){
					setFalseChild( iCurrent, addVertex( iFirstVariable + iVar + 1 ) );
				}
				iCurrent = getFalseChild( iCurrent );
			}
		}
		if( abPath[iVar] ){ //set the True value
			setTrueChild( iCurrent, getValueVertex( dValue ) );
		}
		else{ //set the False value
			setFalseChild( iCurrent, getValueVertex( dValue ) );
		}
	}

	//if bTwoTimeSteps = true then abValues has the structure [pre,post,pre,post,...]
	//otherwise abValues has the structure [pre,pre,pre,...]
	//and aiVars has the structure [pre,pre,pre,..]
	public void addPartialPath( int[] aiVariables, boolean[] abValues, double dValue, boolean bTwoTimeSteps ){	
		long iCurrent = -1;
		int iValueIdx = 0, iCurrentVar = 0, iVarIdx = 0, iNextVarIdx = 0;
		if( m_iRoot == -1 ){
			if( aiVariables.length == 0 ){
				m_iRoot = getValueVertex( dValue );
				return;
			}
			if( bTwoTimeSteps ){
				m_iRoot = addVertex( aiVariables[0] * 2 );
			}
			else{
				m_iRoot = addVertex( aiVariables[0] );
			}
		}
		iCurrent = m_iRoot;
		for( iValueIdx = 0 ; iValueIdx < abValues.length - 1 ; iValueIdx++ ){
			iCurrentVar = getVariableId( iCurrent );
			if( iCurrentVar >= m_cVariables )
				Logger.getInstance().logError( "CompactAlgebraicDecisionDiagram", "addPartialPath", "Current var " + iCurrentVar + " greater than " + m_cVariables );
			if( bTwoTimeSteps ){
				if( iValueIdx % 2 == 0 ){//pre-action
					iVarIdx = aiVariables[iValueIdx / 2] * 2;
					iNextVarIdx = iVarIdx + 1;
				}
				else{ //post-action - move on to the next var
					iVarIdx = aiVariables[iValueIdx / 2] * 2 + 1;
					iNextVarIdx = aiVariables[iValueIdx / 2 + 1] * 2;
				}
			}
			else{
				iVarIdx = aiVariables[iValueIdx];
				iNextVarIdx = aiVariables[iValueIdx + 1];
			}
			
			if( abValues[iValueIdx] ){ //goto the True child
				if( !hasTrueChild( iCurrent ) ){
					setTrueChild( iCurrent, addVertex( iNextVarIdx ) );
				}
				iCurrent = getTrueChild( iCurrent );
			}
			else{ //goto the False child
				if( !hasFalseChild( iCurrent ) ){
					setFalseChild( iCurrent, addVertex( iNextVarIdx ) );
				}
				iCurrent = getFalseChild( iCurrent );
			}
		}
		if( abValues[iValueIdx] ){ //set the True value
			setTrueChild( iCurrent, getValueVertex( dValue ) );
		}
		else{ //set the False value
			setFalseChild( iCurrent, getValueVertex( dValue ) );
		}
	}

	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#valueAt(boolean[])
	 */
	public double valueAt( boolean[] abPath ){
		long iCurrent = m_iRoot;
		int iVar = 0;
		while( !isLeaf( iCurrent ) ){
			iVar = getVariableId( iCurrent );
			if( abPath[iVar] )
				iCurrent = getTrueChild( iCurrent );
			else
				iCurrent = getFalseChild( iCurrent );
		}
		return getValue( iCurrent );
	}
	
	private int find( int i, int[] a ){
		for( int j = 0 ; j < a.length ; j++ ){
			if( a[j] == i )
				return j;
		}
		return -1;
	}
	
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#valueAt(int[],boolean[])
	 */
	public double valueAt( int[] aiVariables, boolean[] abPath ){
		long iCurrent = m_iRoot;
		int iVar = 0, iIdx = 0;
		while( !isLeaf( iCurrent ) ){
			iVar = getVariableId( iCurrent );
			iIdx = find( iVar, aiVariables );
			if( iIdx == -1 )
				Logger.getInstance().logln( "*" );
			if( abPath[iIdx] )
				iCurrent = getTrueChild( iCurrent );
			else
				iCurrent = getFalseChild( iCurrent );
		}
		return getValue( iCurrent );
	}
	
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#getRootVariable()
	 */
	public long getRootVariable(){
		return getVariableId( m_iRoot );
	}

	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#existentialAbstraction(long, long)
	 */
	public void existentialAbstraction( long iVariable, long iRoot ){
		m_iRoot = existentialAbstraction( iVariable, iRoot, new TreeMap<long[], Long>( ArrayComparator.getLongComparator() ) );
	}
	
	private long existentialAbstraction( long iVariable, long iCurrent, Map<long[],Long> mExisting ){
		long iNew = -1, iTrue = -1, iFalse = -1;
		long iVariableId = getVariableId( iCurrent );
		if( isLeaf( iCurrent ) ){
			long cOccurences = (long)Math.pow( 2, m_cVariables - iVariable );
			iNew = getValueVertex( getValue( iCurrent ) * cOccurences );
		}
		else if( iVariableId == iVariable ){
			iNew = applyOperator( getFalseChild( iCurrent ), getTrueChild( iCurrent ), mExisting, getSum() );
		}
		else if( iVariableId > iVariable ){
			iNew = copyVerticesForExistentialAbstraction( iCurrent, mExisting );
		}
		else{
			iFalse = existentialAbstraction( iVariable, getFalseChild( iCurrent ), mExisting );
			iTrue = existentialAbstraction( iVariable, getTrueChild( iCurrent ), mExisting );
			iNew = addVertex( iVariableId, iFalse, iTrue );
		}
		
		return iNew;
	}
	
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#existentialAbstraction(pomdp.CompactAlgebraicDecisionDiagram.AbstractionFilter)
	 */
	public AlgebraicDecisionDiagram existentialAbstraction( AbstractionFilter aFilter ){
		CompactAlgebraicDecisionDiagram addAbstracted = new CompactAlgebraicDecisionDiagram( m_cVariables, true );
		addAbstracted.existentialAbstraction( aFilter, m_iRoot, this );
		return addAbstracted;
	}
	
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#existentialAbstraction(pomdp.CompactAlgebraicDecisionDiagram.AbstractionFilter, long)
	 */
	private void existentialAbstraction( AbstractionFilter aFilter, long iRoot, CompactAlgebraicDecisionDiagram add ){
		Map<long[],Long> mSumCache = new TreeMap<long[],Long>( ArrayComparator.getLongComparator() );
		Map<long[],Long> mAbstractCache = new TreeMap<long[],Long>( ArrayComparator.getLongComparator() );
		m_iRoot = existentialAbstraction( aFilter, aFilter.getFirstVariableId(), iRoot, add, mSumCache, mAbstractCache );
		//System.out.prlongln( getTreeString() );
		//eliminateUnrefVertexes(); looks like this will be difficult to do
	}
	
	public AlgebraicDecisionDiagram existentialAbstractionTopDown( AbstractionFilter aFilter ){
		CompactAlgebraicDecisionDiagram addAbstracted = new CompactAlgebraicDecisionDiagram( this );
		addAbstracted.existentialAbstractionTopDown( aFilter, m_iRoot );
		return addAbstracted;
	}
	private void existentialAbstractionTopDown( AbstractionFilter aFilter, long iRoot ){
		Map<long[],Long> mSumCache = new TreeMap<long[],Long>( ArrayComparator.getLongComparator() );
		Map<long[],Long> mAbstractCache = new TreeMap<long[],Long>( ArrayComparator.getLongComparator() );
		m_iRoot = existentialAbstractionTopDown( aFilter, 0, iRoot, mSumCache, mAbstractCache );
	}

	public static boolean bDebug = false;
	
	private long existentialAbstractionTopDown( AbstractionFilter aFilter, long iExpectedVariableId, long iCurrent,
			Map<long[],Long> mSumCache, Map<long[],Long> mAbstractCache ){
		long iNew = -1, iTrue = -1, iFalse = -1;
		int iVariableId = getVariableId( iCurrent ), iNextVariableId = iVariableId + 1;
		double dValue = 0.0;
		//if( bDebug )
		//	System.out.prlongln( getTreeString( iCurrent, 0 ) );
		if( isLeaf( iCurrent) ){
			if( iExpectedVariableId < m_cVariables ){
				long cOccurences = (long)Math.pow( 2, m_cVariables - iExpectedVariableId );
				dValue = getValue( iCurrent );
				iNew = getValueVertex( dValue * cOccurences );
			}
			else{
				dValue = getValue( iCurrent );
				iNew = getValueVertex( dValue );
			}
		}
		else{
			long[] aiKey = makeKey( iCurrent );
			if( mAbstractCache.containsKey( aiKey ) )
				iNew = mAbstractCache.get( aiKey );
			else{
				//if( iCurrent == 42 )
					//System.out.prlongln( getTreeString( iCurrent, 0 ) );
				iFalse = getFalseChild( iCurrent );
				iTrue = getTrueChild( iCurrent );
				if( aFilter.abstractVariable( iVariableId ) ){
					//System.out.prlongln( getTreeString( iCurrent, 0 ) );
					iNew = applyOperator( iFalse, iTrue, mSumCache, getSum() );
					//System.out.prlongln( getTreeString( iNew, 0 ) );
					if( !isLeaf( iNew ) )
						iNew = existentialAbstractionTopDown( aFilter, iNextVariableId, iNew, mSumCache, mAbstractCache );
					/*
					iFalse = getFalseChild( iNew );
					iTrue = getTrueChild( iNew );
					iVariableId = getVariableId( iNew );
					*/
				}
				else{
					iFalse = existentialAbstractionTopDown( aFilter, iNextVariableId, iFalse, mSumCache, mAbstractCache );
					iTrue = existentialAbstractionTopDown( aFilter, iNextVariableId, iTrue, mSumCache, mAbstractCache );
					
					iNew = addVertex( iVariableId, iFalse, iTrue );
				}
	
				mAbstractCache.put( aiKey, iNew );
			}
			//TODO - find bug
			if( iVariableId > iExpectedVariableId && aFilter.sumMissingLevels() ){ //if a level is missing
				long cMissingLevels = iVariableId - iExpectedVariableId, iMissingLevel = 0;
				//System.out.prlongln( getTreeString( iNew, 0 ) );
				for( iMissingLevel = 0 ; iMissingLevel < cMissingLevels ; iMissingLevel++ ){
					iNew = applyOperator( iNew, iNew, mSumCache, getSum() );
				}
				//System.out.prlongln( getTreeString( iNew, 0 ) );
			}
			
		}
		
		return iNew;
	}
	
	private long existentialAbstraction( AbstractionFilter aFilter, int iExpectedVariableId, long iCurrent, CompactAlgebraicDecisionDiagram add,
			Map<long[],Long> mSumCache, Map<long[],Long> mAbstractCache ){
		long iNew = -1, iTrue = -1, iFalse = -1;
		int iVariableId = add.getVariableId( iCurrent ), iNextExpected = -1;
		if( add.isLeaf( iCurrent) ){
			double dValue = add.getValue( iCurrent );
			if( iExpectedVariableId < aFilter.getLastVariableId() ){
				int cLevels = aFilter.countAbstractionVariablesBetween( iExpectedVariableId, aFilter.getLastVariableId() );
				int cOccurences = (int)Math.pow( 2, cLevels );
				iNew = getValueVertex( dValue * cOccurences );
			}
			else{
				iNew = getValueVertex( dValue );
			}
		}
		else{
			long[] aiKey = makeKey( iExpectedVariableId, iCurrent );
			if( mAbstractCache.containsKey( aiKey ) )
				iNew = mAbstractCache.get( aiKey );
			else{
				iNextExpected = aFilter.firstVariableAfter( iExpectedVariableId );
				if( iExpectedVariableId < iVariableId ){		
					iFalse = existentialAbstraction( aFilter, iNextExpected, iCurrent, add, mSumCache, mAbstractCache );
					iTrue = iFalse;
				}
				else{
					iFalse = existentialAbstraction( aFilter, iNextExpected, add.getFalseChild( iCurrent ), add, mSumCache, mAbstractCache );
					iTrue = existentialAbstraction( aFilter, iNextExpected, add.getTrueChild( iCurrent ), add, mSumCache, mAbstractCache );
				}	
				if( aFilter.abstractVariable( iExpectedVariableId ) ){
					iNew = applyOperator( iFalse, iTrue, mSumCache, getSum() );
				}
				else{
					iNew = addVertex( iExpectedVariableId, iFalse, iTrue );
				}
	
				mAbstractCache.put( aiKey, iNew );
			}
			
		}
		
		return iNew;
	}

	private long copyVerticesForExistentialAbstraction( long iCurrent, Map<long[], Long> mExisting ){
		long[] aiKey = makeKey( iCurrent );
		long iNew = -1, iTrue = -1, iFalse = -1;
		long iVariableId = getVariableId( iCurrent );
		if( mExisting.containsKey( aiKey ) )
			iNew = mExisting.get( aiKey );
		else{
			if( isLeaf( iCurrent ) ){
				iNew = getValueVertex( getValue( iCurrent ) * 2 );
			}
			else{
				iFalse = copyVerticesForExistentialAbstraction( getFalseChild( iCurrent ), mExisting );
				iTrue = copyVerticesForExistentialAbstraction( getTrueChild( iCurrent ), mExisting );
				iNew = addVertex( iVariableId );
				setChildren( iNew, iFalse, iTrue );
			}
		}
		mExisting.put( aiKey, iNew );
		return iNew;
	}

	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#copy()
	 */
	public CompactAlgebraicDecisionDiagram copy() {
		return new CompactAlgebraicDecisionDiagram( this );
	}

	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#translateVariables(long)
	 */
	public void translateVariables( int iOffset ){
		for( int i = 0 ; i < m_vVertices.size() ; i++ ){
			long[] aVertexData = m_vVertices.elementAt( i );
			aVertexData[VARIABLE_ID] = aVertexData[VARIABLE_ID] - iOffset;
		}
		m_cVariables -= iOffset;
	}

	protected int compare( long iCurrent1, CompactAlgebraicDecisionDiagram add1, long iCurrent2, CompactAlgebraicDecisionDiagram add2 ){
		boolean bLeaf1 = add1.isLeaf( iCurrent1 );
		boolean bLeaf2 = add2.isLeaf( iCurrent2 );
		if( bLeaf1 && bLeaf2 ){
			double dValue1 = add1.getValue( iCurrent1 ), dValue2 = add2.getValue( iCurrent2 );
			if( dValue1 > dValue2 )
				return 1;
			else if( dValue1 == dValue2 )
				return 0;
			else
				return -1;
		}
		if( bLeaf1 )
			return 1;
		if( bLeaf2 )
			return -1;
		int iVariableId1 = add1.getVariableId( iCurrent1 );
		int iVariableId2 = add2.getVariableId( iCurrent2 );
		if( iVariableId1 != iVariableId2 )
			return iVariableId1 - iVariableId2;
		int iResult = compare( add1.getTrueChild( iCurrent1 ), add1, add2.getTrueChild( iCurrent2 ), add2 );
		if( iResult != 0 )
			return iResult;
		return compare( add1.getFalseChild( iCurrent1 ), add1, add2.getFalseChild( iCurrent2 ), add2 );
	}
	
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#compareTo(pomdp.CompactAlgebraicDecisionDiagram)
	 */
	public int compareTo( CompactAlgebraicDecisionDiagram addOther ){
		return compare( m_iRoot, this, addOther.m_iRoot, addOther );
	}
	
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#compareTo(java.lang.Object)
	 */
	public int compareTo( AlgebraicDecisionDiagram addOther ){
		if( addOther instanceof CompactAlgebraicDecisionDiagram ){
			return compareTo( (CompactAlgebraicDecisionDiagram)addOther );
		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#product(double)
	 */
	public void product( double dFactor ){
		Map<Double,Long> mNewLeaves = new TreeMap<Double,Long>();
		long[] aValue = null;
		double dValue = 0.0;
		long iLeaf = 0, iCurrent = 0;
		for( Entry<Double,Long> e : m_mLeaves.entrySet() ){
			iLeaf = e.getValue();
			dValue = e.getKey();
			mNewLeaves.put( dValue * dFactor, iLeaf );
			setLeafValue( iLeaf, dValue * dFactor );
		}
		m_mLeaves = mNewLeaves;
		for( iCurrent = 0 ; iCurrent < m_vValueSum.size(); iCurrent++ ){
			dValue = m_vValueSum.elementAt( iCurrent );
			m_vValueSum.set( iCurrent, dValue * dFactor );
		}
		m_dValueSum *= dFactor;
	}

	//there may be a bug if the root of both ADDs is not the first variable  
	private double innerProduct( long iAlpha, CompactAlgebraicDecisionDiagram addAlpha, 
			long iBelief, CompactAlgebraicDecisionDiagram addBelief, long iLastVariable,
			Map<long[],Double> mCachedResults ){
		double dValue = 0.0, dFalseValue = 0.0, dTrueValue = 0.0, dSumValues = 0.0;
		long cOccurences = 0;
		long iAlphaVariableId = addAlpha.getVariableId( iAlpha ), iBeliefVariableId = addBelief.getVariableId( iBelief );
		
		m_cIPOperations++;
		
		//Logger.getInstance().log( "ADD", 0, "dotProduct", "a = " + vAlpha + "b = " + vBelief );
		if( addAlpha.isLeaf( iAlpha ) && addBelief.isLeaf( iBelief ) ){
			//Logger.getInstance().log( "ADD", 0, "dotProduct", "two value nodes" );
			dValue = addAlpha.getValue( iAlpha ) * addBelief.getValue( iBelief );
			cOccurences = (long)Math.pow( 2, m_cVariables - iLastVariable - 1 );
			return dValue * cOccurences;
		}
		else if( addAlpha.isLeaf( iAlpha ) ){
			//Logger.getInstance().log( "ADD", 0, "dotProduct", "alpha is a value" );
			dSumValues = addBelief.getValueSum( iBelief );
			dValue = addAlpha.getValue( iAlpha );
			cOccurences = (long)Math.pow( 2, iBeliefVariableId - iLastVariable - 1 );
			return dValue * dSumValues * cOccurences;
		}
		else if( addBelief.isLeaf( iBelief ) ){
			//Logger.getInstance().log( "ADD", 0, "dotProduct", "b is a value" );
			dSumValues = addAlpha.getValueSum( iAlpha );
			dValue = addBelief.getValue( iBelief );
			cOccurences = (long)Math.pow( 2, iAlphaVariableId - iLastVariable - 1 );
			return dValue * dSumValues * cOccurences;
		}
		else{
			m_cIPOperations--;//count only actual products
			//Logger.getInstance().log( "ADD", 0, "dotProduct", "two inner nodes" );
//			String sKey = makeKey( vAlpha, vBelief );
//			if( mCachedResults.containsKey( sKey ) )
//				return mCachedResults.get( sKey );
			long[] aiKey = makeKey( iAlpha, iBelief );
			if( mCachedResults.containsKey( aiKey ) ){
				dValue = mCachedResults.get( aiKey );
				if( iAlphaVariableId == iBeliefVariableId ){
					cOccurences = (long)Math.pow( 2, iAlphaVariableId - iLastVariable - 1 );
				}
				else if( iAlphaVariableId < iBeliefVariableId ){
					cOccurences = (long)Math.pow( 2, iAlphaVariableId - iLastVariable - 1 );
				}
				else{//iAlphaVariableId > iBeliefVariableId
					cOccurences = (long)Math.pow( 2, iBeliefVariableId - iLastVariable - 1 );
				}
				dValue = cOccurences * dValue;
			}
			else{
				if( iAlphaVariableId == iBeliefVariableId ){
					dFalseValue = innerProduct( addAlpha.getFalseChild( iAlpha ), addAlpha, 
							addBelief.getFalseChild( iBelief ), addBelief, 
							iAlphaVariableId, mCachedResults );
					dTrueValue = innerProduct( addAlpha.getTrueChild( iAlpha ), addAlpha, 
							addBelief.getTrueChild( iBelief ), addBelief, 
							iAlphaVariableId, mCachedResults );
					cOccurences = (long)Math.pow( 2, iAlphaVariableId - iLastVariable - 1 );
				}
				else if( iAlphaVariableId < iBeliefVariableId ){
					dFalseValue = innerProduct( addAlpha.getFalseChild( iAlpha ), addAlpha, 
							iBelief, addBelief, 
							iAlphaVariableId, mCachedResults );
					dTrueValue = innerProduct( addAlpha.getTrueChild( iAlpha ), addAlpha, 
							iBelief, addBelief, 
							iAlphaVariableId, mCachedResults );
					cOccurences = (long)Math.pow( 2, iAlphaVariableId - iLastVariable - 1 );
				}
				else{//iAlphaVariableId > iBeliefVariableId
					dFalseValue = innerProduct( iAlpha, addAlpha, 
							addBelief.getFalseChild( iBelief ), addBelief, 
							iBeliefVariableId, mCachedResults );
					dTrueValue = innerProduct( iAlpha, addAlpha, 
							addBelief.getTrueChild( iBelief ), addBelief, 
							iBeliefVariableId, mCachedResults );
					cOccurences = (long)Math.pow( 2, iBeliefVariableId - iLastVariable - 1 );
				}
				dValue = cOccurences * ( dFalseValue + dTrueValue );
				mCachedResults.put( aiKey, ( dFalseValue + dTrueValue ) );
			}
			return dValue;
		}
	}
	
	//addOther must be a probabilistic ADD
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#innerProduct(pomdp.CompactAlgebraicDecisionDiagram)
	 */
	public double innerProduct( AlgebraicDecisionDiagram addOther ){
		CompactAlgebraicDecisionDiagram addBelief = (CompactAlgebraicDecisionDiagram)addOther;
		double dValue = innerProduct( m_iRoot, this, addBelief.m_iRoot, addBelief, -1, new TreeMap<long[],Double>( ArrayComparator.getLongComparator() ) );
		return dValue;
	}

	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#dominates(pomdp.CompactAlgebraicDecisionDiagram)
	 */
	public boolean dominates( AlgebraicDecisionDiagram addOther ){
		CompactAlgebraicDecisionDiagram add = (CompactAlgebraicDecisionDiagram)addOther;
		if( add.m_dMaxValue > m_dMaxValue )
			return false;
		if( add.getValueSum() > getValueSum() )
			return false;
		return dominates( m_iRoot, this, add.m_iRoot, add );
	}

	//Does add1 dominates add2
	private boolean dominates( long iCurrent1, CompactAlgebraicDecisionDiagram add1, long iCurrent2, CompactAlgebraicDecisionDiagram add2 ){
		if( add1.isLeaf( iCurrent1 ) && add2.isLeaf( iCurrent2 ) ){
			return add1.getValue( iCurrent1 ) >= add2.getValue( iCurrent2 );
		}
		else{
			long iVariableId1 = add1.getVariableId( iCurrent1 );
			long iVariableId2 = add2.getVariableId( iCurrent2 );
			if( iVariableId1 == iVariableId2 ){
				if( add1.getValueSum( iCurrent1 ) < add2.getValueSum( iCurrent2 ) )
					return false;
				return dominates( add1.getFalseChild( iCurrent1 ), add1, add2.getFalseChild( iCurrent2 ), add2 ) && 
					dominates( add1.getTrueChild( iCurrent1 ), add1, add2.getTrueChild( iCurrent2 ), add2 );
			}
			else if( iVariableId1 < iVariableId2 ){
				return dominates( add1.getFalseChild( iCurrent1 ), add1, iCurrent2, add2 ) && 
					dominates( add1.getTrueChild( iCurrent1 ), add1, iCurrent2, add2 );
			}
			else{// iVariableId1 > iVariableId2 ){
				return dominates( iCurrent1, add1, add2.getFalseChild( iCurrent2 ), add2 ) && 
					dominates( iCurrent1, add1, add2.getTrueChild( iCurrent2 ), add2 );
			}
		}
	}
	
	public boolean equals( AlgebraicDecisionDiagram addOther ){
		if( addOther.getMaxValue() != m_dMaxValue )
			return false;
		CompactAlgebraicDecisionDiagram add = (CompactAlgebraicDecisionDiagram)addOther;
		return equals( m_iRoot, this, add.m_iRoot, add );
	}

	private boolean equals( long iCurrent1, CompactAlgebraicDecisionDiagram add1, long iCurrent2, CompactAlgebraicDecisionDiagram add2 ){
		if( add1.isLeaf( iCurrent1 ) && add2.isLeaf( iCurrent2 ) ){
			return add1.getValue( iCurrent1 ) == add2.getValue( iCurrent2 );
		}
		else{
			long iVariableId1 = add1.getVariableId( iCurrent1 );
			long iVariableId2 = add2.getVariableId( iCurrent2 );
			if( iVariableId1 == iVariableId2 ){
				return equals( add1.getFalseChild( iCurrent1 ), add1, add2.getFalseChild( iCurrent2 ), add2 ) && 
				equals( add1.getTrueChild( iCurrent1 ), add1, add2.getTrueChild( iCurrent2 ), add2 );
			}
			else if( iVariableId1 < iVariableId2 ){
				return equals( add1.getFalseChild( iCurrent1 ), add1, iCurrent2, add2 ) && 
					equals( add1.getTrueChild( iCurrent1 ), add1, iCurrent2, add2 );
			}
			else{// iVariableId1 > iVariableId2 ){
				return equals( iCurrent1, add1, add2.getFalseChild( iCurrent2 ), add2 ) && 
					equals( iCurrent1, add1, add2.getTrueChild( iCurrent2 ), add2 );
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#getVertexCount()
	 */
	public long getVertexCount() {
		return m_vVertices.size();
	}

	private long m_cIPOperations = 0;
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#getLastInnerProductOperationsCount()
	 */
	public long getLastInnerProductOperationsCount() {
		long c = m_cIPOperations;
		m_cIPOperations = 0;
		return c;
	}
	
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#reduceToMin(double)
	 */
	public void reduceToMin( double dSpan ){
		if( isLeaf( m_iRoot ) )
			return;
		
		if( dSpan > 0.0 ){
			double dLowerBound = Double.MAX_VALUE * -1;
			Map<Double,Long> mReduced = new TreeMap<Double,Long>();
			long iLeaf = 0, iLowerBound = 0;
			double dValue = 0.0;
			long[] aLowerBound = null;
			for( Entry<Double,Long> e : m_mLeaves.entrySet() ){
				iLeaf = e.getValue();
				dValue = getValue( iLeaf );
				if( dValue != 0.0 ){
					if( dValue < dLowerBound + dSpan ){
						replaceVertex( iLeaf, iLowerBound );
						m_bReduced = false;
					}
					else{
						iLowerBound = iLeaf;
						dLowerBound = dValue;
						mReduced.put( dLowerBound, iLeaf );
					}
				}
			}
			m_mLeaves = mReduced;
		}
		reduce();
	}

	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#getTouchedVertexCount()
	 */
	public long getTouchedVertexCount(){
		return m_cTouchedVertexes;
	}

	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#translateVariables(pomdp.VariableTranslator)
	 */
	public void translateVariables( VariableTranslator vt ){
		for( int i = 0 ; i < m_vVertices.size() ; i++ ){
			if( !isLeaf( i ) ){
				long[] aVertexData = m_vVertices.elementAt( i );
				int iNew = vt.translate( (int)aVertexData[VARIABLE_ID] );
				aVertexData[VARIABLE_ID] = iNew;
			}

		}
		m_cVariables = vt.translateVariableCount( m_cVariables );
	}
	
	private BinaryOperator getSum(){
		if( m_boSum == null )
			m_boSum = new Sum();
		return m_boSum;
	}
	private BinaryOperator getProduct(){
		if( m_boProduct == null )
			m_boProduct = new Product();
		return m_boProduct;
	}
		
	/* (non-Javadoc)
	 * @see pomdp.AlgebraicDecisionDiagram#productAndAbstract(pomdp.CompactAlgebraicDecisionDiagram, long)
	 */
	public AlgebraicDecisionDiagram productAndAbstract( 
			AlgebraicDecisionDiagram add, long iFirstVariableToReduce ){
		CompactAlgebraicDecisionDiagram addOther = (CompactAlgebraicDecisionDiagram)add;
		CompactAlgebraicDecisionDiagram addResult = new CompactAlgebraicDecisionDiagram( m_cVariables, true );
		//System.out.prlongln( getTreeString() );
		//System.out.prlongln( addOther.getTreeString() );
		m_cTouchedVertexes = 0;
		addResult.m_iRoot = addResult.productAndAbstract( m_iRoot, this, addOther,
				new TreeMap<long[],Double>( ArrayComparator.getLongComparator() ), iFirstVariableToReduce );
		addResult.m_cVariables /= 2;
		return addResult;
	}

	private long productAndAbstract( long iCurrent1, CompactAlgebraicDecisionDiagram add1,
			CompactAlgebraicDecisionDiagram add2,
			Map<long[], Double> mCache, long iFirstVariableToReduce ){
		//m_cTouchedVertexes++;
		long iNew = -1, iVariableId = add1.getVariableId( iCurrent1 );
		if( add1.getVariableId( iCurrent1 ) < iFirstVariableToReduce ){
			iNew = addVertex( iVariableId );
			setFalseChild( iNew, productAndAbstract( add1.getFalseChild( iCurrent1 ), add1, add2, mCache, iFirstVariableToReduce ) );
			setTrueChild( iNew, productAndAbstract( add1.getTrueChild( iCurrent1 ), add1, add2, mCache, iFirstVariableToReduce ) );
		}
		else{
			//System.out.prlongln( v1.getTreeString( 0 ) );
			m_cIPOperations = 0;
			double dInnerProductValue = innerProduct( iCurrent1, add1, add2.m_iRoot, add2, iFirstVariableToReduce - 1, mCache );
			m_cTouchedVertexes += m_cIPOperations;
			iNew = getValueVertex( dInnerProductValue );
		}
		return iNew;
	}

	public double getValueSum(){
		return m_dValueSum;
	}
	public double getMaxValue(){
		return m_dMaxValue;
	}
	public long getVariableCount() {
		return m_cVariables;
	}
	public static void resetFactories() {
		g_ivFactory.clear();
		g_ivFactory = new LongVectorFactory();
		g_dvFactory.clear();
		g_dvFactory = new DoubleVectorFactory();
		g_avFactory.clear();
		g_avFactory = new LongArrayVectorFactory();
		g_vvFactory.clear();
		g_vvFactory = new ArrayVectorFactory<LongVector>();
	}
	public void validateSize() {
		long cVertexes = getVertexCount();
		m_vVertices.validateSize( cVertexes );
		//m_mLeaves.validateSize( cVertexes );
		m_vParents.validateSize( cVertexes );
		m_vValueSum.validateSize( cVertexes );
	}
	
	
	public void save( FileWriter fw ) throws IOException{
		fw.write( "<ADD VertexCount = \"" + m_vVertices.size() + "\" VariableCount = \"" + m_cVariables + "\" Root = \"" + m_iRoot + "\">" );
		long iVertex = 0;
		for( iVertex = 0 ; iVertex < getVertexCount() ; iVertex++ ){
			if( isLeaf( iVertex ) ){
				fw.write( "<Value id = \"" + iVertex + 
						"\" value = \"" + getValue( iVertex ) + 
						"\"/>" );
			}
			else{
				fw.write( "<Vertex id = \"" + iVertex + 
						"\" variable = \"" + getVariableId( iVertex ) + 
						"\" false = \"" + getFalseChild( iVertex ) + 
						"\" true = \"" + getTrueChild( iVertex ) + 
						"\"/>" );
			}		
		}
		fw.write( "</ADD>" );
	}
	
	public void parseXML( Element eADD ){
		NodeList nlChildren = eADD.getChildNodes();
		Element eChild = null;
		int iChild = 0;
		long iVertex = 0, iTrueChild = 0, iFalseChild = 0, iId = 0;
		long iVariableId = 0;
		int cVariables = Integer.parseInt( eADD.getAttribute( "VariableCount" ) );
		long cVertexes = Long.parseLong( eADD.getAttribute( "VertexCount" ) );
		long iRoot = Long.parseLong( eADD.getAttribute( "Root" ) );
		double dValue = 0;
		HashMap<Long,Long> mIds = new HashMap<Long,Long>();
		
		m_cVariables = cVariables;
		
		for( iChild = 0 ; iChild < nlChildren.getLength() ; iChild++ ){
			eChild = (Element) nlChildren.item( iChild );
			iVertex = Long.parseLong( eChild.getAttribute( "id" ) );
			if( eChild.getNodeName().equals( "Value" ) ){
				dValue = Double.parseDouble( eChild.getAttribute( "value" ) );
				iId = getValueVertex( dValue );
			}
			else if( eChild.getNodeName().equals( "Vertex" ) ){
				iVariableId = Long.parseLong( eChild.getAttribute( "variable" ) );
				iId = addVertex( iVariableId );
			}
			mIds.put( iVertex, iId );
		}
		for( iChild = 0 ; iChild < nlChildren.getLength() ; iChild++ ){
			eChild = (Element) nlChildren.item( iChild );
			iVertex = Long.parseLong( eChild.getAttribute( "id" ) );
			iId = mIds.get( iVertex );
			if( eChild.getNodeName().equals( "Vertex" ) ){
				iTrueChild = Long.parseLong( eChild.getAttribute( "true" ) );
				setTrueChild( iId, mIds.get( iTrueChild ) );
				iFalseChild = Long.parseLong( eChild.getAttribute( "false" ) );
				setFalseChild( iId, mIds.get( iFalseChild ) );
			}
		}
		m_iRoot = mIds.get( iRoot );
		if( !isLeaf( m_iRoot ) )
			reduce();
	}
	
	public Element getDOM( Document doc ){
		Element eADD = doc.createElement( "ADD" );
		Element eVertex = null;
		
		eADD.setAttribute( "VertexCount",  "" + m_vVertices.size() );
		eADD.setAttribute( "VariableCount",  "" + m_cVariables );
		eADD.setAttribute( "Root",  "" + m_iRoot );

		long iVertex = 0;
		for( iVertex = 0 ; iVertex < getVertexCount() ; iVertex++ ){
			if( isLeaf( iVertex ) ){
				eVertex = doc.createElement( "Value" );
				eVertex.setAttribute( "value",  "" + getValue( iVertex ) );
			}
			else{
				eVertex = doc.createElement( "Vertex" );
				eVertex.setAttribute( "variable",  "" + getVariableId( iVertex ) );
				eVertex.setAttribute( "false",  "" + getFalseChild( iVertex ) );
				eVertex.setAttribute( "true",  "" + getTrueChild( iVertex ) );
			}
			eVertex.setAttribute( "id",  "" + iVertex );
			eADD.appendChild( eVertex );

		}
		return eADD;
	}
	public double innerProduct( double[] adProbabilities ){
		return innerProduct( m_iRoot, adProbabilities, 1.0 );
	}
	private double innerProduct( long iCurrent, double[] adProbabilities, double dProbability ){
		if( dProbability == 0.0 )
			return 0.0;
		if( isLeaf( iCurrent ) )
			return getValue( iCurrent ) * dProbability;
		int iVariableId = getVariableId( iCurrent );
		double dProb = adProbabilities[iVariableId];
		long iFalseChild = getFalseChild( iCurrent );
		long iTrueChild = getTrueChild( iCurrent );
		double dFalseValue =  innerProduct( iFalseChild, adProbabilities, dProbability * ( 1 - dProb ) );
		double dTrueValue =  innerProduct( iTrueChild, adProbabilities, dProbability * dProb );
		return dFalseValue + dTrueValue;
	}
	
	private double m_dSumVisited = 0.0;
	public double innerProduct( PathProbabilityEstimator p ){
		Vector<Pair<Integer, Boolean>> vAssignment = new Vector<Pair<Integer, Boolean>>();
		m_dSumVisited = 0.0;
		double dValue = innerProduct( m_iRoot, p, vAssignment );
		return dValue;
	}
	private double innerProduct( long iCurrent, PathProbabilityEstimator p, Vector<Pair<Integer, Boolean>> vAssignment ){
		if( isLeaf( iCurrent ) ){
			double dProbability = p.valueAt( vAssignment );
			double dValue = getValue( iCurrent );
			m_dSumVisited += dProbability;
			return dValue * dProbability;
		}
		int iVariableId = getVariableId( iCurrent );
		long iFalseChild = getFalseChild( iCurrent );
		long iTrueChild = getTrueChild( iCurrent );
		Pair<Integer, Boolean> pAssignment = new Pair<Integer, Boolean>( iVariableId, false );
		vAssignment.add( pAssignment );
		double dFalseValue =  innerProduct( iFalseChild, p, vAssignment );
		pAssignment.setValue( true );
		double dTrueValue =  innerProduct( iTrueChild, p, vAssignment );
		vAssignment.remove( pAssignment );
		return dFalseValue + dTrueValue;
	}
	public double innerProduct( AlgebraicDecisionDiagram[] addComponentProbabilities, int[][] aiComponentVariables ){
		boolean[] abPath = new boolean[m_cVariables];
		return innerProduct( m_iRoot, 0, 0, addComponentProbabilities, aiComponentVariables, abPath );
	}
	private double innerProduct( long iCurrent, int iExpectedVarId, int iComponent, AlgebraicDecisionDiagram[] addComponentProbabilities, int[][] aiComponentVariables, boolean[] abPath ){
		double dValue = 0.0;
		double dProb = 1.0;
		double dFalseValue = 0.0;
		double dTrueValue = 0.0;
		long iFalseChild = iCurrent;
		long iTrueChild = iCurrent;
		
		if( isLeaf( iCurrent ) && getValue( iCurrent ) == 0.0 )
			return 0.0;
		
		if( iExpectedVarId == m_cVariables ){
			dValue = getValue( iCurrent );
			dProb = addComponentProbabilities[iComponent].valueAt( abPath );
		}
		else{
			int iVariableId = getVariableId( iCurrent );
			int iNewComponent = iComponent;
			if( aiComponentVariables[iComponent][aiComponentVariables[iComponent].length - 1] == ( iExpectedVarId - 1 ) )
				iNewComponent = iComponent + 1;
			if( iComponent != iNewComponent ){
				dProb = addComponentProbabilities[iComponent].valueAt( abPath );		
			}
			if( dProb != 0.0 ){
				if( iExpectedVarId == iVariableId ){
					iFalseChild = getFalseChild( iCurrent );
					iTrueChild = getTrueChild( iCurrent );
				}
				abPath[iExpectedVarId] = false;
				dFalseValue = innerProduct( iFalseChild, iExpectedVarId + 1, iNewComponent, addComponentProbabilities, aiComponentVariables, abPath );
				abPath[iExpectedVarId] = true;
				dTrueValue = innerProduct( iTrueChild, iExpectedVarId + 1, iNewComponent, addComponentProbabilities, aiComponentVariables, abPath );
				dValue = ( dFalseValue + dTrueValue );
			}
		}
		return dValue * dProb;
	}
	
	public Iterator<Entry<Integer, Double>> getNonZeroEntries() {
		// BUGBUG - can get partial paths but then would need to make them into complete paths
		return null;
	}
	
	@Override
	public void setUnspecifiedVariablesToWorstCase( Vector<Integer> vUnspecifiedVariables ) {
		for( int iVariable : vUnspecifiedVariables )
			m_iRoot = setUnspecifiedVariableToWorstCase( m_iRoot, iVariable );
		reduce();
	}
	
	private long setUnspecifiedVariableToWorstCase( long iCurrentVertex, int iVariable ){
		int iCurrentVariable = getVariableId( iCurrentVertex );
		if( iCurrentVariable > iVariable ){
			long iZeroVertex = getValueVertex( 0.0 );
			long iNewVertex = addVertex( iVariable );
			setTrueChild( iNewVertex, iZeroVertex );
			setFalseChild( iNewVertex, iCurrentVertex );
			return iNewVertex;
		}
		
		long iTrueChild = getTrueChild( iCurrentVertex );
		long iFalseChild = getFalseChild( iCurrentVertex );
		
		setTrueChild( iCurrentVertex, setUnspecifiedVariableToWorstCase( iTrueChild, iVariable ) );
		setFalseChild( iCurrentVertex, setUnspecifiedVariableToWorstCase( iFalseChild, iVariable ) );
		return iCurrentVertex;
	}

}
