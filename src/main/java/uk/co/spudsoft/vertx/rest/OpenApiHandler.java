/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.co.spudsoft.vertx.rest;

import io.swagger.v3.core.filter.OpenAPISpecFilter;
import io.swagger.v3.core.filter.SpecFilter;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.HostAndPort;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vert.x web handler for outputting openapi definitions based on RestEasy Jax-RS endpoints.
 *
 * @author jtalbut
 */
public class OpenApiHandler implements Handler<RoutingContext> {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(OpenApiHandler.class);

  /**
   * Request path that will return the description from the OpenAPI document as a standalone HTML document.
   */
  public static final String SCHEMA_DESCRIPTION = "/schema/description/";
  
  private final Application app;
  private final OpenAPIConfiguration openApiConfiguration;
  private String openApiContextId;
  private final String basePath;

  /**
   * Constructor.
   *
   * @param app The Jax-RS application, typically the Main class.
   * @param openApiConfiguration The Open API configuration, may be null.
   * @param basePath Base path to be prepended to any paths.
   * 
   */
  public OpenApiHandler(Application app, OpenAPIConfiguration openApiConfiguration, String basePath) {
    this(app
            , Objects.requireNonNull(openApiConfiguration, "openApiConfiguration may not be null")
            , Objects.requireNonNull(basePath, "basePath may not be null")
            , true
    );
  }  
  
  /**
   * Private constructor to avoid CT_CONSTRUCTOR_THROW.
   * 
   * To avoid CT_CONSTRUCTOR_THROW the public constructor must consist purely of a call to this constructor and
   * must perform all validation that may throw an exception.
   * A factory method could equally be used, as long as the actual constructor does not throw any exceptions.
   *
   * @param app The Jax-RS application, typically the Main class.
   * @param openApiConfiguration The Open API configuration, may be null.
   * @param basePath Base path to be prepended to any paths.
   * @param checked Unused argument that serves purely to distinguish from the public constructor.
   */
  private OpenApiHandler(Application app, OpenAPIConfiguration openApiConfiguration, String basePath, boolean checked) {
    this.app = app;
    this.openApiConfiguration = openApiConfiguration;
    
    if (basePath.endsWith("/")) {
      basePath = basePath.substring(0, basePath.length() - 1);
    }
    
    this.basePath = basePath;
  }
  
  /**
   * UiHandler factory method.
   * @return a new UiHandler object.
   */
  public UiHandler getUiHandler() {
    return new UiHandler();
  }
  
  private static String hapToHost(HostAndPort hap, boolean isSsl) {
    int nativePort = isSsl ? 443 : 80;
    if (hap.port() == nativePort) {
      return hap.host();
    } else {
      return hap.host() + ":" + hap.port();
    }
  }
  
  /**
   * The Vertx handler for converting a request into an HTML page the uses 
   * <a href="https://github.com/Rhosys/openapi-explorer">OpenAPI-Explorer</a> to reference /openapi.yaml.
   * 
   * The HTML page uses <a href="https://www.unpkg.com/">unpkg</a> to reference 
   * <a href="https://unpkg.com/openapi-explorer@0/dist/browser/openapi-explorer.min.js">openapi-explorer.min.js</a>.
   * 
   */
  public static class UiHandler implements Handler<RoutingContext> {

    static String buildPath(RoutingContext event) {
      HttpServerRequest request = event.request();      
      MultiMap headers = request.headers();
      String proto = headers == null ? null : headers.get("X-Forwarded-Proto");
      HostAndPort hap = request.authority();
      String host;
      if (proto == null) {
        if (request.isSSL()) {
          proto = "https://";
          host = hapToHost(hap, true);
        } else {
          proto = "http://";
          host = hapToHost(hap, false);
        }
      } else {
        proto = proto + "://";
        host = hapToHost(hap, "https".equals(proto));
      }
      
      return proto
              + host
              + "/openapi.yaml"
              ;
    }
    
    @Override
    public void handle(RoutingContext event) {

      String path = buildPath(event);
      
      String html = """
                <!doctype html>
                <html>
                  <head>
                    <script type="module" src="https://unpkg.com/openapi-explorer@0/dist/browser/openapi-explorer.min.js"></script>
                  </head>
                  <body>
                    <openapi-explorer spec-url="PATH"> </openapi-explorer>
                  </body>
                </html>
                """
              .replaceAll("PATH", path);      
      
      event.response().setStatusCode(200).end(html);
    }
    
  }

  /**
   * The Java OpenAPI implementation caches the OpenID context per context ID. In live usage this is almost never an
   * issue because there is only one OpenAPI configuration required in a given service. However this completely breaks
   * unit tests, that want to use different configurations. For that purpose it is possible to set a context ID.
   *
   * @param openContextId The Open API context ID to use.
   * @return this for fluent usage.
   */
  public OpenApiHandler setOpenContextId(String openContextId) {
    this.openApiContextId = openContextId;
    return this;
  }
  
