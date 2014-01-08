package pomdp.utilities.sort;

import java.util.Comparator;
import java.util.Vector;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class BubbleSort implements SortStrategy {

	public Object[] sort( Vector vUnsorted, Comparator comp ){
		Object[] aoUnsorted = vUnsorted.toArray();
		return sort( aoUnsorted, comp );
	}

	public Object[] sort( Object[] aoUnsorted, Comparator comp ){
		bubbleSort( aoUnsorted, comp );
		return aoUnsorted;
	}
	
	private void bubbleSort( Object[] aoUnsorted, Comparator comp ){
		int i = 0, j = 0;
		boolean bDone = false;
		for( i = 0 ; i < aoUnsorted.length - 1 && !bDone ; i++ ){
			bDone = true;
			for( j = 0 ; j < aoUnsorted.length - 1 - i ; j++ ){
				if( comp.compare( aoUnsorted[j], aoUnsorted[j + 1] ) > 0 ){
					swap( aoUnsorted, j, j + 1 );
					bDone = false;
				}
			}
		}		
	}

	protected void swap( Object[] array, int i, int j ) {
		Object oAux = array[i];
		array[i] = array[j];
		array[j] = oAux;
		
	}
	protected void swap( int[] array, int i, int j ) {
		int iAux = array[i];
		array[i] = array[j];
		array[j] = iAux;
		
	}

	public void sort( int[] aiUnsorted ) {
		int i = 0, j = 0;
		boolean bDone = false;
		for( i = 0 ; i < aiUnsorted.length - 1 && !bDone ; i++ ){
			bDone = true;
			for( j = 0 ; j < aiUnsorted.length - 1 - i ; j++ ){
				if( aiUnsorted[j] > aiUnsorted[j + 1] ){
					swap( aiUnsorted, j, j + 1 );
					bDone = false;
				}
			}
		}		
	}

	@Override
	public void sort(Vector<Comparable> v) {
		throw new NotImplementedException();
	}

}
