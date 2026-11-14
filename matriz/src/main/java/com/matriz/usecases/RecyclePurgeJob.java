package com.matriz.usecases;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.matriz.recycle.RecycleBin;

/**
 * Eliminates for good what has been in the recycle bin for 30 days (R20). It runs once a day; the lists
 * and the restores already ignore what expired, so a late or missed run changes nothing for the user.
 * The tests call {@link #purge()} themselves and switch the schedule off with the cron value "-".
 */
@Component
public class RecyclePurgeJob {

    private static final Logger log = LoggerFactory.getLogger(RecyclePurgeJob.class);

    private final RecycleBin bin;
    private final Clock clock;

    RecyclePurgeJob(RecycleBin bin, Clock clock) {
        this.bin = bin;
        this.clock = clock;
    }

    @Scheduled(cron = "${matriz.recycle.purge.cron:0 0 3 * * *}")
    @Transactional
    public int purge() {
        int removed = bin.purgeExpired(clock.instant());
        if (removed > 0) {
            log.info("Recycle bin: {} items eliminated for good", removed);
        }
        return removed;
    }
}
