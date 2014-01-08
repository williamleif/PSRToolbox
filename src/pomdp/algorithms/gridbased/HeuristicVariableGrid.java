package pomdp.algorithms.gridbased;

import java.util.*;
import java.util.Map.Entry;

import pomdp.environments.POMDP;
import pomdp.utilities.BeliefMapComparator;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;

/**
 * Created by IntelliJ IDEA.
 * User: muaddib
 * Date: Feb 25, 2006
 * Time: 6:34:48 PM
 * To change this template use File | Settings | File Templates.
 */

public class HeuristicVariableGrid extends FixedResolutionGrid {


   private double m_valueThreshold;
   private EntropyEvaluator m_entropyEvaluator;

   public HeuristicVariableGrid(POMDP pomdp, double threshold){ 
      super(pomdp);
      //m_mGridPointValues = new TreeMap(EntropyBeliefMapComparator.getInstance(EntropyBeliefMapComparator.EntropyComputationType.NumberOfPositiveEntries));
      m_valueThreshold = threshold;
      m_entropyEvaluator = EntropyEvaluator.getInstance(EntropyComputationType.NumberOfPositiveEntries);
   }


   private boolean checkIfShouldAddAvgBS(int i, int j){

      BeliefState bs1 = (BeliefState)m_vGridPoints.get(i);
      BeliefState bs2 = (BeliefState)m_vGridPoints.get(j);

      boolean result1 = false;
      boolean result2 = false;

      // first condition

      // check if the actions are different
      int a1 = applyH(bs1, false);
      int a2 = applyH(bs2, false);

      if (a1 != a2){ // only if the actions are different

         double v1 = Math.abs(this.computeQValue(bs1,a1) - this.computeQValue(bs2,a1));
         double v2 = Math.abs(this.computeQValue(bs1,a2) - this.computeQValue(bs2,a2));

         if (v1 >= this.m_valueThreshold || v2 >= this.m_valueThreshold)
            result1 = true;
      }

      // the second condition

      int o1 = -1;
      int o2 = -1;
      double maxProb1 = -1;
      double maxProb2 = -1;

      if (result1){   // check the second condition only if the first stands

         int numberOfObservations = this.m_pPOMDP.getObservationCount();
         for (int o=0;o<numberOfObservations;o++){
            double prob1 = bs1.probabilityOGivenA(a1,o);
            double prob2 = bs2.probabilityOGivenA(a2,o);
            if (prob1 > maxProb1)
            {
               maxProb1 = prob1;
               o1 = o;
            }
            if (prob2 > maxProb2){
               maxProb2 = prob2;
               o2 = o;
            }
         }

         if (o1 == -1 || o2 == -1)
            Logger.getInstance().logln("checkIfShouldAddAvgBS: BUGBUG: negative observation in checkIfShouldAddAvgBS");

         result2 = (o1 == o2);
      }

      return result1 && result2;
   }

   private BeliefState createAvgBS(int i, int j){

      BeliefState bs1 = (BeliefState)m_vGridPoints.get(i);
      BeliefState bs2 = (BeliefState)m_vGridPoints.get(j);

      return this.getAvgBeliefState(bs1,bs2);
   }

   private BeliefState getAvgBeliefState(BeliefState bs1, BeliefState bs2){

      Map avgEntries = new TreeMap();

      Iterator entriesIterator1 = bs1.getNonZeroEntries().iterator();
      Iterator entriesIterator2 = bs2.getNonZeroEntries().iterator();

      // going over the first bs, adding the 0.5 of its value to the result
      while (entriesIterator1.hasNext()){
         Entry e = (Entry)entriesIterator1.next();
         Integer state = (Integer)(e.getKey());
         Double value = (Double)(e.getValue());
         Double newValue = new Double (0.5 * value.doubleValue());
         avgEntries.put(state, newValue);
      }

      // going over the second bs, adding the 0.5 of its value to the existing in the result
      while (entriesIterator2.hasNext()){
         Entry e = (Entry)entriesIterator2.next();
         Integer state = (Integer)(e.getKey());
         Double value = (Double)(e.getValue());
         Double newValue = new Double (0.5 * value.doubleValue());
         Double oldValue = (Double)avgEntries.get(state);
         if (oldValue == null)
            avgEntries.put(state, newValue);
         else {
            Double newValue2 = new Double(newValue.doubleValue() + oldValue.doubleValue());
            avgEntries.put(state, newValue2);
         }
      }

       BeliefState avfBS = m_pPOMDP.getBeliefStateFactory().getBeliefState(avgEntries);

       return avfBS;
   }

   protected void refineGrid(){

      // for each two bs's in the grid-points we must check whether to add the bs in the middle
      int numOfGridPoints = m_vGridPoints.size();
      //Trace.Write("in refine: "+numOfGridPoints);

      if( m_iFinestResolution == 0 ){   // if it is for the first time
			super.refineGrid();           // then just insert the states
      }
      else                             // if it is for the second time
      {
         for (int i=0;i<numOfGridPoints;i++){
            for (int j=i+1;j<numOfGridPoints;j++){
               boolean shouldAddAvgBS = checkIfShouldAddAvgBS(i,j);

               if (shouldAddAvgBS){
                  BeliefState avgBs = createAvgBS(i,j);
                  addPoint(avgBs, i, j);
               }
            }
         }
      }
   }

