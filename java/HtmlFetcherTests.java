package edu.usfca.cs272.tests.sockets;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import edu.usfca.cs272.sockets.HtmlFetcher;
import edu.usfca.cs272.sockets.HttpsFetcher;

/// Tests the [HtmlFetcher] class.
///
/// @author CS 272 Software Development (University of San Francisco)
/// @author Claude Opus 5.5 (Anthropic)
/// @version Fall 2026
@Tag("v4.0") @Tag("v4.1") @Tag("v4.2")
@ExtendWith(HtmlFetcherTests.TestCounter.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class HtmlFetcherTests {
	// ███████╗████████╗ ██████╗ ██████╗
	// ██╔════╝╚══██╔══╝██╔═══██╗██╔══██╗
	// ███████╗   ██║   ██║   ██║██████╔╝
	// ╚════██║   ██║   ██║   ██║██╔═══╝
	// ███████║   ██║   ╚██████╔╝██║
	// ╚══════╝   ╚═╝    ╚═════╝ ╚═╝
	/*
	 * ...and read this! Please do not spam web servers by rapidly re-running all of
	 * these tests over and over again. You risk being blocked by the web server if
	 * you make making too many requests in too short of a time period!
	 *
	 * Focus on one test or one group of tests at a time instead. If you do that,
	 * you will not have anything to worry about!
	 */

	/// Creates a new instance of this class.
	public HtmlFetcherTests() {}

	/// Tests the [HtmlFetcher#isHtml(Map)] method.
	///
	/// @see HtmlFetcher#isHtml(Map)
	@Nested
	@Order(1)
	@TestMethodOrder(OrderAnnotation.class)
	public class HtmlTypeTests {
		/// Creates a new instance of this class.
		public HtmlTypeTests() {}

		/// Skips these tests if any previous tests have failed.
		@BeforeAll
		public static void skipIfFailed() {
			Assumptions.assumeTrue(TestCounter.failed == 0, "Skipping tests due to previous failures.");
		}

		/// Tests the [HtmlFetcher#isHtml(Map)] method for URLs that do not point
		/// to valid HTML webpages.
		///
		/// @param link the link to test
		/// @throws IOException from [URL#openConnection()]
		///
		/// @see HtmlFetcher#isHtml(Map)
		@ParameterizedTest
		@ValueSource(strings = {
				"input/simple/no_extension",
				"input/simple/double_extension.html.txt",
				"input/gutenberg/pg1661-h/images/cover.jpg"
		})
		@Order(1)
		public void testNotHtml(String link) throws IOException {
			URI uri = GITHUB.resolve(link);

			Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
				Map<String, List<String>> headers = getHeaders(uri);
				Assertions.assertFalse(HtmlFetcher.isHtml(headers), () -> debug(uri, headers));
			});
		}

		/// Tests the [HtmlFetcher#isHtml(Map)] method for URLs that do point to
		/// valid HTML webpages.
		///
		/// @param link the link to test
		/// @throws IOException from [URL#openConnection()]
		///
		/// @see HtmlFetcher#isHtml(Map)
		@ParameterizedTest
		@ValueSource(strings = {
				"input",
				"input/simple/",
				"input/simple/empty.html",
				"input/birds/falcon.html",
				"input/birds/falcon.html#file=hello.jpg",
				"https://www.cs.usfca.edu/~cs272/redirect/nowhere",
				"https://www.cs.usfca.edu/~cs272/recurse/"
		})
		@Order(2)
		public void testIsHtml(String link) throws IOException {
			URI uri = GITHUB.resolve(link);

			Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
				Map<String, List<String>> headers = getHeaders(uri);
				Assertions.assertTrue(HtmlFetcher.isHtml(headers), () -> debug(uri, headers));
			});
		}
	}

	/// Tests the status code methods.
	///
	/// @see HtmlFetcher#getStatusCode(Map)
	@Nested
	@Order(2)
	@TestMethodOrder(OrderAnnotation.class)
	public class StatusCodeTests {
		/// Creates a new instance of this class.
		public StatusCodeTests() {}

		/// Skips these tests if any previous tests have failed.
		@BeforeAll
		public static void skipIfFailed() {
			Assumptions.assumeTrue(TestCounter.failed == 0, "Skipping tests due to previous failures.");
		}

		/// Tests that the status code is 200.
		///
		/// @param link the link to fetch
		/// @throws IOException from [#testStatusCode(String, int)]
		/// @see HtmlFetcher#getStatusCode(Map)
		@ParameterizedTest
		@ValueSource(strings = {
				"input/simple/no_extension",
				"input/simple/double_extension.html.txt",
				"input/birds/yellowthroat.html"
		})
		@Order(1)
		public void test200(String link) throws IOException {
			testStatusCode(link, 200);
		}

		/// Tests the status code for other codes.
		///
		/// @param code the expected code
		/// @param link the link to fetch
		/// @throws IOException from [#testStatusCode(String, int)]
		/// @see HtmlFetcher#getStatusCode(Map)
		@ParameterizedTest
		@CsvSource({
				"301, input",
				"302, http://www.cs.usfca.edu/~cs272/",
				"301, https://www.cs.usfca.edu/~cs272/redirect/loop1",
				"404, https://www.cs.usfca.edu/~cs272/redirect/nowhere",
				"410, https://www.cs.usfca.edu/~cs272/redirect/gone"
		})
		@Order(2)
		public void testOther(int code, String link) throws IOException {
			testStatusCode(link, code);
		}
	}

	/// Tests the redirect status code methods.
	///
	/// @see HtmlFetcher#getRedirect(Map)
	@Nested
	@Order(3)
	@TestMethodOrder(OrderAnnotation.class)
	public class RedirectLocationTests {
		/// Creates a new instance of this class.
		public RedirectLocationTests() {}

		/// Skips these tests if any previous tests have failed.
		@BeforeAll
		public static void skipIfFailed() {
			Assumptions.assumeTrue(TestCounter.failed == 0, "Skipping tests due to previous failures.");
		}

		/// Tests the location of a redirect.
		///
		/// @param source the link to fetch
		/// @param target the link to redirect to
		///
		/// @see HtmlFetcher#getRedirect(Map)
		@ParameterizedTest
		@CsvSource({
				"https://www.cs.usfca.edu/~cs272/redirect/loop1, https://www.cs.usfca.edu/~cs272/redirect/loop2",
				"https://www.cs.usfca.edu/~cs272/redirect/loop2, https://www.cs.usfca.edu/~cs272/redirect/loop1",
				"https://www.cs.usfca.edu/~cs272/redirect/one, https://www.cs.usfca.edu/~cs272/redirect/two",
				"https://www.cs.usfca.edu/~cs272/redirect/two, https://www.cs.usfca.edu/~cs272/redirect/three"
		})
		@Order(1)
		public void testRedirect(String source, String target) {
			URI uri = URI.create(source);

			Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
				Map<String, List<String>> headers = getHeaders(uri);
				String expected = target;
				String actual = HtmlFetcher.getRedirect(headers);
				Assertions.assertEquals(expected, actual, () -> debug(uri, headers));
			});
		}

		/// Tests when the status code is not a redirect.
		///
		/// @param link the link to fetch
		/// @throws IOException from [URL#openConnection()]
		///
		/// @see HtmlFetcher#getRedirect(Map)
		@ParameterizedTest
		@ValueSource(strings = {
				"input/simple/no_extension",
				"https://www.cs.usfca.edu/~cs272/redirect/nowhere",
				"https://www.cs.usfca.edu/~cs272/redirect/gone"
		})
		@Order(2)
		public void testNotRedirect(String link) throws IOException {
			URI uri = GITHUB.resolve(link);

			Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
				Map<String, List<String>> headers = getHeaders(uri);
				Assertions.assertNull(HtmlFetcher.getRedirect(headers), () -> debug(uri, headers));
			});
		}
	}

	/// Tests fetching HTML for troublesome links.
	///
	/// @see HtmlFetcher#fetch(String)
	/// @see HtmlFetcher#fetch(URI)
	@Nested
	@Order(4)
	@TestMethodOrder(OrderAnnotation.class)
	public class FetchHtmlTests {
		/// Creates a new instance of this class.
		public FetchHtmlTests() {}

		/// Skips these tests if any previous tests have failed.
		@BeforeAll
		public static void skipIfFailed() {
			Assumptions.assumeTrue(TestCounter.failed == 0, "Skipping tests due to previous failures.");
		}

		/// Test that attempting to fetch pages that do not have valid HTML results in
		/// a null value.
		///
		/// @param link the link to fetch
		@ParameterizedTest
		@ValueSource(strings = {
				"input/simple/no_extension",
				"input/simple/double_extension.html.txt",
				"https://www.cs.usfca.edu/~cs272/redirect/nowhere"
		})
		@Order(1)
		public void testNotValidHtml(String link) {
			URI uri = GITHUB.resolve(link);

			Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
				String html = HtmlFetcher.fetch(uri);
				Map<String, List<String>> headers = getHeaders(uri);
				Assertions.assertNull(html, () -> debug(uri, headers));
			});
		}

		/// Tests the HTML returned for a valid page.
		@Test
		@Order(2)
		public void testHtmlYellow() {
			String link = "input/birds/yellowthroat.html";
			URI uri = GITHUB.resolve(link);

			String expected = """
					<!DOCTYPE html>
	
					<html lang="en">
					<head>
					<title>Web Crawler Test</title>
					<meta charset="utf-8">
					</head>
					
					<body>
					<p>
					Yellowthroat is a common type of bird.
					</p>
					
					
					<p>
					<a href="birds.html">Home</a>
					</p>
					
					</body>
					</html>
					""";

			Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
				String html = HtmlFetcher.fetch(uri);
				compareText(expected, html);
			});
		}

		/// Tests the HTML returned for a valid page.
		@Test
		@Order(3)
		public void testHtmlHello() {
			String link = "input/simple/hello.html";
			URI uri = GITHUB.resolve(link);

			Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
				String html = HtmlFetcher.fetch(uri);
				compareText(HELLO, html);
			});
		}
	}

	/// Tests fetching HTML for redirects.
	///
	/// @see HtmlFetcher#fetch(String, int)
	/// @see HtmlFetcher#fetch(URI, int)
	@Nested
	@Order(5)
	@TestMethodOrder(OrderAnnotation.class)
	public class FetchRedirectTests {
		/// Creates a new instance of this class.
		public FetchRedirectTests() {}

		/// Skips these tests if any previous tests have failed.
		@BeforeAll
		public static void skipIfFailed() {
			Assumptions.assumeTrue(TestCounter.failed == 0, "Skipping tests due to previous failures.");
		}

		/// Tests that null is returned when a link does not resolve within a specific
		/// number of redirects.
		///
		/// @param redirects the number of redirects to try
		@ParameterizedTest
		@ValueSource(ints = {
				-1, 0, 1, 2
		})
		@Order(1)
		public void testUnsuccessfulRedirect(int redirects) {
			URI one = URI.create("https://www.cs.usfca.edu/~cs272/redirect/one");

			Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
				String html = HtmlFetcher.fetch(one, redirects);
				Assertions.assertNull(html, "\nThere should be no HTML found with the given number of redirects.\n");
			});
		}

		/// Tests that proper HTML is returned when a link DOES resolve within a
		/// specific number of redirects.
		///
		/// @param redirects the number of redirects to try
		@ParameterizedTest
		@ValueSource(ints = {
				3, 4, 5
		})
		@Order(2)
		public void testSuccessfulRedirect(int redirects) {
			String one = "https://www.cs.usfca.edu/~cs272/redirect/one";

			Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
				String html = HtmlFetcher.fetch(one, redirects);
				Assertions.assertNotNull(html, "\nNo HTML content returned. If redirects are followed correctly, non-null HTML should be returned.\n");
				compareText(HELLO, html);
			});
		}
	}

	/// Tests if the status code returned is as expected.
	///
	/// @param link the URI to fetch
	/// @param code the expected status code
	/// @throws IOException from [URL#openConnection()]
	///
	/// @see HtmlFetcher#getStatusCode(Map)
	public static void testStatusCode(String link, int code) throws IOException {
		URI uri = GITHUB.resolve(link);

		Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
			Map<String, List<String>> headers = getHeaders(uri);
			int actual = HtmlFetcher.getStatusCode(headers);
			Assertions.assertEquals(code, actual, () -> debug(uri, headers));
		});
	}

	/// Use built-in Java URL connection to get headers for debugging. Uses
	/// [URLConnection] since that provides headers in the same format as the
	/// [HttpsFetcher] class.
	///
	/// @param uri the URI to fetch
	/// @return the headers
	/// @throws IOException if unable to connect
	public static Map<String, List<String>> getHeaders(URI uri) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();

		try {
			connection.setInstanceFollowRedirects(false);
			connection.setRequestProperty("Connection", "close");

			Map<String, List<String>> headers = connection.getHeaderFields();
			Map<String, List<String>> lowercase = new HashMap<>();

			for (var entry : headers.entrySet()) {
				String key = entry.getKey() == null ? null : entry.getKey().toLowerCase();
				lowercase.put(key, entry.getValue());
			}

			return lowercase;
		}
		finally {
			connection.disconnect();
		}
	}

	/// Cleans whitespace and then compares the actual text to the expected.
	///
	/// @param expected the expected text
	/// @param actual the actual text
	public static void compareText(String expected, String actual) {
		Assertions.assertEquals(cleanWhitespace(expected), cleanWhitespace(actual));
	}

	/// Cleans up whitespace for comparison.
	///
	/// @param text the text to clean
	/// @return the cleaned text
	public static String cleanWhitespace(String text) {
		return text.strip().replaceAll("\r\n?", "\n");
	}

	/// Produces output for debugging.
	///
	/// @param uri the uri
	/// @param headers the headers
	/// @return the output
	public static final String debug(URI uri, Map<String, List<String>> headers) {
		StringBuilder output = new StringBuilder();
		output.append("\nURI:\n");
		output.append(uri.toString());
		output.append("\n\nHeaders:\n");

		for (var entry : headers.entrySet()) {
			String key = entry.getKey();

			// skip custom headers
			if (key == null || !key.toLowerCase().startsWith("x-")) {
				output.append(entry.getKey());
				output.append(" -> ");
				output.append(entry.getValue());
				output.append("\n");
			}
		}

		output.append("\n");
		return output.toString();
	}

	/// Base URI for the GitHub test website.
	public static final URI GITHUB = URI.create("https://usf-cs272n-fall2026.github.io/project-web/");

	/// How long to wait for individual tests to complete.
	public static final Duration TIMEOUT = Duration.ofSeconds(45);

	/// Expected HTML for the hello page, also the end of the redirect chain.
	public static final String HELLO = """
			<!DOCTYPE html>
			<html lang="en">
			  <head>
			    <meta charset="utf-8">
			    <title>Hello World</title>
			  </head>
			  <body>
			    <!-- page content -->
			    <h1>Hello World!</h1>
			    <p>Hello, world. Hello... World? HELLO WORLD!</p>

			    <script>
			    window.alert("Hello world!");
			    </script>
			  </body>
			</html>
			""";

	/// Counts the number of tests that pass and fail.
	///
	/// @see TestWatcher
	public static class TestCounter implements TestWatcher {
		/// Tracks number of successes.
		public static int passed = 0;

		/// Tracks number of failures.
		public static int failed = 0;

		/// Initializes the test counter.
		public TestCounter() {
			passed = 0;
			failed = 0;
		}

		@Override
		public void testSuccessful(ExtensionContext context) {
			passed++;
		}

		@Override
		public void testFailed(ExtensionContext context, Throwable cause) {
			failed++;
		}
	}
}