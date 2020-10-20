package com.ford.info.aspect;

import java.util.logging.Logger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(1)
public class ExecptionHandlingAspect {


	public static Logger logger = java.util.logging.Logger.getLogger(LoggingAspect.class.getName());

	@Around("execution(* com.ford.info.controller.*.*(..))")
	public Object aroundForControllerPackage(ProceedingJoinPoint theProceedingJoinPoint) throws Throwable {

		String method = theProceedingJoinPoint.getSignature().toShortString();

		long begin = System.currentTimeMillis();

		Object result = null;

		try {

			result = theProceedingJoinPoint.proceed();

		} catch (Exception e) {
			logger.info(e.getMessage());

			throw e;
		}

		long end = System.currentTimeMillis();

		long duration = end - begin;

		logger.info("Method Name "+method+" \n duration " + duration / 1000.0 + " seconds");

		return result;
	}

	@Around("execution(* com.ford.info.service.*.*(..))")
	public Object aroundForServicePackage(ProceedingJoinPoint theProceedingJoinPoint) throws Throwable {

		String method = theProceedingJoinPoint.getSignature().toShortString();

		long begin = System.currentTimeMillis();

		Object result = null;

		try {

			result = theProceedingJoinPoint.proceed();

		} catch (Exception e) {
			logger.info(e.getMessage());

			throw e;
		}

		long end = System.currentTimeMillis();

		long duration = end - begin;

		logger.info("\\n method name: "+method+" duration: " + duration / 1000.0 + " seconds");

		return result;
	}


}
