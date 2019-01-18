/**
 * Copyright © 2019 Apache IoTDB(incubating) (dev@iotdb.apache.org)
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.iotdb.db.sql;

import org.apache.iotdb.db.sql.parse.AstNode;
import org.apache.iotdb.db.sql.parse.ParseDriver;
import org.apache.iotdb.db.sql.parse.ParseException;

/**
 * ParseContextGenerator is a class that offers methods to generate AstNode Tree
 *
 */
public final class ParseGenerator {

  /**
   * Parse the input {@link String} command and generate an AstNode Tree.
   */
  public static AstNode generateAST(String command) throws ParseException {
    ParseDriver pd = new ParseDriver();
    return pd.parse(command);
  }

}
