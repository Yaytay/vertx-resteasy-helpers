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
package uk.co.spudspoft.vertx.rest.serialisers;

import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;


/**
 *
 * @author njt
 */
public class JsonObjectMessageBodyReaderTest {
  
  @Test
  public void testReadFrom() throws Exception {
    
    JsonObjectMessageBodyReader reader = new JsonObjectMessageBodyReader();
    String input = "{\"first\":1,\"second\":2}";
    try (InputStream entityStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))) {
      JsonObject jo = reader.readFrom(JsonObject.class, null, null, MediaType.APPLICATION_JSON_TYPE, null, entityStream);
      assertEquals(2, jo.size());
      assertEquals(2, jo.getInteger("second"));
    }
    
  }
  
}
