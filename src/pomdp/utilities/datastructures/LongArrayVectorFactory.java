package pomdp.utilities.datastructures;

public class LongArrayVectorFactory extends VectorFactory<LongArrayVector> {

	public LongArrayVectorFactory(){
		super();
		ITERATION_SIZE = 10000;
	}
	public LongArrayVector newInstance(int iSize, boolean bSmall) {
		return new LongArrayVector( iSize, this, bSmall );
	}

}
