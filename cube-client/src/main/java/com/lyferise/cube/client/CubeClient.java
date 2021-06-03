package com.lyferise.cube.client;

import com.lyferise.cube.client.websockets.WebSocketsClient;
import com.lyferise.cube.concurrency.Signal;
import com.lyferise.cube.protocol.MessageReader;
import com.lyferise.cube.protocol.MessageWriter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

import static com.lyferise.cube.protocol.MessageCode.AUTH;
import static com.lyferise.cube.protocol.MessageCode.AUTH_SUCCESS;

@Slf4j
public class CubeClient {
    private final WebSocketsClient client;
    private Signal authSignal;

    @SneakyThrows
    public CubeClient(final String address, final String email, final String password) {
        client = new WebSocketsClient(new URI(address), this::dispatch);
        if (!client.connectBlocking()) {
            throw new UnsupportedOperationException("Failed to connect to " + address);
        }
    }

    @SneakyThrows
    public void reconnect(final String email, final String password) {
        if (!client.reconnectBlocking()) {
            throw new UnsupportedOperationException("Failed to reconnect");
        }
        authenticate(email, password);
    }

    @SneakyThrows
    public void close() {
        client.closeBlocking();
    }

    private void dispatch(final byte[] data) {
        final var reader = new MessageReader(data);
        switch (reader.getMessageCode()) {
            case AUTH_SUCCESS -> {
                log.info("AUTH_SUCCESS");
                authSignal.set();
            }
            default -> log.info("dispatch {}", reader.getMessageCode());
        }
    }

    private void authenticate(final String email, final String password) {
        authSignal = new Signal();
        final MessageWriter writer = new MessageWriter(AUTH);
        writer.write(email);
        writer.write(password);
        client.send(writer.toByteArray());
        if (!authSignal.await(5000)) {
            throw new UnsupportedOperationException("Auth timeout");
        }
    }
}