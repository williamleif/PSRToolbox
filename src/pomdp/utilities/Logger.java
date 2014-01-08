package pomdp.utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class Logger {
	private int m_iMaximalLevel;
	private static Map<Long,Logger> m_mInstances = new HashMap<Long,Logger>();
	private Runtime m_rtRuntime;
	private PrintStream m_psOutput;
	private boolean m_bSilent = true;
	private boolean m_bNoOutput = true;

	private Logger(){
		m_iMaximalLevel = 2;
		m_rtRuntime = Runtime.getRuntime();
		//m_psOutput = System.out;
	}
	
	public void finalize(){
		m_psOutput.flush();
		m_psOutput.close();
	}
	
	public void setOutputStream( String sFileName ) throws FileNotFoundException{
		if(m_bNoOutput)
			return;
		m_psOutput = new PrintStream( new FileOutputStream( sFileName ) );		
	}
	public void setOutputStream( String sPath, String sFileName ) throws IOException{
		File f = new File(sPath);
		if(!f.exists())
			if(!f.mkdirs())
				throw new IOException("Could not create output directory");
		if(m_bNoOutput)
			return;
		m_psOutput = new PrintStream( new FileOutputStream( sPath + "\\" + sFileName ) );		
	}
	
	public static Logger getInstance(){
		long lId = Thread.currentThread().getId();
		if(!m_mInstances.containsKey(lId))
			m_mInstances.put(lId, new Logger());
		return m_mInstances.get(lId);
	}
	
	public void log (String sMessage)
	{
		if(m_bNoOutput)
			return;
		m_psOutput.print(sMessage);
		if(!m_bSilent && m_psOutput != System.out)
			System.out.print(sMessage);
		m_psOutput.flush();	
	}
	public void logln(Object o)
	{
		if(m_bNoOutput)
			return;
		String sMessage = o.toString();
		m_psOutput.println(sMessage);
		if(!m_bSilent && m_psOutput != System.out)
			System.out.println(sMessage);
		m_psOutput.flush();	
	}
	public void logln(String sMessage)
	{
		if(m_bNoOutput)
			return;
		if(m_psOutput == System.out)
			System.out.println("BUGBUG");
		m_psOutput.println(sMessage);
		if(!m_bSilent && m_psOutput != System.out)
			System.out.println(sMessage);
		m_psOutput.flush();	
	}
	public void logln()
	{
		if(m_bNoOutput)
			return;
		m_psOutput.println();
		if(!m_bSilent && m_psOutput != System.out)
			System.out.println();
		m_psOutput.flush();	
	}
	public void log( String sClassName, int iLevel, String sMethodName, String sMessage ){
		if( iLevel <= m_iMaximalLevel ){
			String sFullMsg = sClassName + ":" + sMethodName + ":" + sMessage;
			log(sFullMsg);
		}
	}
	
	public void logError( String sClassName, String sMethodName, String sMessage ){
		System.err.println( sClassName + ":" + sMethodName + ":" + sMessage );
	}
	public void logFull( String sClassName, int iLevel, String sMethodName, String sMessage ){
		if( iLevel <= m_iMaximalLevel ){
			String sFullMsg = sClassName + ":" + sMethodName + ":" + sMessage +
				", memory: " + 
				" total " + m_rtRuntime.totalMemory() / 1000000 +
				" free " + m_rtRuntime.freeMemory() / 1000000 +
				" max " + m_rtRuntime.maxMemory() / 1000000;
			logln(sFullMsg);
		}
	}
	public void setOutput(boolean bAllow){
		m_bNoOutput = !bAllow;
	}
	public void setSilent(boolean bSilent){
		m_bSilent = bSilent;
	}
}
