package com.ecom.atendimento.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.yc.pr.sec.AccessTokenFilter;
import com.yc.pr.sec.JwtAccessDeniedHandler;
import com.yc.pr.sec.JwtAuthenticationEntryPoint;
import com.yc.pr.sec.JwtUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuração de Spring Security para ecom.atendimento.
 *
 * Implementa autenticação JWT usando yc.pr library.
 * - Rotas públicas: /actuator/**, /unsec/**, /open/**
 * - Demais rotas requerem autenticação JWT via header Authorization: Bearer {token}
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${spring.application.name}")
    private String appName;

	@Value("${yc.security.keys.accessToken.secret}")
	private String accessTokenSecret;

	@Value("${yc.security.keys.accessToken.expirationMinutes}")
	private int accessTokenExpirationMinutes;

	@Value("${yc.security.keys.refreshToken.secret}")
	private String refreshTokenSecret;

	@Value("${yc.security.keys.refreshToken.expirationDays}")
	private int refreshTokenExpirationDays;

	@Value("${management.security.allowed-ips}")
	private String allowedIps;

	@Value("${management.security.allowed-subnets}")
	private String allowedSubnets;

	@Bean
	JwtUtils jwtUtils() {
		return new JwtUtils(this.accessTokenSecret, this.accessTokenExpirationMinutes, this.refreshTokenSecret,
				this.refreshTokenExpirationDays);
	}

	@Bean
	AccessTokenFilter accessTokenFilter() {
		return new AccessTokenFilter(this.appName);
	}

	@Bean
	JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
		return new JwtAuthenticationEntryPoint();
	}

	@Bean
	JwtAccessDeniedHandler jwtAccessDeniedHandler() {
		return new JwtAccessDeniedHandler();
	}

	private String buildIpExpression() {
		StringBuilder expression = new StringBuilder();

		// Adicionar IPs específicos
		if (allowedIps != null && !allowedIps.trim().isEmpty()) {
			String[] ips = allowedIps.split(",");
			for (String ip : ips) {
				ip = ip.trim();
				if (!ip.isEmpty()) {
					if (expression.length() > 0) {
						expression.append(" or ");
					}
					expression.append("hasIpAddress('").append(ip).append("')");
				}
			}
		}

		// Adicionar subnets
		if (allowedSubnets != null && !allowedSubnets.trim().isEmpty()) {
			String[] subnets = allowedSubnets.split(",");
			for (String subnet : subnets) {
				subnet = subnet.trim();
				if (!subnet.isEmpty()) {
					if (expression.length() > 0) {
						expression.append(" or ");
					}
					expression.append("hasIpAddress('").append(subnet).append("')");
				}
			}
		}

		// Se não há configuração, permitir apenas localhost
		if (expression.length() == 0) {
			expression.append("hasIpAddress('127.0.0.1') or hasIpAddress('::1')");
		}

		log.debug("\n   > IP expression for /actuator endpoints: {}", expression.toString());
		return expression.toString();
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http/*, URLFilter urlFilter*/) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.exceptionHandling(handling -> handling.authenticationEntryPoint(this.jwtAuthenticationEntryPoint())
						.accessDeniedHandler(this.jwtAccessDeniedHandler()))
				.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(requests -> requests
						.requestMatchers("/actuator/**")
							.access(new WebExpressionAuthorizationManager(this.buildIpExpression()))

                        .requestMatchers("/open/**")
                        	.permitAll()

						.requestMatchers("/metrics/**")
							.permitAll()

						.anyRequest()
							.authenticated());

		// URLFilter is automatically registered by Spring MVC (@Component annotation)
		// Commenting out to avoid duplicate filter execution (was being registered twice)
		// http.addFilterBefore(urlFilter, SecurityContextHolderFilter.class);
		http.addFilterBefore(this.accessTokenFilter(), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
