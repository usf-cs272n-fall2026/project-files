package edu.usfca.cs272.tests;

import static edu.usfca.cs272.tests.ThreadBuildTests.timeIndexOneMany;
import static edu.usfca.cs272.tests.ThreadBuildTests.timeIndexSingleMulti;
import static edu.usfca.cs272.tests.ThreadSearchTests.timeSearchOneMany;
import static edu.usfca.cs272.tests.ThreadSearchTests.timeSearchSingleMulti;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;

import edu.usfca.cs272.tests.utils.ProjectBenchmarks;

/// Tests that multithreading is faster than single-threading.
///
/// @author CS 272 Software Development (University of San Francisco)
/// @version Fall 2026
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class ThreadTimedTests extends ProjectBenchmarks {
	/// Creates a new instance of this class.
	public ThreadTimedTests() {}

	/// Tests multithreaded build times. These tests are slow.
	@Nested
	@Order(1)
	@TestMethodOrder(OrderAnnotation.class)
	@Tag("bench-v3.1")
	@Tag("bench-v4.0") @Tag("bench-v4.1") @Tag("bench-v4.2")
	public class IndexBenchmarks {
		/// Creates a new instance of this class.
		public IndexBenchmarks() {}

		/// Tests that 4 worker threads build at least 1.2 times faster than 1 worker thread.
		@Test
		@Order(1)
		public void fasterIndexOneMany() {
			timeIndexOneMany(1.2);
		}

		/// Tests that 4 worker threads build at least 1.2 times faster than single-threading.
		@Test
		@Order(2)
		public void fasterSingleMulti() {
			timeIndexSingleMulti(1.2);
		}
	}

	/// Tests multithreaded search times. These tests are slow.
	@Nested
	@Order(2)
	@TestMethodOrder(OrderAnnotation.class)
	@Tag("bench-v3.1")
	@Tag("bench-v4.0") @Tag("bench-v4.1") @Tag("bench-v4.2")
	public class SearchBenchmarks {
		/// Creates a new instance of this class.
		public SearchBenchmarks() {}

		/// Tests that 4 worker threads search at least 1.2 times faster than 1 worker thread.
		@Test
		@Order(1)
		public void fasterSearchOneMany() {
			timeSearchOneMany(1.2);
		}

		/// Tests that 4 worker threads search at least 1.2 times faster than single-threading.
		@Test
		@Order(2)
		public void fasterSearchSingleMulti() {
			timeSearchSingleMulti(1.2);
		}
	}
}
