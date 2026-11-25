package com.epam.rd.autocode.assessment.appliances.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j(topic = "SERVICE")
@Aspect
@Component
public class LoggingServices {

    @Around("@within(com.epam.rd.autocode.assessment.appliances.aspect.Loggable) || @annotation(com.epam.rd.autocode.assessment.appliances.aspect.Loggable)")
    public Object log(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + joinPoint.getSignature().getName();
        long started = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            log.debug("{} ok in {} ms", method, elapsedMillis(started));
            return result;
        } catch (Throwable ex) {
            log.warn("{} failed in {} ms: {}", method, elapsedMillis(started), ex.getClass().getSimpleName());
            throw ex;
        }
    }

    private long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}
