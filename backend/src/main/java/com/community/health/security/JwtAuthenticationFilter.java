package com.community.health.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        logger.info("Auth Header: {}", header);
        String token = null;
        if (header != null) {
            if (header.startsWith("Bearer ")) {
                token = header.substring(7).trim();
            } else {
                // 兼容不带 Bearer 前缀的情况
                token = header.trim();
            }

            // 去除前后引号（有些工具复制出来带引号）
            if (token.startsWith("\"") && token.endsWith("\"")) {
                token = token.substring(1, token.length() - 1);
            }

            // 简单的校验，确保提取的是类似 JWT 的格式 (至少包含两个点)
            if (token.split("\\.").length < 3) {
                logger.warn("Token format invalid: {}", token);
                token = null;
            } else {
                logger.info("Extracted Token: {}", token);
            }
        }

        if (token != null) {
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    String username = jwtTokenProvider.getUsername(token);
                    logger.info("Token valid for user: {}", username);

                    // 如果上下文已经有认证，但用户名不一致，重新设置
                    Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
                    if (currentAuth == null || !currentAuth.getName().equals(username)) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        UsernamePasswordAuthenticationToken authenticationToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                        logger.info("Authentication set in SecurityContext for user: {}", username);
                    }
                } else {
                    logger.warn("Token validation failed for token: {}", token);
                }
            } catch (Exception e) {
                logger.error("Error setting user authentication: {}", e.getMessage(), e);
            }
        }
        filterChain.doFilter(request, response);
    }
}
