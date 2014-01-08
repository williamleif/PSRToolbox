package pomdp.utilities.factored;

import java.util.Iterator;
import java.util.Vector;

import pomdp.utilities.Logger;
import pomdp.utilities.factored.AlgebraicDecisionDiagram.AbstractionFilter;

public class AlgebraicDecisionDiagramVertex {
	private AlgebraicDecisionDiagramVertex m_vTrue, m_vFalse;
	protected Vector<AlgebraicDecisionDiagramVertex> m_vParents;
	private int m_iVariableId;
	private long m_iVertexId;
	protected long m_iGlobalVertexId;
	protected double m_dValueSum;
	
	//public static int[] g_aIDStatus = new int[1000000];
	
	public static int g_cLiveVertexes = 0, g_cVertexes = 0;
	
	public AlgebraicDecisionDiagramVertex( int iVariableId, int iVerticeId ){
		m_iVariableId = iVariableId;
		m_vTrue = null;
		m_vFalse = null;
		m_iVertexId = iVerticeId;
		m_vParents = new Vector<AlgebraicDecisionDiagramVertex>();
		m_dValueSum = Double.MAX_VALUE * -1;
		g_cLiveVertexes++;
		m_iGlobalVertexId = g_cVertexes;
		g_cVertexes++;
	}
	
	public AlgebraicDecisionDiagramVertex( int iVariableId, long iVerticeId, AlgebraicDecisionDiagramVertex vFalse, AlgebraicDecisionDiagramVertex vTrue ){
		m_iVariableId = iVariableId;
		m_vTrue = vTrue;
		m_vFalse = vFalse;
		m_iVertexId = iVerticeId;
		m_vParents = new Vector<AlgebraicDecisionDiagramVertex>();
		m_dValueSum = Double.MAX_VALUE * -1;
		g_cLiveVertexes++;
		m_iGlobalVertexId = g_cVertexes;
		g_cVertexes++;
	}
	
	public long getVertexId(){
		return m_iVertexId;
	}
	public int getVariableId(){
		return m_iVariableId;
	}
	public AlgebraicDecisionDiagramVertex getTrueChild(){
		return m_vTrue;
	}
	public AlgebraicDecisionDiagramVertex getFalseChild(){
		return m_vFalse;
	}
	public void setTrue( AlgebraicDecisionDiagramVertex vTrue ){
		if( ( vTrue != null ) && !vTrue.isLeaf() && vTrue.getVariableId() <= getVariableId() )
			Logger.getInstance().logln( "BUGBUG" );
		if( m_vTrue != null ){
			m_dValueSum -= m_vTrue.m_dValueSum;
		}
		if( vTrue != null ){
			m_dValueSum += vTrue.m_dValueSum;
			vTrue.addParent( this );
		}
		m_vTrue = vTrue;
	}
	public void setFalse( AlgebraicDecisionDiagramVertex vFalse ){
		if( ( vFalse != null ) && !vFalse.isLeaf() && vFalse.getVariableId() <= getVariableId() )
			Logger.getInstance().logln( "BUGBUG" );
		if( m_vFalse != null ){
			m_dValueSum -= m_vFalse.m_dValueSum;
		}
		if( vFalse != null ){
			m_dValueSum += vFalse.m_dValueSum;
			vFalse.addParent( this );
		}
		m_vFalse = vFalse;
	}
	
	private void addParent( AlgebraicDecisionDiagramVertex vParent ){
		m_vParents.add( vParent );
	}
	
	public Iterator<AlgebraicDecisionDiagramVertex> getParents(){
		return m_vParents.iterator();
	}

	public String toString(){
		return "(id=" + m_iVertexId + ",var=" + m_iVariableId + getFalseChild() + getTrueChild() + ")";
	}

	public String getTreeString( int iDepth ){
		String sOffset = "";
		for( int i = 0 ; i < iDepth ; i++ )
			sOffset += "  ";
		String sTree = sOffset + "(id=" + m_iVertexId + ",var=" + m_iVariableId + "\n";
		if( getFalseChild() == null )
			sTree += sOffset + "  null\n";
		else
			sTree += getFalseChild().getTreeString( iDepth + 1 ) + "\n";
		if( getTrueChild() == null )
			sTree += sOffset + "  null\n";
		else
			sTree += getTrueChild().getTreeString( iDepth + 1 ) + "\n";
		sTree += sOffset + ")";
		return sTree;
	}
/*
	public void insertTree( Vector<Vector<Integer>> vVariables ){
		Vector<Integer> vCurrentVariable = null;
		if( vVariables.size() <= m_iVariableId )
			vVariables.setSize( m_iVariableId + 1 );
		if( vVariables.elementAt( m_iVariableId ) == null ){
			vVariables.set( m_iVariableId, new Vector<Integer>() );
		}
		vCurrentVariable = vVariables.get( m_iVariableId );
		if( !vCurrentVariable.contains( m_iVertexId ) ){
			vCurrentVariable.add( m_iVertexId );
			getFalseChild().insertTree( vVariables );
			getTrueChild().insertTree( vVariables );
		}
	}
*/
	public void replaceChild( AlgebraicDecisionDiagramVertex vCurrent, AlgebraicDecisionDiagramVertex vNew ){
		if( getTrueChild() != null && getTrueChild().equals( vCurrent ) )
			setTrue( vNew );
		if( getFalseChild() != null && getFalseChild().equals( vCurrent ) )
			setFalse( vNew );
		
	}

