package com.xuxiaocheng.WList.Client;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserInformation;
import com.xuxiaocheng.WList.Commons.Options.DuplicatePolicy;
import com.xuxiaocheng.WList.Commons.Options.FilterPolicy;
import com.xuxiaocheng.WList.Commons.Options.OrderDirection;
import com.xuxiaocheng.WList.Commons.Utils.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public record ClientConfiguration(int threadCount, int progressStartDelay, int progressInterval,
                                  int limitPerPage, @NotNull FilterPolicy filterPolicy,
                                  @NotNull LinkedHashMap<VisibleFileInformation.@NotNull Order, @NotNull OrderDirection> fileOrders,
                                  @NotNull DuplicatePolicy duplicatePolicy,
                                  @NotNull LinkedHashMap<VisibleUserInformation.@NotNull Order, @NotNull OrderDirection> userOrders,
                                  @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, @NotNull OrderDirection> userGroupOrders,
                                  boolean copyNoTempFile) {
    public static final @NotNull HInitializer<File> Location = new HInitializer<>("ClientConfigurationLocation");
    private static final @NotNull HInitializer<ClientConfiguration> instance = new HInitializer<>("ClientConfiguration");

    public static @NotNull ClientConfiguration get() {
        return ClientConfiguration.instance.getInstance();
    }

    public static void set(final @NotNull ClientConfiguration configuration) throws IOException {
        ClientConfiguration.instance.reinitialize(configuration);
        ClientConfiguration.dumpToFile();
    }

    public static @NotNull ClientConfiguration parse(final @Nullable InputStream stream) throws IOException {
        final Map<String, Object> config = stream == null ? Map.of() : YamlHelper.loadYaml(stream);
        final Collection<Pair.ImmutablePair<String, String>> errors = new LinkedList<>();
        final ClientConfiguration configuration = new ClientConfiguration(
                YamlHelper.getConfig(config, "thread_count", Runtime.getRuntime().availableProcessors(),
                        o -> YamlHelper.transferIntegerFromStr(o, errors, "thread_count", BigInteger.ONE, YamlHelper.IntegerMax)).intValue(),
                YamlHelper.getConfig(config, "progress_start_delay", TimeUnit.MILLISECONDS.toMillis(300),
                        o -> YamlHelper.transferIntegerFromStr(o, errors, "progress_start_delay", BigInteger.ZERO, YamlHelper.IntegerMax)).intValue(),
                YamlHelper.getConfig(config, "progress_interval", TimeUnit.MILLISECONDS.toMillis(500),
                        o -> YamlHelper.transferIntegerFromStr(o, errors, "progress_interval", BigInteger.ONE, YamlHelper.IntegerMax)).intValue(),
                YamlHelper.getConfig(config, "limit_per_page", 20,
                        o -> YamlHelper.transferIntegerFromStr(o, errors, "limit_per_page", BigInteger.ONE, BigInteger.valueOf(100))).intValue(),
                YamlHelper.getConfig(config, "filter_policy", FilterPolicy.Both,
                        o -> YamlHelper.transferEnumFromStr(o, errors, "filter_policy", FilterPolicy.class)),
                YamlHelper.getConfig(config, "file_orders", () -> {
                    final LinkedHashMap<VisibleFileInformation.Order, OrderDirection> orders = new LinkedHashMap<>();
                    orders.put(VisibleFileInformation.Order.Directory, OrderDirection.DESCEND);
                    orders.put(VisibleFileInformation.Order.Name, OrderDirection.ASCEND);
                    return orders;
                }, o -> { final Map<String, Object> map = YamlHelper.transferMapNode(o, errors, "file_orders");
                    if (map == null) return null;
                    final LinkedHashMap<VisibleFileInformation.Order, OrderDirection> orders = new LinkedHashMap<>();
                    for (final Map.Entry<String, Object> e: map.entrySet()) {
                        final VisibleFileInformation.Order order = VisibleFileInformation.Order.of(e.getKey());
                        if (order == null) {
                            HLog.getInstance("DefaultLogger").log(HLogLevel.WARN, "Invalid file order name.", ParametersMap.create().add("name", e.getKey()).add("direction", e.getValue().toString()));
                            continue;
                        }
                        final String d = YamlHelper.transferString(e.getValue(), errors, "file_orders(" + e.getKey() + ')');
                        if (d == null) continue;
                        final OrderDirection direction = OrderDirection.of(d);
                        if (direction == null) {
                            HLog.getInstance("DefaultLogger").log(HLogLevel.WARN, "Invalid file order direction.", ParametersMap.create().add("name", e.getKey()).add("direction", d));
                            continue;
                        }
                        orders.put(order, direction);
                    }
                    return orders;
                }),
                YamlHelper.getConfig(config, "duplicate_policy", DuplicatePolicy.ERROR,
                        o -> YamlHelper.transferEnumFromStr(o, errors, "duplicate_policy", DuplicatePolicy.class)),
                YamlHelper.getConfig(config, "user_orders", () -> {
                    final LinkedHashMap<VisibleUserInformation.Order, OrderDirection> orders = new LinkedHashMap<>();
                    orders.put(VisibleUserInformation.Order.Id, OrderDirection.ASCEND);
                    return orders;
                }, o -> { final Map<String, Object> map = YamlHelper.transferMapNode(o, errors, "file_orders");
                    if (map == null) return null;
                    final LinkedHashMap<VisibleUserInformation.Order, OrderDirection> orders = new LinkedHashMap<>();
                    for (final Map.Entry<String, Object> e: map.entrySet()) {
                        final VisibleUserInformation.Order order = VisibleUserInformation.Order.of(e.getKey());
                        if (order == null) {
                            HLog.getInstance("DefaultLogger").log(HLogLevel.WARN, "Invalid user order name.", ParametersMap.create().add("name", e.getKey()).add("direction", e.getValue().toString()));
                            continue;
                        }
                        final String d = YamlHelper.transferString(e.getValue(), errors, "user_orders(" + e.getKey() + ')');
                        if (d == null) continue;
                        final OrderDirection direction = OrderDirection.of(d);
                        if (direction == null) {
                            HLog.getInstance("DefaultLogger").log(HLogLevel.WARN, "Invalid user order direction.", ParametersMap.create().add("name", e.getKey()).add("direction", d));
                            continue;
                        }
                        orders.put(order, direction);
                    }
                    return orders;
                }),
                YamlHelper.getConfig(config, "user_group_orders", () -> {
                    final LinkedHashMap<VisibleUserGroupInformation.Order, OrderDirection> orders = new LinkedHashMap<>();
                    orders.put(VisibleUserGroupInformation.Order.Id, OrderDirection.ASCEND);
                    return orders;
                }, o -> { final Map<String, Object> map = YamlHelper.transferMapNode(o, errors, "user_group_orders");
                    if (map == null) return null;
                    final LinkedHashMap<VisibleUserGroupInformation.Order, OrderDirection> orders = new LinkedHashMap<>();
                    for (final Map.Entry<String, Object> e: map.entrySet()) {
                        final VisibleUserGroupInformation.Order order = VisibleUserGroupInformation.Order.of(e.getKey());
                        if (order == null) {
                            HLog.getInstance("DefaultLogger").log(HLogLevel.WARN, "Invalid user group order name.", ParametersMap.create().add("name", e.getKey()).add("direction", e.getValue().toString()));
                            continue;
                        }
                        final String d = YamlHelper.transferString(e.getValue(), errors, "user_group_orders(" + e.getKey() + ')');
                        if (d == null) continue;
                        final OrderDirection direction = OrderDirection.of(d);
                        if (direction == null) {
                            HLog.getInstance("DefaultLogger").log(HLogLevel.WARN, "Invalid user group order direction.", ParametersMap.create().add("name", e.getKey()).add("direction", d));
                            continue;
                        }
                        orders.put(order, direction);
                    }
                    return orders;
                }),
                YamlHelper.getConfig(config, "copy_no_temp_file", true,
                        o -> YamlHelper.transferBooleanFromStr(o, errors, "copy_no_temp_file")).booleanValue()
        );
        YamlHelper.throwErrors(errors);
        return configuration;
    }

    public static void dump(final @NotNull ClientConfiguration configuration, final @NotNull OutputStream stream) throws IOException {
        final Map<String, Object> config = new LinkedHashMap<>();
        config.put("thread_count", configuration.threadCount);
        config.put("progress_start_delay", configuration.progressStartDelay);
        config.put("progress_interval", configuration.progressInterval);
        config.put("limit_per_page", configuration.limitPerPage);
        config.put("filter_policy", configuration.filterPolicy.name());
        config.put("file_orders", configuration.fileOrders.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().name(), e -> e.getValue().name(), (a, b) -> a, LinkedHashMap::new)));
        config.put("duplicate_policy", configuration.duplicatePolicy.name());
        config.put("user_orders", configuration.userOrders.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().name(), e -> e.getValue().name(), (a, b) -> a, LinkedHashMap::new)));
        config.put("user_group_orders", configuration.userGroupOrders.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().name(), e -> e.getValue().name(), (a, b) -> a, LinkedHashMap::new)));
        config.put("copy_no_temp_file", configuration.copyNoTempFile);
        YamlHelper.dumpYaml(config, stream);
    }

    public static synchronized void parseFromFile() throws IOException {
        final File file = ClientConfiguration.Location.getInstanceNullable();
        if (file == null) {
            ClientConfiguration.set(ClientConfiguration.parse(null));
            return;
        }
        HFileHelper.ensureFileAccessible(file, true);
        final ClientConfiguration configuration;
        try (final InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
            configuration = ClientConfiguration.parse(stream);
        }
        ClientConfiguration.set(configuration);
    }

    public static synchronized void dumpToFile() throws IOException {
        final File file = ClientConfiguration.Location.getInstanceNullable();
        if (file == null) return;
        final ClientConfiguration configuration = ClientConfiguration.instance.getInstance();
        HFileHelper.writeFileAtomically(file, stream -> ClientConfiguration.dump(configuration, stream));
    }

    public static void quicklySetLocation(final @NotNull File file) throws IOException {
        ClientConfiguration.Location.reinitialize(file);
        try {
            ClientConfiguration.parseFromFile();
        } catch (final IOException exception) {
            try {
                ClientConfiguration.set(ClientConfiguration.parse(null));
            } catch (final IOException e) {
                exception.addSuppressed(e);
                ClientConfiguration.Location.uninitializeNullable();
            }
            throw exception;
        }
    }
}
