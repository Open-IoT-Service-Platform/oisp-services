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
import org.apache.flink.table.annotation.DataTypeHint;
import org.apache.flink.table.api.*;
import org.apache.flink.table.functions.ScalarFunction;
import static org.apache.flink.table.api.DataTypes.*;

import java.sql.Time;
import java.time.*;
import java.time.format.DateTimeFormatter;

/// define function logic
public class Epoch2SQLTimestamp extends ScalarFunction {
	public  @DataTypeHint("TIMESTAMP(3)")
	LocalDateTime eval(Long epochTime, String zone)
	{
		ZoneId zoneId = ZoneId.of(zone);
		return Instant.ofEpochMilli(epochTime).atZone(zoneId).toLocalDateTime();
	}
}

