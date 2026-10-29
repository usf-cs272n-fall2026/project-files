package edu.usfca.cs272.tests;

import static edu.usfca.cs272.tests.utils.ProjectFlag.COUNTS;
import static edu.usfca.cs272.tests.utils.ProjectFlag.EXACT;
import static edu.usfca.cs272.tests.utils.ProjectFlag.INDEX;
import static edu.usfca.cs272.tests.utils.ProjectFlag.PARTIAL;
import static edu.usfca.cs272.tests.utils.ProjectFlag.RESULTS;
import static edu.usfca.cs272.tests.utils.ProjectFlag.TEXT;
import static edu.usfca.cs272.tests.utils.ProjectFlag.THREADS;
import static edu.usfca.cs272.tests.utils.ProjectPath.ACTUAL;
import static edu.usfca.cs272.tests.utils.ProjectPath.EXPECTED;
import static edu.usfca.cs272.tests.utils.ProjectPath.HELLO;
import static edu.usfca.cs272.tests.utils.ProjectPath.QUERY;
import static edu.usfca.cs272.tests.utils.ProjectPath.QUERY_COMPLEX;
import static edu.usfca.cs272.tests.utils.ProjectPath.QUERY_SIMPLE;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import edu.usfca.cs272.tests.utils.ProjectBenchmarks;
import edu.usfca.cs272.tests.utils.ProjectFlag;
import edu.usfca.cs272.tests.utils.ProjectPath;
import edu.usfca.cs272.tests.utils.ProjectTests;

/// Tests exact and partial search with multithreading.
///
/// @author CS 272 Software Development (University of San Francisco)
/// @version Fall 2026
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@ExtendWith(ProjectTests.TestCounter.class)
public class ThreadSearchTests extends ProjectBenchmarks {
	/// Creates a new instance of this class.
	public ThreadSearchTests() {}

	/// Tests that worker threads are used. May be unreliable outside GitHub Actions.
	@Nested
	@Order(1)
	@TestMethodOrder(OrderAnnotation.class)
	@Tag("v3.0") @Tag("v3.1")
	@Tag("v4.0") @Tag("v4.1") @Tag("v4.2")
	public class ApproachTests {
		/// Creates a new instance of this class.
		public ApproachTests() {}

		/// Tests that partial search with 2 threads creates worker threads.
		@Test
		@Order(1)
		public void testPartial() {
			String[] args = {
					TEXT.flag, ProjectPath.TEXT.text,
					PARTIAL.flag, ProjectPath.QUERY_COMPLEX.text,
					THREADS.flag, ProjectBenchmarks.Threads.TWO.text
			};

			Runnable test = () -> {
				assertNoExceptions(args, LONG_TIMEOUT);
				System.out.println("Random: " + Math.random());
			};

			ProjectBenchmarks.assertMultithreaded(test);
		}
	}

	/// Tests exact search output.
	@Nested
	@Order(2)
	@TestMethodOrder(OrderAnnotation.class)
	@Tag("v3.0") @Tag("v3.1")
	public class ExactTests {
		/// Whether to run partial search instead of exact search.
		public boolean partial;

		/// Creates a new instance of this class.
		public ExactTests() {}

		/// Uses exact search.
		@BeforeEach
		public void setup() {
			partial = false;
		}

		/// Tests searching the `simple` files.
		///
		/// @param threads the number of threads
		@ParameterizedTest
		@Order(1)
		@EnumSource()
		public void testSimple(ProjectBenchmarks.Threads threads) {
			testSearch(partial, "simple", ProjectPath.SIMPLE, threads);
		}

		/// Tests searching the `stems` files.
		///
		/// @param threads the number of threads
		@ParameterizedTest
		@Order(2)
		@EnumSource()
		public void testStems(ProjectBenchmarks.Threads threads) {
			testSearch(partial, "words", ProjectPath.STEMS, threads);
		}

		/// Tests searching the `rfcs` files.
		///
		/// @param threads the number of threads
		@ParameterizedTest
		@Order(3)
		@EnumSource()
		public void testRfcs(ProjectBenchmarks.Threads threads) {
			testSearch(partial, "letters", ProjectPath.RFCS, threads);
		}

