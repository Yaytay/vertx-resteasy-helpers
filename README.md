# Vertx RESTEasy Helpers
A few helpers for using Resteasy JAX-RS with a Vert.x HTTP Server.

There helpers currently in the library are:
- JaxRsHandler
- OpenApiHandler
- Timer

## JaxRsHandler

Although RESTEasy supports the use of Vert.x it does not include Vert.x contextual objects in the JAX-RS context.
The JaxRsHandler is a simple Vert.x HTTP route that includes the Vert.x RoutingContext in the JAX-RS context.

If a micrometer MeterRegistry is provided the JaxRsHandler will also configure Timer Filters for to enable the timing of JAX-RS requests.

### Usage:
```
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
```

## OpenApiHandler

The OpenApiHandler provides a simple way to add a Vertx route that outputs Open API documentation based upon JAX-RS endpoints.

The handler OpenAPIConfiguration to be passed in to it, that controls the generic information in the output.
The OpenAPIConfiguration may also contain the full canonical name of a class that implements the OpenAPISpecFilter interface.
This permits full programmatic control of the output Open API document.

### Usage:
```
    HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(0));

    Router router = Router.router(vertx);

    List<Object> controllers = Arrays.asList(new SampleHandler1(), new SampleHandler2());
    List<Object> providers = Arrays.asList(new JacksonJsonProvider(new ObjectMapper(), JacksonJsonProvider.BASIC_ANNOTATIONS));

    OpenAPIConfiguration openApiConfig = createOpenapiConfiguration(controllers);
    OpenApiHandler openApiHandler = new OpenApiHandler(this, openApiConfig, "/api/");
    openApiHandler.setOpenContextId("OpenApiHandlerTest.testHandler");
    
    router.route("/api/*").handler(new JaxRsHandler(vertx, null, "/api", controllers, providers));
    router.getWithRegex("/openapi\\..*").handler(openApiHandler);
```
with:
```
  private OpenAPIConfiguration createOpenapiConfiguration(List<Object> resources) {
    return new SwaggerConfiguration()
            .resourceClasses(Stream.concat(resources.stream(), Stream.of(this)).map(r -> r.getClass().getCanonicalName())
                    .collect(Collectors.toSet()))
            .prettyPrint(true)
            .filterClass(SampleOpenApiFilter.class.getCanonicalName())
            .openAPI(
                    new OpenAPI()
                            .info(
                                    new Info()
                                            .title("OpenApiHandlerTest")
                                            .version("0")
                            )
            );
  }
```

## Timer

The Timer functionality consists of two JAX-RS filters, a ContainerRequestFilter and a ContainerResponseFilter.
The ContainerRequestFilter records the start time of the request in the ContainerRequestContext, and the ContainerResponseFilter then uses
this to calculate the request duration and to update metrics in a micrometer MeterRegistry.

The filters are only active on classes/methods that feature the micrometer @Timed annotation.
Consult the documentation for the @Timed annotation for all the options it supports.
If no value (name) is provided in the @Timed annotation then one will be generated based on the canonical name of the class and method.
This name will probably be ugly and incompatible with other names in your metrics, so specifying a nice name is strongly recommended.

The handler can extract specific extra tag values from the requst.
Extra tags are usually specified like this:
```
    @Timed(value = "resource3", extraTags = {"bob", "value"})
```
The filters here permit expressions to be used in the values used for the tags.
For example:
```
    @Timed(extraTags = {"query", "$query.bob", "method", "$method", "host", "$header.host", "path-part", "$path.part", "henry", "$henry"})
```
This would set the "query" tag to the value of the "bob" query string parameter.
It will also set the "henry" tag to the value of the "henry" query string parameter as that is the default if the $ part is not recognised.
Path parts will only be recognised if they are configured as named parts on the JAX-RS endpoint.

Be very careful when specifying extra tags, Prometheus has a limit on the total number of time series it handles well, and each 
different combination of tags counts as a time series.
High cardinality tags can ruin Prometheus performance.
Aim for tags with just a few values (<20 as a rule) and definitely do not use primary keys as tag values.