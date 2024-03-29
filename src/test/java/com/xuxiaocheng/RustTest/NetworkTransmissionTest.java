package com.xuxiaocheng.RustTest;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.Rust.NetworkTransmission;
import com.xuxiaocheng.StaticLoader;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Execution(ExecutionMode.CONCURRENT)
public class NetworkTransmissionTest {
    @BeforeAll
    public static void initialize() {
        StaticLoader.load();
        NetworkTransmission.load();
        HLog.LoggerCreateCore.reinitialize(n -> HLog.createInstance(n, HLogLevel.DEBUG.getLevel(), false));
        HLog.create("DefaultLogger");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void test(final boolean nativeStart) {
        NetworkTransmission.setClientUseNativeStart(nativeStart);
        final Pair.ImmutablePair<NetworkTransmission.RsaPrivateKey, ByteBuf> request = NetworkTransmission.clientStart();
        final Pair.ImmutablePair<ByteBuf, NetworkTransmission.AesKeyPair> response = NetworkTransmission.serverStart(request.getSecond(), "WList");
        final UnionPair<NetworkTransmission.AesKeyPair, UnionPair<String, String>> check = NetworkTransmission.clientCheck(request.getFirst(), response.getFirst(), "WList");
        final NetworkTransmission.AesKeyPair client = Objects.requireNonNull(check).getT(), server = Objects.requireNonNull(response.getSecond());
        for (int i = 0; i < 10; ++i) {
            final ByteBuf c1 = ByteBufAllocator.DEFAULT.buffer().writeBytes(("Wlist test. (from client): " + i).getBytes(StandardCharsets.UTF_8));
            final ByteBuf c2 = Objects.requireNonNull(NetworkTransmission.clientEncrypt(client, c1));
            final ByteBuf c3 = Objects.requireNonNull(NetworkTransmission.serverDecrypt(server, c2));
            if (!Arrays.equals(ByteBufIOUtil.allToByteArray(c1), ByteBufIOUtil.allToByteArray(c3))) throw new AssertionError();
            c1.release(); c2.release(); c3.release();

            final ByteBuf s1 = ByteBufAllocator.DEFAULT.buffer().writeBytes(("Wlist test. (from server): " + i).getBytes(StandardCharsets.UTF_8));
            final ByteBuf s2 = Objects.requireNonNull(NetworkTransmission.serverEncrypt(server, s1));
            final ByteBuf s3 = Objects.requireNonNull(NetworkTransmission.clientDecrypt(client, s2));
            if (!Arrays.equals(ByteBufIOUtil.allToByteArray(s1), ByteBufIOUtil.allToByteArray(s3))) throw new AssertionError();
            s1.release(); s2.release(); s3.release();
        }
    }

    @Test
    public void empty() {
        final Pair.ImmutablePair<NetworkTransmission.RsaPrivateKey, ByteBuf> request = NetworkTransmission.clientStart();
        final Pair.ImmutablePair<ByteBuf, NetworkTransmission.AesKeyPair> response = NetworkTransmission.serverStart(request.getSecond(), "WList");
        final UnionPair<NetworkTransmission.AesKeyPair, UnionPair<String, String>> check = NetworkTransmission.clientCheck(request.getFirst(), response.getFirst(), "WList");
        final NetworkTransmission.AesKeyPair client = Objects.requireNonNull(check).getT(), server = Objects.requireNonNull(response.getSecond());

        Objects.requireNonNull(NetworkTransmission.clientEncrypt(client, Unpooled.EMPTY_BUFFER)).release();
        Assertions.assertNull(NetworkTransmission.serverDecrypt(server, Unpooled.EMPTY_BUFFER));
        Objects.requireNonNull(NetworkTransmission.serverEncrypt(server, Unpooled.EMPTY_BUFFER)).release();
        Assertions.assertNull(NetworkTransmission.clientDecrypt(client, Unpooled.EMPTY_BUFFER));

        final ByteBuf buffer = ByteBufAllocator.DEFAULT.compositeBuffer().addComponents(true, Unpooled.EMPTY_BUFFER,
                ByteBufAllocator.DEFAULT.buffer().writeBytes("Wlist test.".getBytes(StandardCharsets.UTF_8)),
                Unpooled.EMPTY_BUFFER);
        Objects.requireNonNull(NetworkTransmission.clientEncrypt(client, buffer)).release();
        Objects.requireNonNull(NetworkTransmission.serverEncrypt(server, buffer)).release();
        buffer.release();

        final ByteBuf empty = ByteBufAllocator.DEFAULT.compositeBuffer().addComponents(true, Unpooled.EMPTY_BUFFER, Unpooled.EMPTY_BUFFER);
        Objects.requireNonNull(NetworkTransmission.clientEncrypt(client, empty)).release();
        Objects.requireNonNull(NetworkTransmission.serverEncrypt(server, empty)).release();
        empty.release();
    }

    @Test
    public void multiEncrypt() {
        final Pair.ImmutablePair<NetworkTransmission.RsaPrivateKey, ByteBuf> request = NetworkTransmission.clientStart();
        final Pair.ImmutablePair<ByteBuf, NetworkTransmission.AesKeyPair> response = NetworkTransmission.serverStart(request.getSecond(), "WList");
        final UnionPair<NetworkTransmission.AesKeyPair, UnionPair<String, String>> check = NetworkTransmission.clientCheck(request.getFirst(), response.getFirst(), "WList");
        final NetworkTransmission.AesKeyPair client = Objects.requireNonNull(check).getT(), server = Objects.requireNonNull(response.getSecond());
        for (int i = 0; i < 10; ++i) {
            final ByteBuf c1 = ByteBufAllocator.DEFAULT.compositeBuffer().addComponents(true,
                    ByteBufAllocator.DEFAULT.buffer().writeBytes(("Wlist test. (from client): " + i).getBytes(StandardCharsets.UTF_8)),
                    ByteBufAllocator.DEFAULT.buffer().writeBytes(("Double message: " + i).getBytes(StandardCharsets.UTF_8))
            );
            final ByteBuf c2 = Objects.requireNonNull(NetworkTransmission.clientEncrypt(client, c1));
            final ByteBuf c3 = Objects.requireNonNull(NetworkTransmission.serverDecrypt(server, c2));
            if (!Arrays.equals(ByteBufIOUtil.allToByteArray(c1), ByteBufIOUtil.allToByteArray(c3))) throw new AssertionError();
            c1.release(); c2.release(); c3.release();

            final ByteBuf s1 = ByteBufAllocator.DEFAULT.compositeBuffer().addComponents(true,
                    ByteBufAllocator.DEFAULT.buffer().writeBytes(("Wlist test. (from server): " + i).getBytes(StandardCharsets.UTF_8)),
                    ByteBufAllocator.DEFAULT.buffer().writeBytes(("Double message: " + i).getBytes(StandardCharsets.UTF_8))
            );
            final ByteBuf s2 = Objects.requireNonNull(NetworkTransmission.serverEncrypt(server, s1));
            final ByteBuf s3 = Objects.requireNonNull(NetworkTransmission.clientDecrypt(client, s2));
            if (!Arrays.equals(ByteBufIOUtil.allToByteArray(s1), ByteBufIOUtil.allToByteArray(s3))) throw new AssertionError();
            s1.release(); s2.release(); s3.release();
        }
    }

    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Fork(1)
    @Threads(Threads.MAX)
    @Warmup(iterations = 3, time = 10, timeUnit = TimeUnit.MILLISECONDS, batchSize = 1)
    @Measurement(iterations = 50, time = 10, timeUnit = TimeUnit.MILLISECONDS, batchSize = 1)
    public static class SpeedTest {
        @Test
        public void test() throws RunnerException {
            new Runner(new OptionsBuilder()
                    .include(SpeedTest.class.getSimpleName())
                    .result("./test/network_transmission.json")
                    .resultFormat(ResultFormatType.JSON)
                    .build()).run();
        }

        @Benchmark
        public void rust(final @NotNull Blackhole blackhole) {
            NetworkTransmission.setClientUseNativeStart(true);
            final Pair.ImmutablePair<NetworkTransmission.RsaPrivateKey, ByteBuf> pair = NetworkTransmission.clientStart();
            pair.getSecond().release();
            blackhole.consume(pair);
        }

        @Benchmark
        public void java(final @NotNull Blackhole blackhole) {
            NetworkTransmission.setClientUseNativeStart(false);
            final Pair.ImmutablePair<NetworkTransmission.RsaPrivateKey, ByteBuf> pair = NetworkTransmission.clientStart();
            pair.getSecond().release();
            blackhole.consume(pair);
        }
    }
}
