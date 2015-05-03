

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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HttpSender {

	private final String USER_AGENT = "openthingsSync/1.0";
	public static String POST = "POST";
	public static String GET = "GET";
	public static String PUT = "PUT";
	public static String DELETE = "DELETE";

	HttpURLConnection conn;

	public HttpSender(String url) {
		try {
			conn = (HttpURLConnection)new URL(url).openConnection();
			conn.setUseCaches(false);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(50000);
			conn.setRequestProperty("User-Agent", USER_AGENT);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public HttpSender addHeaders(Map headers) {
		Iterator it = headers.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> entry = (Map.Entry<String, String>)it.next();
			addHeader(entry.getKey(), entry.getValue());
		}
		return this;
	}

	public HttpSender addHeader(String key, String value) {
		conn.addRequestProperty(key, value);
		return this;
	}

	public HttpResponse doCall() throws IOException {
		return doCall(null);
	}

	public HttpResponse doCall(String body) throws IOException {

		if(body==null) {
			System.out.println("Call with NO body.");
			conn.connect();
		} else {
			System.out.println("Call with body:");
			System.out.println(body);
			conn.setDoOutput(true);
			byte[] bytes = body.getBytes();
			if(!conn.getRequestMethod().equals(DELETE)) {
				OutputStream out = conn.getOutputStream();
				try {
					out.write(bytes);
				} finally {
					close(out);
				}
			}
		}

		BufferedReader in;
		if(conn.getResponseCode()>299) {
			in = new BufferedReader(
					new InputStreamReader(conn.getErrorStream()));
		} else {
			in = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
		}
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		System.out.println(conn.getResponseCode());
		System.out.println(response.toString().replace("</br>", "\n").replace("<br>", "\n").replace("</p>", "</p>\n"));
		System.out.println(getOutHeaders(conn.getHeaderFields()));
		return new HttpResponse(conn.getResponseCode(), response.toString(), conn.getHeaderFields());
	}


	public HttpSender setMethod(String httpMethod) {
		try {
			conn.setRequestMethod(httpMethod);
		} catch (ProtocolException e) {
			e.printStackTrace();
		}
		return this;
	}

	private static void close(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				// ignore error
				//logger.log(Level.FINEST, "IOException closing stream", e);
			}
		}
	}

	public void close() {
		if(conn==null) return;
		try {
			conn.disconnect();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static String getOutHeaders(Map<String, List<String>> headers) throws IOException {

		StringBuffer response = new StringBuffer();
		Iterator<Map.Entry<String, List<String>>> it = headers.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, List<String>> entry = it.next();
			response.append(entry.getKey() + ": ");
			for(String se:entry.getValue()) {
				response.append(se);
			}
			response.append("\n");
		}

		return response.toString();

	}

}
