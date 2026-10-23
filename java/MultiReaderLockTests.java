package edu.usfca.cs272.tests.concurrent;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

import edu.usfca.cs272.concurrent.MultiReaderLock;
import edu.usfca.cs272.concurrent.MultiReaderLock.SimpleLock;

/// A test suite for the [MultiReaderLock] class.
///
/// @author CS 272 Software Development (University of San Francisco)
/// @author Claude Opus 5.5 via Claude Code (Anthropic)
/// @version Fall 2026
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("v3.0") @Tag("v3.1")
public class MultiReaderLockTests {
	/// Creates a new instance of this class.
	public MultiReaderLockTests() {}

	/// Tests the read lock of the [MultiReaderLock] class.
	@Nested
	@Order(1)
	@TestMethodOrder(OrderAnnotation.class)
	public class ReadLockTests {
		/// Creates a new instance of this class.
		public ReadLockTests() {}

		/// Makes sure the read lock methods are implemented before running any of the
		/// read lock tests.
		@BeforeAll
		public static void readLockImplemented() {
			MultiReaderLock lock = new MultiReaderLock();

			try {
				lock.readLock().lock();
				lock.readLock().unlock();
			}
			catch (UnsupportedOperationException ex) {
				fail("The readLock().lock() and readLock().unlock() methods must be implemented.", ex);
			}
		}

		/// Tests that multiple worker threads can hold the read lock at the same time.
		/// Each worker acquires the read lock and then waits until told to release it,
		/// so all workers must hold the read lock simultaneously for the test to pass.
		///
		/// @throws InterruptedException if interrupted while waiting
		@RepeatedTest(3)
		@Order(1)
		public void testMultipleReaders() throws InterruptedException {
			int workers = 4;

			MultiReaderLock lock = new MultiReaderLock();
			CountDownLatch acquired = new CountDownLatch(workers);
			CountDownLatch release = new CountDownLatch(1);

			Thread[] threads = new Thread[workers];

			for (int i = 0; i < workers; i++) {
				threads[i] = new Thread(() -> {
					lock.readLock().lock();

					try {
						acquired.countDown();
						release.await();
					}
					catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
					finally {
						lock.readLock().unlock();
					}
				});

				threads[i].setDaemon(true);
				threads[i].start();
			}

			try {
				boolean allAcquired = acquired.await(1, TimeUnit.SECONDS);

				assertAll(
						() -> assertTrue(allAcquired, "All workers should acquire the read lock simultaneously."),
						() -> assertEquals(workers, lock.readers()));
			}
			finally {
				release.countDown();
			}

			for (Thread thread : threads) {
				thread.join(1000);
			}

			assertEquals(0, lock.readers());
		}

		/// Tests that multiple worker threads can lock and unlock the read lock one at
		/// a time. Each worker records the number of active readers while it holds the
		/// read lock, and must finish before the next worker starts.
		///
		/// @throws InterruptedException if interrupted while waiting
		@RepeatedTest(3)
		@Order(2)
		public void testSequentialReaders() throws InterruptedException {
			int workers = 2;

			MultiReaderLock lock = new MultiReaderLock();
			int[] observed = new int[workers];

			for (int i = 0; i < workers; i++) {
				int index = i;

				Thread thread = new Thread(() -> {
					lock.readLock().lock();

					try {
						observed[index] = lock.readers();
					}
					finally {
						lock.readLock().unlock();
					}
				});

				thread.setDaemon(true);
				thread.start();
				thread.join(1000);

				assertAll(
						() -> assertFalse(thread.isAlive(), "Worker " + index + " should finish within the timeout."),
						() -> assertEquals(1, observed[index], "Worker " + index + " should be the only active reader."),
						() -> assertEquals(0, lock.readers()));
			}
		}

		/// Tests that unlocking the read lock without any active readers throws an
		/// exception.
		@Test
		@Order(3)
		public void testUnlockWithoutReaders() {
			MultiReaderLock lock = new MultiReaderLock();

			assertThrows(IllegalStateException.class, () -> lock.readLock().unlock());
		}
	}

