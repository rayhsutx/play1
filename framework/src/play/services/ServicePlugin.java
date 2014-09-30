package play.services;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.exceptions.UnexpectedException;
import play.utils.PThreadFactory;

public class ServicePlugin extends PlayPlugin {
	
	private static final Map<String, ServiceHolder> services = new ConcurrentHashMap<String, ServiceHolder>();
	private ExecutorService executor = null;

	public ServicePlugin() {
	}
	
	public static void addService(Service service)
	{
		synchronized (services)
		{
			ServiceHolder holder = null;
			String className = service.getClass().getName();
			if (service.isMutex() && services.containsKey(className))
				throw new MutexServiceException("service '" + className + "' has been loaded");
			
			Logger.debug("added service '%s'", className);
			holder = services.get(className);
			if (holder == null)
				holder = new ServiceHolder();
			holder.services.add(service);
			services.put(className, holder);
		}
	}

	@Override
	public final String getStatus() {
		StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        
        out.println("Loaded services:");
        out.println("~~~~~~~~~~~~~~~~~");
        synchronized (services)
        {
	        Iterator<Entry<String, ServiceHolder>> iterator = services.entrySet().iterator();
	        while (iterator.hasNext())
	        {
	        	Entry<String, ServiceHolder> entry = iterator.next();
	        	String serviceName = entry.getKey();
	        	ServiceHolder holder = entry.getValue();
	        	out.println(serviceName);
	        	for (int i = 0; i < holder.services.size(); i++)
	        	{
	        		Service s = holder.services.get(i);
		        	out.printf("=======Service %s %d=======\n", serviceName, i);
		        	out.println(s.getStatus());
		        	out.printf("=======end of %s %d=======\n", serviceName, i);
	        	}
	        }
        }
        
		return sw.toString();
	}

	@Override
	public void afterApplicationStart() {
		if (!"on".equalsIgnoreCase(Play.configuration.getProperty("application.ServicePlugin", "on")))
		{
			Logger.info("ServicePlugin disabled");
			return;
		}
		
		List<Class<?>> serviceClasses = new ArrayList<Class<?>>();
		for (Class<?> clazz : Play.classloader.getAllClasses()) {
			String serviceEnabled = Play.configuration.getProperty("service." + clazz.getName() , "on");
            if (Service.class.isAssignableFrom(clazz) && "on".equalsIgnoreCase(serviceEnabled)) {
            	Logger.info("service '%s' loaded", clazz.getName());
                serviceClasses.add(clazz);
            }
        }
		
		if (serviceClasses.size() == 0)
			return;
		
		executor = Executors.newFixedThreadPool(serviceClasses.size(), new PThreadFactory("Service"));
		for (Class<?> clazz : serviceClasses)
		{
			try {
				Service s = (Service) clazz.newInstance();
				executor.execute(s);
			} catch (InstantiationException e) {
				throw new UnexpectedException("service could not be instantiated", e);
			} catch (IllegalAccessException e) {
				throw new UnexpectedException("service could not be instantiated", e);
			} catch (Throwable e) {
				throw new UnexpectedException(e);
			}
		}
	}

	@Override
	public void onApplicationStop() {
		Iterator<Entry<String, ServiceHolder>> iterator = services.entrySet().iterator();
		while (iterator.hasNext())
		{
			Entry<String, ServiceHolder> entry = iterator.next();
			ServiceHolder holder = entry.getValue();
			for (Service s : holder.services)
				s.stopService();
			holder.services.clear();
			iterator.remove();
		}
		if (executor != null)
			executor.shutdownNow();
	}

	public static int getIndexOfService(Runnable r) {
		String className = r.getClass().getName();
		Logger.debug("index of service '%s'", className);
		int index = services.get(className).services.indexOf(r);
		return index;
	}
	
	static class ServiceHolder
	{
		public List<Service> services = new LinkedList<Service>();
	}
	
	public static class MutexServiceException extends RuntimeException 
	{
		private static final long serialVersionUID = 2059460512156585774L;

		public MutexServiceException(String message) {
			super(message);
		}
		
	}
}
