package pomdp.utilities.factored;

import java.util.Vector;

import pomdp.utilities.Logger;

public class AlgebraicDecisionDiagramValue extends
		AlgebraicDecisionDiagramVertex {
	
	private double m_dValue;

	public AlgebraicDecisionDiagramValue( double dValue, int iVerticeId ){
		super( Integer.MAX_VALUE, iVerticeId );
		m_dValueSum = 0;
		m_dValue = dValue; 
	}
	public AlgebraicDecisionDiagramValue( double dValue, int iHiddenSubtreeHeight, int iVerticeId ){
		super( Integer.MAX_VALUE, iVerticeId );
		m_dValueSum = Math.pow( 2, iHiddenSubtreeHeight ) * dValue;
		m_dValue = dValue; 
	}
	
	public void setTrue( AlgebraicDecisionDiagramVertex vTrue ){
		Logger.getInstance().log( "AlgebraicDecisionDiagramValue", 0, "setTrue",  "not implemented" );
	}
	public void setFalse( AlgebraicDecisionDiagramVertex vFalse ){
		Logger.getInstance().log( "AlgebraicDecisionDiagramValue", 0, "setFalse",  "not implemented" );
	}
	
	public double getValue(){
		return m_dValue;
	}

	public String toString(){
		return "(v=" + m_dValue + ")";
	}
	public String getTreeString( int iDepth ){
		String sOffset = "";
		for( int i = 0 ; i < iDepth ; i++ )
			sOffset += "  ";
		return sOffset + toString();
	}
	public void insertTree( Vector<Vector<Integer>> vVariables ){
	}

	public void setValue( double dValue ){
		m_dValue = dValue;
	}
	
	public double sumValues( int iLastVariable, int cVariables ){
		return m_dValue * Math.pow( 2, cVariables - iLastVariable - 1 );
	}
	
	public boolean isLeaf() {
		return true;
	}

}