	/// Tests the write lock of the [MultiReaderLock] class.
	@Nested
	@Order(2)
	@TestMethodOrder(OrderAnnotation.class)
	public class WriteLockTests {
		/// Creates a new instance of this class.
		public WriteLockTests() {}

		/// Makes sure the write lock methods are implemented before running any of the
		/// write lock tests.
		@BeforeAll
		public static void writeLockImplemented() {
			MultiReaderLock lock = new MultiReaderLock();

			try {
				lock.writeLock().lock();
				lock.writeLock().unlock();
			}
			catch (UnsupportedOperationException ex) {
				fail("The writeLock().lock() and writeLock().unlock() methods must be implemented.", ex);
			}
		}

		/// Tests that only one worker thread can hold the write lock at a time. All
		/// workers wait until told to start, then try to acquire the write lock at the
		/// same time. Each worker records the number of active writers while it holds
		/// the write lock, which should never be more than one.
		///
		/// @throws InterruptedException if interrupted while waiting
		@RepeatedTest(3)
		@Order(1)
		public void testMultipleWriters() throws InterruptedException {
			int workers = 4;

			MultiReaderLock lock = new MultiReaderLock();
			CountDownLatch start = new CountDownLatch(1);
			CountDownLatch finished = new CountDownLatch(workers);

			int[] observed = new int[workers];

			for (int i = 0; i < workers; i++) {
				int index = i;

				Thread thread = new Thread(() -> {
					try {
						start.await();
						lock.writeLock().lock();

						try {
							// gives other workers a chance to (incorrectly) acquire the lock
							Thread.sleep(10);

							observed[index] = lock.writers();
						}
						finally {
							lock.writeLock().unlock();
						}
					}
					catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
					finally {
						finished.countDown();
					}
				});

				thread.setDaemon(true);
				thread.start();
			}

			start.countDown();
			boolean allFinished = finished.await(1, TimeUnit.SECONDS);

			assertAll(
					() -> assertTrue(allFinished, "All workers should acquire and release the write lock."),
					() -> assertArrayEquals(new int[] { 1, 1, 1, 1 }, observed, "Each worker should be the only active writer."),
					() -> assertEquals(0, lock.writers()));
		}

		/// Tests that multiple worker threads can lock and unlock the write lock one at
		/// a time. Each worker records the number of active writers while it holds the
		/// write lock, and must finish before the next worker starts.
		///
		/// @throws InterruptedException if interrupted while waiting
		@RepeatedTest(3)
		@Order(2)
		public void testSequentialWriters() throws InterruptedException {
			int workers = 2;

			MultiReaderLock lock = new MultiReaderLock();
			int[] observed = new int[workers];

			for (int i = 0; i < workers; i++) {
				int index = i;

				Thread thread = new Thread(() -> {
					lock.writeLock().lock();

					try {
						observed[index] = lock.writers();
					}
					finally {
						lock.writeLock().unlock();
					}
				});

				thread.setDaemon(true);
				thread.start();
				thread.join(1000);

				assertAll(
						() -> assertFalse(thread.isAlive(), "Worker " + index + " should finish within the timeout."),
						() -> assertEquals(1, observed[index], "Worker " + index + " should be the only active writer."),
						() -> assertEquals(0, lock.writers()));
			}
		}

		/// Tests that unlocking the write lock without any active writers throws an
		/// exception.
		@Test
		@Order(3)
		public void testUnlockWithoutWriters() {
			MultiReaderLock lock = new MultiReaderLock();

			assertThrows(IllegalStateException.class, () -> lock.writeLock().unlock());
		}

