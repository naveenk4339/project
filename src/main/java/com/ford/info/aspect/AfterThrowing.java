package com.ford.info.aspect;

import java.util.logging.Logger;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(1)
public class AfterThrowing {
	public static Logger logger = java.util.logging.Logger.getLogger(LoggingAspect.class.getName());

	@org.aspectj.lang.annotation.AfterThrowing(pointcut = "execution(* com.ford.info.*.*.*(..))", throwing = "theExc")
	public void afterThrowingAllMethods(JoinPoint theJoinPoint, Throwable theExc) {

		String method = theJoinPoint.getSignature().toShortString();

		logger.info("\n excuting @Afterthrowing method: " + method);

		logger.info("\n excuting @Afterthrowing  the execption : " + theExc);

	}

}
