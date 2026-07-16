package com.crm.billing.service;

import com.crm.billing.entity.PaymentRequestSequence;
import com.crm.billing.repository.PaymentRequestSequenceRepository;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * {@code PaymentRequestSequenceRepository} is mocked, but its answers are backed by a real
 * in-memory map guarded by a {@link ReentrantLock} that only releases after both the read and the
 * write step complete — the same critical section shape a real {@code SELECT ... FOR UPDATE} row
 * lock would enforce. This exercises the real {@link PaymentRequestNumberingService} /
 * {@link PaymentRequestSequenceInitializer} code under real concurrent threads without needing a
 * live database.
 */
class PaymentRequestNumberingServiceTest {

    @RepeatedTest(3)
    void concurrentIssuance_isGapFreeAndCollisionFree() throws InterruptedException {
        PaymentRequestSequenceRepository repository = mock(PaymentRequestSequenceRepository.class);
        Map<String, PaymentRequestSequence> store = Collections.synchronizedMap(new HashMap<>());
        ReentrantLock lock = new ReentrantLock();

        when(repository.existsByWorkspaceIdAndYear(anyLong(), anyInt())).thenAnswer(inv ->
                store.containsKey(key(inv.getArgument(0), inv.getArgument(1))));

        doAnswer(inv -> {
            PaymentRequestSequence seq = inv.getArgument(0);
            store.putIfAbsent(key(seq.getWorkspaceId(), seq.getYear()), seq);
            return seq;
        }).when(repository).saveAndFlush(any());

        when(repository.findForUpdate(anyLong(), anyInt())).thenAnswer(inv -> {
            lock.lock(); // held until the caller's subsequent save() below — mimics a row lock's span
            Long workspaceId = inv.getArgument(0);
            Integer year = inv.getArgument(1);
            return java.util.Optional.ofNullable(store.get(key(workspaceId, year)));
        });

        doAnswer(inv -> {
            PaymentRequestSequence seq = inv.getArgument(0);
            store.put(key(seq.getWorkspaceId(), seq.getYear()), seq);
            lock.unlock(); // release only after the increment is written back
            return seq;
        }).when(repository).save(any());

        PaymentRequestSequenceInitializer initializer = new PaymentRequestSequenceInitializer(repository);
        PaymentRequestNumberingService numberingService = new PaymentRequestNumberingService(repository, initializer);

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        CopyOnWriteArrayList<String> numbers = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    numbers.add(numberingService.nextNumber(1L, 2026));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        ready.await();
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        Set<String> unique = numbers.stream().collect(Collectors.toSet());
        assertThat(unique).hasSize(threads); // no collisions
        Set<Long> suffixes = unique.stream().map(n -> Long.valueOf(n.substring(n.length() - 4)) - 1000).collect(Collectors.toSet());
        assertThat(suffixes).containsExactlyInAnyOrderElementsOf(
                java.util.stream.LongStream.rangeClosed(1, threads).boxed().collect(Collectors.toSet())); // gap-free 1..N
    }

    private static String key(Long workspaceId, Integer year) {
        return workspaceId + ":" + year;
    }
}
