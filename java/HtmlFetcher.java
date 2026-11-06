package edu.usfca.cs272.sockets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/// A specialized version of [HttpsFetcher] that follows redirects and returns
/// HTML content if possible.
///
/// @see HttpsFetcher
///
/// @author CS 272 Software Development (University of San Francisco)
/// @version Fall 2026
public class HtmlFetcher {
	/// Returns `true` if and only if there is a "content-type" header (assume
	/// lowercase) and the first value of that header starts with the value
	/// "text/html" (case-insensitive).
	///
	/// @param headers the HTTP/1.1 headers to parse (keys must be lowercase)
	/// @return `true` if the headers indicate the content type is HTML
	///
	/// @see HttpsFetcher#processHttpHeaders(BufferedReader)
	public static boolean isHtml(Map<String, List<String>> headers) {
		// TODO Fix isHtml(Map) implementation.
		throw new UnsupportedOperationException("Not yet implemented.");
	}

	/// Parses the HTTP status code from the provided HTTP headers, assuming the
	/// status line is stored under the `null` key.
	///
	/// @param headers the HTTP/1.1 headers to parse (keys must be lowercase)
	/// @return the HTTP status code or -1 if unable to parse for any reasons
	///
	/// @see HttpsFetcher#processHttpHeaders(BufferedReader)
	public static int getStatusCode(Map<String, List<String>> headers) {
		// TODO Fix getStatusCode(Map) implementation.
		throw new UnsupportedOperationException("Not yet implemented.");
	}

	/// If the HTTP status code is between 300 and 399 (inclusive) indicating a
	/// redirect, returns the first redirect location if it is provided. Otherwise
	/// returns `null`.
	///
	/// @param headers the HTTP/1.1 headers to parse (keys must be lowercase)
	/// @return the first redirected location if the headers indicate a redirect
	///
	/// @see HttpsFetcher#processHttpHeaders(BufferedReader)
	public static String getRedirect(Map<String, List<String>> headers) {
		// TODO Fix getRedirect(Map) implementation.
		throw new UnsupportedOperationException("Not yet implemented.");
	}

	/// Efficiently fetches HTML using HTTP/1.1 and sockets.
	///
	/// The HTTP body will only be fetched and processed if the status code is 200
	/// and the content-type is HTML. In that case, the HTML will be returned as a
	/// single joined [String] using the [System#lineSeparator()].
	///
	/// Otherwise, the HTTP body will not be fetched. However, if the status code is
	/// a redirect, then the location of the redirect will be recursively followed
	/// up to the specified number of times. Once the number of redirects falls to 0
	/// or lower, then redirects will no longer be followed.
	///
	/// If valid HTML cannot be fetched within the specified number of redirects,
	/// then `null` is returned.
	///
	/// @param uri the [URI] to fetch
	/// @param redirects the number of times to follow redirects
	/// @return the HTML or `null` if unable to fetch valid HTML
	///
	/// @see HttpsFetcher#openConnection(URI)
	/// @see HttpsFetcher#printGetRequest(PrintWriter, URI)
	/// @see HttpsFetcher#processHttpHeaders(BufferedReader)
	///
	/// @see Collectors#joining(CharSequence)
	/// @see System#lineSeparator()
	///
	/// @see #isHtml(Map)
	/// @see #getRedirect(Map)
	public static String fetch(URI uri, int redirects) {
		String html = null;

		try (
				Socket socket = HttpsFetcher.openConnection(uri);
				PrintWriter request = new PrintWriter(socket.getOutputStream());
				InputStreamReader input = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
				BufferedReader response = new BufferedReader(input);
		) {
			// TODO Fill in fetch(URI, int) implementation.
			throw new UnsupportedOperationException("Not yet implemented.");
		}
		catch (IOException | IllegalArgumentException e) {
			html = null;
		}

		return html;
	}

	/// Converts the [String] into a [URI] object and then calls
	/// [#fetch(URI, int)].
	///
	/// @param uri the [URI] to fetch
	/// @param redirects the number of times to follow redirects
	/// @return the HTML or `null` if unable to fetch valid HTML
	///
	/// @see #fetch(URI, int)
	public static String fetch(String uri, int redirects) {
		try {
			return fetch(new URI(uri), redirects);
		}
		catch (NullPointerException | URISyntaxException e) {
			return null;
		}
	}

	/// Converts the [String] into a [URI] object and then calls
	/// [#fetch(URI, int)] with 0 redirects.
	///
	/// @param uri the [URI] to fetch
	/// @return the HTML or `null` if unable to fetch valid HTML
	///
	/// @see #fetch(URI, int)
	public static String fetch(String uri) {
		return fetch(uri, 0);
	}

	/// Calls [#fetch(URI, int)] with 0 redirects.
	///
	/// @param uri the [URI] to fetch
	/// @return the HTML or `null` if unable to fetch valid HTML
	public static String fetch(URI uri) {
		return fetch(uri, 0);
	}

	/// Demonstrates this class.
	///
	/// @param args unused
	public static void main(String[] args) {
		String link = "https://usf-cs272n-fall2026.github.io/project-web/input/birds/falcon.html";
		System.out.println(link);
		System.out.println(fetch(link));
	}

	/// Prevent instantiating this class of static methods.
	private HtmlFetcher() {}
}
