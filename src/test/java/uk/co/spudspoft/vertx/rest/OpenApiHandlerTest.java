/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudspoft.vertx.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jakarta.ws.rs.core.Application;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class OpenApiHandlerTest extends Application {
  
  private static final Logger logger = LoggerFactory.getLogger(OpenApiHandlerTest.class);
  
  @Test
  public void testHandler(Vertx vertx, VertxTestContext testContext) throws Exception {

    HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(0));

    Router router = Router.router(vertx);

    List<Object> controllers = Arrays.asList(new SampleHandler1(), new SampleHandler2());
    List<Object> providers = Arrays.asList(new JacksonJsonProvider(new ObjectMapper(), JacksonJsonProvider.BASIC_ANNOTATIONS));

    OpenAPIConfiguration openApiConfig = createOpenapiConfiguration(true, true, controllers);
    OpenApiHandler openApiHandler = new OpenApiHandler(this, openApiConfig, "/api/");
    openApiHandler.setOpenContextId("OpenApiHandlerTest.testHandler");
    
    router.route("/api/*").handler(new JaxRsHandler(vertx, null, "/api", controllers, providers));
    router.getWithRegex("/openapi\\..*").handler(openApiHandler);

    httpServer
            .requestHandler(router)
            .listen()
            .onFailure(as -> testContext.failNow(as))
            .onSuccess(hs -> {
              RestAssured.port = hs.actualPort();
              
              vertx.executeBlocking(promise -> {
                testContext.verify(() -> {

                  String body = when().get("/api/one").then().statusCode(200).extract().body().asString();
                  assertThat(body, equalTo("One"));
                  body = when().get("/api/two").then().statusCode(200).extract().body().asString();
                  assertThat(body, equalTo("Two"));

                  body = given().cookie("bob", "fred").get("/openapi.yaml").then().log().all().statusCode(200).extract().body().asString();
                  logger.debug("Open API YAML: {}", body);
                  assertThat(body, containsString("/api/one"));
                  assertThat(body, containsString("/api/two"));
                });
                testContext.completeNow();
              });
            });
    

  }
  
  @Test
  public void testNotPrettyYaml(Vertx vertx, VertxTestContext testContext) throws Exception {

    HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(0));

    Router router = Router.router(vertx);

    List<Object> controllers = Arrays.asList(new SampleHandler1(), new SampleHandler2());
    List<Object> providers = Arrays.asList(new JacksonJsonProvider(new ObjectMapper(), JacksonJsonProvider.BASIC_ANNOTATIONS));

    OpenAPIConfiguration openApiConfig = createOpenapiConfiguration(false, false, controllers);
    OpenApiHandler openApiHandler = new OpenApiHandler(this, openApiConfig, "/api/");
    openApiHandler.setOpenContextId("OpenApiHandlerTest.testNotPrettyYaml");
    
    router.route("/api/*").handler(new JaxRsHandler(vertx, null, "/api", controllers, providers));
    router.getWithRegex("/openapi\\..*").handler(openApiHandler);

    httpServer
            .requestHandler(router)
            .listen()
            .onFailure(as -> testContext.failNow(as))
            .onSuccess(hs -> {
              RestAssured.port = hs.actualPort();
              
              vertx.executeBlocking(promise -> {
                testContext.verify(() -> {

                  String body = when().get("/api/one").then().statusCode(200).extract().body().asString();
                  assertThat(body, equalTo("One"));
                  body = when().get("/api/two").then().statusCode(200).extract().body().asString();
                  assertThat(body, equalTo("Two"));

                  body = given().cookie("bob", "fred").get("/openapi.yaml").then().log().all().statusCode(200).extract().body().asString();
                  logger.debug("Open API YAML: {}", body);
                  assertThat(body, containsString("/api/one"));
                  assertThat(body, containsString("/api/two"));
                });
                testContext.completeNow();
              });
            });
    

  }
  
  @Test
  public void testDefaultContextId(Vertx vertx, VertxTestContext testContext) throws Exception {

    HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(0));

    Router router = Router.router(vertx);

    List<Object> controllers = Arrays.asList(new SampleHandler1(), new SampleHandler2());
    List<Object> providers = Arrays.asList(new JacksonJsonProvider(new ObjectMapper(), JacksonJsonProvider.BASIC_ANNOTATIONS));

    OpenAPIConfiguration openApiConfig = createOpenapiConfiguration(false, false, controllers);
    OpenApiHandler openApiHandler = new OpenApiHandler(this, openApiConfig, "");
    
    router.route("/api/*").handler(new JaxRsHandler(vertx, null, "/api", controllers, providers));
    router.getWithRegex("/openapi\\..*").handler(openApiHandler);

    httpServer
            .requestHandler(router)
            .listen()
            .onFailure(as -> testContext.failNow(as))
            .onSuccess(hs -> {
              RestAssured.port = hs.actualPort();
              
              vertx.executeBlocking(promise -> {
                testContext.verify(() -> {

                  String body = when().get("/api/one").then().statusCode(200).extract().body().asString();
                  assertThat(body, equalTo("One"));
                  body = when().get("/api/two").then().statusCode(200).extract().body().asString();
                  assertThat(body, equalTo("Two"));

                  body = when().get("/openapi.json").then().log().all().statusCode(200).extract().body().asString();
                  logger.debug("Open API JSON: {}", body);
                  assertThat(body, containsString("/one"));
                  assertThat(body, containsString("/two"));
                });
                testContext.completeNow();
              });
            });
    

  }
  
  @Test
  public void testNoResources(Vertx vertx, VertxTestContext testContext) throws Exception {

    HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(0));

    Router router = Router.router(vertx);

    List<Object> controllers = Arrays.asList();
    List<Object> providers = Arrays.asList(new JacksonJsonProvider(new ObjectMapper(), JacksonJsonProvider.BASIC_ANNOTATIONS));

    OpenAPIConfiguration openApiConfig = createOpenapiConfiguration(true, true, controllers);
    OpenApiHandler openApiHandler = new OpenApiHandler(this, openApiConfig, "/api");
    openApiHandler.setOpenContextId("OpenApiHandlerTest.testNoResources");
    
    router.route("/api/*").handler(new JaxRsHandler(vertx, null, "/api", controllers, providers));
    router.getWithRegex("/openapi\\..*").handler(openApiHandler);

    httpServer
            .requestHandler(router)
            .listen()
            .onFailure(as -> testContext.failNow(as))
            .onSuccess(hs -> {
              RestAssured.port = hs.actualPort();
              
              vertx.executeBlocking(promise -> {
                testContext.verify(() -> {

                  String body = when().get("/openapi.json").then().log().all().statusCode(200).extract().body().asString();
                  logger.debug("Open API JSON: {}", body);
                });
                testContext.completeNow();
              });
            });
    

  }

  @Test
  public void testHandlerWithoutConfig() throws Exception {

    assertThrows(NullPointerException.class, () -> new OpenApiHandler(this, null, "/api/"));
    
    assertThrows(NullPointerException.class, () -> new OpenApiHandler(this, createOpenapiConfiguration(true, true, Collections.emptyList()), null));
    
  }
  
  @Test
  public void testMultiMapToMap() {
    assertNotNull(OpenApiHandler.multiMapToMap(null));
    assertEquals(0, OpenApiHandler.multiMapToMap(null).size());
    assertEquals("carol", OpenApiHandler.multiMapToMap(new HeadersMultiMap().add("bob", "fred").add("bob", "carol")).get("bob").get(1));
  }

  private OpenAPIConfiguration createOpenapiConfiguration(boolean pretty, boolean filter, List<Object> resources) {
    return new SwaggerConfiguration()
            .resourceClasses(Stream.concat(resources.stream(), Stream.of(this)).map(r -> r.getClass().getCanonicalName())
                    .collect(Collectors.toSet()))
            .prettyPrint(pretty)
            .filterClass(filter ? SampleOpenApiFilter.class.getCanonicalName() : null)
            .openAPI(
                    new OpenAPI()
                            .info(
                                    new Info()
                                            .title("OpenApiHandlerTest")
                                            .version("0")
                            )
            );
  }
  
}
