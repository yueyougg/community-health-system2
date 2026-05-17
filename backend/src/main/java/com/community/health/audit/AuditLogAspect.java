package com.community.health.audit;

import com.community.health.model.OperationLog;
import com.community.health.repository.OperationLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditLogAspect {

    private final OperationLogRepository operationLogRepository;

    public AuditLogAspect(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    @AfterReturning("@annotation(auditLog)")
    public void afterReturning(JoinPoint joinPoint, AuditLog auditLog) {
        OperationLog log = new OperationLog();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "anonymous";
        log.setUsername(username);
        log.setModuleName(auditLog.module());
        log.setActionName(auditLog.action());

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            log.setHttpMethod(request.getMethod());
            log.setRequestPath(request.getRequestURI());
            log.setIpAddress(request.getRemoteAddr());
        }
        operationLogRepository.save(log);
    }
}