		/// Tests that unlocking the write lock from a thread that does not hold it
		/// throws an exception and does not change the number of writers or the active
		/// writer.
		///
		/// @throws InterruptedException if interrupted while waiting
		@Test
		@Order(4)
		public void testUnlockByOtherThread() throws InterruptedException {
			MultiReaderLock lock = new MultiReaderLock();
			Throwable[] thrown = new Throwable[1];

			lock.writeLock().lock();

			try {
				Thread thread = new Thread(() -> {
					try {
						lock.writeLock().unlock();
					}
					catch (Throwable ex) {
						thrown[0] = ex;
					}
				});

				thread.setDaemon(true);
				thread.start();
				thread.join(1000);

				assertAll(
						() -> assertInstanceOf(ConcurrentModificationException.class, thrown[0]),
						() -> assertEquals(1, lock.writers()),
						() -> assertTrue(lock.isActiveWriter(), "The original thread should still be the active writer."));
			}
			finally {
				lock.writeLock().unlock();
			}
		}
	}

	/// Tests that the read and write locks of the [MultiReaderLock] class take
	/// turns properly when mixed together.
	@Nested
	@Order(3)
	@TestMethodOrder(OrderAnnotation.class)
	public class MixedLockTests {
		/// Creates a new instance of this class.
		public MixedLockTests() {}

		/// Makes sure the read and write lock methods are implemented before running
		/// any of the mixed lock tests.
		@BeforeAll
		public static void mixedLockImplemented() {
			ReadLockTests.readLockImplemented();
			WriteLockTests.writeLockImplemented();
		}

		/// Tests a reader followed by a writer.
		///
		/// @throws InterruptedException if interrupted while waiting
		@RepeatedTest(3)
		@Order(1)
		public void testReadThenWrite() throws InterruptedException {
			assertTakesTurns("R", "W");
		}

		/// Tests a writer followed by a reader.
		///
		/// @throws InterruptedException if interrupted while waiting
		@RepeatedTest(3)
		@Order(2)
		public void testWriteThenRead() throws InterruptedException {
			assertTakesTurns("W", "R");
		}

		/// Tests two readers followed by a writer.
		///
		/// @throws InterruptedException if interrupted while waiting
		@RepeatedTest(3)
		@Order(3)
		public void testReadersThenWrite() throws InterruptedException {
			assertTakesTurns("RR", "W");
		}

		/// Tests a writer followed by two readers.
		///
		/// @throws InterruptedException if interrupted while waiting
		@RepeatedTest(3)
		@Order(4)
		public void testWriteThenReaders() throws InterruptedException {
			assertTakesTurns("W", "RR");
		}

		/// Tests a reader, then a writer, then a reader, then a writer.
		///
		/// @throws InterruptedException if interrupted while waiting
		@RepeatedTest(3)
		@Order(5)
		public void testAlternating() throws InterruptedException {
			assertTakesTurns("R", "W", "R", "W");
		}

