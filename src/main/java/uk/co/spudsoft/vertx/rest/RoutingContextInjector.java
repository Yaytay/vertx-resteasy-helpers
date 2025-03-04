/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.vertx.rest;

import io.vertx.ext.web.RoutingContext;
import org.jboss.resteasy.spi.ContextInjector;

/**
 * ContextInjector to inject the vert.x web routing context into the Jax-RS context.
 * 
 * @author jtalbut
 */
public class RoutingContextInjector implements ContextInjector<RoutingContext, RoutingContext> {

  private final RoutingContext context;

  /**
   * Constructor.
   * @param routingContext The routing context that is to be injected into the Jax-RS context.
   */
  public RoutingContextInjector(RoutingContext routingContext) {
    this.context = routingContext;
  }

  @Override
  public RoutingContext resolve(Class<? extends RoutingContext> type, java.lang.reflect.Type type1, java.lang.annotation.Annotation[] antns) {
    return context;
  }
  
}

