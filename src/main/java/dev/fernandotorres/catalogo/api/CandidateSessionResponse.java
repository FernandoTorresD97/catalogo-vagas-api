package dev.fernandotorres.catalogo.api;

public record CandidateSessionResponse(boolean authenticated, Long id, String fullName, String email) {

    public static CandidateSessionResponse anonymous() {
        return new CandidateSessionResponse(false, null, null, null);
    }
}
