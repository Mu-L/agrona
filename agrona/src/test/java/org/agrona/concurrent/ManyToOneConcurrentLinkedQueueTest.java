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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Queue;
import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

class ManyToOneConcurrentLinkedQueueTest
{
    private final Queue<Integer> queue = new ManyToOneConcurrentLinkedQueue<>();

    @Test
    void shouldBeEmpty()
    {
        assertThat(queue.size(), is(0));
        assertTrue(queue.isEmpty());
    }

    @Test
    void shouldNotBeEmpty()
    {
        assertTrue(queue.offer(1));
        assertFalse(queue.isEmpty());
        assertThat(queue.size(), is(1));
    }

    @Test
    void shouldFailToPoll()
    {
        assertNull(queue.poll());
    }

    @Test
    void shouldOfferItem()
    {
        assertTrue(queue.offer(7));
        assertThat(queue.size(), is(1));
    }

    @Test
    void shouldExchangeItem()
    {
        final int testItem = 1;

        assertTrue(queue.offer(testItem));
        assertThat(queue.poll(), is(testItem));
    }

    @Test
    void shouldExchangeInFifoOrder()
    {
        final int numItems = 7;

        for (int i = 0; i < numItems; i++)
        {
            assertTrue(queue.offer(i));
        }

        assertThat(queue.size(), is(numItems));

        for (int i = 0; i < numItems; i++)
        {
            assertThat(queue.poll(), is(i));
        }

        assertThat(queue.size(), is(0));
        assertTrue(queue.isEmpty());
    }

    @Test
    void shouldExchangeInFifoOrderInterleaved()
    {
        final int numItems = 7;

        for (int i = 0; i < numItems; i++)
        {
            assertTrue(queue.offer(i));
            assertThat(queue.poll(), is(i));
        }

        assertThat(queue.size(), is(0));
        assertTrue(queue.isEmpty());
    }

    @Test
    void shouldToString()
    {
        assertThat(queue.toString(), is("{}"));

        for (int i = 0; i < 5; i++)
        {
            assertTrue(queue.offer(i));
        }

        assertThat(queue.toString(), is("{0, 1, 2, 3, 4}"));
    }

    @Test
    @Timeout(10)
    void shouldTransferConcurrently()
    {
        final int count = 1_000_000;
        final int numThreads = 2;
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final Runnable producer =
            () ->
            {
                for (int i = 0, items = count / numThreads; i < items; i++)
                {
                    assertTrue(queue.offer(i));
                }
            };

        for (int i = 0; i < numThreads; i++)
        {
            executor.execute(producer);
        }

        for (int i = 0; i < count; i++)
        {
            while (null == queue.poll())
            {
                Thread.yield();
            }
        }

        executor.shutdown();
        assertTrue(queue.isEmpty());
    }
}
