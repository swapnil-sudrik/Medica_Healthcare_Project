package com.fspl.medica_healthcare.filters;



import com.fspl.medica_healthcare.exceptions.RecordNotFoundException;
import com.fspl.medica_healthcare.repositories.LoginDetailsRepository;
import com.fspl.medica_healthcare.services.JwtService;
import com.fspl.medica_healthcare.services.UserInfoService;
import com.fspl.medica_healthcare.utils.EncryptionUtil;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserInfoService userDetailsService;

    @Autowired
    private LoginDetailsRepository loginDetailsRepository;

    @Autowired
    private EncryptionUtil encryptionUtil;

    private static final Logger log = Logger.getLogger(JwtAuthFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        String username = null;
        try {
            HttpSession session = request.getSession(false); // Get existing session (if present)
            String token = null;
            String encryptedToken = null;

            // 1. get token from session
            if ( session != null) {
                encryptedToken = (String) session.getAttribute("jwtToken");
            }

            // 2. Check if JWT token is present in the Authorization header
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (encryptedToken==null &&  authHeader != null && authHeader.startsWith("Bearer ")) {
                encryptedToken = authHeader.substring(7);
            }


            if (encryptedToken == null){
                request.setAttribute("token", "notFound");
                filterChain.doFilter(request, response);
                return;
            }

           token= encryptionUtil.decrypt(encryptedToken);

            if (token == null){
                request.setAttribute("token", "notFound");
                filterChain.doFilter(request, response);
                return;
            }


            try {
                username = jwtService.extractUsername(token); // Extract username from token
            } catch (Exception e) {
                e.printStackTrace();
                // Invalid token (e.g., malformed or expired)
                log.error("An unexpected error occurred while extractUsername() from token : "+ ExceptionUtils.getStackTrace(e)+ " username: "+ username);
                request.setAttribute("tokenError", "invalidToken");
                filterChain.doFilter(request, response);
                return;
            }

//        SecurityContextHolder.getContext().getAuthentication() == null
//        ** -checks whether there is no authentication information stored in the SecurityContext.
//        1:- SecurityContextHolder.getContext() retrieves the SecurityContext, which holds security-related information (like authentication details) for the current request.
//        2:- .getAuthentication() fetches the authentication object (if available).
//        3:-   == null checks if no authentication details are currently set.

            if (username!=null && token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (loginDetailsRepository.findByUsername(username).isEmpty()) {
                    request.setAttribute("logout", "invalidToken");
                    filterChain.doFilter(request, response);
                    return;
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (userDetails==null){
                    request.setAttribute("logout", "invalidToken");
                    filterChain.doFilter(request, response);
                    return;
                }

                if (jwtService.validateToken(token, userDetails)) {


//                    UsernamePasswordAuthenticationToken = 1) Before authentication: It contains only the username and password.
//                                                          2) After authentication: It contains the UserDetails and authorities, and its isAuthenticated() flag is true.

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // 3. Store authentication in the session for future requests
                    if (session == null) {
                        session = request.getSession(true); // Create session if not exists
//                        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext().getAuthentication());
                        session.setAttribute("jwtToken", token);
                    }
                } else {
                    // Token validation failed
                    request.setAttribute("tokenError", "invalidToken");
                    filterChain.doFilter(request, response);
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error in JwtAuthFilter : "+ ExceptionUtils.getStackTrace(e)+ " username : "+username);
        }
    }
}


