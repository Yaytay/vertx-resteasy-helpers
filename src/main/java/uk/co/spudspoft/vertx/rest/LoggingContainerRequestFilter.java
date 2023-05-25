/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.co.spudspoft.vertx.rest;

import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static uk.co.spudspoft.vertx.rest.LoggingContainerResponseFilter.*;

/**
 * JAX-RS filter to log requests.
 * 
 * If the logger is set to trace level all headers will be logged as a single multi-line log entry
 * , otherwise if the logger is set to debug level one log line will be output.
 * 
 * @author njt
 */
public class LoggingContainerRequestFilter implements ContainerRequestFilter {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(LoggingContainerRequestFilter.class);

  @Context
  private RoutingContext routingContext;
  
  /**
   * The key within the ContainerRequestContext for the generated name of the timer.
   */
  public static final String TIMESTAMP_NAME = LoggingContainerRequestFilter.class.getName() + ".Timestamp";
  
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    
    requestContext.setProperty(TIMESTAMP_NAME, System.currentTimeMillis());
    if (logger.isTraceEnabled()) {
      logger.trace(buildTrace(routingContext, requestContext));
    } else if (logger.isDebugEnabled()) {
      StringBuilder builder = new StringBuilder();
      appendClientIp(builder, routingContext, requestContext);
      builder.append(getUser(requestContext)).append(" ");
      
      builder.append(requestContext.getMethod()).append(" ").append(requestContext.getUriInfo().getRequestUri().toString()).append(' ');
      builder.append(requestContext.getHeaderString("Host")).append(" ");
      builder.append(requestContext.getHeaderString("Referer")).append(" ");
      builder.append('"').append(requestContext.getHeaderString("User-Agent")).append("\" ");
      
      logger.debug(builder.toString());
    }    
  }

  static String buildTrace(RoutingContext routingContext, ContainerRequestContext requestContext) {
    StringBuilder builder = new StringBuilder();
    if (routingContext != null) {
      builder.append(routingContext.request().remoteAddress()).append(":\n");
    }
    if (requestContext != null) {
      builder.append(requestContext.getMethod()).append(" ").append(requestContext.getUriInfo().getRequestUri()).append('\n');
      if (requestContext.getHeaders() != null) {
        requestContext.getHeaders().forEach((k, v) -> {
          v.forEach(v2 -> {
            builder.append(k).append(": ").append(v2).append('\n');
          });
        });
      }
    }
    return builder.toString();
  }
  
}
