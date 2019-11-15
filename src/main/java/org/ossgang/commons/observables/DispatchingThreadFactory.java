package org.ossgang.commons.observables;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory to be used by the internal {@link DispatchingObservable} thread pool. This is pretty much a copy
 * of Executors.DefaultThreadFactory, with the following differences:
 * <ul>
 *     <li>Threads are named "ossgang-Observable-dispatcher-X"</li>
 *     <li>It produces Daemon threads</li>
 * </ul>
 */
class DispatchingThreadFactory implements ThreadFactory {
    private static final AtomicInteger THREAD_INDEX = new AtomicInteger(0);
    private static final String NAME_PREFIX = "ossgang-Observable-dispatcher-";
    private final ThreadGroup group;

    DispatchingThreadFactory() {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, NAME_PREFIX + THREAD_INDEX.getAndIncrement(), 0);
        if (!t.isDaemon()) {
            t.setDaemon(true);
        }
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }
}
