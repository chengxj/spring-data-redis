/*
 * Copyright 2014-2016 the original author or authors.
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
package org.springframework.data.redis.connection.lettuce;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.lang.reflect.InvocationTargetException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.redis.connection.AbstractConnectionUnitTestBase;
import org.springframework.data.redis.connection.RedisServerCommands.ShutdownOption;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionUnitTestSuite.LettuceConnectionUnitTests;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionUnitTestSuite.LettucePipelineConnectionUnitTests;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ LettuceConnectionUnitTests.class, LettucePipelineConnectionUnitTests.class })
public class LettuceConnectionUnitTestSuite {

	@SuppressWarnings("rawtypes")
	public static class LettuceConnectionUnitTests extends AbstractConnectionUnitTestBase<RedisAsyncCommands> {

		protected LettuceConnection connection;
		private RedisClient clientMock;
		protected StatefulRedisConnection<byte[], byte[]> statefulConnectionMock;
		protected RedisAsyncCommands<byte[], byte[]> asyncCommandsMock;
		protected RedisCommands syncCommandsMock;

		@SuppressWarnings({ "unchecked" })
		@Before
		public void setUp() throws InvocationTargetException, IllegalAccessException {

			clientMock = mock(RedisClient.class);
			statefulConnectionMock = mock(StatefulRedisConnection.class);
			when(clientMock.connect((RedisCodec) any())).thenReturn(statefulConnectionMock);

			asyncCommandsMock = getNativeRedisConnectionMock();
			syncCommandsMock = mock(RedisCommands.class);

			when(statefulConnectionMock.async()).thenReturn(getNativeRedisConnectionMock());
			when(statefulConnectionMock.sync()).thenReturn(syncCommandsMock);
			connection = new LettuceConnection(0, clientMock);
		}

		/**
		 * @see DATAREDIS-184
		 */
		@Test
		public void shutdownWithNullOptionsIsCalledCorrectly() {

			connection.shutdown(null);
			verify(syncCommandsMock, times(1)).shutdown(true);
		}

		/**
		 * @see DATAREDIS-184
		 */
		@Test
		public void shutdownWithNosaveOptionIsCalledCorrectly() {

			connection.shutdown(ShutdownOption.NOSAVE);
			verify(syncCommandsMock, times(1)).shutdown(false);
		}

		/**
		 * @see DATAREDIS-184
		 */
		@Test
		public void shutdownWithSaveOptionIsCalledCorrectly() {

			connection.shutdown(ShutdownOption.SAVE);
			verify(syncCommandsMock, times(1)).shutdown(true);
		}

		/**
		 * @see DATAREDIS-267
		 */
		@Test
		public void killClientShouldDelegateCallCorrectly() {

			String ipPort = "127.0.0.1:1001";
			connection.killClient("127.0.0.1", 1001);
			verify(syncCommandsMock, times(1)).clientKill(eq(ipPort));
		}

		/**
		 * @see DATAREDIS-270
		 */
		@Test
		public void getClientNameShouldSendRequestCorrectly() {

			connection.getClientName();
			verify(syncCommandsMock, times(1)).clientGetname();
		}

		/**
		 * @see DATAREDIS-277
		 */
		@Test(expected = IllegalArgumentException.class)
		public void slaveOfShouldThrowExectpionWhenCalledForNullHost() {
			connection.slaveOf(null, 0);
		}

		/**
		 * @see DATAREDIS-277
		 */
		@Test
		public void slaveOfShouldBeSentCorrectly() {

			connection.slaveOf("127.0.0.1", 1001);
			verify(syncCommandsMock, times(1)).slaveof(eq("127.0.0.1"), eq(1001));
		}

		/**
		 * @see DATAREDIS-277
		 */
		@Test
		public void slaveOfNoOneShouldBeSentCorrectly() {

			connection.slaveOfNoOne();
			verify(syncCommandsMock, times(1)).slaveofNoOne();
		}

		/**
		 * @see DATAREDIS-348
		 */
		@Test(expected = InvalidDataAccessResourceUsageException.class)
		public void shouldThrowExceptionWhenAccessingRedisSentinelsCommandsWhenNoSentinelsConfigured() {
			connection.getSentinelConnection();
		}

		/**
		 * @see DATAREDIS-431
		 */
		@Test
		public void dbIndexShouldBeSetWhenObtainingConnection() {

			connection = new LettuceConnection(null, 0, clientMock, null, 1);
			connection.getNativeConnection();

			verify(syncCommandsMock, times(1)).select(1);
		}
	}

	public static class LettucePipelineConnectionUnitTests extends LettuceConnectionUnitTests {

		@Override
		@Before
		public void setUp() throws InvocationTargetException, IllegalAccessException {
			super.setUp();
			this.connection.openPipeline();
		}

		/**
		 * @see DATAREDIS-528
		 */
		@Test
		public void shutdownWithSaveOptionIsCalledCorrectly() {

			connection.shutdown(ShutdownOption.SAVE);
			verify(asyncCommandsMock, times(1)).shutdown(true);
		}

		/**
		 * @see DATAREDIS-528
		 */
		@Test
		public void shutdownWithNosaveOptionIsCalledCorrectly() {

			connection.shutdown(ShutdownOption.NOSAVE);
			verify(asyncCommandsMock, times(1)).shutdown(false);
		}

		/**
		 * @see DATAREDIS-528
		 */
		@Test
		public void slaveOfShouldBeSentCorrectly() {

			connection.slaveOf("127.0.0.1", 1001);
			verify(asyncCommandsMock, times(1)).slaveof(eq("127.0.0.1"), eq(1001));
		}

		/**
		 * @see DATAREDIS-528
		 */
		@Test
		public void shutdownWithNullOptionsIsCalledCorrectly() {

			connection.shutdown(null);
			verify(asyncCommandsMock, times(1)).shutdown(true);
		}

		/**
		 * @see DATAREDIS-528
		 */
		@Test
		public void killClientShouldDelegateCallCorrectly() {

			String ipPort = "127.0.0.1:1001";
			connection.killClient("127.0.0.1", 1001);
			verify(asyncCommandsMock, times(1)).clientKill(eq(ipPort));
		}

		/**
		 * @see DATAREDIS-528
		 */
		@Test
		public void slaveOfNoOneShouldBeSentCorrectly() {

			connection.slaveOfNoOne();
			verify(asyncCommandsMock, times(1)).slaveofNoOne();
		}

		/**
		 * @see DATAREDIS-528
		 */
		@Test
		public void getClientNameShouldSendRequestCorrectly() {

			connection.getClientName();
			verify(asyncCommandsMock, times(1)).clientGetname();
		}
	}
}
