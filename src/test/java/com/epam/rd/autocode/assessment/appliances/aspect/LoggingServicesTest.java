package com.epam.rd.autocode.assessment.appliances.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoggingServicesTest {

    private final LoggingServices aspect = new LoggingServices();

    private ProceedingJoinPoint joinPoint() {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn(String.class);
        when(signature.getName()).thenReturn("length");
        return joinPoint;
    }

    @Test
    void returnsResultOfProceededCall() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint();
        Object expected = new Object();
        when(joinPoint.proceed()).thenReturn(expected);
        assertSame(expected, aspect.log(joinPoint));
    }

    @Test
    void rethrowsOriginalException() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint();
        IllegalStateException failure = new IllegalStateException("boom");
        when(joinPoint.proceed()).thenThrow(failure);
        assertEquals(failure, assertThrows(IllegalStateException.class, () -> aspect.log(joinPoint)));
    }
}
