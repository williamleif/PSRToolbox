package pomdp.utilities.sort;

import java.util.Comparator;
import java.util.Vector;

public interface SortStrategy {
	public void sort( int[] aiUnsorted );
	public Object[] sort( Vector vUnsorted, Comparator comp );
	public Object[] sort( Object[] aoUnsorted, Comparator comp );
	public void sort( Vector<Comparable> v );
}