		/// Tests searching one Gutenberg file.
		///
		/// @param threads the number of threads
		@ParameterizedTest
		@Order(4)
		@EnumSource()
		public void testGutenElectric(ProjectBenchmarks.Threads threads) {
			testSearch(partial, "complex", ProjectPath.GUTEN_ELECTRIC, threads);
		}

		/// Tests searching the `gutenberg` files.
		///
		/// @param threads the number of threads
		@ParameterizedTest
		@Order(5)
		@EnumSource()
		public void testGuten(ProjectBenchmarks.Threads threads) {
			testSearch(partial, "complex", ProjectPath.GUTEN, threads);
		}

		/// Tests searching every text file for `respect`.
		///
		/// @param threads the number of threads
		@ParameterizedTest
		@Order(6)
		@EnumSource()
		public void testTextRespect(ProjectBenchmarks.Threads threads) {
			testSearch(partial, "respect", ProjectPath.TEXT, threads);
		}

		/// Tests searching every text file with complex queries.
		///
		/// @param threads the number of threads
		@ParameterizedTest
		@Order(7)
		@EnumSource()
		@Tag("v4.0") @Tag("v4.1") @Tag("v4.2")
		public void testTextComplex(ProjectBenchmarks.Threads threads) {
			testSearch(partial, "complex", ProjectPath.TEXT, threads);
		}

		/// Frees memory for the tests that follow.
		@AfterAll
		public static void freeMemory() {
			ProjectTests.freeMemory();
		}
	}

	/// Tests partial search output, reusing the exact search tests.
	@Nested
	@Order(3)
	@TestMethodOrder(OrderAnnotation.class)
	@Tag("v3.0") @Tag("v3.1")
	public class PartialTests extends ExactTests {
		/// Creates a new instance of this class.
		public PartialTests() {}

		/// Uses partial search.
		@Override
		@BeforeEach
		public void setup() {
			super.partial = true;
		}
	}

	/// Tests invalid `--threads` values and flag combinations.
	@Nested
	@Order(4)
	@TestMethodOrder(OrderAnnotation.class)
	@Tag("v3.0") @Tag("v3.1")
	@Tag("v4.0") @Tag("v4.1") @Tag("v4.2")
	public class ExceptionTests {
		/// Creates a new instance of this class.
		public ExceptionTests() {}

		/// Tests a negative number of threads.
		///
		/// @throws Exception if an exception occurs
		@Test
		@Order(1)
		public void testNegativeThreads() throws Exception {
			String[] args = { TEXT.flag, HELLO.text, THREADS.flag, "-4", EXACT.flag, QUERY_SIMPLE.text, RESULTS.flag };
			Files.deleteIfExists(RESULTS.path);
			assertNoExceptions(args, SHORT_TIMEOUT);
			Assertions.assertTrue(Files.exists(RESULTS.path), RESULTS.value);
		}

		/// Tests zero threads.
		///
		/// @throws Exception if an exception occurs
		@Test
		@Order(2)
		public void testZeroThreads() throws Exception {
			String[] args = { TEXT.flag, HELLO.text, THREADS.flag, "0", EXACT.flag, QUERY_SIMPLE.text, RESULTS.flag };
			Files.deleteIfExists(RESULTS.path);
			assertNoExceptions(args, SHORT_TIMEOUT);
			Assertions.assertTrue(Files.exists(RESULTS.path), RESULTS.value);
		}

		/// Tests a decimal number of threads.
		///
		/// @throws Exception if an exception occurs
		@Test
		@Order(3)
		public void testFractionThreads() throws Exception {
			String[] args = { TEXT.flag, HELLO.text, THREADS.flag, "3.14", EXACT.flag, QUERY_SIMPLE.text, RESULTS.flag };
			Files.deleteIfExists(RESULTS.path);
			assertNoExceptions(args, SHORT_TIMEOUT);
			Assertions.assertTrue(Files.exists(RESULTS.path), RESULTS.value);
		}

		/// Tests a non-numeric number of threads.
		///
		/// @throws Exception if an exception occurs
		@Test
		@Order(4)
		public void testWordThreads() throws Exception {
			String[] args = { TEXT.flag, HELLO.text, THREADS.flag, "fox", EXACT.flag, QUERY_SIMPLE.text, RESULTS.flag };
			Files.deleteIfExists(RESULTS.path);
			assertNoExceptions(args, SHORT_TIMEOUT);
			Assertions.assertTrue(Files.exists(RESULTS.path), RESULTS.value);
		}

