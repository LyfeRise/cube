package com.lyferise.cube.time;

import java.time.ZonedDateTime;

import static java.time.Instant.ofEpochMilli;
import static java.time.ZoneId.of;
import static java.time.ZonedDateTime.ofInstant;

public interface CubeClock {

    long CUBE_EPOCH = 1577836800000L;

    long getMillisecondsSinceEpoch();

    static ZonedDateTime toUTC(final long millisecondsSinceEpoch) {
        return ofInstant(
                ofEpochMilli(CUBE_EPOCH + millisecondsSinceEpoch),
                of("UTC"));
    }
}