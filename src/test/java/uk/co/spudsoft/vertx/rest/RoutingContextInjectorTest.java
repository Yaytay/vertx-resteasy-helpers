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

import static io.restassured.RestAssured.when;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(VertxExtension.class)
public class RoutingContextInjectorTest {

  /**
   * Test JAX-RS resource that uses @Context RoutingContext parameter.
   */
  @Path("/test")
  public static class TestResource {
    
    private static final Logger logger = LoggerFactory.getLogger(TestResource.class);
    
    private final Vertx vertx;

    public TestResource(Vertx vertx) {
      this.vertx = vertx;
    }
    
    @GET
    @Path("/routing-context")
    @Produces(MediaType.TEXT_PLAIN)
    public String getWithRoutingContext(@Context RoutingContext routingContext) {
      if (routingContext == null) {
        return "NULL";
      }

      // Verify we can access request data through the injected RoutingContext
      String userAgent = routingContext.request().getHeader("User-Agent");
      String path = routingContext.request().path();

      return "SUCCESS:" + path + ":" + (userAgent != null ? "HAS_USER_AGENT" : "NO_USER_AGENT");
    }


    @GET
    @Path("/suspended")
    @Produces(MediaType.TEXT_PLAIN)
    public void getSuspended(@Context HttpServerRequest httpServerRequest, @Context RoutingContext routingContext, @Suspended AsyncResponse asyncResponse) {
      if (routingContext == null) {
        asyncResponse.resume("null");
      }

      // Verify we can access request data through the injected RoutingContext
      try {
        String userAgent = routingContext.request().getHeader("User-Agent");
        String path = routingContext.request().path();

        Thread alien = new Thread(() -> {
          asyncResponse.resume("SUCCESS ASYNC:" + path + ":" + (userAgent != null ? "HAS_USER_AGENT" : "NO_USER_AGENT"));
        }, "Alien Thread");
        alien.start();
      } catch (Throwable ex) {
        asyncResponse.resume("Failed: " + ex.toString());
      }
    }

    @GET
    @Path("/simple")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSimple() {
      return "SIMPLE";
    }

    @GET
    @Path("/context-info")
    @Produces(MediaType.TEXT_PLAIN)
    public String getContextInfo(@Context HttpServerRequest httpServerRequest) {
      RoutingContext routingContext = RoutingContextInjector.getContext(httpServerRequest);
      if (routingContext == null) {
        return "NO_CONTEXT";
      }

      // Get some basic info to verify the context is working
      String method = routingContext.request().method().name();
      String uri = routingContext.request().absoluteURI();

      return "METHOD:" + method + ",URI_CONTAINS_TEST:" + uri.contains("/test/context-info");
    }
  }

  @Test
  public void testRoutingContextInjection(Vertx vertx, VertxTestContext testContext) throws Exception {
    HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(0));

    Router router = Router.router(vertx);

    List<Object> controllers = Arrays.asList(new TestResource(vertx));
    List<Object> providers = Arrays.asList(new JacksonJsonProvider(JsonMapper.builder().build(), JacksonJsonProvider.BASIC_ANNOTATIONS));

    // Set up the JAX-RS handler which should inject RoutingContext
    router.route("/api/*").handler(new JaxRsHandler(vertx, null, "/api", controllers, providers));

    httpServer
            .requestHandler(router)
            .listen()
            .onFailure(testContext::failNow)
            .onSuccess(hs -> {
              RestAssured.port = hs.actualPort();

              vertx.executeBlocking(() -> {
                testContext.verify(() -> {
                  // Test that RoutingContext is properly injected
                  String response = given()
                          .header("User-Agent", "TestAgent/1.0")
                          .get("/api/test/routing-context")
                          .then()
                          .statusCode(200)
                          .extract().body().asString();

                  assertThat(response, equalTo("SUCCESS:/api/test/routing-context:HAS_USER_AGENT"));

                  // Test that RoutingContext is properly injected with an asynchronous method
                  String suspendedResponse = given()
                          .header("User-Agent", "TestAgent/1.0")
                          .get("/api/test/suspended")
                          .then()
                          .statusCode(200)
                          .extract().body().asString();

                  assertThat(suspendedResponse, equalTo("SUCCESS ASYNC:/api/test/suspended:HAS_USER_AGENT"));

                  // Test another endpoint that uses RoutingContext
                  String contextResponse = when()
                          .get("/api/test/context-info")
                          .then()
                          .statusCode(200)
                          .extract().body().asString();

                  assertThat(contextResponse, equalTo("METHOD:GET,URI_CONTAINS_TEST:true"));

                  // Test that regular endpoints still work (no @Context parameter)
                  String simpleResponse = when()
                          .get("/api/test/simple")
                          .then()
                          .statusCode(200)
                          .extract().body().asString();

                  assertThat(simpleResponse, equalTo("SIMPLE"));
                });
                testContext.completeNow();
                return null;
              });
            });
  }

}
