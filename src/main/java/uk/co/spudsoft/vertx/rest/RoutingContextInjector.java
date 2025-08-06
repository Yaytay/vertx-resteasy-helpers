/*
 * Copyright (C) 2025 jtalbut
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
package uk.co.spudsoft.vertx.rest;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.spi.ContextInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ContextProvider to inject the Vert.x web routing context into JAX-RS resources.
 * 
 * This provider retrieves the RoutingContext that was previously stored in the 
 * ResteasyContext's context data map by the JaxRsHandler.
 * 
 * @author jtalbut
 */
@Provider
public class RoutingContextInjector implements ContextInjector<RoutingContext, RoutingContext> {

  private static final Logger logger = LoggerFactory.getLogger(RoutingContextInjector.class);
  private static final Map<HttpServerRequest, RoutingContext> KNOWN_CONTEXTS = new ConcurrentHashMap<>();
  
  public static void addContext(HttpServerRequest request, RoutingContext context) {
    KNOWN_CONTEXTS.put(request, context);
  }
  
  public static RoutingContext getContext(HttpServerRequest request) {
    return KNOWN_CONTEXTS.get(request);
  }
  
  public static void removeContext(HttpServerRequest request) {
    KNOWN_CONTEXTS.remove(request);
    logger.debug("There are {} entries in the known contexts map", KNOWN_CONTEXTS.size());
  }
  
  @Override
  public RoutingContext resolve(Class<? extends RoutingContext> rawType, Type genericType, Annotation[] annotations) {
    HttpServerRequest request = ResteasyContext.getContextData(HttpServerRequest.class);
    if (request == null) {
      return null;
    }
    return KNOWN_CONTEXTS.get(request);
  }
}
