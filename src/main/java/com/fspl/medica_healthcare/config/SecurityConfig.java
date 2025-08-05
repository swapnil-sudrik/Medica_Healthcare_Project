package com.fspl.medica_healthcare.config;

import com.fspl.medica_healthcare.filters.JwtAuthFilter;
import com.fspl.medica_healthcare.security.CustomAccessDeniedHandler;
import com.fspl.medica_healthcare.security.CustomAuthenticationEntryPoint;
import com.fspl.medica_healthcare.services.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Autowired
	private CustomAuthenticationEntryPoint point;

	@Autowired
	private CustomAccessDeniedHandler accessDeniedHandler;

	@Autowired
	private JwtAuthFilter authFilter;

	@Bean
	public UserDetailsService userDetailsService() {
		return new UserInfoService(); // Ensure UserInfoService implements UserDetailsService
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))  //create session only if required
				.csrf(AbstractHttpConfigurer::disable)
//				.cors(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS properly
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/auth/**", "/otp/**").permitAll()
						.requestMatchers("/super-admin/**").hasAuthority("SUPER_ADMIN")
						.requestMatchers("/api/prescriptions/**").hasAnyAuthority("DOCTOR")
						.requestMatchers("/users/**").hasAnyAuthority("DOCTOR", "SUPER_ADMIN", "ADMIN", "NURSE")
						.requestMatchers("/hospital/**").hasAnyAuthority("ADMIN", "SUPER_ADMIN")
						.requestMatchers("/patients/**").hasAnyAuthority("ADMIN", "DOCTOR", "RECEPTIONIST")
						.requestMatchers("/reports/**").hasAnyAuthority("ADMIN", "DOCTOR", "RECEPTIONIST")
						.requestMatchers("/api/appointments/**").hasAnyAuthority("RECEPTIONIST", "DOCTOR")
						.requestMatchers("/hospital/**").hasAuthority("ADMIN")
						.anyRequest().authenticated()
				)
				.exceptionHandling(ex -> ex.authenticationEntryPoint(point))
				.exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler));

		http.addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);
		http.formLogin(
				form ->
						form.loginPage("/login")
								.loginProcessingUrl("/login")
								.defaultSuccessUrl("/patientsWeb", true)
								.failureUrl("/login?error=true")
								//              .defaultSuccessUrl("/home")
								.permitAll());
		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of("http://127.0.0.1:5500","http://localhost:5173")); // Add allowed frontend origins
		configuration.setAllowCredentials(true); // Allow credentials (for JWT tokens)
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type","Cache-Control", "X-Requested-With"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(); // Password encoding
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
		authenticationProvider.setUserDetailsService(userDetailsService());
		authenticationProvider.setPasswordEncoder(passwordEncoder());
		return authenticationProvider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
}
