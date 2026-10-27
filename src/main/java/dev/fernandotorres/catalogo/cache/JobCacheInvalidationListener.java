package dev.fernandotorres.catalogo.cache;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

@Component
public class JobCacheInvalidationListener {

    private final JobRedisCache cache;

    public JobCacheInvalidationListener(JobRedisCache cache) {
        this.cache = cache;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobPostingChanged(JobPostingChangedEvent event) {
        cache.evictJob(event);
    }
}
