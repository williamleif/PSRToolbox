package pomdp.utilities.distance;


public class L2Distance extends LDistance {

	protected static L2Distance m_l2Distance;
	
	protected L2Distance(){
		super();
	}
	
	protected double applyDistanceMetric( double dAccumulated, double dValue1, double dValue2 ){
		double dDiff = dValue1 - dValue2;
		return dAccumulated + dDiff * dDiff;
	}

	protected double applyFinal(double dAccumulated ){
		return Math.sqrt( dAccumulated );
	}

	public static DistanceMetric getInstance() {
		if( m_l2Distance == null )
			m_l2Distance = new L2Distance();
		return m_l2Distance;
	}
}
