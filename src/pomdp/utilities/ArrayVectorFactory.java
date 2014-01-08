package pomdp.utilities;

import pomdp.utilities.datastructures.VectorFactory;

public class ArrayVectorFactory<VType> extends VectorFactory<ArrayVector<VType>> {
	
	public ArrayVectorFactory(){
		super();
		ITERATION_SIZE = 10000;
	}

	public ArrayVector<VType> newInstance(int iSize, boolean bSmall) {
		return new ArrayVector<VType>( iSize, this, bSmall );
	}
		
}
