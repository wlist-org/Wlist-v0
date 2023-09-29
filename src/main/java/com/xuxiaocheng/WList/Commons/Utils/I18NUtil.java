package com.xuxiaocheng.WList.Commons.Utils;

import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public final class I18NUtil {
    private I18NUtil() {
        super();
    }

    public static final @NotNull HInitializer<ResourceBundle> Resources = new HInitializer<>("InternationalizationResourceBundle",
            ResourceBundle.getBundle("lang/wlist", new UTF8Control()));

    public static @NotNull @Nls String get(final @NotNull String identifier, final @Nullable Object @Nullable ... args) {
        return MessageFormat.format(I18NUtil.Resources.getInstance().getString(identifier), args);
    }

    public static class UTF8Control extends ResourceBundle.Control {
        @Override
        public @Nullable ResourceBundle newBundle(final String baseName, final Locale locale, final String format, final ClassLoader loader, final boolean reload) throws IllegalAccessException, InstantiationException, IOException {
            final String bundleName = this.toBundleName(baseName, locale);
            switch (format) {
                case "java.properties" -> {
                    if (bundleName.contains("://"))
                        return null;
                    final String resourceName = this.toResourceName(bundleName, "properties");
                    InputStream stream = null;
                    if (reload) {
                        final URL url = loader.getResource(resourceName);
                        if (url != null) {
                            final URLConnection connection = url.openConnection();
                            connection.setUseCaches(false);
                            stream = connection.getInputStream();
                        }
                    } else
                        stream = loader.getResourceAsStream(resourceName);
                    if (stream == null)
                        return null;
                    try (final Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                        return new PropertyResourceBundle(reader);
                    }
                }
                case "java.class" -> {
                    try {
                        return (ResourceBundle) loader.loadClass(bundleName).getConstructor().newInstance();
                    } catch (final ClassNotFoundException | NoSuchMethodException | InvocationTargetException exception) {
                        throw new RuntimeException(exception);
                    }
                }
                default -> throw new IllegalArgumentException("Unknown resource format: " + format);
            }
        }
    }
}
