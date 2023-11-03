package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Provider;

public class PageProvider {
//    @SuppressWarnings("unchecked")
//    @UiThread
//    public static <C extends StorageConfiguration, V extends ViewBinding> void getConfiguration(final @NotNull Activity activity, final @NotNull StorageTypes<C> type, final @Nullable C old, final @NotNull @WorkerThread Consumer<@NotNull C> callback) {
//        final ConfigurationGetter<C, V> getter = (ConfigurationGetter<C, V>) PageFileProviderConfigurations.map.get(type);
//        final V view = Objects.requireNonNull(getter).buildPage(activity, old);
//        new AlertDialog.Builder(activity).setTitle(StorageTypeGetter.identifier(type)).setView(view.getRoot())
//                .setNegativeButton(R.string.cancel, null)
//                .setPositiveButton(R.string.confirm, (b, h) -> {
//                    Main.runOnBackgroundThread(activity, HExceptionWrapper.wrapRunnable(() -> {
//                        final Pair.ImmutablePair<C, Boolean> configuration = getter.checkValid(activity, view);
//                        if (configuration.getSecond().booleanValue())
//                            callback.accept(configuration.getFirst());
//                        else
//                            Main.runOnUiThread(activity, () -> PageFileProviderConfigurations.getConfiguration(activity, type, configuration.getFirst(), callback));
//                    }));
//                }).show();
//    }
//
//    private interface ConfigurationGetter<C extends StorageConfiguration, V extends ViewBinding> {
//        @UiThread
//        @NotNull V buildPage(@NotNull Activity activity, final @Nullable C old);
//        @WorkerThread
//        Pair.@NotNull ImmutablePair<@NotNull C, @NotNull Boolean> checkValid(final @NotNull Activity activity, final @NotNull V view);
//    }
//
//    // TODO
//    private static final @NotNull ConfigurationGetter<LanzouConfiguration, PageFileProviderLanzouBinding> Lanzou = new ConfigurationGetter<>() {
//        @Override
//        public @NotNull PageFileProviderLanzouBinding buildPage(final @NotNull Activity activity, final @Nullable LanzouConfiguration old) {
//            final PageFileProviderLanzouBinding binding = PageFileProviderLanzouBinding.inflate(activity.getLayoutInflater());
//            binding.pageFileProviderName.setText(R.string.page_file_provider_lanzou);
//            return binding;
//        }
//
//        @Override
//        public Pair.@NotNull ImmutablePair<@NotNull LanzouConfiguration, @NotNull Boolean> checkValid(final @NotNull Activity activity, final @NotNull PageFileProviderLanzouBinding view) {
//            final String name = ViewUtil.getText(view.pageFileProviderName);
//            final String passport = ViewUtil.getText(view.pageFileProviderPassport);
//            final String password = ViewUtil.getText(view.pageFileProviderPassword);
//            final LanzouConfiguration configuration = new LanzouConfiguration();
//            final Collection<Object> errors = new ArrayList<>();
//            configuration.load(Map.of("name", name, "passport", passport, "password", password, "directly_login", "true"), errors);
//            if (!errors.isEmpty()) {
//                activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), errors.toString(), Toast.LENGTH_SHORT).show());
//                return Pair.ImmutablePair.makeImmutablePair(configuration, Boolean.FALSE);
//            }
//            return Pair.ImmutablePair.makeImmutablePair(configuration, Boolean.TRUE);
//        }
//    };
//
//    private static final @NotNull Map<StorageTypes<?>, ConfigurationGetter<?, ?>> map = Map.of(
//            StorageTypes.Lanzou, PageFileProviderConfigurations.Lanzou
//    );
}
