package pomdp.utilities.datastructures;

import java.util.Iterator;

public interface PriorityQueue{
	public PriorityQueueElement extractMax();
	public void insert( PriorityQueueElement element );
	public void adjust( PriorityQueueElement element );
	public Iterator iterator();
	public boolean isEmpty();
	public int size();
	public void clear();
	public int swapCount();
}
