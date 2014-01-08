package pomdp.utilities.datastructures;

import pomdp.utilities.Logger;

public class Matrix {
	protected int m_iSize;
	protected double[][] m_adCoefs;
	protected double[] m_adValues;
	
	public Matrix( int iSize ){
		m_iSize = iSize;
		m_adCoefs = new double[iSize][iSize];
		m_adValues = new double[iSize];
		int iRow = 0, iColumn = 0;
		for( iRow = 0 ; iRow < m_iSize ; iRow++ ){
			for( iColumn = 0 ; iColumn < m_iSize ; iColumn++ ){
				m_adCoefs[iRow][iColumn] = 0.0;
			}
			m_adValues[iRow] = 0.0;
		}
	}
	
	public void setValue( int iRow, double dValue ){
		m_adValues[iRow] = dValue;
	}
	
	public void setCoef( int iRow, int iColumn, double dValue ){
		m_adCoefs[iRow][iColumn] = dValue;
	}
	
	protected void multiplyRow( int iRow, double dValue ){
		int iColumn = 0;
		for( iColumn = 0 ; iColumn < m_iSize ; iColumn++ )
			m_adCoefs[iRow][iColumn] *= dValue;
		m_adValues[iRow] *= dValue;
	}
	
	protected void addRow( int iRow1, int iRow2, double dValue ){
		int iColumn = 0;
		for( iColumn = 0 ; iColumn < m_iSize ; iColumn++ )
			m_adCoefs[iRow2][iColumn] += dValue * m_adCoefs[iRow1][iColumn];
		m_adValues[iRow2] += dValue * m_adValues[iRow1];
	}
	
	protected void gaussianElimination(){
		int iRow = 0, iOtherRow = 0;
		for( iRow = 0 ; iRow < m_iSize ; iRow++ ){
			multiplyRow( iRow, 1.0 / m_adCoefs[iRow][iRow] );
			for( iOtherRow = 0 ; iOtherRow < m_iSize ; iOtherRow++ ){
				if( iOtherRow != iRow ){
					addRow( iRow, iOtherRow, m_adCoefs[iOtherRow][iRow] * -1.0 );
				}
			}
		}
	}
	
	public double[] solve(){
		gaussianElimination();
		return m_adValues;
	}

	protected void printRow( int iRow ){
		int iColumn = 0;
		for( iColumn = 0 ; iColumn < m_iSize ; iColumn++ ){
			Logger.getInstance().log( m_adCoefs[iRow][iColumn] + " * x" + iColumn + " + " );
		}
		Logger.getInstance().logln( " = " + m_adValues[iRow] );
	}
	
	public void print(){
		int iRow = 0, iColumn = 0;
		for( iRow = 0 ; iRow < m_iSize ; iRow++ ){
			printRow( iRow );
		}
		Logger.getInstance().logln( "+++++++++++++++++++++" );
	}
	
	public static void main( String[] args ){
		Matrix m = new Matrix( 3 );
		m.setCoef( 0, 0, 1 );
		m.setCoef( 0, 1, 2 );
		m.setCoef( 0, 2, 3 );
		m.setCoef( 1, 0, 3 );
		m.setCoef( 1, 1, 2 );
		m.setCoef( 1, 2, 1 );
		m.setCoef( 2, 0, 2 );
		m.setCoef( 2, 1, 1 );
		m.setCoef( 2, 2, 3 );
		m.setValue( 0, 1.0 );
		m.setValue( 1, 2.0 );
		m.setValue( 2, 3.0 );
		m.print();
		m.solve();
		m.print();
	}
}
