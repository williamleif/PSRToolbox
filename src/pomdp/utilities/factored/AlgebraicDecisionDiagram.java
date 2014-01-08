package pomdp.utilities.factored;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface AlgebraicDecisionDiagram extends Comparable<AlgebraicDecisionDiagram> {

	/**
	 * Returns the unique identifier of the ADD
	 */
	public abstract long getId();

	/**
	 * Computes and returns the product of the two ADDs
	 */
	public abstract AlgebraicDecisionDiagram product( AlgebraicDecisionDiagram addOther );

	/**
	 * Computes and returns the sum of the two ADDs
	 */
	public abstract AlgebraicDecisionDiagram sum( AlgebraicDecisionDiagram addOther );
	/**
	 * Computes and returns the max of the two ADDs
	 */
	public abstract AlgebraicDecisionDiagram max( AlgebraicDecisionDiagram addOther );

	/**
	 * Reduces the ADD to its minimal form
	 */
	public abstract void reduce();
	
	/**
	 * Reduces the ADD to its minimal form
	 */
	public abstract void reduce( AbstractionFilter aFilter );

	/**
	 * Returns a string representation
	 */
	public abstract String toString();

	/**
	 * Returns a string representation, formated as a tree
	 */
	public abstract String getTreeString();

	/**
	 * Adds a complete path and its leaf value to the ADD
	 */
	public abstract void addPath( boolean[] abPath, double dValue );

	/**
	 * Adds a partial path and its leaf value to the ADD
	 */
	public abstract void addPartialPath( int[] aiVariables, boolean[] abValues, double dValue, boolean bTwoTimeSteps );	

	/**
	 * Sets at the end of each path in the DAG that does not end with a leaf (ends with a null) the 
	 * leaf corresponding to the default value.
	 * Should be executed after finishing to insert all non-zero values for example.
	 */
	public abstract void finalizePaths(double dDefaultValue);

	/**
	 * Gets the value given a full path
	 */
	public abstract double valueAt( boolean[] abPath );
	
	/**
	 * Gets the value given a partial path
	 */
	public abstract double valueAt( int[] aiVariables, boolean[] abValues );


	/**
	 * existential abstraction = variable elimination
	 * Replaces a set of variables with the sum of their immediate children
	 */
	public abstract AlgebraicDecisionDiagram existentialAbstraction( AbstractionFilter aFilter );
	
	/**
	 * returns a (deep) copy of the ADD 
	 * @return
	 */
	public abstract AlgebraicDecisionDiagram copy();

	/**
	 * scalar product of the current ADD by a scalar factor
	 * @param dFactor
	 */
	public abstract void product(double dFactor);

	/**
	 * Computes an inner product of two ADDs and returns the value \sum_i v_1(i)*v_2(i)
	 * @param addOther
	 * @return
	 */
	public abstract double innerProduct( AlgebraicDecisionDiagram addOther );

	/**
	 * Computes an inner product of an ADD and a factored probability over variable values
	 * @param addOther
	 * @return
	 */
	public abstract double innerProduct( double[] adVariableProbabilities );

	/**
	 * Computes whether one ADD dominates (pointwise) another ADD. That is - for all i v_1(i) >= v_2(i) 
	 * @param addOther
	 * @return
	 */
	public abstract boolean dominates( AlgebraicDecisionDiagram addOther );

	/**
	 * Checks if two ADDs are equal
	 * @param addOther
	 * @return
	 */
	public abstract boolean equals( AlgebraicDecisionDiagram addOther );

	/**
	 * Approximates the ADD by replacing each 
	 * @param dSpan
	 */
	public abstract void reduceToMin( double dSpan );

	/**
	 * Translate a set of variables into another set of variable. 
	 * A translation of variable X into Y means that all nodes corresponding to X must be replaced by Y.
	 * It is assumed that Y does not previously appear in the ADD. 
	 * @param vt
	 */
	public abstract void translateVariables( VariableTranslator vt );

	/**
	 * Returns the sum of the values of the ADD - equivalent to an eliminating all the variables.
	 * @return
	 */
	public abstract double getValueSum();
	
	/**
	 * Allows a user to release all the nodes of the ADD. Called when the ADD is no longer useful.
	 */
	public void release();

	/**
	 * Returns the maximal value within the ADD.
	 * @return
	 */
	public double getMaxValue();

	/**
	 * This interface allows the abstraction (elimination) of multiple variables in a single pass.
	 * It is possible to use only the abstractVariable method that specifies which variables should be removed.
	 * @author guyshani
	 *
	 */
	public interface AbstractionFilter{
		public boolean abstractVariable( int iVariable );
		public int lastVariable();
		public int firstVariableAfter( int iVariable );
		public boolean sumMissingLevels();
		public int countVariablesBetween( int iVar1, int iVar2 );
		public int countAbstractionVariablesBetween( int iVar1, int iVar2 );
		public int getLastVariableId();
		public int getFirstVariableId();
	}
	 /**
	  * This class allows the translation of multiple variables in a single pass.
	  * It is possible to use only the translate method that given a variable returns the translated variable.
	  * If a variable should not be translated than the translate(x)=x.
	  * @author guyshani
	  *
	  */
	public interface VariableTranslator extends Serializable{
		public int translate( int iVar );
		public int translateVariableCount( int cVariables );
	}

	
	/**
	 * The BinaryOperator interface and its implementing classes Product and Sum make replace the need for multiple
	 * case statements inside the ADD apply method.
	 * @author guyshani
	 *
	 */
	public interface BinaryOperator{
		public double compute( double d1, double d2 );
	}
	public class Product implements BinaryOperator{
		public double compute( double d1, double d2 ){
			return d1 * d2;
		}
	}
	public class Sum implements BinaryOperator{
		public double compute( double d1, double d2 ){
			return d1 + d2;
		}
	}
	public class Max implements BinaryOperator{
		public double compute( double d1, double d2 ){
			
			double dMax = d1, dSum = d1 + d2;
			if( d2 > d1 )
				dMax = d2;
			if( dSum > dMax )
				return dSum;
			return dMax;
			
			/*
			if( d1 > d2 )
				return d1;
			return d2;
			*/
		}
	}
	/**
	 * Saves an ADD to an XML format
	 * @param fw
	 * @throws IOException
	 */
	public void save( FileWriter fw ) throws IOException;

	/**
	 * Parses the XML element into an ADD
	 * @param eADD
	 */
	public void parseXML( Element eADD );

	/**
	 * Returns a DOM (XML) representation of the ADD
	 * @param doc
	 * @return
	 */
	public Element getDOM( Document doc );

	/**
	 * Returns the size of the ADD - the number of vertexes.
	 * @return
	 */
	public long getVertexCount();

	public abstract void setUnspecifiedVariablesToWorstCase( Vector<Integer> unspecifiedVariables );

	public abstract double innerProduct( PathProbabilityEstimator pbe );
}