/*
 * Copyright (C) 2023 njt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.co.spudspoft.vertx.rest;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.Response.StatusType;
import jakarta.ws.rs.core.SecurityContext;
import java.net.URISyntaxException;
import java.security.Principal;
import org.jboss.resteasy.core.Headers;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 *
 * @author njt
 */
public class LoggingContainerResponseFilterTest {
  
  /**
   * Test of buildTrace method, of class LoggingContainerResponseFilter.
   * @throws java.net.URISyntaxException
   */
  @Test
  public void testBuildTrace() throws URISyntaxException {
    ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
    StatusType status = mock(StatusType.class);
    when(status.getStatusCode()).thenReturn(418);
    when(status.getReasonPhrase()).thenReturn("I'm a teapot");
    when(responseContext.getStatusInfo()).thenReturn(status);
    assertEquals("418 I'm a teapot\n", LoggingContainerResponseFilter.buildTrace(null, responseContext));
    Headers<Object> headers = new Headers<>();
    headers.add("Host", "bob");
    headers.add("Set-Cookie", "first");
    headers.add("Set-Cookie", "second");
    when(responseContext.getHeaders()).thenReturn(headers);
    assertEquals("418 I'm a teapot\nHost: bob\nSet-Cookie: first\nSet-Cookie: second\n", LoggingContainerResponseFilter.buildTrace(null, responseContext));
    
    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest httpServerRequest = mock(HttpServerRequest.class);
    when(routingContext.request()).thenReturn(httpServerRequest);
    when(httpServerRequest.remoteAddress()).thenReturn(SocketAddress.inetSocketAddress(123, "1.1.1.1"));
    assertEquals("1.1.1.1:123:\n418 I'm a teapot\nHost: bob\nSet-Cookie: first\nSet-Cookie: second\n", LoggingContainerResponseFilter.buildTrace(routingContext, responseContext));
    assertEquals("1.1.1.1:123:\n", LoggingContainerRequestFilter.buildTrace(routingContext, null));
  }
  
  /**
   * Test of getUser method, of class LoggingContainerResponseFilter.
   */
  @Test
  public void testGetUser() {
    ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    assertEquals("-", LoggingContainerResponseFilter.getUser(requestContext));
    SecurityContext securityContext = mock(SecurityContext.class);
    when(requestContext.getSecurityContext()).thenReturn(securityContext);
    assertEquals("-", LoggingContainerResponseFilter.getUser(requestContext));
    Principal principal = mock(Principal.class);
    when(securityContext.getUserPrincipal()).thenReturn(principal);
    assertEquals("-", LoggingContainerResponseFilter.getUser(requestContext));
    when(principal.getName()).thenReturn("bob");
    assertEquals("bob", LoggingContainerResponseFilter.getUser(requestContext));
  }

  /**
   * Test of getDuration method, of class LoggingContainerResponseFilter.
   */
  @Test
  public void testGetDuration() {
    ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    assertEquals("-", LoggingContainerResponseFilter.getDuration(requestContext, 100));
    when(requestContext.getProperty(LoggingContainerRequestFilter.TIMESTAMP_NAME)).thenReturn("Nope");
    assertEquals("-", LoggingContainerResponseFilter.getDuration(requestContext, 100));
    when(requestContext.getProperty(LoggingContainerRequestFilter.TIMESTAMP_NAME)).thenReturn(47L);
    assertEquals("53", LoggingContainerResponseFilter.getDuration(requestContext, 100));
  }

  /**
   * Test of appendClientIp method, of class LoggingContainerResponseFilter.
   */
  @Test
  public void testAppendClientIp() {
    ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    StringBuilder builder = new StringBuilder();
    LoggingContainerResponseFilter.appendClientIp(builder, null, requestContext);
    assertEquals(" ", builder.toString());
    
    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest httpServerRequest = mock(HttpServerRequest.class);
    when(routingContext.request()).thenReturn(httpServerRequest);
    when(httpServerRequest.remoteAddress()).thenReturn(SocketAddress.inetSocketAddress(123, "1.1.1.1"));

    builder = new StringBuilder();
    LoggingContainerResponseFilter.appendClientIp(builder, routingContext, requestContext);
    assertEquals("1.1.1.1:123 ", builder.toString());
    
    when(requestContext.getHeaderString("X-Cluster-Client-IP")).thenReturn("2.2.2.2");

    builder = new StringBuilder();
    LoggingContainerResponseFilter.appendClientIp(builder, routingContext, requestContext);
    assertEquals("1.1.1.1:123(2.2.2.2) ", builder.toString());
    
    when(requestContext.getHeaderString("X-Forwarded-For")).thenReturn("3.3.3.3, 4.4.4.4,5.5.5.5");

    builder = new StringBuilder();
    LoggingContainerResponseFilter.appendClientIp(builder, routingContext, requestContext);
    assertEquals("1.1.1.1:123(2.2.2.2|3.3.3.3, 4.4.4.4,5.5.5.5) ", builder.toString());
    
    when(requestContext.getHeaderString("X-Cluster-Client-IP")).thenReturn(null);

    builder = new StringBuilder();
    LoggingContainerResponseFilter.appendClientIp(builder, routingContext, requestContext);
    assertEquals("1.1.1.1:123(3.3.3.3, 4.4.4.4,5.5.5.5) ", builder.toString());
  }
  
}
