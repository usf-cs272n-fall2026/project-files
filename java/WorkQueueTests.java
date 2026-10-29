package edu.usfca.cs272.tests.concurrent;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import edu.usfca.cs272.concurrent.WorkQueue;

/// A test suite for the [WorkQueue] class.
///
/// @author CS 272 Software Development (University of San Francisco)
/// @author Claude Opus 5.5 via Claude Code (Anthropic)
/// @version Fall 2026
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("v3.0") @Tag("v3.1")
public class WorkQueueTests {
	/// The maximum amount of time any single test should take.
	private static final Duration TIMEOUT = Duration.ofSeconds(2);

	/// Creates a new instance of this class.
	public WorkQueueTests() {}

	/// Makes sure the finish method is implemented before running any of the work
	/// queue tests.
	@BeforeAll
	public static void finishImplemented() {
		WorkQueue queue = new WorkQueue(1);

		try {
			assertTimeoutPreemptively(TIMEOUT, queue::finish, "An empty work queue should finish immediately.");
		}
		catch (UnsupportedOperationException ex) {
			fail("The finish() method must be implemented.", ex);
		}
		finally {
			queue.shutdown();
		}
	}

	/// Sleeps for the given number of milliseconds, restoring the interrupt status
	/// if interrupted.
	///
	/// @param millis the number of milliseconds to sleep
	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	/// Waits until the given latch reaches zero, restoring the interrupt status if
	/// interrupted.
	///
	/// @param latch the latch to wait on
	private static void await(CountDownLatch latch) {
		try {
			latch.await();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	/// Tests creating a [WorkQueue] with different numbers of worker threads.
	@Nested
	@Order(1)
	@TestMethodOrder(OrderAnnotation.class)
	public class ConstructorTests {
		/// Creates a new instance of this class.
		public ConstructorTests() {}

		/// Tests that the default constructor uses the default number of workers.
		@Test
		@Order(1)
		public void testDefaultSize() {
			WorkQueue queue = new WorkQueue();

			try {
				assertEquals(WorkQueue.DEFAULT, queue.size());
			}
			finally {
				queue.shutdown();
			}
		}

		/// Tests that the constructor uses the requested number of workers.
		///
		/// @param threads the number of worker threads
		@ParameterizedTest
		@ValueSource(ints = { 1, 2, 7 })
		@Order(2)
		public void testCustomSize(int threads) {
			WorkQueue queue = new WorkQueue(threads);

			try {
				assertEquals(threads, queue.size());
			}
			finally {
				queue.shutdown();
			}
		}

		/// Tests that the constructor rejects fewer than one worker.
		///
		/// @param threads the invalid number of worker threads
		@ParameterizedTest
		@ValueSource(ints = { 0, -1 })
		@Order(3)
		public void testInvalidSize(int threads) {
			assertThrows(IllegalArgumentException.class, () -> new WorkQueue(threads));
		}
	}

	/// Tests running tasks with the [WorkQueue#execute(Runnable)] method.
	@Nested
	@Order(2)
	@TestMethodOrder(OrderAnnotation.class)
	public class ExecuteTests {
		/// Creates a new instance of this class.
		public ExecuteTests() {}

		/// Tests that every task added to the queue is run exactly once.
		@RepeatedTest(3)
		@Order(1)
		public void testAllTasksRun() {
			int tasks = 100;

			WorkQueue queue = new WorkQueue(4);
			AtomicInteger count = new AtomicInteger();

			try {
				assertTimeoutPreemptively(TIMEOUT, () -> {
					for (int i = 0; i < tasks; i++) {
						queue.execute(count::incrementAndGet);
					}

					queue.finish();
				});

				assertEquals(tasks, count.get());
			}
			finally {
				queue.shutdown();
			}
		}

		/// Tests that the worker threads run tasks at the same time. Each task waits
		/// until told to finish, so all workers must be running a task simultaneously
		/// for the test to pass.
		///
		/// @throws InterruptedException if interrupted while waiting
		@RepeatedTest(3)
		@Order(2)
		public void testTasksRunInParallel() throws InterruptedException {
			int workers = 4;

			WorkQueue queue = new WorkQueue(workers);
			CountDownLatch started = new CountDownLatch(workers);
			CountDownLatch release = new CountDownLatch(1);

			try {
				for (int i = 0; i < workers; i++) {
					queue.execute(() -> {
						started.countDown();
						await(release);
					});
				}

				assertTrue(started.await(1, TimeUnit.SECONDS), "All workers should run a task simultaneously.");

				release.countDown();
				assertTimeoutPreemptively(TIMEOUT, queue::finish);
			}
			finally {
				release.countDown();
				queue.shutdown();
			}
		}

		/// Tests that a task throwing a runtime exception does not stop the worker
		/// from running later tasks, and is still counted as finished.
		@Test
		@Order(3)
		public void testTaskException() {
			int tasks = 10;

			WorkQueue queue = new WorkQueue(1);
			AtomicInteger count = new AtomicInteger();

			try {
				assertTimeoutPreemptively(TIMEOUT, () -> {
					queue.execute(() -> {
						throw new IllegalStateException("Intentional exception for testing.");
					});

					for (int i = 0; i < tasks; i++) {
						queue.execute(count::incrementAndGet);
					}

					queue.finish();
				}, "The work queue should finish even if a task throws an exception.");

				assertEquals(tasks, count.get());
			}
			finally {
				queue.shutdown();
			}
		}

		/// Tests that tasks cannot be added after the queue is shutdown.
		@Test
		@Order(4)
		public void testExecuteAfterShutdown() {
			WorkQueue queue = new WorkQueue(1);
			queue.shutdown();

			assertThrows(IllegalStateException.class, () -> queue.execute(() -> {}));
		}
	}

	/// Tests waiting for pending tasks with the [WorkQueue#finish()] method.
	@Nested
	@Order(3)
	@TestMethodOrder(OrderAnnotation.class)
	public class FinishTests {
		/// Creates a new instance of this class.
		public FinishTests() {}

		/// Tests that finishing with no tasks returns immediately.
		@Test
		@Order(1)
		public void testFinishEmpty() {
			WorkQueue queue = new WorkQueue(2);

			try {
				assertTimeoutPreemptively(TIMEOUT, queue::finish, "An empty work queue should finish immediately.");
			}
			finally {
				queue.shutdown();
			}
		}

		/// Tests that finish waits until all slow tasks are done.
		@RepeatedTest(3)
		@Order(2)
		public void testFinishWaits() {
			int tasks = 10;

			WorkQueue queue = new WorkQueue(2);
			AtomicInteger count = new AtomicInteger();

			try {
				assertTimeoutPreemptively(TIMEOUT, () -> {
					for (int i = 0; i < tasks; i++) {
						queue.execute(() -> {
							sleep(10);
							count.incrementAndGet();
						});
					}

					queue.finish();
				});

				assertEquals(tasks, count.get(), "All tasks should be done once finish returns.");
			}
			finally {
				queue.shutdown();
			}
		}

		/// Tests that the queue can be reused after finishing.
		@Test
		@Order(3)
		public void testFinishReuse() {
			int tasks = 10;

			WorkQueue queue = new WorkQueue(2);
			AtomicInteger count = new AtomicInteger();

			try {
				assertTimeoutPreemptively(TIMEOUT, () -> {
					for (int round = 1; round <= 3; round++) {
						for (int i = 0; i < tasks; i++) {
							queue.execute(count::incrementAndGet);
						}

						queue.finish();
						assertEquals(round * tasks, count.get(), "All tasks should be done after round " + round + ".");
					}
				});

				assertEquals(2, queue.size(), "Finishing should not change the number of workers.");
			}
			finally {
				queue.shutdown();
			}
		}

		/// Tests that finish waits for tasks that are added by other tasks.
		@RepeatedTest(3)
		@Order(4)
		public void testNestedTasks() {
			int outer = 5;
			int inner = 5;

			WorkQueue queue = new WorkQueue(3);
			AtomicInteger count = new AtomicInteger();

			try {
				assertTimeoutPreemptively(TIMEOUT, () -> {
					for (int i = 0; i < outer; i++) {
						queue.execute(() -> {
							for (int j = 0; j < inner; j++) {
								queue.execute(() -> {
									sleep(5);
									count.incrementAndGet();
								});
							}
						});
					}

					queue.finish();
				});

				assertEquals(outer * inner, count.get(), "Tasks added by other tasks should be done too.");
			}
			finally {
				queue.shutdown();
			}
		}

		/// Tests that multiple threads can wait on finish at the same time, and that
		/// none of them return until the pending tasks are done.
		///
		/// @throws InterruptedException if interrupted while waiting
		@Test
		@Order(5)
		public void testMultipleFinishers() throws InterruptedException {
			int finishers = 3;

			WorkQueue queue = new WorkQueue(2);
			CountDownLatch release = new CountDownLatch(1);
			CountDownLatch finished = new CountDownLatch(finishers);

			try {
				queue.execute(() -> await(release));

				for (int i = 0; i < finishers; i++) {
					Thread thread = new Thread(() -> {
						queue.finish();
						finished.countDown();
					});

					thread.setDaemon(true);
					thread.start();
				}

				boolean early = finished.await(200, TimeUnit.MILLISECONDS);
				release.countDown();
				boolean done = finished.await(1, TimeUnit.SECONDS);

				assertAll(
						() -> assertFalse(early, "Finish should wait while a task is still running."),
						() -> assertTrue(done, "Every thread waiting on finish should return once the task is done."));
			}
			finally {
				release.countDown();
				queue.shutdown();
			}
		}
	}

	/// Tests shutting down the [WorkQueue] with the [WorkQueue#join()] method.
	@Nested
	@Order(4)
	@TestMethodOrder(OrderAnnotation.class)
	public class JoinTests {
		/// Creates a new instance of this class.
		public JoinTests() {}

		/// Tests that join waits for all pending tasks before returning.
		@RepeatedTest(3)
		@Order(1)
		public void testJoinFinishesWork() {
			int tasks = 10;

			WorkQueue queue = new WorkQueue(2);
			AtomicInteger count = new AtomicInteger();

			assertTimeoutPreemptively(TIMEOUT, () -> {
				for (int i = 0; i < tasks; i++) {
					queue.execute(() -> {
						sleep(10);
						count.incrementAndGet();
					});
				}

				queue.join();
			});

			assertEquals(tasks, count.get(), "All tasks should be done once join returns.");
		}

		/// Tests that join terminates every worker thread.
		@Test
		@Order(2)
		public void testJoinTerminatesWorkers() {
			int workers = 3;

			WorkQueue queue = new WorkQueue(workers);
			CountDownLatch started = new CountDownLatch(workers);
			Set<Thread> threads = ConcurrentHashMap.newKeySet();

			assertTimeoutPreemptively(TIMEOUT, () -> {
				// each task blocks until every worker has picked one up
				for (int i = 0; i < workers; i++) {
					queue.execute(() -> {
						threads.add(Thread.currentThread());
						started.countDown();
						await(started);
					});
				}

				queue.join();
			});

			assertAll(
					() -> assertEquals(workers, threads.size(), "Every worker should have run a task."),
					() -> assertTrue(threads.stream().noneMatch(Thread::isAlive), "No worker should be alive after join."));
		}

		/// Tests that tasks cannot be added after the queue is joined.
		@Test
		@Order(3)
		public void testExecuteAfterJoin() {
			WorkQueue queue = new WorkQueue(1);

			assertTimeoutPreemptively(TIMEOUT, queue::join);
			assertThrows(IllegalStateException.class, () -> queue.execute(() -> {}));
		}
	}

	/// Tests interrupting threads that are waiting on the [WorkQueue]. Each thread
	/// interrupts itself before waiting, which causes the wait to throw right away.
	@Nested
	@Order(5)
	@TestMethodOrder(OrderAnnotation.class)
	public class InterruptTests {
		/// Creates a new instance of this class.
		public InterruptTests() {}

		/// Tests that an interrupted thread returns from finish with its interrupt
		/// status set.
		@Test
		@Order(1)
		public void testFinishInterrupted() {
			WorkQueue queue = new WorkQueue(1);
			CountDownLatch release = new CountDownLatch(1);

			try {
				// keeps work pending so finish has to wait
				queue.execute(() -> await(release));

				boolean interrupted = assertTimeoutPreemptively(TIMEOUT, () -> {
					Thread.currentThread().interrupt();
					queue.finish();
					return Thread.interrupted();
				}, "finish() should return when interrupted.");

				assertTrue(interrupted, "finish() should keep the interrupt status set.");
			}
			finally {
				release.countDown();
				queue.shutdown();
			}
		}

		/// Tests that an interrupted thread returns from join with its interrupt
		/// status set.
		@Test
		@Order(2)
		public void testJoinInterrupted() {
			WorkQueue queue = new WorkQueue(1);
			CountDownLatch release = new CountDownLatch(1);

			try {
				// keeps work pending so join has to wait
				queue.execute(() -> await(release));

				boolean interrupted = assertTimeoutPreemptively(TIMEOUT, () -> {
					Thread.currentThread().interrupt();
					queue.join();
					return Thread.interrupted();
				}, "join() should return when interrupted.");

				assertTrue(interrupted, "join() should keep the interrupt status set.");
			}
			finally {
				release.countDown();
				queue.shutdown();
			}
		}
	}
}
