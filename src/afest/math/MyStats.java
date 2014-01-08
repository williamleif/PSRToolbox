package afest.math;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MathException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.solvers.BrentSolver;
import org.apache.commons.math.special.Erf;

/**
 * Useful function for statistics.
 */
public final class MyStats
{
	
	private MyStats() {}

	/**
	 * Return the value of the error function calculated using the Apache Commons Math.
	 * @param x Where to evaluate the error function at.
	 * @return the value of the error function evaluated at x.
	 */
	public static double erf(double x)
	{
			try
			{
				double value = Erf.erf(x);
				return value;
			}
			catch (MathException e)
			{
				throw new afest.math.exceptions.MathException("Algorithm failed to converge! "+e.getMessage());
			}
	}
	
	/**
	 * Return the inverse error function.  I.e. Given y, what is x such that y = erf(x).
	 * @param y The value to find the x for.
	 * @param absoluteAccuracy Precision of the value returned.
	 * @param maxIterations Maximum number of iterations allowed to reach the precision.
	 * @return Return x such that given y, y = erf(x).
	 */
	public static double erfinv(final double y, double absoluteAccuracy, int maxIterations)
	{
		double start = 0;
		double end = 1;
		while (erf(end) < y)
		{
			end = end * 2;
		}
		BrentSolver brentsolver = new BrentSolver(absoluteAccuracy);
		try
		{
			double x = brentsolver.solve(maxIterations, new UnivariateRealFunction()
			{
				@Override
				public double value(double x) throws FunctionEvaluationException
				{
					return erf(x)-y;
				}
			}, start, end);
			return x;
		}
		catch (MaxIterationsExceededException e)
		{
			throw new afest.math.exceptions.MathException("Algorithm failed to converge for erfinv! "+e.getMessage());
		}
		catch (FunctionEvaluationException e)
		{
			throw new afest.math.exceptions.MathException("Algorithm failed to converge for erf! "+e.getMessage());
		}
	}
	
	/**
	 * Return the inverse error function.  I.e. Given y, what is x such that y = erf(x).  
	 * The maximum number of iteration is set to the default of the BrentSolver in Apache Commons Math.
	 * @param y The value to find the x for.
	 * @param absoluteAccuracy Precision of the value returned.
	 * @return Return x such that given y, y = erf(x).
	 */
	public static double erfinv(final double y, double absoluteAccuracy)
	{
		return erfinv(y, absoluteAccuracy, BrentSolver.DEFAULT_MAXIMUM_ITERATIONS);
	}
	
	/**
	 * Return the inverse error function.  I.e. Given y, what is x such that y = erf(x).
	 * The accuracy is set to the default of the BrentSolver in Apache Commons Math.
	 * @param y The value to find the x for.
	 * @param maxIterations Maximum number of iterations allowed to reach the precision.
	 * @return Return x such that given y, y = erf(x).
	 */
	public static double erfinv(final double y, int maxIterations)
	{
		return erfinv(y, BrentSolver.DEFAULT_ABSOLUTE_ACCURACY, maxIterations);
	}
	
	/**
	 * Return the inverse error function.  I.e. Given y, what is x such that y = erf(x).  
	 * The maximum number of iteration and the accuracy are set to the default of the BrentSolver in 
	 * Apache Commons Math.
	 * @param y The value to find the x for.
	 * @return Return x such that given y, y = erf(x).
	 */
	public static double erfinv(final double y)
	{
		return erfinv(y, BrentSolver.DEFAULT_ABSOLUTE_ACCURACY, BrentSolver.DEFAULT_MAXIMUM_ITERATIONS);
	}
	
}
