/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.vertx.rest;

import uk.co.spudsoft.vertx.rest.JaxRsHandler;
import uk.co.spudsoft.vertx.rest.OpenApiHandler;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.core.net.impl.HostAndPortImpl;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jakarta.ws.rs.core.Application;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import static java.util.regex.Pattern.DOTALL;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesRegex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.vertx.rest.OpenApiHandler.UiHandler;

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
    List<Object> providers = Arrays.asList(new JacksonJsonProvider(DatabindCodec.mapper(), JacksonJsonProvider.BASIC_ANNOTATIONS));

    String test = DatabindCodec.mapper().writeValueAsString(new ResponseType("field1", "field2"));
    assertEquals("{\"field1\":\"field1\",\"field2\":\"field2\"}", test);
    
    OpenAPIConfiguration openApiConfig = createOpenapiConfiguration(true, true, controllers, false);
    OpenApiHandler openApiHandler = new OpenApiHandler(this, openApiConfig, "/api/", null);
    openApiHandler.setOpenContextId("OpenApiHandlerTest.testHandler");
    
    router.route("/api/*").handler(new JaxRsHandler(vertx, null, "/api", controllers, providers));
    router.getWithRegex("/openapi\\..*").handler(openApiHandler);
    router.get("/openapi").handler(openApiHandler.getUiHandler());

    httpServer
            .requestHandler(router)
            .listen()
            .onFailure(as -> testContext.failNow(as))
            .onSuccess(hs -> {
              RestAssured.port = hs.actualPort();
              
              vertx.executeBlocking(() -> {
                testContext.verify(() -> {

                  String body = when().get("/api/one").then().statusCode(200).extract().body().asString();
                  assertThat(body, equalTo("One"));

                  body = given().cookie("bob", "fred").get("/openapi.yaml").then().log().all().statusCode(200).extract().body().asString();
                  logger.debug("Open API YAML: {}", body);
                  assertThat(body, containsString("/api/one"));
                  assertThat(body, containsString("/api/two"));
                  assertThat(body, containsString("3.0.1"));
                  Pattern pattern = Pattern.compile(".*field1:[\\s\\n]+type.*", DOTALL);
                  assertThat(body, matchesRegex(pattern));

                  body = given().get("/openapi").then().log().all().statusCode(200).extract().body().asString();
                  logger.debug("Open API UI: {}", body);
                  
                });
                testContext.completeNow();
                return null;
              });
            });

  }
  
  @Test
  public void testHandlerWith310(Vertx vertx, VertxTestContext testContext) throws Exception {

    HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(0));

    Router router = Router.router(vertx);

    List<Object> controllers = Arrays.asList(new SampleHandler1(), new SampleHandler2());
    List<Object> providers = Arrays.asList(new JacksonJsonProvider(DatabindCodec.mapper(), JacksonJsonProvider.BASIC_ANNOTATIONS));

    OpenAPIConfiguration openApiConfig = createOpenapiConfiguration(true, true, controllers, true);
    OpenApiHandler openApiHandler = new OpenApiHandler(this, openApiConfig, "/api/", null);
    openApiHandler.setOpenContextId("OpenApiHandlerTest.testHandlerWith310");
    
    router.route("/api/*").handler(new JaxRsHandler(vertx, null, "/api", controllers, providers));
    router.getWithRegex("/openapi\\..*").handler(openApiHandler);
    router.getWithRegex("/openapi/schema/description/.*").handler(openApiHandler);
    router.get("/openapi").handler(openApiHandler.getUiHandler());

    httpServer
            .requestHandler(router)
            .listen()
            .onFailure(as -> testContext.failNow(as))
            .onSuccess(hs -> {
              RestAssured.port = hs.actualPort();
              
              vertx.executeBlocking(() -> {
                testContext.verify(() -> {

                  String body = when().get("/api/one").then().statusCode(200).extract().body().asString();
                  assertThat(body, equalTo("One"));

                  body = given().cookie("bob", "fred").get("/openapi.yaml").then().log().all().statusCode(200).extract().body().asString();
                  logger.debug("Open API YAML: {}", body);
                  assertThat(body, containsString("/api/one"));
                  assertThat(body, containsString("/api/two"));
                  assertThat(body, containsString("3.1.0"));
                  Pattern pattern = Pattern.compile(".*field1:[\\s\\n]+type.*", DOTALL);
                  assertThat(body, matchesRegex(pattern));

                  given().get("/openapi/schema/description/Condition").then().log().all().statusCode(404).body(equalTo("Not Found"));
                  given().get("/openapi/schema/description/ResponseType").then().log().all().statusCode(200).body(equalTo("<html><body>This is the response type.\n</body></html>"));
                  
                  body = given().get("/openapi").then().log().all().statusCode(200).extract().body().asString();
                  logger.debug("Open API UI: {}", body);
                  
                });
                testContext.completeNow();
                return null;
              });
            });
    

  }
  
  @Test
  public void testNotPrettyYaml(Vertx vertx, VertxTestContext testContext) throws Exception {

    HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(0));

    Router router = Router.router(vertx);

    List<Object> controllers = Arrays.asList(new SampleHandler1(), new SampleHandler2());
    List<Object> providers = Arrays.asList(new JacksonJsonProvider(DatabindCodec.mapper(), JacksonJsonProvider.BASIC_ANNOTATIONS));

    OpenAPIConfiguration openApiConfig = createOpenapiConfiguration(false, false, controllers, false);
    OpenApiHandler openApiHandler = new OpenApiHandler(this, openApiConfig, "/api/", null);
    openApiHandler.setOpenContextId("OpenApiHandlerTest.testNotPrettyYaml");
    
    router.route("/api/*").handler(new JaxRsHandler(vertx, null, "/api", controllers, providers));
    router.getWithRegex("/openapi\\..*").handler(openApiHandler);

    httpServer
            .requestHandler(router)
            .listen()
            .onFailure(as -> testContext.failNow(as))
            .onSuccess(hs -> {
              RestAssured.port = hs.actualPort();
              
              vertx.executeBlocking(() -> {
                testContext.verify(() -> {

                  String body = when().get("/api/one").then().statusCode(200).extract().body().asString();
                  assertThat(body, equalTo("One"));

                  body = given().cookie("bob", "fred").get("/openapi.yaml").then().log().all().statusCode(200).extract().body().asString();
                  logger.debug("Open API YAML: {}", body);
                  assertThat(body, containsString("/api/one"));
                  assertThat(body, containsString("/api/two"));
                });
                testContext.completeNow();
                return null;
              });
            });
    

  }
  
  @Test
  public void testDefaultContextId(Vertx vertx, VertxTestContext testContext) throws Exception {

    HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(0));

    Router router = Router.router(vertx);

    List<Object> controllers = Arrays.asList(new SampleHandler1(), new SampleHandler2());
    List<Object> providers = Arrays.asList(new JacksonJsonProvider(DatabindCodec.mapper(), JacksonJsonProvider.BASIC_ANNOTATIONS));

    OpenAPIConfiguration openApiConfig = createOpenapiConfiguration(false, false, controllers, false);
    OpenApiHandler openApiHandler = new OpenApiHandler(this, openApiConfig, "", null);
    
    router.route("/api/*").handler(new JaxRsHandler(vertx, null, "/api", controllers, providers));
    router.getWithRegex("/openapi\\..*").handler(openApiHandler);

    httpServer
            .requestHandler(router)
            .listen()
            .onFailure(as -> testContext.failNow(as))
            .onSuccess(hs -> {
              RestAssured.port = hs.actualPort();
              
              vertx.executeBlocking(() -> {
                testContext.verify(() -> {

                  String body = when().get("/api/one").then().statusCode(200).extract().body().asString();
                  assertThat(body, equalTo("One"));

                  body = when().get("/openapi.json").then().log().all().statusCode(200).extract().body().asString();
                  logger.debug("Open API JSON: {}", body);
                  assertThat(body, containsString("/one"));
                  assertThat(body, containsString("/two"));
                });
                testContext.completeNow();
                return null;
              });
            });
    

  }
  
  @Test
  public void testNoResources(Vertx vertx, VertxTestContext testContext) throws Exception {

    HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(0));

    Router router = Router.router(vertx);

    List<Object> controllers = Arrays.asList();
    List<Object> providers = Arrays.asList(new JacksonJsonProvider(DatabindCodec.mapper(), JacksonJsonProvider.BASIC_ANNOTATIONS));

    OpenAPIConfiguration openApiConfig = createOpenapiConfiguration(true, true, controllers, false);
    OpenApiHandler openApiHandler = new OpenApiHandler(this, openApiConfig, "/api", null);
    openApiHandler.setOpenContextId("OpenApiHandlerTest.testNoResources");
    
    router.route("/api/*").handler(new JaxRsHandler(vertx, null, "/api", controllers, providers));
    router.getWithRegex("/openapi\\..*").handler(openApiHandler);

    httpServer
            .requestHandler(router)
            .listen()
            .onFailure(as -> testContext.failNow(as))
            .onSuccess(hs -> {
              RestAssured.port = hs.actualPort();
              
              vertx.executeBlocking(() -> {
                testContext.verify(() -> {

                  String body = when().get("/openapi.json").then().log().all().statusCode(200).extract().body().asString();
                  logger.debug("Open API JSON: {}", body);
                });
                testContext.completeNow();
                return null;
              });
            });
    

  }

  @Test
  public void testHandlerWithoutConfig() throws Exception {

    assertThrows(NullPointerException.class, () -> new OpenApiHandler(this, null, "/api/", null));
    
    assertThrows(NullPointerException.class, () -> new OpenApiHandler(this, createOpenapiConfiguration(true, true, Collections.emptyList(), false), null, null));
    
  }
  
  @Test
  public void testUiPath() throws Exception {    
    RoutingContext event = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    Mockito.when(request.authority()).thenReturn(new HostAndPortImpl("bob", 80));
    Mockito.when(event.request()).thenReturn(request);
    MultiMap headers = new HeadersMultiMap();
    headers.set("x-forwarded-proto", "https");
    Mockito.when(request.headers()).thenReturn(headers);
    
    assertEquals("https://bob/openapi.yaml", UiHandler.buildPath(event));    
    
    event = mock(RoutingContext.class);
    request = mock(HttpServerRequest.class);
    Mockito.when(request.authority()).thenReturn(new HostAndPortImpl("bob", 1234));
    Mockito.when(event.request()).thenReturn(request);
    Mockito.when(request.isSSL()).thenReturn(false);
    
    assertEquals("http://bob:1234/openapi.yaml", UiHandler.buildPath(event));    
    
    event = mock(RoutingContext.class);
    request = mock(HttpServerRequest.class);
    Mockito.when(request.authority()).thenReturn(new HostAndPortImpl("bob", 443));
    Mockito.when(event.request()).thenReturn(request);
    Mockito.when(request.isSSL()).thenReturn(true);
    
    assertEquals("https://bob/openapi.yaml", UiHandler.buildPath(event));
  }
  
  @Test
  public void testMultiMapToMap() {
    assertNotNull(OpenApiHandler.multiMapToMap(null));
    assertEquals(0, OpenApiHandler.multiMapToMap(null).size());
    assertEquals("carol", OpenApiHandler.multiMapToMap(new HeadersMultiMap().add("bob", "fred").add("bob", "carol")).get("bob").get(1));
  }

  private OpenAPIConfiguration createOpenapiConfiguration(boolean pretty, boolean filter, List<Object> resources, boolean openAPI31) {
    return new SwaggerConfiguration()
            .resourceClasses(Stream.concat(resources.stream(), Stream.of(this)).map(r -> r.getClass().getCanonicalName())
                    .collect(Collectors.toSet()))
            .prettyPrint(pretty)
            .openAPI31(openAPI31)
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