		/// Tests `--threads` without a value.
		///
		/// @throws Exception if an exception occurs
		@Test
		@Order(5)
		public void testDefaultThreads() throws Exception {
			String[] args = { TEXT.flag, HELLO.text, THREADS.flag, EXACT.flag, QUERY_SIMPLE.text, RESULTS.flag };
			Files.deleteIfExists(RESULTS.path);
			assertNoExceptions(args, SHORT_TIMEOUT);
			Assertions.assertTrue(Files.exists(RESULTS.path), RESULTS.value);
		}

		/// Tests searching with threads without output flags.
		///
		/// @throws Exception if an exception occurs
		@Test
		@Order(6)
		public void testNoOutput() throws Exception {
			String[] args = { TEXT.flag, HELLO.text, THREADS.flag, EXACT.flag, QUERY_SIMPLE.text };
			assertNoExceptions(args, SHORT_TIMEOUT);
		}

		/// Tests `--threads` and `--partial` without values.
		///
		/// @throws Exception if an exception occurs
		@Test
		@Order(7)
		public void testOnlyThreadsAndPartial() throws Exception {
			String[] args = { THREADS.flag, PARTIAL.flag };
			assertNoExceptions(args, SHORT_TIMEOUT);
		}

		/// Tests `--threads` and `--results` without input.
		///
		/// @throws Exception if an exception occurs
		@Test
		@Order(8)
		public void testOnlyThreadsAndOutput() throws Exception {
			String[] args = { THREADS.flag, RESULTS.flag };
			Files.deleteIfExists(RESULTS.path);
			assertNoExceptions(args, SHORT_TIMEOUT);
			Assertions.assertTrue(Files.exists(RESULTS.path), RESULTS.value);
		}
	}

	/// Tests multithreaded search times. Skipped if earlier tests failed.
	@Nested
	@Order(4)
	@TestMethodOrder(OrderAnnotation.class)
	@Tag("bench-v3.0")
	public class ThreadTests {
		/// Creates a new instance of this class.
		public ThreadTests() {}

		/// Skips these tests if earlier tests failed.
		///
		/// @param info the test information
		@BeforeAll
		public static void checkStatus(TestInfo info) {
			// disable if there were earlier failures
			ProjectTests.TestCounter.assertNoFailures(info);
		}

		/// Tests 4 worker threads against 1 worker thread.
		@Test
		@Order(1)
		public void okaySearchOneMany() {
			// shouldn't be significantly slower
			timeSearchOneMany(0.75);
		}

		/// Tests 4 worker threads against single-threading.
		@Test
		@Order(2)
		public void slowSearchSingleMulti() {
			// shouldn't be significantly slower
			timeSearchSingleMulti(0.75);
		}
	}

	/// Tests index, word count, and search output together.
	@Nested
	@Order(5)
	@TestMethodOrder(OrderAnnotation.class)
	@Tag("v3.0") @Tag("v3.1")
	@Tag("v4.0") @Tag("v4.1") @Tag("v4.2")
	public class ComboTests {
		/// Creates a new instance of this class.
		public ComboTests() {}

		/// Tests all output for every text file with 2 threads.
		///
		/// @param partial whether to run partial search instead of exact search
		@ParameterizedTest
		@ValueSource(booleans = { false, true })
		@Order(1)
		public void testCountsIndexResults(boolean partial) {
			ProjectPath input = ProjectPath.TEXT;
			ProjectBenchmarks.Threads threads = ProjectBenchmarks.Threads.TWO;

			String query = "complex";
			ProjectFlag search = partial ? PARTIAL : EXACT;
			String type = search.name().toLowerCase();

			String indexName = String.format("index-%s", input.id);
			String countsName = String.format("counts-%s", input.id);
			String resultsName = String.format("%s-%s-%s", type, query, input.id);

			Path indexActual = ACTUAL.resolve(indexName + "-" + threads.text + ".json");
			Path countsActual = ACTUAL.resolve(countsName + "-" + threads.text + ".json");
			Path resultsActual = ACTUAL.resolve(resultsName + "-" + threads.text + ".json");

			String[] args = {
					TEXT.flag, input.text,
					INDEX.flag, indexActual.toString(),
					COUNTS.flag, countsActual.toString(),
					search.flag, ProjectPath.QUERY.resolve(query + ".txt").toString(),
					RESULTS.flag, resultsActual.toString(),
					THREADS.flag, threads.text
			};

			Map<Path, Path> files = Map.of(
					countsActual, EXPECTED.resolve("counts").resolve(countsName + ".json"),
					indexActual, EXPECTED.resolve("index").resolve(indexName + ".json"),
					resultsActual, EXPECTED.resolve(type).resolve(resultsName + ".json")
			);

			Executable test = () -> ProjectTests.checkOutput(args, files);
			Assertions.assertTimeoutPreemptively(LONG_TIMEOUT, test);
		}
	}

