/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.vertx.rest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.ext.web.RoutingContext;
import org.jboss.resteasy.spi.ContextInjector;

/**
 * ContextInjector to inject the vert.x web routing context into the Jax-RS context.
 * 
 * @author jtalbut
 */
public class RoutingContextInjector implements ContextInjector<RoutingContext, RoutingContext> {

  private final RoutingContext routingContext;

  /**
   * Constructor.
   * @param routingContext The routing context that is to be injected into the Jax-RS context.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "RoutingContext may be changed in handlers")
  public RoutingContextInjector(RoutingContext routingContext) {
    this.routingContext = routingContext;
  }

  @Override
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "RoutingContext may be changed in handlers")
  public RoutingContext resolve(Class<? extends RoutingContext> type, java.lang.reflect.Type type1, java.lang.annotation.Annotation[] antns) {
    return routingContext;
  }
  
}

