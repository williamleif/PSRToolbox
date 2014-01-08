package pomdp.utilities.sort;

import java.util.Comparator;
import java.util.Vector;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class RadixSort implements SortStrategy {

	public Object[] sort( Vector vUnsorted, Comparator comp ){
		Object[] aoUnsorted = vUnsorted.toArray();
		return sort( aoUnsorted, comp );
	}

	public Object[] sort( Object[] aoUnsorted, Comparator comp ){
		radixSort( aoUnsorted, comp );
		return aoUnsorted;
	}
	
	private void radixSort( Object[] aoUnsorted, Comparator comp ){
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

	public void sort(int[] aiUnsorted) {
		throw new NotImplementedException();
	}

	@Override
	public void sort(Vector<Comparable> v) {
		throw new NotImplementedException();
	}
}
