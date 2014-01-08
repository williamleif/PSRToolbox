package pomdp.utilities.concurrent;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import pomdp.environments.POMDP;
import pomdp.utilities.Logger;
import pomdp.utilities.SocketReader;


public class RemoteTaskExecutionThread extends TaskExecutionThread {

	private Socket m_sRemoteConnection;
	private ObjectInputStream m_oisInput;
	private ObjectOutputStream m_oosOutput;
	private POMDP m_pPOMDP;
	
	private static long[] g_cTime = new long[3];
	private static long[] g_cCounts = new long[3];
	private static long g_cTasks = 0;
	
	public RemoteTaskExecutionThread( ThreadPool tp, Socket sRemoteConnection, POMDP pomdp ) throws IOException{
		super( tp );
		m_pPOMDP = pomdp;
		m_sRemoteConnection = sRemoteConnection;
		m_oosOutput = new ObjectOutputStream( m_sRemoteConnection.getOutputStream() );
		m_oisInput = new ObjectInputStream( m_sRemoteConnection.getInputStream() );
		m_oosOutput.writeObject( pomdp );
	}
	public void run(){
		try{
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			while( !m_bKilled ){
				Task tOriginal = m_tpPool.getNextTask(), tProcessed = null;
				try{
					Document docTask = builder.newDocument();
					Element eTask = tOriginal.getDOM( docTask );
					//Logger.getInstance().logln( "Processing task " + tOriginal.getId() + " remotely" );
					long lTimeBefore = System.currentTimeMillis();

					DOMSource source = new DOMSource( eTask );
					StreamResult result = new StreamResult( m_sRemoteConnection.getOutputStream() );
					transformer.transform( source, result );
					m_sRemoteConnection.getOutputStream().flush();

					SocketReader sr = new SocketReader( m_sRemoteConnection );
					InputSource is = new InputSource( sr ); 
					docTask = builder.parse( is );
					tProcessed = Task.restoreTask( docTask, m_pPOMDP );

					long lTimeAfter = System.currentTimeMillis();
					computeTime( tOriginal, lTimeAfter - lTimeBefore );
					//Logger.getInstance().logln( "Task " + tOriginal.getId() + " processed remotely" );
					tOriginal.copyResults( tProcessed );
					m_tpPool.taskDone( tOriginal );
				}
				catch( Exception e ){
					Logger.getInstance().logError( "RemoteTaskExecutionThread", "run", "Unable to execute task remotely - t = " + tOriginal.getClass() + " - " + e );
					e.printStackTrace();
			
					System.exit( 0 );
					
					m_tpPool.addTask( tOriginal );
				}
			}
		}
		catch( Exception e ){
			Logger.getInstance().logError( "RemoteTaskExecutionThread", "run", "Remote execution thread failed - " + e );
			e.printStackTrace();
			System.exit( 0 );
		}
	}
	/*
	public void run(){
		while( !m_bKilled ){
			Task tOriginal = m_tpPool.getNextTask( false ), tProcessed = null;
			try{
				//Logger.getInstance().logln( "Processing task " + tOriginal.getId() + " remotely" );
				long lTimeBefore = System.currentTimeMillis();
				m_oosOutput.writeObject( tOriginal );
				m_oosOutput.flush();
				tProcessed = (Task)m_oisInput.readObject();
				long lTimeAfter = System.currentTimeMillis();
				computeTime( tOriginal, lTimeAfter - lTimeBefore );
				//Logger.getInstance().logln( "Task " + tOriginal.getId() + " processed remotely" );
				tOriginal.copyResults( tProcessed );
				m_tpPool.taskDone( tOriginal );
			}
			catch( Exception e ){
				Logger.getInstance().logError( "RemoteTaskExecutionThread", "run", "Unable to execute task remotely - t = " + tOriginal.getClass() + " - " + e );
				e.printStackTrace();
		
				System.exit( 0 );
				
				m_tpPool.addTask( tOriginal );
			}
		}
	}
	*/
	private void computeTime( Task t, long lTime ){
		if( t instanceof ComputeLowLevelG ){
			g_cTime[0] += lTime;
			g_cCounts[0]++;
		}
		else if( t instanceof FindMaxAlphas ){
			g_cTime[1] += lTime;
			g_cCounts[1]++;
		}
		else if( t instanceof ComputeDiscountedReward ){
			g_cTime[2] += lTime;
			g_cCounts[2]++;
		}
		g_cTasks++;
		if( g_cTasks % 50 == 0 ){
			Logger.getInstance().logln( "Average times: ComputeG - " + ( g_cCounts[0] == 0 ? 0 : g_cTime[0] / g_cCounts[0] ) + 
					" FindMaxAlphas - " + ( g_cCounts[1] == 0 ? 0 : g_cTime[1] / g_cCounts[1] ) + 
					" ComputeDiscountedReward - " + ( g_cCounts[2] == 0 ? 0 : g_cTime[2] / g_cCounts[2] ) );
		}
	}
}
