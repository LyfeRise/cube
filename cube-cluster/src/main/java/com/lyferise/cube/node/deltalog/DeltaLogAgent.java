package com.lyferise.cube.node.deltalog;

import com.lyferise.cube.agents.Agent;
import com.lyferise.cube.concurrency.RingBuffer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

import static java.lang.Thread.sleep;

@Slf4j
public class DeltaLogAgent extends Agent {
    private final RingBuffer<DeltaLogQuery> readQueue = new RingBuffer<>();
    private final RingBuffer<DeltaLogRecordGroup> appendQueue = new RingBuffer<>();
    private final Consumer<DeltaLogQueryResult> resultHandler;
    private final DeltaLog deltaLog;
    private final int batchSize;

    public DeltaLogAgent(
            final DeltaLog deltaLog,
            final int batchSize,
            final Consumer<DeltaLogQueryResult> resultHandler) {

        this.deltaLog = deltaLog;
        this.batchSize = batchSize;
        this.resultHandler = resultHandler;
    }

    @SneakyThrows
    public void enqueue(final DeltaLogQuery readQuery) {
        readQueue.put(readQuery);
    }

    @SneakyThrows
    public void enqueue(final DeltaLogRecordGroup recordGroup) {
        appendQueue.put(recordGroup);
    }

    @Override
    protected void execute() {

        // write
        var writeCount = 0;
        DeltaLogRecordGroup writeGroup;
        while (writeCount < batchSize && (writeGroup = appendQueue.poll()) != null) {
            deltaLog.append(writeGroup);
            writeCount += writeGroup.getRecords().size();
        }

        // read
        var readCount = 0;
        DeltaLogQuery query;
        while (readCount < batchSize && (query = readQueue.poll()) != null) {
            final var records = deltaLog.read(query).getRecords();
            resultHandler.accept(new DeltaLogQueryResult(query.getQueryId(), records));
            readCount += records.size();
        }
    }

    @Override
    @SneakyThrows
    protected void waitForSignal() {

        // if there is nothing to read or write, wait 50 milliseconds
        if (readQueue.isEmpty() && appendQueue.isEmpty()) sleep(50);
    }

    @Override
    protected void onError(final Exception e) {
        log.error("Delta log agent error", e);
    }
}