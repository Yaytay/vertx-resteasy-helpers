/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudspoft.vertx.rest;

import io.swagger.v3.core.filter.OpenAPISpecFilter;
import io.swagger.v3.core.model.ApiDescription;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @author njt
 */
@SuppressWarnings({"rawtypes"})
public class SampleOpenApiFilter implements OpenAPISpecFilter {

  @Override
  public Optional<OpenAPI> filterOpenAPI(OpenAPI oapi, Map<String, List<String>> map, Map<String, String> map1, Map<String, List<String>> map2) {
    return Optional.of(oapi);
  }

  @Override
  public Optional<PathItem> filterPathItem(PathItem pi, ApiDescription ad, Map<String, List<String>> map, Map<String, String> map1, Map<String, List<String>> map2) {
    return Optional.of(pi);
  }

  @Override
  public Optional<Operation> filterOperation(Operation oprtn, ApiDescription ad, Map<String, List<String>> map, Map<String, String> map1, Map<String, List<String>> map2) {
    return Optional.of(oprtn);
  }

  @Override
  public Optional<Parameter> filterParameter(Parameter prmtr, Operation oprtn, ApiDescription ad, Map<String, List<String>> map, Map<String, String> map1, Map<String, List<String>> map2) {
    return Optional.of(prmtr);
  }

  @Override
  public Optional<RequestBody> filterRequestBody(RequestBody rb, Operation oprtn, ApiDescription ad, Map<String, List<String>> map, Map<String, String> map1, Map<String, List<String>> map2) {
    return Optional.of(rb);
  }

  @Override
  public Optional<ApiResponse> filterResponse(ApiResponse ar, Operation oprtn, ApiDescription ad, Map<String, List<String>> map, Map<String, String> map1, Map<String, List<String>> map2) {
    return Optional.of(ar);
  }

  @Override
  public Optional<Schema> filterSchema(Schema schema, Map<String, List<String>> map, Map<String, String> map1, Map<String, List<String>> map2) {
    return Optional.of(schema);
  }

  @Override
  public Optional<Schema> filterSchemaProperty(Schema schema, Schema schema1, String string, Map<String, List<String>> map, Map<String, String> map1, Map<String, List<String>> map2) {
    return Optional.of(schema);
  }

  @Override
  public boolean isRemovingUnreferencedDefinitions() {
    return false;
  }

}
