package org.ossgang.commons;

import org.ossgang.commons.awaitables.Await;

import java.lang.ref.WeakReference;
import java.time.Duration;

/**
 * Test Creation Utility For GC-bases Tests
 */
public class GcTests {
    private GcTests() {
        throw new UnsupportedOperationException("static only");
    }

    /**
     * Force the GC to run a few times. Note that (in Oracles JVM) multiple GC runs are needed to resolve indirections
     * and cycles, therefore this methods makes sure that the GC run at least 10 times.
     */
    public static void forceGc() {
        for (int run = 0; run < 10; run++) {
            WeakReference<?> ref = new WeakReference<>(new Object());
            Await.await(() -> wasGarbageCollected(ref)) //
                    .withErrorMessage("Weak ref should have been collected already!") //
                    .withRetryInterval(Duration.ofMillis(100)) //
                    .withRetryCount(100) //
                    .indefinitely();
        }
    }

    public static boolean wasGarbageCollected(WeakReference<?> weakHolder) {
        System.gc();
        return weakHolder.get() == null;
    }
}
