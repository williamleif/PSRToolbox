package pomdp.utilities.sort;

import java.util.Comparator;
import java.util.Vector;

import pomdp.utilities.BeliefState;
import pomdp.utilities.Logger;

public class QuickSort implements SortStrategy {

	public QuickSort(){}
	
	public void sort( Object[] abElements, double[] adWeights ){
		quickSort( abElements, adWeights, 0, abElements.length - 1 );
	}
	
	protected void quickSort( Object[] abElements, double[] adWeights, int iStart, int iEnd ){
		if( iStart >= iEnd )
			return;
		int iPivot = partition( abElements, adWeights, iStart, iEnd );
		//printArray( aoUnsorted, iStart, iEnd, iPivot );
		//Logger.getInstance().logln( "quickSort " + iStart + " " + iPivot + " " + iEnd );
		quickSort( abElements, adWeights, iStart, iPivot - 1 );
		quickSort( abElements, adWeights, iPivot + 1, iEnd );
	}
	
	protected int partition( Object[] abElements, double[] adWeights, int iStart, int iEnd ) {
		int iLeft = iStart + 1, iRight = iEnd;
		double dPivot = adWeights[iStart];
		while( true ){
			while( ( iRight > iLeft ) && ( dPivot < adWeights[iRight] ) )
				iRight--;
			while( ( iRight > iLeft ) && (  dPivot > adWeights[iLeft] ) )
				iLeft++;
			if( iLeft < iRight ){
				swap( abElements, iLeft, iRight );
				swap( adWeights, iLeft, iRight );
			}
			else{
				swap( abElements, iLeft, iStart );
				swap( adWeights, iLeft, iStart );
				return iLeft;
			}
		}
	}
	
	protected int partition( Comparable[] abElements, int iStart, int iEnd ) {
		int iLeft = iStart + 1, iRight = iEnd;
		Comparable cPivot = abElements[iStart];
		while( true ){
			while( ( iRight > iLeft ) && ( cPivot.compareTo( abElements[iRight] ) < 0 ) )
				iRight--;
			while( ( iRight > iLeft ) && (  cPivot.compareTo( abElements[iLeft] ) < 0 ) )
				iLeft++;
			if( iLeft < iRight ){
				swap( abElements, iLeft, iRight );
			}
			else{
				swap( abElements, iLeft, iStart );
				return iLeft;
			}
		}
	}

	public Object[] sort( Vector vUnsorted, Comparator comp ){
		Object[] aoUnsorted = vUnsorted.toArray();
		return sort( aoUnsorted, comp );
	}

	protected void quickSort( Object[] aoUnsorted, Comparator comp, int iStart, int iEnd ){
		if( iStart >= iEnd )
			return;
		int iPivot = partition( aoUnsorted, comp, iStart, iEnd );
		printArray( aoUnsorted, iStart, iEnd, iPivot );
		//Logger.getInstance().logln( "quickSort " + iStart + " " + iPivot + " " + iEnd );
		quickSort( aoUnsorted, comp, iStart, iPivot - 1 );
		quickSort( aoUnsorted, comp, iPivot + 1, iEnd );
	}
	
	protected void quickSort( Comparable[] aoUnsorted, int iStart, int iEnd ){
		if( iStart >= iEnd )
			return;
		int iPivot = partition( aoUnsorted, iStart, iEnd );
		printArray( aoUnsorted, iStart, iEnd, iPivot );
		//Logger.getInstance().logln( "quickSort " + iStart + " " + iPivot + " " + iEnd );
		quickSort( aoUnsorted, iStart, iPivot - 1 );
		quickSort( aoUnsorted, iPivot + 1, iEnd );
	}
	
	protected void printArray( Object[] aoUnsorted, int start, int end, int pivot ){
		int i = 0;
		BeliefState bs = null;
		for( i = start ; i <= end ; i++ ){
			bs = (BeliefState)aoUnsorted[i];
			if( i == pivot )
				Logger.getInstance().log( "[" + bs.getGridInterpolations() + "], " );
			else
				Logger.getInstance().log( bs.getGridInterpolations() + ", " );
		}
		Logger.getInstance().logln();
	}

	protected int partition( Object[] aoUnsorted, Comparator comp, int iStart, int iEnd ){
		int iLeft = iStart + 1, iRight = iEnd;
		Object oPivot = aoUnsorted[iStart];
		while( true ){
			while( ( iRight > iLeft ) && ( comp.compare( oPivot, aoUnsorted[iRight] ) < 0 ) )
				iRight--;
			while( ( iRight > iLeft ) && (  comp.compare( oPivot, aoUnsorted[iLeft] ) >= 0 ) )
				iLeft++;
			if( iLeft < iRight ){
				swap( aoUnsorted, iLeft, iRight );
			}
			else{
				swap( aoUnsorted, iLeft, iStart );
				return iLeft;
			}
		}
	}

	protected void swap( Object[] array, int i, int j ) {
		Object oAux = array[i];
		array[i] = array[j];
		array[j] = oAux;
		
	}

	protected void swap( double[] array, int i, int j ) {
		double dAux = array[i];
		array[i] = array[j];
		array[j] = dAux;
		
	}

	public Object[] sort( Object[] aoUnsorted, Comparator comp ){
		quickSort( aoUnsorted, comp, 0, aoUnsorted.length - 1 );
		return aoUnsorted;
	}

	public void sort(int[] aiUnsorted) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sort(Vector<Comparable> v) {
		
	}

}
