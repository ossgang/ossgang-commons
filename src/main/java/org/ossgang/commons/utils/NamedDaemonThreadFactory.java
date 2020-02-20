package org.ossgang.commons.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory used to create internal thread pools. This is pretty much a copy
 * of Executors.DefaultThreadFactory, with the following differences:
 * <ul>
 *     <li>Threads are named "(prefix)-X", where the prefix can be defined</li>
 *     <li>It produces Daemon threads</li>
 * </ul>
 */
public class NamedDaemonThreadFactory implements ThreadFactory {
    private static final AtomicInteger THREAD_INDEX = new AtomicInteger(0);
    private final ThreadGroup group;
    private final String prefix;

    private NamedDaemonThreadFactory(String prefix) {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.prefix = prefix;
    }

    public static NamedDaemonThreadFactory daemonThreadFactoryWithPrefix(String prefix) {
        return new NamedDaemonThreadFactory(prefix);
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, prefix + THREAD_INDEX.getAndIncrement(), 0);
        if (!t.isDaemon()) {
            t.setDaemon(true);
        }
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }
}
