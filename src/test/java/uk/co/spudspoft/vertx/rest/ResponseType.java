/*
 * Copyright (C) 2022 njt
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
package uk.co.spudspoft.vertx.rest;

/**
 *
 * @author njt
 */
public class ResponseType {
  
  private String field1;
  private String field2;

  public ResponseType(String field1, String field2) {
    this.field1 = field1;
    this.field2 = field2;
  }
  
  public String getField1() {
    return field1;
  }

  public void setField1(String field1) {
    this.field1 = field1;
  }

  public String getField2() {
    return field2;
  }

  public void setField2(String field2) {
    this.field2 = field2;
  }
  
}
