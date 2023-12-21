/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.vertx.rest;

import uk.co.spudsoft.vertx.rest.JaxRsHandler;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class JaxRsHandlerTest {
  
  @Test
  public void testHandle(Vertx vertx) {
    // Just to validate that it's OK to use without a MeterRegistry
    JaxRsHandler handler = new JaxRsHandler(vertx, null, "/api/*", Arrays.asList(), Arrays.asList());
  }
  
}
