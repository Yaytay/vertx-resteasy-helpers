/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.vertx.rest;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;
import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.vertx.rest.serialisers.JsonArrayMessageBodyReader;
import uk.co.spudsoft.vertx.rest.serialisers.JsonArrayMessageBodyWriter;
import uk.co.spudsoft.vertx.rest.serialisers.JsonObjectMessageBodyReader;
import uk.co.spudsoft.vertx.rest.serialisers.JsonObjectMessageBodyWriter;

/**
 * Vert.x web handler that delegates requests to RESTeasy.
 *
 *  The Vert.x RoutingContext will be available in the @Context of the RESTeasy handlers and helpers are added for metrics and logging.
 *
 * @author jtalbut
 */
public class JaxRsHandler implements Handler<RoutingContext> {

  private static final Logger logger = LoggerFactory.getLogger(JaxRsHandler.class);

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
  public JaxRsHandler(Vertx vertx
          , MeterRegistry meterRegistry
          , String basePath
          , List<Object> controllers
          , List<Object> providers
  ) {
    this(vertx, meterRegistry, basePath, controllers, providers, true, true);
  }
  
  /**
   * Constructor.
   *
   * @param vertx The Vert.x instance.
   * @param meterRegistry Micrometer meter registry used by the TimeContainer*Filters, if null the timer filters will not be used.
   * @param basePath The base path in the vertx handler context at which to insert the RESTeasy handler.
   * @param controllers The JAX-RS controllers to use.
   * @param providers Any additional JAX-RS providers to use.
   * @param enableTiming Install a timing request and response filter ({@link TimerContainerRequestFilter}).
   * @param enableLogging Install a logging request and response filter ({@link LoggingContainerRequestFilter}).
   */
  public JaxRsHandler(Vertx vertx
          , MeterRegistry meterRegistry
          , String basePath
          , List<Object> controllers
          , List<Object> providers
          , boolean enableTiming
          , boolean enableLogging
  ) {
    this.deployment = new VertxResteasyDeployment();
    deployment.setResources(controllers);

    // Add the RoutingContextProvider to the providers list
    List<Object> allProviders = new ArrayList<>(providers);
    allProviders.add(new RoutingContextInjector());
    deployment.setProviders(allProviders);

    deployment.start();
    requestHandler = new VertxRequestHandler(vertx, deployment, basePath);

    // Register our custom context injector BEFORE starting deployment
    ResteasyProviderFactory providerFactory = ResteasyProviderFactory.getInstance();
   
    if (meterRegistry != null && enableTiming) {
      providerFactory.getContainerRequestFilterRegistry().registerSingleton(new TimerContainerRequestFilter(meterRegistry));
      providerFactory.getContainerResponseFilterRegistry().registerSingleton(new TimerContainerResponseFilter(meterRegistry));
    }
    if (enableLogging) {
      providerFactory.getContainerRequestFilterRegistry().registerSingleton(new LoggingContainerRequestFilter());
      providerFactory.getContainerResponseFilterRegistry().registerSingleton(new LoggingContainerResponseFilter());
    }

    providerFactory.registerProvider(JsonArrayMessageBodyReader.class);
    providerFactory.registerProvider(JsonArrayMessageBodyWriter.class);
    providerFactory.registerProvider(JsonObjectMessageBodyReader.class);
    providerFactory.registerProvider(JsonObjectMessageBodyWriter.class);
  }

  @Override
  public void handle(RoutingContext routingContext) {
    // Store in ResteasyContext data map
    RoutingContextInjector.addContext(routingContext.request(), routingContext);

    routingContext.addEndHandler(ar -> {
      RoutingContextInjector.removeContext(routingContext.request());
    });
    
    try {
      requestHandler.handle(routingContext.request());
    } catch (Throwable ex) {
      logger.warn("Failed to handle request to {}: ", routingContext.request().absoluteURI(), ex);
      throw ex;
    }
  }

}
