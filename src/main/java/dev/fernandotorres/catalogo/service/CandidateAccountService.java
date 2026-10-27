package dev.fernandotorres.catalogo.service;

import dev.fernandotorres.catalogo.api.RegisterCandidateRequest;
import dev.fernandotorres.catalogo.domain.CandidateAccount;
import dev.fernandotorres.catalogo.persistence.CandidateAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class CandidateAccountService {

    private final CandidateAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public CandidateAccountService(CandidateAccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CandidateAccount register(RegisterCandidateRequest request) {
        if (accounts.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(CONFLICT, "Já existe uma conta com esse e-mail");
        }

        var account = new CandidateAccount(
                request.fullName(), request.email(), passwordEncoder.encode(request.password())
        );
        try {
            return accounts.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(CONFLICT, "Já existe uma conta com esse e-mail", exception);
        }
    }
}