	public void setVerticeId( int iVertexId ){
		m_iVertexId = iVertexId;
	}

	public void setVariableId( int iVariableId ){
		m_iVariableId = iVariableId;
	}


	public void setValueSum( int cVariables ){
		double dFalseValue = 0.0, dTrueValue = 0.0;
		int cLeaves  = (int)Math.pow( 2, cVariables - getVariableId() - 1 );
		int cOccurences = -1;
		if( getFalseChild() instanceof AlgebraicDecisionDiagramValue ){
			dFalseValue = cLeaves * ((AlgebraicDecisionDiagramValue) getFalseChild()).getValue();
		}
		else{
			cOccurences = (int)Math.pow( 2, getFalseChild().getVariableId() - getVariableId() - 1 );
			dFalseValue = getFalseChild().getValueSum() * cOccurences;
				//Logger.getInstance().log( "ADDVertex", 0, "setValueSum",  "Gap between current and child" );
		}
		if( getTrueChild() instanceof AlgebraicDecisionDiagramValue ){
			dTrueValue = cLeaves * ((AlgebraicDecisionDiagramValue) getTrueChild()).getValue();
		}
		else{
			cOccurences = (int)Math.pow( 2, getTrueChild().getVariableId() - getVariableId() - 1 );
			dTrueValue = getTrueChild().getValueSum() * cOccurences;
				//Logger.getInstance().log( "ADDVertex", 0, "setValueSum",  "Gap between current and child" );
		}
		m_dValueSum = ( dTrueValue + dFalseValue );
	}
	
	public double setValueSum( AbstractionFilter aFilter, int cVariables ){
		double dFalseValue = 0.0, dTrueValue = 0.0;
		int iChildVariableId = 0;
		int cLeaves  = (int)Math.pow( 2, cVariables - m_iVariableId - 1 );
		int cOccurences = -1, cLevels = 0;
		if( aFilter != null ){
			cLevels = aFilter.countVariablesBetween( m_iVariableId, cVariables );
			cLeaves  = (int)Math.pow( 2, cLevels - 1 );
		}
		if( m_vFalse.isLeaf() ){
			dFalseValue = cLeaves * m_vFalse.getValue();
		}
		else{
			iChildVariableId = m_vFalse.getVariableId();
			if( aFilter == null ){
				cLevels = iChildVariableId - m_iVariableId;
			}
			else{
				cLevels = aFilter.countVariablesBetween( m_iVariableId, iChildVariableId );
			}
			cOccurences = (int)Math.pow( 2, cLevels - 1 );
			dFalseValue = m_vFalse.getValueSum() * cOccurences;
		}
		if( m_vTrue.isLeaf() ){
			dTrueValue = cLeaves * m_vTrue.getValue();
		}
		else{
			iChildVariableId = m_vTrue.getVariableId();
			if( aFilter == null ){
				cLevels = iChildVariableId - m_iVariableId;
			}
			else{
				cLevels = aFilter.countVariablesBetween( m_iVariableId, iChildVariableId );
			}
			cOccurences = (int)Math.pow( 2, cLevels - 1 );
			dTrueValue = m_vTrue.getValueSum() * cOccurences;
		}
		setValueSum( dTrueValue + dFalseValue );
		return m_dValueSum;
	}
	

	
	public void setValueSum( double dSum ){
		m_dValueSum = dSum;
	}
	public double getValueSum(){
		if( m_dValueSum == Double.MAX_VALUE * -1 ){
			Logger.getInstance().log( "ADDVertex", 0, "getValueSum",  "not initialized" );
		}
		return m_dValueSum;
	}

	public double sumValues( int iLastVariable, int cVariables ){
		return getFalseChild().sumValues( m_iVariableId, cVariables ) + 
			getTrueChild().sumValues( m_iVariableId, cVariables );
	}

	public boolean isLeaf() {
		return false;
	}

	public double getValue(){
		return 0;
	}
	public void clear() {
		if( getFalseChild() != null ){
			getFalseChild().removeParent( this );
		}
		if( getTrueChild() != null ){
			getTrueChild().removeParent( this );
		}
		m_vTrue = m_vFalse = null;
		if( m_vParents != null ){
			for( AlgebraicDecisionDiagramVertex vParent : m_vParents )
				vParent.replaceChild( this, null );
			m_vParents.clear();
		}
		 m_vParents = null;
	}
	public void removeParent( AlgebraicDecisionDiagramVertex vParent ){
		m_vParents.remove( vParent );	
	}

	public void clearSubTree() {
		if( getFalseChild() != null ){
			getFalseChild().clearSubTree();
			getFalseChild().removeParent( this );
		}
		if( getTrueChild() != null ){
			getTrueChild().clearSubTree();
			getTrueChild().removeParent( this );
		}
		clear();
	}

	public void clearParents() {
		m_vParents.clear();
	}
	
	public void computeParents( AlgebraicDecisionDiagramVertex vParent ){
		if( vParent != null )
			m_vParents.add( vParent );
		if( getFalseChild() != null ){
			getFalseChild().computeParents( this );
		}
		if( getTrueChild() != null ){
			getTrueChild().computeParents( this );
		}
	}
}
