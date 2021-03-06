package com.ford.info.aspect;

import java.util.List;
import java.util.logging.Logger;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(3)
public class LoggingAspect {

	public static Logger logger = java.util.logging.Logger.getLogger(LoggingAspect.class.getName());

	@Pointcut("execution(* com.ford.info.controller.*.*(..))")
	private void forControllerPackage() {
	}

	@Pointcut("execution(* com.ford.info.service.*.*(..))")
	private void forServicePackage() {
	}

	@Pointcut("execution(* com.ford.info.dao.*.*(..))")
	private void forDaoPackage() {
	}

	@Pointcut("forControllerPackage() || forServicePackage() || forDaoPackage()")
	private void forAppFlow() {
	}

	@Before("forAppFlow()")
	public void before(JoinPoint theJoinPoint) {
		
		
		String theMethod = theJoinPoint.getSignature().toShortString();
		logger.info(" in @Before: calling method: " + theMethod);
		
	
		Object[] args = theJoinPoint.getArgs();
		
		// loop thru and display args
		for (Object tempArg : args) {
			logger.info("argument: " + tempArg);
		}
		
	}
	
	
	
	@After("execution(* com.ford.info.*.*.*(..)))")
	public void afterFinallyAllMethods(JoinPoint theJoinPoint) {

		String method = theJoinPoint.getSignature().toShortString();

		logger.info("\n excuting  @after finally method " + method);

	}

	
	

	@AfterReturning(pointcut = "execution(* com.ford.info.*.*.*(..))", returning = "result")
	public void afterReturningAllMethods(JoinPoint theJoinPoint, List<com.ford.info.request.Vehicles> result) {


		String method = theJoinPoint.getSignature().toShortString();
		logger.info("\n Executing @AfterReturning on method: " + method);

		
		logger.info("\n result is: " + result);

	}

}
