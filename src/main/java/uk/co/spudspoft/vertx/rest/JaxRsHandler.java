/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudspoft.vertx.rest;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * Vert.x web handler that delegates requests to RESTeasy.
 * 
 *  The Vert.x RoutingContext will be available in the @Context of the RESTeasy handlers and helpers are added for metrics and logging.
 * 
 * @author jtalbut
 */
public class JaxRsHandler implements Handler<RoutingContext> {

  private final ResteasyDeployment deployment;
  private final VertxRequestHandler requestHandler;

  /**
   * Constructor.
   * 
   * @param vertx The Vert.x instance.
   * @param meterRegistry Micrometer meter registry used by the TimeContainer*Filters, if null the timer filters will not be used.
   * @param basePath The base path in the vertx handler context at which to insert the RESTeasy handler.
   * @param controllers The JAX-RS controllers to use.
   * @param providers Any additional JAX-RS providers to use.
   */
  public JaxRsHandler(Vertx vertx, MeterRegistry meterRegistry, String basePath, List<Object> controllers, List<Object> providers) {
    this.deployment = new VertxResteasyDeployment();
    deployment.setResources(controllers);
    deployment.setProviders(providers);
    deployment.start();
    requestHandler = new VertxRequestHandler(vertx, deployment, basePath);

    ResteasyProviderFactory providerFactory = ResteasyProviderFactory.getInstance();
    if (meterRegistry != null) {
      providerFactory.getContainerRequestFilterRegistry().registerSingleton(new TimerContainerRequestFilter(meterRegistry));
      providerFactory.getContainerResponseFilterRegistry().registerSingleton(new TimerContainerResponseFilter(meterRegistry));
    }
    providerFactory.getContainerRequestFilterRegistry().registerSingleton(new LoggingContainerRequestFilter());
    providerFactory.getContainerResponseFilterRegistry().registerSingleton(new LoggingContainerResponseFilter());
  }

  @Override
  public void handle(RoutingContext rc) {
    ResteasyProviderFactory
            .getInstance()
            .getContextInjectors()
            .put(RoutingContext.class, new RoutingContextInjector(rc));

    requestHandler.handle(rc.request());
  }

}
