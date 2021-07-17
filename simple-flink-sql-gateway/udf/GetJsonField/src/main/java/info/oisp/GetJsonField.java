/*
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

package info.oisp;

import com.google.gson.*;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.functions.ScalarFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetJsonField extends ScalarFunction {
	private static final Logger LOG = LoggerFactory.getLogger(GetJsonField.class);

	public
	String eval(String object, String field)
	{
		String result = null;
		try {
			JsonObject jsonObject = JsonParser.parseString(object).getAsJsonObject();
			JsonElement property = jsonObject.get(field);
			if (property.isJsonObject()) {
				result = property.getAsJsonObject().get("value").getAsString();
			} else {
				result = jsonObject.get(field).getAsString();
			}

		} catch(JsonParseException | NullPointerException| IllegalStateException e) {
			LOG.warn("Could not get JSON Field: " + e.getMessage());
		}
		return result;
	}
}
