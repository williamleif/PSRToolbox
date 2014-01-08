package cpsr.environment.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActObSequenceSet 
{
	private Map<Integer, Integer> indexMap;
	int counter;
	
	public ActObSequenceSet()
	{
		indexMap = new HashMap<Integer, Integer>();
		counter = 0;
	}
	
	public void addActObSequence(List<ActionObservation> actobs)
	{
		int id = computeID(actobs);
		
		if(!indexMap.containsKey(id))
		{
			indexMap.put(id, counter);
			counter++;
		}
	}
	
	public int indexOf(List<ActionObservation> actobs)
	{
		int id = computeID(actobs);
		Integer index =  indexMap.get(id);
		if(index == null)
		{
			return -1;
		}
		else
		{
			return index;
		}
	}
	
	public int size()
	{
		return indexMap.size();
	}
	
	private int computeID(List<ActionObservation> actobs)
	{
		int iteration = 0;
		int id = 0;
		for(ActionObservation actob : actobs)
		{
			id += (int)actob.getID()+iteration*actob.maxID();
			iteration++;
		}
		
		return id;
	}
}
