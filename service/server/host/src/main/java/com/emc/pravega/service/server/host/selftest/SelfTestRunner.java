/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.emc.pravega.service.server.host.selftest;

import ch.qos.logback.classic.LoggerContext;
import com.emc.pravega.common.io.StreamHelpers;
import com.emc.pravega.common.util.PropertyBag;
import com.emc.pravega.service.server.store.ServiceBuilderConfig;
import lombok.Cleanup;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Main entry point for Self Tester.
 */
public class SelfTestRunner {
    public static void main(String[] args) throws Exception {
       // test();
        // Configure slf4j to not log anything (console or whatever). This interferes with the console interaction.
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        //context.getLoggerList().get(0).setLevel(Level.TRACE);
        context.reset();

        TestConfig testConfig = getTestConfig();
        ServiceBuilderConfig builderConfig = getBuilderConfig();

        // Create a new SelfTest.
        @Cleanup
        SelfTest test = new SelfTest(testConfig, builderConfig);

        // Star the test.
        test.startAsync().awaitRunning(testConfig.getTimeout().toMillis(), TimeUnit.MILLISECONDS);

        // Wait for the test to finish.
        test.awaitFinished().join();

        // Make sure the test is stopped.
        test.stopAsync().awaitTerminated();
    }

    private static void test() throws Exception {
        TruncateableArray ca = new TruncateableArray();
        for (int i = 0; i < 10; i++) {
            byte[] data = ("data_" + i).getBytes();
            ca.append(data);
        }
        for (int i = 0; i < ca.getLength() / 2; i++) {
            ca.truncate(1);
            int length = ca.getLength() - 2 * i;
            byte[] data = new byte[length];
            StreamHelpers.readAll(ca.getReader(i, length), data, 0, length);
            System.out.println(String.format("%d: %s", i, new String(data)));
        }

        //        TruncateableArray ca = new TruncateableArray();
        //        for (int i = 0; i < 100; i += 5) {
        //            byte[] data = new byte[5];
        //            data[0] = (byte) i;
        //            data[1] = (byte) (i + 1);
        //            data[2] = (byte) (i + 2);
        //            data[3] = (byte) (i + 3);
        //            data[4] = (byte) (i + 4);
        //            ca.append(new ByteArrayInputStream(data), data.length);
        //        }
        //
        //        for(int i=0;i<13;i++){
        //            ca.truncate(1);
        //        }
        //
        //        for (int i = 0; i < ca.getLength(); i++) {
        //            System.out.println(i + ": " + ca.get(i));
        //        }
        //
        //        CircularArray a = new CircularArray(20 * 1024 * 1024);
        //        AppendContentGenerator acg = new AppendContentGenerator(12345);
        //        for (int i = 0; i < 1000; i++) {
        //            int length = AppendContentGenerator.HEADER_LENGTH + i;
        //            byte[] data = acg.newAppend(length);
        //            a.append(new ByteArrayInputStream(data), data.length);
        //        }
        //
        //        int offset = 0;
        //        while (offset < a.getLength()) {
        //            AppendContentGenerator.ValidationResult vr = AppendContentGenerator.validate(a, offset);
        //            System.out.println(String.format("Offset: %s, Validation = %s", offset, vr));
        //            if (vr.isFailed() || vr.isMoreDataNeeded()) {
        //                break;
        //            }
        //
        //            offset += vr.getLength();
        //        }
    }

    private static ServiceBuilderConfig getBuilderConfig() {
        return ServiceBuilderConfig.getDefaultConfig();
    }

    private static TestConfig getTestConfig() {
        return new TestConfig(TestConfig.convert(TestConfig.COMPONENT_CODE,
                PropertyBag.create()
                           .with(TestConfig.PROPERTY_SEGMENT_COUNT, 1)
                           .with(TestConfig.PROPERTY_PRODUCER_COUNT, 1)
                           .with(TestConfig.PROPERTY_OPERATION_COUNT, 1000)
                           .with(TestConfig.PROPERTY_MIN_APPEND_SIZE, 100)
                           .with(TestConfig.PROPERTY_MAX_APPEND_SIZE, 1024)
                           .with(TestConfig.PROPERTY_MAX_TRANSACTION_SIZE, 20)
                           .with(TestConfig.PROPERTY_TRANSACTION_FREQUENCY, 30)
                           .with(TestConfig.PROPERTY_THREAD_POOL_SIZE, 50)
                           .with(TestConfig.PROPERTY_TIMEOUT_MILLIS, 3000)));
    }
}
