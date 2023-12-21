/*
 * Copyright (C) 2023 njt
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
package uk.co.spudsoft.vertx.rest.serialisers;

import uk.co.spudsoft.vertx.rest.serialisers.JsonArrayMessageBodyWriter;
import io.vertx.core.json.JsonArray;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author njt
 */
public class JsonArrayMessageBodyWriterTest {
  
  @Test
  public void testIsWritableFrom() {
    
    JsonArrayMessageBodyWriter writer = new JsonArrayMessageBodyWriter();
    assertTrue(writer.isWriteable(JsonArray.class, null, null, MediaType.APPLICATION_JSON_TYPE));
    assertFalse(writer.isWriteable(String.class, null, null, MediaType.APPLICATION_JSON_TYPE));
  }
  
  @Test
  public void testWriteTo() throws Exception {
    
    JsonArrayMessageBodyWriter writer = new JsonArrayMessageBodyWriter();
    JsonArray ja = new JsonArray("[1,2]");
    String json;
    try (ByteArrayOutputStream entityStream = new ByteArrayOutputStream()) {
      writer.writeTo(ja, JsonArray.class, null, null, MediaType.APPLICATION_JSON_TYPE, null, entityStream);
      json = entityStream.toString(StandardCharsets.UTF_8);
    }
    assertEquals("[1,2]", json);
  }
  
}