   protected void addPoint(BeliefState bs, int i, int j){

      Map entries = bs.getNonZeroEntriesMap();

      if (m_mGridPointValues.containsKey(entries))
         return;

      double dValue = interpolateValue(bs);

      m_mGridPointValues.put( entries, new Pair(bs, new Double( dValue )) );

      if( !m_vGridPoints.contains( bs ) ) {

         int maxIndex = Math.max(i,j); // it is the index from which the search will start
         int numberOfPoints = m_vGridPoints.size();
         int suitableIndex;
         double thisEntropy = this.m_entropyEvaluator.evaluateEntropy(bs.getNonZeroEntriesMap());
         double foundEntropy = 0;

         // finding the suitable place for the bs
         for (suitableIndex=maxIndex;suitableIndex<numberOfPoints;suitableIndex++)
         {
            BeliefState foundBs = (BeliefState)m_vGridPoints.get(suitableIndex);
            foundEntropy = this.m_entropyEvaluator.evaluateEntropy(foundBs.getNonZeroEntriesMap());
            if (foundEntropy >= thisEntropy)
               break;
         }

         m_vGridPoints.add(suitableIndex, bs );
      }
   }

   protected double interpolateValue(BeliefState bs){

      double dValue = 0;

      Map givenBsEntries = bs.getNonZeroEntriesMap();

      if (m_mGridPointValues.containsKey(givenBsEntries))
         return getGridPointValue(givenBsEntries);

      // searching for belief-state which give positive probability ONLY to state
      // to which bs gives. In another words, each state which assigned with positive
      // probability in the found belief-state MUST be assigned with positive probability
      // in bs too.
      double c = 0;
      Map gridPointsValuesForSearch = new TreeMap(BeliefMapComparator.getInstance());

      gridPointsValuesForSearch.putAll(m_mGridPointValues);

      int numberOfPoints = m_vGridPoints.size();
      for (int i=numberOfPoints-1;i>=0;i--){

         //extracting the entry with the maximum entropy
         double maxEntropy = Double.MIN_VALUE;
         Map maxEntries = ((BeliefState)m_vGridPoints.get(i)).getNonZeroEntriesMap();

         // first of all the size must be smaller or equal
         if (maxEntries.size() <= givenBsEntries.size()){ // consider it

            Iterator samePosItFound = maxEntries.keySet().iterator();
            boolean isSuitableBs = true;
            while ( samePosItFound.hasNext() )
            {
               Integer keyIn = (Integer)samePosItFound.next();
               if (givenBsEntries.get(keyIn) == null){  // does given bs assigns positive value too?
                  isSuitableBs = false;     // if not, it is not suitable
                  break;
               }
            }

            // if it is suitable, extract the c constant for found bs
            if (isSuitableBs){

               // getting the value of the found
               double foundValue = getGridPointValue(maxEntries);

               // getting the constant
               c = getMaxMultiplyingConstant(givenBsEntries, maxEntries);

               // getting the belief-state to continue
               givenBsEntries = makeEntriesCalculation(givenBsEntries, maxEntries, c);

               // adding to dValue
               dValue += c * foundValue;
            }
         }

         if (givenBsEntries.size() == 0) // break if there is no entry in the givenBsEntries
            break;
      }

      return dValue;
   }

   // method for the calculations on the belief-state (interpolation)

   // Calclates the b1-c*b2 and updates the b1 entries
   public Map makeEntriesCalculation(Map entries1, Map entries2, double c){

      Map rEntries = new TreeMap();

      Iterator entries2It = entries2.entrySet().iterator();

      rEntries.putAll(entries1); // copy all from entries1 to result

      // for each value of this belief-state
      while(entries2It.hasNext()){

         Entry e = (Entry)entries2It.next();
         Integer key = (Integer)e.getKey();
         Double firstValue = (Double)entries1.get(key);
         Double secondValue = (Double)e.getValue();

         Double newValue = null;
         newValue = new Double(firstValue.doubleValue() - c*(secondValue.doubleValue()));

         if (newValue.doubleValue() > 0)
            rEntries.put(key, newValue);
         else
            if (newValue.doubleValue() < -FixedResolutionGrid.EPSILON)
               Logger.getInstance().logln("negative value in interpolation:"+ newValue.doubleValue());
            else // == 0
               rEntries.remove(key);
      }

      return rEntries;
   }

   // Method for getting the max c such that
   // (b1 - c*b2) gives positive posibility for all the states
   public double getMaxMultiplyingConstant(Map entries1, Map entries2){

      double c = Double.MAX_VALUE;

      Iterator secondIt = entries2.entrySet().iterator();

      // find the minimal value in this belief-state
      while (secondIt.hasNext()){
         Entry entry = (Entry)secondIt.next();
         Integer state = (Integer)entry.getKey();
         double value2 = ((Double)entry.getValue()).doubleValue();
         double value1 = ((Double)entries1.get(state)).doubleValue();

         double value = value1/value2;   // value is the factor

         if (value < c)           // minValue will be the c constant
            c = value;
      }

      return c;
   }
}
