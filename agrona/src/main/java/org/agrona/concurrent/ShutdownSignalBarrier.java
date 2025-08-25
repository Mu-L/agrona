/*
 * Copyright 2014-2025 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agrona.concurrent;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * One time barrier for blocking one or more threads until a SIGINT or SIGTERM signal is received from the operating
 * system or by programmatically calling {@link #signal()}. Useful for shutting down a service.
 */
public class ShutdownSignalBarrier
{
    /**
     * Signals the barrier will be registered for.
     */
    private static final String[] SIGNAL_NAMES = { "INT", "TERM" };
    private static final ArrayList<CountDownLatch> LATCHES = new ArrayList<>();

    static
    {
        final Runnable handler = ShutdownSignalBarrier::signalAndClearAll;
        for (final String name : SIGNAL_NAMES)
        {
            SigInt.register(name, handler);
        }
    }

    private final CountDownLatch latch = new CountDownLatch(1);

    /**
     * Construct and register the barrier ready for use.
     */
    public ShutdownSignalBarrier()
    {
        synchronized (LATCHES)
        {
            LATCHES.add(latch);
        }
    }

    /**
     * Programmatically signal awaiting threads on the latch associated with this barrier.
     */
    public void signal()
    {
        final boolean found;
        synchronized (LATCHES)
        {
            found = LATCHES.remove(latch);
        }

        if (found)
        {
            latch.countDown();
        }
    }

    /**
     * Programmatically signal all awaiting threads.
     */
    public void signalAll()
    {
        signalAndClearAll();
    }

    /**
     * Remove the barrier from the shutdown signals.
     */
    public void remove()
    {
        synchronized (LATCHES)
        {
            LATCHES.remove(latch);
        }
    }

    /**
     * Await the reception of the shutdown signal.
     */
    public void await()
    {
        try
        {
            latch.await();
        }
        catch (final InterruptedException ignore)
        {
            Thread.currentThread().interrupt();
        }
    }

    private static void signalAndClearAll()
    {
        final Object[] latches;
        synchronized (LATCHES)
        {
            latches = LATCHES.toArray();
            LATCHES.clear();
        }

        for (final Object latch : latches)
        {
            ((CountDownLatch)latch).countDown();
        }
    }
}
