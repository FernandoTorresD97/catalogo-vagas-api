package dev.fernandotorres.catalogo.security;

import dev.fernandotorres.catalogo.persistence.CandidateAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CandidateAccountDetailsService implements UserDetailsService {

    private final CandidateAccountRepository accounts;

    public CandidateAccountDetailsService(CandidateAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Conta não encontrada"));
    }
}
