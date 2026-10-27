package dev.fernandotorres.catalogo.persistence;

import dev.fernandotorres.catalogo.domain.CandidateAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidateAccountRepository extends JpaRepository<CandidateAccount, Long> {
    Optional<CandidateAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