	/// Tests multithreaded search results against the expected output.
	///
	/// @param partial whether to run partial search instead of exact search
	/// @param query the name of the query file
	/// @param input the input path
	/// @param threads the number of threads
	public static void testSearch(boolean partial, String query, ProjectPath input, ProjectBenchmarks.Threads threads) {
		ProjectFlag search = partial ? PARTIAL : EXACT;
		String type = search.name().toLowerCase();
		String single = String.format("%s-%s-%s.json", type, query, input.id);
		String threaded = String.format("%s-%s-%s-%s.json", type, query, input.id, threads.text);

		Path actual = ACTUAL.resolve(threaded).normalize();
		Path expected = EXPECTED.resolve(type).resolve(single).normalize();
		Path queries = QUERY.resolve(query + ".txt").normalize();

		String[] args = {
				TEXT.flag, input.text, search.flag, queries.toString(),
				RESULTS.flag, actual.toString(),
				THREADS.flag, threads.text
		};

		Executable debug = () -> checkOutput(args, actual, expected);
		Assertions.assertTimeoutPreemptively(LONG_TIMEOUT, debug);
	}

	/// Times searching with 1 versus 4 threads.
	///
	/// @param target the minimum speedup
	public static void timeSearchOneMany(double target) {
		timeSearchOneMany(target, ProjectBenchmarks.Threads.FOUR.num);
	}

	/// Times searching with 1 versus many threads.
	///
	/// @param target the minimum speedup
	/// @param threads the number of threads
	public static void timeSearchOneMany(double target, int threads) {
		String[] args1 = {
				TEXT.flag, ProjectPath.TEXT.text,
				PARTIAL.flag, QUERY_COMPLEX.text,
				THREADS.flag, String.valueOf(1)
		};

		String[] args2 = {
				TEXT.flag, ProjectPath.TEXT.text,
				PARTIAL.flag, QUERY_COMPLEX.text,
				THREADS.flag, String.valueOf(threads)
		};

		// make sure code runs without exceptions before testing
		assertNoExceptions(args1, SHORT_TIMEOUT);
		assertNoExceptions(args2, SHORT_TIMEOUT);

		// then test the timing
		assertTimeoutPreemptively(LONG_TIMEOUT, () -> {
			double result = compare("Search", "1 Worker", args1, threads + " Workers", args2);
			Supplier<String> debug = () -> String.format(SPEEDUP, threads, result, target, "1 worker");
			assertTrue(result >= target, debug);
		});
	}

	/// Times searching single-threaded versus 4 threads.
	///
	/// @param target the minimum speedup
	public static void timeSearchSingleMulti(double target) {
		timeSearchSingleMulti(target, ProjectBenchmarks.Threads.FOUR.num);
	}

	/// Times searching single-threaded versus many threads.
	///
	/// @param target the minimum speedup
	/// @param threads the number of threads
	public static void timeSearchSingleMulti(double target, int threads) {
		String[] args1 = { TEXT.flag, ProjectPath.TEXT.text, PARTIAL.flag, QUERY_COMPLEX.text };

		String[] args2 = {
				TEXT.flag, ProjectPath.TEXT.text,
				PARTIAL.flag, QUERY_COMPLEX.text,
				THREADS.flag, String.valueOf(threads)
		};

		// make sure code runs without exceptions before testing
		assertNoExceptions(args1, SHORT_TIMEOUT);
		assertNoExceptions(args2, SHORT_TIMEOUT);

		// then test the timing
		assertTimeoutPreemptively(LONG_TIMEOUT, () -> {
			double result = compare("Search", "Single", args1, threads + " Workers", args2);
			Supplier<String> debug = () -> String.format(SPEEDUP, threads, result, target, "single-threading");
			assertTrue(result >= target, debug);
		});
	}
}
