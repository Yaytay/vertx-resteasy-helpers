/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.co.spudspoft.vertx.rest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import org.jboss.resteasy.core.interception.jaxrs.PostMatchContainerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author njt
 */
public class TimerContainerRequestFilter implements ContainerRequestFilter {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(TimerContainerRequestFilter.class);

  /**
   * The key within the ContainerRequestContext for the Micrometer Sample.
   */
  public static final String TIMER_KEY = TimerContainerRequestFilter.class.getName() + ".Sample";
  /**
   * The key within the ContainerRequestContext for the annotation on the request method.
   */
  public static final String TIMER_ANNOTATION = TimerContainerRequestFilter.class.getName() + ".Annotation";
  /**
   * The key within the ContainerRequestContext for the generated name of the timer.
   */
  public static final String TIMER_NAME = TimerContainerRequestFilter.class.getName() + ".Name";

  private final MeterRegistry meterRegistry;

  /**
   * Constructor.
   * @param meterRegistry The MeterRegistry to use for recording timings.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "MeterRegistry may be changed in handlers")
  public TimerContainerRequestFilter(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }
  
  private static boolean isNullOrEmpty(String value) {
    if ((value == null) || value.isEmpty()) {
      return true;
    }
    return false;
  }

  @Override
  public void filter(ContainerRequestContext crc) throws IOException {
    if (crc instanceof PostMatchContainerRequestContext) {
      try {
        PostMatchContainerRequestContext matchedContext = (PostMatchContainerRequestContext) crc;
        Method requestMethod = matchedContext.getResourceMethod().getMethod();
        Timed timed = requestMethod.getAnnotation(Timed.class);
        if (timed != null) {
          if (isNullOrEmpty(timed.value())) {
            requestMethodToMeterName(requestMethod, crc);
          }
        } else {
          timed = requestMethod.getDeclaringClass().getAnnotation(Timed.class);
          if (timed != null) {
            if (isNullOrEmpty(timed.value())) {
              requestMethodToMeterName(requestMethod, crc);
            } else {
              String name = timed.value() + "_" + requestMethod.getName();
              crc.setProperty(TIMER_NAME, name);
            }
          }
        }        
        
        if (timed != null) {          
          Timer.Sample sample = Timer.start(meterRegistry);
          crc.setProperty(TIMER_KEY, sample);
          crc.setProperty(TIMER_ANNOTATION, timed);
        }
      } catch (Throwable ex) {
        logger.error("Failed to initialise timer for request: ", ex);
      }
    }
  }

  private void requestMethodToMeterName(Method requestMethod, ContainerRequestContext crc) {
    String name = requestMethod.toString()
            .trim()
            .replaceAll("\\.", "_")
            .replaceAll(" ", "_")
            .replaceAll("\\(", "_")
            .replaceAll("\\)", "")
            .replaceAll(",", "_")
            .replaceAll("__", "_")
            ;
    crc.setProperty(TIMER_NAME, name);
  }

  /**
   * Get the current micrometer sample.
   * Designed for use within the request method - will only return a valid name after the filter has been called.
   * @param crc the request context.
   * @return the current micrometer sample.
   */
  public static Timer.Sample getSample(ContainerRequestContext crc) {
    return (Timer.Sample) crc.getProperty(TIMER_KEY);
  }

  /**
   * Get the annotation from the request method.
   * Designed for use within the request method - will only return a valid name after the filter has been called.
   * @param crc the request context.
   * @return the annotation from the request method.
   */
  public static Timed getAnnotation(ContainerRequestContext crc) {
    return (Timed) crc.getProperty(TIMER_ANNOTATION);
  }

  /**
   * Get the name of the timer, extracted from the request context and constructed from the method name.
   * Designed for use within the request method - will only return a valid name after the filter has been called.
   * @param crc the request context.
   * @return the name of the timer.
   */
  public static String getName(ContainerRequestContext crc) {
    return (String) crc.getProperty(TIMER_NAME);
  }
}
