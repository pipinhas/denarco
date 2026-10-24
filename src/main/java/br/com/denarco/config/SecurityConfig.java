package br.com.denarco.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Regras de acesso do sistema: quais páginas são públicas, quais exigem login
 * e quais exigem um perfil específico.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(acesso -> acesso
                        .requestMatchers("/login", "/cadastro", "/cadastro/corporativo", "/senha/**",
                                "/privacidade", "/error", "/css/**", "/js/**", "/img/**", "/vendor/**").permitAll()
                        // Console do banco H2: só existe no perfil dev.
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/gestao/**").hasRole("GESTOR")
                        .requestMatchers("/atendimento/**", "/painel/**", "/api/painel/**", "/consulta/**").hasRole("SERVIDOR")
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("senha")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?erro"))
                .logout(logout -> logout.logoutSuccessUrl("/login?saiu"))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    // BCrypt gera um hash de mão única: nem o sistema consegue recuperar a senha original.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Quem está acima herda as permissões de quem está abaixo (ex.: ADMIN acessa /gestao).
    @Bean
    public RoleHierarchy hierarquiaDePerfis() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMIN > ROLE_GESTOR
                ROLE_GESTOR > ROLE_SERVIDOR
                ROLE_SERVIDOR > ROLE_CIDADAO
                """);
    }
}
