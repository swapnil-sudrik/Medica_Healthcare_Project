package com.fspl.medica_healthcare.filters;

import com.fspl.medica_healthcare.services.JwtService;
import com.fspl.medica_healthcare.services.UserInfoService;
import com.fspl.medica_healthcare.utils.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {

        String encryptedToken = ((ServletServerHttpRequest) request)
                .getServletRequest().getParameter("token");

        String token = encryptionUtil.decrypt(encryptedToken);
        System.out.println("token from web socket : "+ token);

        ((ServletServerHttpRequest) request).getServletRequest().getSession();

        if (token != null) {
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = userInfoService.loadUserByUsername(username);
            if (jwtService.validateToken(token, userDetails)){
                attributes.put("username", username);
                return true;
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