  @Override
  public void handle(RoutingContext event) {
    logger.trace("Handling OpenAPI request");
    try {
      JaxrsOpenApiContextBuilder<?> oacb = new JaxrsOpenApiContextBuilder<>()
              .application(app);
      oacb.setOpenApiConfiguration(openApiConfiguration);
      if (openApiContextId != null) {
        oacb.setCtxId(openApiContextId);
      }
      OpenApiContext ctx = oacb.buildContext(true);
      
      OpenAPIConfiguration config = ctx.getOpenApiConfiguration();
      if (config == null) {
        config = openApiConfiguration;
      }
      
      boolean pretty = false;
      if (Boolean.TRUE.equals(config.isPrettyPrint())) {
        pretty = true;
      }

      OpenAPI oas = ctx.read();
      if (oas != null) {
        if (!basePath.isEmpty()) {
          adjustPaths(oas);
        }      
        if (config.getFilterClass() != null) {
          try {
            OpenAPISpecFilter filterImpl = (OpenAPISpecFilter) Class.forName(
                    ctx.getOpenApiConfiguration().getFilterClass()).getDeclaredConstructor().newInstance();
            SpecFilter f = new SpecFilter();
            oas = f.filter(oas,
                     filterImpl,
                     getQueryParams(event),
                     getCookies(event),
                     getHeaders(event)
            );
          } catch (Exception e) {
            logger.error("failed to load filter", e);
          }
        }
      }

      if (oas == null) {
        logger.error("Failed to create OpenAPI object");
        event.fail(404);
        return ;
      }

      var response = event.response();
      response.putHeader("Access-Control-Request-Method", "GET");
      
      String normalizedPath = event.normalizedPath();
      int startPos = normalizedPath.indexOf(SCHEMA_DESCRIPTION);
      if (startPos > 0) {
        String component = normalizedPath.substring(SCHEMA_DESCRIPTION.length() + startPos);
        logger.debug("Component: {}", component);
        Schema<?> schema = oas.getComponents().getSchemas().get(component);
        if (schema == null) {
          logger.error("Component {} not found in schemas", component);
          event.fail(404);
          return ;
        }
        String description = schema.getDescription();
        if (description != null && !description.isEmpty()) {
          response.putHeader("Content-Type", "text/html");
          response.end("<html><body>" + description + "</body></html>");
        } else {
          response.putHeader("Content-Type", "text/html");
          response.end("<html><body></body></html>");
        }
      } else if (normalizedPath.endsWith(".yaml")) {
        response.putHeader("Content-Type", "application/yaml");
        if (config.isOpenAPI31()) {
          response.end(pretty ? Yaml31.pretty(oas) : Yaml31.mapper().writeValueAsString(oas));
        } else {
          response.end(pretty ? Yaml.pretty(oas) : Yaml.mapper().writeValueAsString(oas));
        }
      } else {
        response.putHeader("Content-Type", MediaType.APPLICATION_JSON);
        if (config.isOpenAPI31()) {
          response.end(pretty ? Json31.pretty(oas) : Json31.mapper().writeValueAsString(oas));
        } else {
          response.end(pretty ? Json.pretty(oas) : Json.mapper().writeValueAsString(oas));
        }
      }
    } catch (Throwable ex) {
      logger.error("failed to generate {}: ",
               event.normalizedPath().endsWith(".yaml") ? "yaml" : "json",
               ex);
      event.fail(500);
    }
  }

  private void adjustPaths(OpenAPI oas) {
    Paths paths = oas.getPaths();    
    if ((paths != null) && !paths.isEmpty()) {
      if (!paths.keySet().stream().findAny().orElse("").startsWith(basePath)) {
        Paths newPaths = new Paths();
        newPaths.setExtensions(paths.getExtensions());
        for (Entry<String, PathItem> entry : paths.entrySet()) {
          newPaths.put(basePath + entry.getKey(), entry.getValue());
        }
        oas.setPaths(newPaths);
      }
    }
  }
  
  private Map<String, List<String>> getQueryParams(RoutingContext routingContext) {
    return multiMapToMap(routingContext.queryParams());
  }

  private Map<String, String> getCookies(RoutingContext routingContext) {
    Map<String, String> result = new HashMap<>();
    for (Cookie cookie : routingContext.request().cookies()) {
      result.put(cookie.getName(), cookie.getValue());
    }
    return result;
  }

  private Map<String, List<String>> getHeaders(RoutingContext routingContext) {
    return multiMapToMap(routingContext.request().headers());
  }

  static Map<String, List<String>> multiMapToMap(MultiMap items) {
    Map<String, List<String>> result = new HashMap<>();
    if (items != null) {
      for (Entry<String, String> entry : items) {
        List<String> list = result.get(entry.getKey());
        if (list == null) {
          list = new ArrayList<>();
          result.put(entry.getKey(), list);
        }
        list.add(entry.getValue());
      }
    }
    return result;
  }

}
