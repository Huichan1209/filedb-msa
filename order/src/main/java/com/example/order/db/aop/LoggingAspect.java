package com.example.order.db.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect
{
    // Repository 패키지 내 모든 메서드 호출 시 적용. 중간에 도메인 이름은 비워서 라이브러리로 복붙 사용 가능하도록 함
    @Around("execution(* com.example..repository.*.*(..))")
    public Object logAroundRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable
    {
        String methodName = joinPoint.getSignature().getName();  // 메서드 이름
        Object[] methodArgs = joinPoint.getArgs();               // 매개변수

        // 메서드 호출 전 로그
        System.out.println("[호출 전] 메서드: " + methodName);
        System.out.println("[호출 전] 매개변수: " + Arrays.toString(methodArgs));

        // 실제 메서드 실행
        Object result = joinPoint.proceed();

        // 메서드 호출 후 리턴값 로그
        System.out.println("[호출 후] 메서드: " + methodName);
        System.out.println("[호출 후] 리턴값: " + result);

        return result;
    }

}