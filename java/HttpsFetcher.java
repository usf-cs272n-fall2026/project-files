package edu.usfca.cs272.sockets;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

/// Demonstrates how to use the built-in [HttpClient] to fetch the headers and
/// content from a URI on the web, as well as how to setup the
/// [Socket] connections to do the same manually (well suited for web crawling).
///
/// @author CS 272 Software Development (University of San Francisco)
/// @version Fall 2026
public class HttpsFetcher {
	// Example HTTP Request and Response:
	// https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Session

	/// Fetches the headers and content for the specified [URI]. The content is
	/// placed as a list of all the lines fetched under the "Content" key.
	///
	/// @param uri the [URI] to fetch
	/// @return a map with the headers and content
	/// @throws IOException if unable to fetch headers and content
	///
	/// @see #openConnection(URI)
	/// @see #printGetRequest(PrintWriter, URI)
	/// @see #processHttpHeaders(BufferedReader)
	public static Map<String, List<String>> fetch(URI uri) throws IOException {
		try (Socket socket = openConnection(uri);
				PrintWriter request = new PrintWriter(socket.getOutputStream());
				InputStreamReader input = new InputStreamReader(socket.getInputStream(), UTF_8);
				BufferedReader response = new BufferedReader(input);) {
			// make HTTP GET request of the web server
			printGetRequest(request, uri);

			// the headers will be first in the response
			Map<String, List<String>> headers = processHttpHeaders(response);

			// read everything remaining in socket reader as the content
			List<String> content = response.lines().toList();
			headers.put("content", content);

			return headers;
		}
	}

	/// Uses a [Socket] to open a connection to the web server associated with the
	/// provided [URI]. Supports HTTP and HTTPS connections.
	///
	/// @param uri the [URI] to connect
	/// @return a socket connection for that [URI]
	/// @throws UnknownHostException if the host is not known
	/// @throws IOException if an I/O error occurs when creating the socket
	///
	/// @see URL#openConnection()
	/// @see URLConnection
	/// @see HttpURLConnection
	public static Socket openConnection(URI uri) throws UnknownHostException, IOException {
		String protocol = uri.getScheme();
		String host = uri.getHost();

		boolean https = protocol != null && protocol.equalsIgnoreCase("https");
		int defaultPort = https ? 443 : 80;
		int port = uri.getPort() < 0 ? defaultPort : uri.getPort();

		SocketFactory factory = https ? SSLSocketFactory.getDefault() : SocketFactory.getDefault();
		return factory.createSocket(host, port);
	}

	/// Writes a simple HTTP v1.1 GET request to the provided socket writer.
	///
	/// @param writer a writer created from a socket connection
	/// @param uri the [URI] to fetch via the socket connection
	/// @throws IOException if unable to write request to socket
	///
	/// @see Builder#GET()
	public static void printGetRequest(PrintWriter writer, URI uri) throws IOException {
		String host = uri.getHost();
		String path = uri.getRawPath();
		String query = uri.getRawQuery();

		// an empty path (e.g. https://example.com) must still request "/"
		String resource = path == null || path.isEmpty() ? "/" : path;
		resource = query == null ? resource : resource + "?" + query;

		writer.printf("GET %s HTTP/1.1\r\n", resource);
		writer.printf("Host: %s\r\n", host);
		writer.printf("Connection: close\r\n");
		writer.printf("\r\n");
		writer.flush();
	}

	/// Gets the header fields from a reader associated with a socket connection.
	/// Requires that the socket reader has not yet been used, otherwise this method
	/// will return unpredictable results.
	///
	/// @param response a reader created from a socket connection
	/// @return a map of header fields to a list of header values
	/// @throws IOException if unable to read from socket
	///
	/// @see URLConnection#getHeaderFields()
	/// @see HttpHeaders#map()
	public static Map<String, List<String>> processHttpHeaders(BufferedReader response) throws IOException {
		Map<String, List<String>> results = new HashMap<>();

		// first line will be the http status line (status code and description)
		// null key used by early Java versions (see URLConnection#getHeaderFields())
		String line = response.readLine();
		results.put(null, List.of(line));

		// remaining lines until first blank line are the other headers
		while ((line = response.readLine()) != null && !line.isBlank()) {
			String[] split = line.split(":\\s+", 2);
			assert split.length == 2;

			split[0] = split[0].toLowerCase();
			results.computeIfAbsent(split[0], _ -> new ArrayList<>()).add(split[1]);
		}

		return results;
	}

	/// Refer to [#fetch(URI)] for details.
	///
	/// @param uri the [URI] to fetch
	/// @return a map with the headers and content
	/// @throws URISyntaxException if unable to convert [String] to [URI]
	/// @throws IOException if unable to fetch headers and content
	///
	/// @see #fetch(URI)
	public static Map<String, List<String>> fetch(String uri) throws URISyntaxException, IOException {
		return fetch(new URI(uri));
	}

	/// Fetches the headers and content for the specified [URI] using the built-in
	/// Java [HttpClient]. The content is placed as a list of all the lines
	/// fetched under the "content" key.
	///
	/// @param client the HTTP client to use
	/// @param uri the [URI] to fetch
	///
	/// @return a map of headers and content
	/// @throws IOException if unable to fetch headers and content
	/// @throws InterruptedException if underlying [Executor] is interrupted
	///
	/// @see java.net.http.HttpRequest.Builder
	public static Map<String, List<String>> fetch(HttpClient client, URI uri) throws IOException, InterruptedException {
		// create placeholder for results
		Map<String, List<String>> results = new HashMap<>();

		// create GET request
		HttpRequest request = HttpRequest.newBuilder(uri)
				.version(HttpClient.Version.HTTP_1_1) // newer HTTP versions supported
				.GET() // HEAD, POST, PUT, and DELETE supported
				.build();

		// synchronously get response
		// convert response body to stream of lines
		HttpResponse<Stream<String>> response = client.send(request, BodyHandlers.ofLines());

		try (Stream<String> body = response.body()) {
			// add content, status code, and headers
			results.put("content", body.toList());
			results.put(null, List.of(Integer.toString(response.statusCode())));
			results.putAll(response.headers().map());
		}

		return results;
	}

	/// Prevent instantiating this class of static methods.
	private HttpsFetcher() {}
}
