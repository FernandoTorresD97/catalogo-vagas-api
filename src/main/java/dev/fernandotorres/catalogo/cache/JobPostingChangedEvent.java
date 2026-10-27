package dev.fernandotorres.catalogo.cache;

public record JobPostingChangedEvent(long jobId, JobSearchSnapshot previous, JobSearchSnapshot current) {
}
