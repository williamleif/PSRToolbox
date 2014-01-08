package pomdp.utilities.concurrent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class DotProduct extends Task {

	private AlphaVector m_avVector;
	private BeliefState m_bsBelief;
	private double m_dProduct;
	
	public DotProduct( AlphaVector av, BeliefState bs ){
		m_avVector = av;
		m_bsBelief = bs;
		m_dProduct = Double.NEGATIVE_INFINITY;
	}
	
	public void execute() {
		m_dProduct = m_avVector.dotProduct( m_bsBelief );
	}
	public double getResult(){
		return m_dProduct;
	}
	public AlphaVector getAlphaVector(){
		return m_avVector;
	}
	
	public void copyResults( Task tProcessed ){
		m_dProduct = ((DotProduct)tProcessed).getResult();		
	}

	
	public Element getDOM(Document doc) throws Exception {
		throw new NotImplementedException();
	}
}