		/// Runs each group of workers in order, where each group is either all readers
		/// (for example "RR" for two readers) or a single writer ("W"). The workers in
		/// a group acquire their lock and hold it until told to release it. While a
		/// group holds its lock, the next group is started and must wait until the
		/// previous group releases its lock.
		///
		/// @param groups the groups of workers to run in order
		/// @throws InterruptedException if interrupted while waiting
		private void assertTakesTurns(String... groups) throws InterruptedException {
			MultiReaderLock lock = new MultiReaderLock();
			CountDownLatch[] acquired = new CountDownLatch[groups.length];
			CountDownLatch[] release = new CountDownLatch[groups.length];
			List<Thread> threads = new ArrayList<>();

			try {
				for (int i = 0; i < groups.length; i++) {
					int group = i;
					boolean write = groups[group].startsWith("W");
					int workers = groups[group].length();

					acquired[group] = new CountDownLatch(workers);
					release[group] = new CountDownLatch(1);

					for (int j = 0; j < workers; j++) {
						Thread thread = new Thread(() -> {
							SimpleLock simple = write ? lock.writeLock() : lock.readLock();
							simple.lock();

							try {
								acquired[group].countDown();
								release[group].await();
							}
							catch (InterruptedException ex) {
								Thread.currentThread().interrupt();
							}
							finally {
								simple.unlock();
							}
						});

						thread.setDaemon(true);
						thread.start();
						threads.add(thread);
					}

					if (group > 0) {
						// gives the workers a chance to (incorrectly) acquire the lock
						acquired[group].await(100, TimeUnit.MILLISECONDS);

						assertEquals(workers, acquired[group].getCount(),
								"Group " + group + " (" + groups[group] + ") should wait for group " + (group - 1) + " (" + groups[group - 1] + ") to unlock.");

						release[group - 1].countDown();
					}

					boolean allAcquired = acquired[group].await(1, TimeUnit.SECONDS);
					int readers = lock.readers();
					int writers = lock.writers();

					assertAll(
							() -> assertTrue(allAcquired, "Group " + group + " (" + groups[group] + ") should acquire the lock."),
							() -> assertEquals(write ? 0 : workers, readers, "Group " + group + " (" + groups[group] + ") has the wrong number of readers."),
							() -> assertEquals(write ? 1 : 0, writers, "Group " + group + " (" + groups[group] + ") has the wrong number of writers."));
				}
			}
			finally {
				for (CountDownLatch latch : release) {
					if (latch != null) {
						latch.countDown();
					}
				}
			}

			for (Thread thread : threads) {
				thread.join(1000);
			}

			assertAll(
					() -> assertEquals(0, lock.readers()),
					() -> assertEquals(0, lock.writers()));
		}
	}

	/// Tests that the active writer of the [MultiReaderLock] class can acquire
	/// additional read and write locks without waiting on itself.
	@Nested
	@Order(4)
	@TestMethodOrder(OrderAnnotation.class)
	public class ActiveWriterTests {
		/// Creates a new instance of this class.
		public ActiveWriterTests() {}

		/// Makes sure the read and write lock methods are implemented before running
		/// any of the active writer tests.
		@BeforeAll
		public static void activeWriterImplemented() {
			ReadLockTests.readLockImplemented();
			WriteLockTests.writeLockImplemented();
		}

		/// Tests that the active writer can also acquire the read lock.
		@Test
		@Order(1)
		public void testWriterCanRead() {
			MultiReaderLock lock = new MultiReaderLock();

			assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
				lock.writeLock().lock();
				lock.readLock().lock();

				int readers = lock.readers();
				int writers = lock.writers();

				lock.readLock().unlock();
				lock.writeLock().unlock();

				assertAll(
						() -> assertEquals(1, readers, "The active writer should also be an active reader."),
						() -> assertEquals(1, writers, "The active writer should still be the only active writer."),
						() -> assertEquals(0, lock.readers()),
						() -> assertEquals(0, lock.writers()));
			}, "The active writer should not wait to acquire the read lock.");
		}

		/// Tests that the active writer can acquire the write lock again, and stays the
		/// active writer until it has unlocked the write lock as many times as it
		/// locked it.
		@Test
		@Order(2)
		public void testWriterCanWriteAgain() {
			MultiReaderLock lock = new MultiReaderLock();

			assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
				lock.writeLock().lock();
				lock.writeLock().lock();

				int nested = lock.writers();
				lock.writeLock().unlock();

				int remaining = lock.writers();
				boolean stillActive = lock.isActiveWriter();
				lock.writeLock().unlock();

				assertAll(
						() -> assertEquals(2, nested, "The active writer should hold the write lock twice."),
						() -> assertEquals(1, remaining, "The active writer should still hold the write lock once."),
						() -> assertTrue(stillActive, "The thread should stay the active writer until fully unlocked."),
						() -> assertFalse(lock.isActiveWriter(), "The thread should no longer be the active writer."),
						() -> assertEquals(0, lock.writers()));
			}, "The active writer should not wait to acquire the write lock again.");
		}
	}
}
