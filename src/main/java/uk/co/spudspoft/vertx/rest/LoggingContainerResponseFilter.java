/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.co.spudspoft.vertx.rest;

import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static uk.co.spudspoft.vertx.rest.LoggingContainerRequestFilter.TIMESTAMP_NAME;

/**
 * JAX-RS filter to log responses.
 * 
 * If the logger is set to trace level all headers will be logged as a single multi-line log entry
 * , otherwise if the logger is set to info level one log line will be output (in an approximation of the Apache http log format).
 * 
 * @author njt
 */
public class LoggingContainerResponseFilter implements ContainerResponseFilter {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(LoggingContainerResponseFilter.class);
  
  @Context
  private RoutingContext routingContext;

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

    if (logger.isTraceEnabled()) {
      logger.trace(buildTrace(routingContext, responseContext));
    } else if (logger.isInfoEnabled()) {
      StringBuilder builder = new StringBuilder();
      appendClientIp(builder, routingContext, requestContext);
      builder.append(getUser(requestContext)).append(" ");
      builder.append(getDuration(requestContext, System.currentTimeMillis())).append(" ");
      builder.append(responseContext.getStatus()).append(" ");
      builder.append(responseContext.getLength()).append(" ");
      builder.append(requestContext.getMethod()).append(" ").append(requestContext.getUriInfo().getRequestUri().toString()).append(' ');
      builder.append(requestContext.getHeaderString("Host")).append(" ");
      builder.append(requestContext.getHeaderString("Referer")).append(" ");
      builder.append(requestContext.getHeaderString("User-Agent")).append(" ");

      logger.info(builder.toString());
    }
  }

  static String buildTrace(RoutingContext routingContext, ContainerResponseContext responseContext) {
    StringBuilder builder = new StringBuilder();
    if (routingContext != null) {
      builder.append(routingContext.request().remoteAddress()).append(":\n");
    }
    builder.append(responseContext.getStatusInfo().getStatusCode()).append(" ").append(responseContext.getStatusInfo().getReasonPhrase()).append('\n');
    if (responseContext.getHeaders() != null) {
      responseContext.getHeaders().forEach((k, v) -> {
        v.forEach(v2 -> {
          builder.append(k).append(": ").append(v2).append('\n');
        });
      });
    }
    return builder.toString();
  }

  static String getUser(ContainerRequestContext requestContext) {
    SecurityContext securityContext = requestContext.getSecurityContext();
    if (securityContext != null) {
      Principal principal = securityContext.getUserPrincipal();
      if (principal != null) {
        String name = principal.getName();
        if (name != null) {
          return name;
        }
      }
    }
    return "-";
  }

  static String getDuration(ContainerRequestContext requestContext, long now) {
    Object timestampObject = requestContext.getProperty(TIMESTAMP_NAME);
    if (timestampObject instanceof Long timestamp) {
      return Long.toString(now - timestamp);
    }
    return "-";
  }

  static void appendClientIp(StringBuilder builder, RoutingContext routingContext, ContainerRequestContext requestContext) {
    if (routingContext != null) {
      builder.append(routingContext.request().remoteAddress());
    }
    String clusterClientIp = requestContext.getHeaderString("X-Cluster-Client-IP");
    String forwardedFor = requestContext.getHeaderString("X-Forwarded-For");
    if (clusterClientIp != null || forwardedFor != null) {
      builder.append('(');
      if (clusterClientIp != null) {
        builder.append(clusterClientIp);
      }
      if (forwardedFor != null) {
        if (clusterClientIp != null) {
          builder.append('|');
        }
        builder.append(forwardedFor);
      }
      builder.append(')');
    }
    builder.append(' ');

  }

}
