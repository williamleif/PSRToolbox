package pomdp.utilities.datastructures;

import java.util.Stack;

public class IntVectorFactory {
	private Stack<IntVector> m_sLargeVectors;
	private Stack<IntVector> m_sSmallVectors;
	private static IntVectorFactory g_ivFactory = null;
	
	private IntVectorFactory(){
		m_sLargeVectors = new Stack<IntVector>();
		m_sSmallVectors = new Stack<IntVector>();
	}
	
	public static IntVectorFactory getInstance(){
		if( g_ivFactory == null ){
			g_ivFactory = new IntVectorFactory();
		}
		return g_ivFactory;
	}
	
	public IntVector getSmallVector(){
		if( m_sSmallVectors.size() != 0 )
			return m_sSmallVectors.pop();
		else
			return new IntVector( 16, this, true );
	}
	public IntVector getLargeVector(){
		if( m_sLargeVectors.size() != 0 )
			return m_sLargeVectors.pop();
		else
			return new IntVector( 256, this, false );
	}
	public void recycleSmall( IntVector v ){
		m_sSmallVectors.push( v );
	}
	public void recycleLarge( IntVector v ){
		m_sLargeVectors.push( v );
	}
}
