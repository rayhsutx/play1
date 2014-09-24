package play.services;

import java.util.concurrent.ThreadFactory;

class ServiceThreadFactory implements ThreadFactory {
	
	private final ThreadGroup group;

	public ServiceThreadFactory() {
		SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
	}

	@Override
	public Thread newThread(Runnable r) {
		String name = null;
		name = String.format("Service-%s-%d", r.getClass().getSimpleName(), ServicePlugin.getIndexOfService(r));
		Thread t = new Thread(group, r, name, 0);
        if (t.isDaemon()) {
            t.setDaemon(false);
        }
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
	}

}
