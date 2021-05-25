package com.lyferise.cube.concurrency;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Runtime.getRuntime;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RingBufferStressTest {

    @Disabled
    @Test
    @SneakyThrows
    public void shouldReadAndWriteUsingMultipleThreads() {

        // 10 million messages
        final int messageCount = 10_000_000;
        final RingBuffer<Integer> ringBuffer = new RingBuffer<>(1024);

        // number of reader/writer threads
        final int threadCount = getRuntime().availableProcessors();

        // writers
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final Thread[] writers = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int k = i;
            writers[i] = new Thread(() -> {
                for (int j = k; j < messageCount; j += threadCount) {
                    while (!ringBuffer.offer(j)) {
                        Thread.yield();
                    }
                }
                latch.countDown();
            });
        }

        final BitSet bitSet = new BitSet();

        // readers
        final Thread[] readers = new Thread[threadCount];
        final ReentrantLock lock = new ReentrantLock();
        for (int i = 0; i < threadCount; i++) {
            readers[i] = new Thread(() -> {
                final BitSet readSet = new BitSet();
                while (!ringBuffer.isEmpty() || latch.getCount() > 0) {
                    final Integer value = ringBuffer.poll();
                    if (value != null) {
                        readSet.set(value);
                    } else {
                        Thread.yield();
                    }
                }

                lock.lock();
                try {
                    bitSet.or(readSet);
                } finally {
                    lock.unlock();
                }
            });
        }

        // start threads
        for (int i = 0; i < threadCount; i++) {
            writers[i].start();
            readers[i].start();
        }

        // wait for threads to terminate
        for (int i = 0; i < threadCount; i++) {
            writers[i].join();
            readers[i].join();
        }

        // verify messages seen by readers
        assertEquals(messageCount, bitSet.cardinality());
        assertEquals(messageCount, bitSet.length());
    }
}