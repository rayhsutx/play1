package play.services;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import play.Invoker;
import play.Invoker.InvocationContext;
import play.Logger;

/**
 * The service is run in background
 * <br /><br />
 * The service would be disabled by setting 'application.[CLASS_NAME]=false'
 */
public abstract class Service extends Invoker.Invocation {

	public static final String invocationType = "service";
	
	private volatile long loadedTime;
	private volatile long startedTime;
	private volatile long stoppedTime;
	private volatile boolean isRunning;
	
	public Service()
	{
		loadedTime = System.currentTimeMillis();
		ServicePlugin.addService(this);
	}
	
	@Override
	public void run() {
		try {
			if (isJPARequired())
				super.run();
			else
				execute();
		} catch (Exception e) {
			Logger.error(e, "error starting service '%s'", getClass().getName());
		}
	}
	
	public boolean isRunning()
	{
		return isRunning;
	}

	public String getStatus()
	{
		StringWriter sw = new StringWriter();
		PrintWriter out = new PrintWriter(sw);
		out.printf("running: %s\n", isRunning());
		out.printf("loaded: %s\n", new Date(loadedTime));
		out.printf("started: %s\n", new Date(startedTime));
		if (stoppedTime > 0)
			out.printf("stopped: %s\n", new Date(stoppedTime));
		out.println();
		String dumped = dump();
		out.println(dumped != null ? dumped : "(empty)");
		
		return sw.toString();
	}
	
	public long getLoadedTime()
	{
		return loadedTime;
	}
	
	public long getStartedTime()
	{
		return startedTime;
	}
	
	public long getStoppedTime()
	{
		return stoppedTime;
	}

	@Override
	public void execute() throws Exception {
		startedTime = System.currentTimeMillis();
		isRunning = true;
		try {
			startService();
		} finally {
			isRunning = false;
			stoppedTime = System.currentTimeMillis();
		}
	}

	@Override
	public InvocationContext getInvocationContext() {
		return new InvocationContext(invocationType, this.getClass().getAnnotations());
	}
	
	/**
	 * Stop service
	 */
	public void stopService()
	{
	}
	
	/**
	 * Determine whether the service needs JPA or not
	 * @return true if the service requires JPA, false otherwise
	 */
	protected boolean isJPARequired()
	{
		return false;
	}
	
	/**
	 * Dump service current status and information
	 * @return
	 */
	protected String dump()
	{
		return null;
	}
	
	/**
	 * The method is to define this service is able to run as multiple instances or not <br />
	 * true if the service can be run multiple instances, false otherwise.
	 * @return true or false whether the service can be run as multiple instances. 
	 */
	public abstract boolean isMutex();
	/**
	 * The actually what service does, the method will be called in background
	 * and it's not running in Play JPA environment.
	 */
	protected abstract void startService();
}
