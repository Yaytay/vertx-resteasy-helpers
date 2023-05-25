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
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author njt
 */
public class LoggingContainerRequestFilterTest {
  
  /**
   * Test of buildTrace method, of class LoggingContainerRequestFilter.
   * @throws java.net.URISyntaxException
   */
  @Test
  public void testBuildTrace() throws URISyntaxException {
    UriInfo uriInfo = new ResteasyUriInfo(new URI("https://bob/fred"));
    ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(requestContext.getMethod()).thenReturn("OPTIONS");
    assertEquals("OPTIONS https://bob/fred\n", LoggingContainerRequestFilter.buildTrace(null, requestContext));
    Headers<String> headers = new Headers<>();
    headers.add("Host", "bob");
    headers.add("Set-Cookie", "first");
    headers.add("Set-Cookie", "second");
    when(requestContext.getHeaders()).thenReturn(headers);
    assertEquals("OPTIONS https://bob/fred\nHost: bob\nSet-Cookie: first\nSet-Cookie: second\n", LoggingContainerRequestFilter.buildTrace(null, requestContext));
    
    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest httpServerRequest = mock(HttpServerRequest.class);
    when(routingContext.request()).thenReturn(httpServerRequest);
    when(httpServerRequest.remoteAddress()).thenReturn(SocketAddress.inetSocketAddress(123, "1.1.1.1"));
    assertEquals("1.1.1.1:123:\nOPTIONS https://bob/fred\nHost: bob\nSet-Cookie: first\nSet-Cookie: second\n", LoggingContainerRequestFilter.buildTrace(routingContext, requestContext));
    assertEquals("1.1.1.1:123:\n", LoggingContainerRequestFilter.buildTrace(routingContext, null));
  }
  
}
