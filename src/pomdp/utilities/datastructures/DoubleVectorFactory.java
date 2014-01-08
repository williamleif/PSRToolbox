package pomdp.utilities.datastructures;

public class DoubleVectorFactory extends VectorFactory<DoubleVector> {
	public DoubleVectorFactory(){
		super();
		ITERATION_SIZE = 10000;
	}

	public DoubleVector newInstance( int iSize, boolean bSmall ){
		return new DoubleVector( iSize, this, bSmall );
	}
}
