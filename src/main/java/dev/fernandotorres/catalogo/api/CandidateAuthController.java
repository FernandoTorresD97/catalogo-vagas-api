package dev.fernandotorres.catalogo.api;

import dev.fernandotorres.catalogo.domain.CandidateAccount;
import dev.fernandotorres.catalogo.service.CandidateAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Candidate authentication", description = "Cadastro, login e sessão de candidatos")
public class CandidateAuthController {

    private final CandidateAccountService accounts;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public CandidateAuthController(
            CandidateAccountService accounts,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository
    ) {
        this.accounts = accounts;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/csrf")
    @Operation(summary = "Obtém token CSRF para ações autenticadas do navegador")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/register")
    @Operation(summary = "Cria uma conta de candidato")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterCandidateRequest request) {
        accounts.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia uma sessão de candidato")
    public CandidateSessionResponse login(
            @Valid @RequestBody LoginCandidateRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password())
            );
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(UNAUTHORIZED, "E-mail ou senha inválidos", exception);
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        return toSession((CandidateAccount) authentication.getPrincipal());
    }

    @GetMapping("/me")
    @Operation(summary = "Consulta a sessão atual")
    public CandidateSessionResponse currentSession() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof CandidateAccount account)) {
            return CandidateSessionResponse.anonymous();
        }
        return toSession(account);
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerra a sessão atual")
    @SecurityRequirement(name = "CandidateSession")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    private CandidateSessionResponse toSession(CandidateAccount account) {
        return new CandidateSessionResponse(true, account.getId(), account.getFullName(), account.getUsername());
    }
}
