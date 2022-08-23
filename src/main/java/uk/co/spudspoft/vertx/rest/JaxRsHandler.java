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
 *
 * @author jtalbut
 */
public class JaxRsHandler implements Handler<RoutingContext> {

  private final ResteasyDeployment deployment;
  private final VertxRequestHandler requestHandler;

  public JaxRsHandler(Vertx vertx, MeterRegistry meterRegistry, String basePath, List<Object> controllers, List<Object> providers) {
    this.deployment = new VertxResteasyDeployment();
    deployment.setResources(controllers);
    deployment.setProviders(providers);
    deployment.start();
    requestHandler = new VertxRequestHandler(vertx, deployment, basePath);

    if (meterRegistry != null) {
      ResteasyProviderFactory providerFactory = ResteasyProviderFactory.getInstance();
      providerFactory.getContainerRequestFilterRegistry().registerSingleton(new TimerContainerRequestFilter(meterRegistry));
      providerFactory.getContainerResponseFilterRegistry().registerSingleton(new TimerContainerResponseFilter(meterRegistry));
    }
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
