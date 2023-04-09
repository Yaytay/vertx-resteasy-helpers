/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudspoft.vertx.rest;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

/**
 *
 * @author njt
 */
@Path("/two")
public class SampleHandler2 {
  
  @GET
  @Produces(value = "application/json")
  @ApiResponse(
          responseCode = "200"
          , description = "The list of all and directories files."
          , content = @Content(
                  mediaType = "application/json"
                  , schema = @Schema(implementation = ResponseType.class)
          )
  )
  public ResponseType get() {
    return new ResponseType("field1", "field2");
  }
  
}
