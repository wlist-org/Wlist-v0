package com.xuxiaocheng.WListClientAndroid.Client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.HeadLibs.Initializer.HInitializer;
import com.xuxiaocheng.WListClient.Utils.YamlHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class PasswordManager {
    private PasswordManager() {
        super();
    }

    @NonNull private static final Map<String, String> InternalPasswords = new ConcurrentHashMap<>();

    public static void registerInternalPassword(@NonNull final String username, @NonNull final String password) throws IOException {
        PasswordManager.InternalPasswords.put(username, password);
        PasswordManager.dumpToFile();
    }

    @Nullable public static String getInternalPassword(@NonNull final String username) {
        return PasswordManager.InternalPasswords.get(username);
    }

    @NonNull private static final HInitializer<File> directory = new HInitializer<>("PasswordManagerDirectory");

    public static void initialize(@NonNull final File directory) throws IOException {
        try {
            PasswordManager.directory.initializeIfNot(HExceptionWrapper.wrapSupplier(() -> {
                final File file = new File(directory, "internal_password.yaml");
                if (!HFileHelper.ensureFileExist(file))
                    throw new IllegalStateException("Failed to create internal server password saver file." + ParametersMap.create().add("file", file));
                final Map<String, Object> map;
                try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
                    map = YamlHelper.loadYaml(inputStream);
                } catch (final FileNotFoundException exception) {
                    throw new RuntimeException("unreachable!", exception);
                }
                PasswordManager.InternalPasswords.putAll(map.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())));
                return directory;
            }));
        } catch (final RuntimeException exception) {
            HExceptionWrapper.unwrapException(exception, IOException.class);
        }
    }

    private static void dumpToFile() throws IOException {
        final File file = new File(PasswordManager.directory.getInstance(), "internal_password.yaml");
        if (!HFileHelper.ensureFileExist(file))
            throw new IllegalStateException("Failed to create internal server password saver file." + ParametersMap.create().add("file", file));
        final Map<String, Object> map = PasswordManager.InternalPasswords.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
            YamlHelper.dumpYaml(map, outputStream);
        } catch (final FileNotFoundException exception) {
            throw new RuntimeException("unreachable!", exception);
        }
    }



    @NonNull private static final Map<InetSocketAddress, Map<String, String>> Passwords = new ConcurrentHashMap<>();

    public static void registerPassword(@NonNull final InetSocketAddress address, @NonNull final String username, @NonNull final String password) {
        PasswordManager.Passwords.computeIfAbsent(address, k -> new ConcurrentHashMap<>()).put(username, password);
    }

    @Nullable public static String getPassword(@NonNull final InetSocketAddress address, @NonNull final String username) {
        final Map<String, String> map = PasswordManager.Passwords.get(address);
        if (map == null) return null;
        return map.get(username);
    }
}
