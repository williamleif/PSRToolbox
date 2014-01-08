package pomdp.algorithms.gridbased;

import java.util.*;

enum EntropyComputationType
{
   Algebric,
   NumberOfPositiveEntries,
   SmallestEntry
}

public class EntropyEvaluator{

   // enum for the computation type of the entropy

   private EntropyComputationType type;

   protected static EntropyEvaluator m_ebmcComparator = null;

   public EntropyEvaluator(EntropyComputationType type){
      this.type = type;
   }

	public static EntropyEvaluator getInstance(EntropyComputationType type){
		if( m_ebmcComparator == null ){
			m_ebmcComparator = new EntropyEvaluator(type);
		}
		return m_ebmcComparator;
	}

   // the compare method
   // Denote:
   // The needed grid-point is the one with the MAXIMUM entropy, i.e. MINIMUM information
   // o all the compare methods return :
   // -1 if the first is with bigger entropy
   //  1 if the first is with smaller entropy
   // 0 if the entropy is the same
   public double evaluateEntropy(Map entries) {

      double result = 0;

      if (type == EntropyComputationType.Algebric)
         result = computeAlgebricEntropy(entries);

      if (type == EntropyComputationType.NumberOfPositiveEntries)
         result = computePositiveEntriesEntropy(entries);

      if (type == EntropyComputationType.SmallestEntry)
         result = computeSmallestEntryEntropy(entries);

      return result;
   }

   private double computeAlgebricEntropy(Map entries){

      int iState = -1;
      double entropy = 0;
		double dValue = 0.0;
		EntropyEvaluator.Belief b = null;

      // for first grid-point
      Iterator itNonZero = entries.entrySet().iterator();

		while( ( iState < Integer.MAX_VALUE ) ) {

         b = new EntropyEvaluator.Belief( itNonZero );
         iState = b.iState;
         dValue = b.dValue;

         entropy += computeAlgebricEntropyForOneElement(dValue);
  		}

      return entropy;
   }

   // help method
   private double computeAlgebricEntropyForOneElement(double x){

      return -1 * x * Math.log(x);
   }

   private double computePositiveEntriesEntropy(Map entries1){

      int result = entries1.entrySet().size();

      return result;
   }

   private double computeSmallestEntryEntropy(Map entries1){

      Iterator itNonZero1 = entries1.entrySet().iterator();

      EntropyEvaluator.Belief b1 = new EntropyEvaluator.Belief( itNonZero1 );

      double result = b1.dValue;
      return result;
   }

   public class Belief{
		public int iState;
		public double dValue;

		public Belief( Iterator it ){
			if( it.hasNext() ){
				Map.Entry e = (Map.Entry)it.next();
				iState = ((Integer) e.getKey()).intValue();
				dValue = ((Double) e.getValue()).doubleValue();
			}
			else{
				iState = Integer.MAX_VALUE;
				dValue = -1.0;
			}
		}
	}
}
