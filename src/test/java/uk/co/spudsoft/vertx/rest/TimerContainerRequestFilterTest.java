/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.vertx.rest;

import uk.co.spudsoft.vertx.rest.JaxRsHandler;
import uk.co.spudsoft.vertx.rest.TimerContainerResponseFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import io.micrometer.core.annotation.Timed;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.when;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.micrometer.impl.PrometheusScrapingHandlerImpl;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import java.util.Arrays;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class TimerContainerRequestFilterTest {
  
  private static final Logger logger = LoggerFactory.getLogger(TimerContainerRequestFilterTest.class);

  @Path("/one")
  @Timed("resource1")
  public static class Resource1 {

    @GET
    public void get(@Suspended final AsyncResponse response) {
      response.resume("One");
    }

  }

  @Path("/two")
  @Timed(extraTags = {"bob", "bobValue", "carol", "carolValue"}, description = "Timings for Resource2", percentiles = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0})
  public static class Resource2 {

    @GET
    public void get(@Suspended final AsyncResponse response) {
      response.resume("Two");
    }

  }

  @Path("/three")
  public static class Resource3 {

    @GET
    @Timed(value = "resource3", extraTags = {"bob", "value"})
    public void get(@Suspended final AsyncResponse response) {
      response.resume("Three");
    }

  }

  @Path("/four")
  public static class Resource4 {

    @GET
    @Timed(extraTags = {"query", "$query.bob", "method", "$method", "host", "$header.host", "path-part", "$path.part", "henry", "$henry"})
    public void get(@Suspended final AsyncResponse response) {
      response.resume("Four");
    }

  }

  @Path("/five")
  public static class Resource5 {

    @GET
    @Timed()
    public void get(@Suspended final AsyncResponse response) {
      response.resume("Five");
    }

  }

  @Test
  public void testFilter(VertxTestContext testContext) throws Exception {

    Vertx vertx = Vertx.vertx(
            new VertxOptions().setMetricsOptions(
                    new MicrometerMetricsOptions()
                            .setPrometheusOptions(
                                    new VertxPrometheusOptions()
                                            .setEnabled(true)
                                            .setStartEmbeddedServer(false)
                            )
                            .setEnabled(true)
            )
    );
    
    PrometheusMeterRegistry meterRegistry = (PrometheusMeterRegistry) BackendRegistries.getDefaultNow();
    
    HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(0));

    Router router = Router.router(vertx);

    List<Object> controllers = Arrays.asList(new Resource1(), new Resource2(), new Resource3(), new Resource4(), new Resource5());
    List<Object> providers = Arrays.asList(new JacksonJsonProvider(new ObjectMapper(), JacksonJsonProvider.BASIC_ANNOTATIONS));

    router.route("/api/*").handler(new JaxRsHandler(vertx, meterRegistry, "/api", controllers, providers));
    router.route("/manage/prometheus").handler(new PrometheusScrapingHandlerImpl());

    httpServer
            .requestHandler(router)
            .listen()
            .onFailure(as -> testContext.failNow(as))
            .onSuccess(hs -> {
              RestAssured.port = hs.actualPort();

              vertx.<Void>executeBlocking(() -> {

                testContext.verify(() -> {

                  String body = when().get("/api/one").then().statusCode(200).extract().body().asString();
                  assertThat(body, equalTo("One"));
                  body = when().get("/api/two").then().statusCode(200).extract().body().asString();
                  assertThat(body, equalTo("Two"));
                  body = when().get("/api/three").then().statusCode(200).extract().body().asString();
                  assertThat(body, equalTo("Three"));
                  body = when().get("/api/four").then().statusCode(200).extract().body().asString();
                  assertThat(body, equalTo("Four"));

                  body = when().get("/manage/prometheus").then().statusCode(200).extract().body().asString();
                  logger.debug("Metrics: {}", body);
                  assertThat(body, startsWith("# HELP"));                  
                  assertThat(body, containsString("resource1_get_seconds_count"));
                  assertThat(body, containsString("public_void_uk_co_spudspoft_vertx_rest_TimerContainerRequestFilterTest_Resource2_get_jakarta_ws_rs_container_AsyncResponse_seconds"));
                  assertThat(body, containsString("resource3_seconds_count"));                  
                  assertThat(body, containsString("public_void_uk_co_spudspoft_vertx_rest_TimerContainerRequestFilterTest_Resource4_get_jakarta_ws_rs_container_AsyncResponse_seconds_max"));
                  assertThat(body, not(containsString("esource5")));
                  assertThat(body, containsString("host=\"localhost:" + hs.actualPort() + "\",method=\"GET\",path_part=\"\",query=\"\",response_code=\"200\""));
                });
                testContext.completeNow();
                return null;
              });
            });

  }

  @Test
  public void testValuesListToTag() {
    assertEquals("", TimerContainerResponseFilter.valuesListToTag(null));
    assertEquals("", TimerContainerResponseFilter.valuesListToTag(Arrays.asList()));
    assertEquals("bob", TimerContainerResponseFilter.valuesListToTag(Arrays.asList("bob")));
    assertEquals("[bob, carol]", TimerContainerResponseFilter.valuesListToTag(Arrays.asList("bob", "carol")));
  }
  
}
