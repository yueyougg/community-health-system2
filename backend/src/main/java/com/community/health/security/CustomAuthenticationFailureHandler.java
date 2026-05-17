package com.community.health.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        String message;
        if (exception instanceof DisabledException) {
            message = "用户已被禁用，请联系管理员";
        } else if (exception.getMessage() != null && exception.getMessage().contains("用户不存在")) {
            message = "用户名或密码错误";
        } else {
            message = "用户名或密码错误";
        }
        
        Map<String, Object> payload = Map.of(
                "success", false,
                "message", message,
                "data", ""
        );
        new ObjectMapper().writeValue(response.getWriter(), payload);
    }
}