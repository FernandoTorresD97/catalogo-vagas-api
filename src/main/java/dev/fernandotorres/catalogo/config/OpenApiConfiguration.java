package dev.fernandotorres.catalogo.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "AdminApiKey",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-Admin-Key"
)
@SecurityScheme(
        name = "CandidateSession",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE,
        paramName = "JSESSIONID",
        description = "Sessão de candidato iniciada em POST /api/v1/auth/login. POSTs autenticados também exigem o token CSRF obtido em GET /api/v1/auth/csrf."
)
@OpenAPIDefinition(info = @Info(
        title = "Catálogo de Vagas API",
        version = "v1",
        description = "API de consulta de vagas com filtros combináveis, paginação, autenticação de candidatos e registro de interesse."
))
public class OpenApiConfiguration {
}
