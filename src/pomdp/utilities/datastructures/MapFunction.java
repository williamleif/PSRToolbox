package pomdp.utilities.datastructures;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MapFunction extends Function{

	protected Map m_mValues;
	
	public MapFunction( int[] aDims ){
		super( aDims );
		m_mValues = new HashMap();
	}

	protected int[] toArray( int arg1, int arg2, int arg3 ){
		int[] aiRet = new int[3];
		aiRet[0] = arg1;
		aiRet[1] = arg2;
		aiRet[2] = arg3;
		return aiRet;
	}
	
	protected String toString( int arg1, int arg2, int arg3 ){
		return arg1 + "," + arg2 + "," + arg3;
	}
	
	protected String toString( int[] params ){
		int iArg = 0;
		String sResult = "";
		for( iArg = 0 ; iArg < params.length ; iArg++ ){
			sResult += params[iArg];
			if( iArg < params.length - 1 )
				sResult += ",";
		}
		return sResult;
	}
	
	public double valueAt( int arg1 ){
		return valueAt( arg1, -1, -1 );
	}

	public double valueAt( int arg1, int arg2 ){
		return valueAt( arg1, arg2, -1 );
	}

	protected double valueAt( String sKey ){
		Double dValue = (Double) m_mValues.get( sKey );
		if( dValue == null )
			return 0.0;
		else
			return dValue.doubleValue();
	}
	
	public double valueAt( int arg1, int arg2, int arg3 ){
		String sKey = toString( arg1, arg2, arg3 );
		return valueAt( sKey );
	}

	public double valueAt( int[] parameters ){
		String sKey = toString( parameters );
		return valueAt( sKey );
	}

	public void setValue( int arg1, double dValue ){
		setValue( arg1, -1, -1, dValue );
	}

	public void setValue( int arg1, int arg2, double dValue ) {
		setValue( arg1, arg2, -1, dValue );
	}

	public void setValue( int arg1, int arg2, int arg3, double dValue ) {
		//int[] aiKey = toArray( arg1, arg2, arg3 );
		String sKey = toString( arg1, arg2, arg3 );
		m_mValues.put( sKey, new Double( dValue ) );
	}

	public void setValue( int[] parameters, double dValue ){
		String sKey = toString( parameters );
		m_mValues.put( sKey, new Double( dValue ) );
	}
	
	public Iterator getNonZeroEntries( int arg1, int arg2 ){
		return null;
	}

	public int countNonZeroEntries( int arg1, int arg2 ){
		return -1;
	}

	public int countEntries() {
		return m_mValues.size();
	}

	public Iterator getNonZeroEntries() {
		return m_mValues.entrySet().iterator();
	}

	public int countNonZeroEntries() {
		return m_mValues.size();
	}	
}
