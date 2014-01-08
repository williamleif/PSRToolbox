package pomdp.utilities.datastructures;

public class LongVectorFactory extends VectorFactory<LongVector> {

	public LongVectorFactory(){
		super();
		ITERATION_SIZE = 100000;
	}
	public LongVector newInstance( int iSize, boolean bSmall ){
		return new LongVector( iSize, this, bSmall );
	}

}
