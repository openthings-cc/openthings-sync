/*
 * Copyright (C) 2014 OpenThings Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.openthings.sender;

import java.util.List;
import java.util.Map;

public class HttpResponse {
	
	public final int responseCode;
	public final String body;
	public final Map<String, List<String>> headers;
	
	public HttpResponse(int responseCode, String body, Map<String, List<String>> headers) {
		this.responseCode =  responseCode;
		this.body =  body;
		this.headers = headers;
	}

}
