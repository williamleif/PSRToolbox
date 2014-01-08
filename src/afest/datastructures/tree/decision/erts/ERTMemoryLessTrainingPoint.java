package afest.datastructures.tree.decision.erts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import afest.datastructures.tree.decision.interfaces.ITrainingPoint;

/**
 * Creates a memory less training point that stores a reference to the labels of the signal and return the value of the right label upon query.
 * Do not modify the labels arrayList as only a reference to it is stored. 
 * @param <O> Type of data contained in the point.
 */
public class ERTMemoryLessTrainingPoint<O extends Serializable> extends ERTMemoryLessPoint implements ITrainingPoint<String, O>
{

	private static final long serialVersionUID = 1L;
	
	private ArrayList<O> fLabels;
	
	/**
	 * Creates a memory less training point.
	 * @param index index of the point to query values for.
	 * @param signalFeatures features over the whole signal.  The value of the feature corresponding to the index will be returned upon query.
	 * @param labels List of all labels, the value of the label at the index will be queried when requested.
	 */
	public ERTMemoryLessTrainingPoint(int index, HashMap<String, ArrayList<Double>> signalFeatures, ArrayList<O> labels)
	{
		super(index, signalFeatures);
		fLabels = labels;
	}

	@Override
	public O getContent()
	{
		return fLabels.get(getIndex());
	}

	/**
	 * Return an ArrayList of MemoryLessTrainingPoints created using the given HashMap of features and labels 
	 * (point are in the same order as the MultiSignal).
	 * @param <O> Type of data contained in the point.
	 * @param features HashMap containing the features to create the MemoryLessTrainingPoints with.
	 * @param labels labels used for the MemoryLessTrainingPoints (must be in the same order as the feature values).
	 * @return an ArrayList of MemoryLessTrainingPoints created using the given features and labels.
	 */
	public static <O extends Serializable> ArrayList<ERTMemoryLessTrainingPoint<O>> getMemoryLessTrainingPoints(
																						HashMap<String, ArrayList<Double>> features, 
																						ArrayList<O> labels)
	{
		ArrayList<ERTMemoryLessTrainingPoint<O>> points = new ArrayList<ERTMemoryLessTrainingPoint<O>>();
		for (int i = 0; i < labels.size(); i++)
		{
			ERTMemoryLessTrainingPoint<O> newPoint = new ERTMemoryLessTrainingPoint<O>(i, features, labels);
			points.add(newPoint);
		}
		return points;
	}
	
}
