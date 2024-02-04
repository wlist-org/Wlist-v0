package com.xuxiaocheng.WListAndroid.UIs.Main.Provider;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.widget.Toast;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.AndroidSupports.StorageTypeGetter;
import com.xuxiaocheng.WList.Server.Storage.Providers.Real.Lanzou.LanzouConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageFileProviderLanzouBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

// TODO
public final class PageProvider {
    private PageProvider() {
        super();
    }

    @SuppressWarnings("unchecked")
    @UiThread
    public static <C extends StorageConfiguration, V extends ViewBinding> void getConfiguration(final @NotNull Activity activity, final @NotNull StorageTypes<C> type, final @Nullable C old, final @NotNull @WorkerThread Consumer<@NotNull C> callback) {
        final ConfigurationGetter<C, V> getter = (ConfigurationGetter<C, V>) PageProvider.map.get(type);
        final V view = Objects.requireNonNull(getter).buildPage(activity, old);
        new AlertDialog.Builder(activity).setTitle(StorageTypeGetter.identifier(type)).setView(view.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (b, h) -> {
                    Main.runOnBackgroundThread(activity, HExceptionWrapper.wrapRunnable(() -> {
                        final Pair.ImmutablePair<C, Boolean> configuration = getter.checkValid(activity, view);
                        if (configuration.getSecond().booleanValue())
                            callback.accept(configuration.getFirst());
                        else
                            Main.runOnUiThread(activity, () -> PageProvider.getConfiguration(activity, type, configuration.getFirst(), callback));
                    }));
                }).show();
    }

    private interface ConfigurationGetter<C extends StorageConfiguration, V extends ViewBinding> {
        @UiThread
        @NotNull V buildPage(@NotNull Activity activity, final @Nullable C old);
        @WorkerThread
        Pair.@NotNull ImmutablePair<@NotNull C, @NotNull Boolean> checkValid(final @NotNull Activity activity, final @NotNull V view);
    }

    private static final @NotNull ConfigurationGetter<LanzouConfiguration, PageFileProviderLanzouBinding> Lanzou = new ConfigurationGetter<>() {
        @Override
        public @NotNull PageFileProviderLanzouBinding buildPage(final @NotNull Activity activity, final @Nullable LanzouConfiguration old) {
            final PageFileProviderLanzouBinding binding = PageFileProviderLanzouBinding.inflate(activity.getLayoutInflater());
            binding.pageFileProviderName.setText(R.string.page_file_provider_lanzou);
            final AtomicBoolean visible = new AtomicBoolean(true);
            binding.pageFileProviderPasswordVisible.setOnClickListener(p -> {
                visible.set(!visible.get());
                binding.pageFileProviderPassword.setInputType(InputType.TYPE_CLASS_TEXT |
                        (visible.get() ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            });
            binding.pageFileProviderPasswordVisible.callOnClick();
            return binding;
        }

        @Override
        public Pair.@NotNull ImmutablePair<@NotNull LanzouConfiguration, @NotNull Boolean> checkValid(final @NotNull Activity activity, final @NotNull PageFileProviderLanzouBinding view) {
            final String name = ViewUtil.getText(view.pageFileProviderName);
            final String passport = ViewUtil.getText(view.pageFileProviderPassport);
            final String password = ViewUtil.getText(view.pageFileProviderPassword);
            final LanzouConfiguration configuration = new LanzouConfiguration();
            final Collection<Object> errors = new ArrayList<>();
            configuration.load(Map.of("name", name, "passport", passport, "password", password, "directly_login", "true"), errors);
            if (!errors.isEmpty()) {
                activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), errors.toString(), Toast.LENGTH_SHORT).show());
                return Pair.ImmutablePair.makeImmutablePair(configuration, Boolean.FALSE);
            }
            configuration.setName(name);
            return Pair.ImmutablePair.makeImmutablePair(configuration, Boolean.TRUE);
        }
    };

    private static final @NotNull Map<StorageTypes<?>, ConfigurationGetter<?, ?>> map = Map.of(
            StorageTypes.Lanzou, PageProvider.Lanzou
    );
}
