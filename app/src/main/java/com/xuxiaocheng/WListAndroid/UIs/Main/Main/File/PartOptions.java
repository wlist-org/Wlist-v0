package com.xuxiaocheng.WListAndroid.UIs.Main.Main.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.SimpleAdapter;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.ListPopupWindow;
import com.hjq.toast.Toaster;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.AndroidSupports.ClientConfigurationSupporter;
import com.xuxiaocheng.WList.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WList.AndroidSupports.FileLocationGetter;
import com.xuxiaocheng.WList.AndroidSupports.InstantaneousProgressStateGetter;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WList.Client.ClientConfiguration;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Options.FilterPolicy;
import com.xuxiaocheng.WList.Commons.Options.OrderDirection;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Main.Main.PageMainAdapter;
import com.xuxiaocheng.WListAndroid.databinding.PageFileOptionsFilterBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileOptionsRefreshBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileOptionsSorterBinding;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class PartOptions extends SFragmentFilePart {
    protected PartOptions(final @NotNull FragmentFile fragment) {
        super(fragment);
    }

    @Override
    protected void sOnTypeChanged(final PageMainAdapter.@NotNull Types type) {
        super.sOnTypeChanged(type);
        this.page().content().activityMainOptions.setVisibility(switch (type) {
            case File -> View.VISIBLE;
            case User -> View.GONE;
        });
    }

    @Override
    protected void iOnBuildPage() {
        super.iOnBuildPage();
        this.pageContent().activityMainOptions.setOnClickListener(v -> {
            if (this.currentFragmentTypes() != PageMainAdapter.Types.File) return;
            final ListPopupWindow popup = new ListPopupWindow(this.activity());
            popup.setWidth(this.pageContent().getRoot().getWidth() >> 1);
            popup.setAnchorView(this.pageContent().activityMainOptions);
            final List<Map<String, Object>> list = new ArrayList<>();
            list.add(Map.of("pos", 1, "image", R.drawable.page_file_options_sorter, "name", this.activity().getString(R.string.page_file_options_sorter)));
            list.add(Map.of("pos", 2, "image", R.drawable.page_file_options_filter, "name", this.activity().getString(R.string.page_file_options_filter)));
            if (this.isConnected()) {
                if (!this.fragment.partList().isOnRoot())
                    list.add(Map.of("pos", 3, "image", R.drawable.page_file_options_refresh, "name", this.activity().getString(R.string.page_file_options_refresh)));
                list.add(Map.of("pos", 4, "image", R.mipmap.app_logo, "name", this.activity().getString(R.string.page_file_options_disconnect)));
                list.add(Map.of("pos", 5, "image", R.mipmap.app_logo, "name", this.activity().getString(R.string.page_file_options_close_server)));
            }
            popup.setAdapter(new SimpleAdapter(this.activity(), list, R.layout.page_file_options_cell, new String[]{"image", "name"}, new int[]{R.id.activity_main_options_cell_image, R.id.activity_main_options_cell_name}));
            final AtomicBoolean clickable = new AtomicBoolean(true);
            popup.setOnItemClickListener((p, w, pos, i) -> {
                if (!clickable.compareAndSet(true, false)) return;
                popup.dismiss();
                if (!(list.get(pos).get("pos") instanceof final Integer position)) return;
                switch (position.intValue()) {
                    case 1 -> this.sort();
                    case 2 -> this.filter();
                    case 3 -> this.refresh();
                    case 4 -> this.activity().disconnect();
                    case 5 -> this.fragment.partConnect().tryCloseServer();
                }
            });
            popup.show();
        });
    }

    @UiThread
    protected void refresh() {
        if (this.fragment.partList().isOnRoot()) return;
        final PageFileOptionsRefreshBinding refresh = PageFileOptionsRefreshBinding.inflate(this.activity().getLayoutInflater());
        new AlertDialog.Builder(this.activity())
                .setTitle(R.string.page_file_options_refresh)
                .setView(refresh.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (d, h) -> {
                    final FileLocation location = this.fragment.partList().currentLocation();
                    Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
                        if (IdentifierNames.RootSelector.equals(FileLocationGetter.storage(location))) return;
                        String title = MessageFormat.format(this.activity().getString(R.string.page_file_options_refresh_title), "");
                        final AtomicLong max = new AtomicLong(0);
                        try {
                            this.fragment.partList().listLoadingAnimation(title, true, 0, 0);
                            final VisibleFileInformation information;
                            try (final WListClientInterface client = this.client()) {
                                information = OperateFilesHelper.getFileOrDirectory(client, this.token(), location, true);
                                if (information == null) return;
                            }
                            title = MessageFormat.format(this.activity().getString(R.string.page_file_options_refresh_title), FileInformationGetter.name(information));
                            this.fragment.partList().listLoadingAnimation(title, true, 0, 0);
                            final String finalTitle = title;
                            FilesAssistant.refresh(this.address(), this.username(), location, Main.ClientExecutors, state -> {
                                final Pair.ImmutablePair<Long, Long> pair = InstantaneousProgressStateGetter.merge(state);
                                max.set(pair.getSecond().longValue()); // TODO: save state.
                                this.fragment.partList().listLoadingAnimation(finalTitle, true, pair.getFirst().longValue(), pair.getSecond().longValue());
                            });
                            Toaster.show(R.string.page_file_options_refresh_success);
                        } finally {
                            this.fragment.partList().listLoadingAnimation(title, false, max.get(), max.get());
                        }
                    }));
                }).show();
    }

    @UiThread
    protected void sort() {
        final PageFileOptionsSorterBinding sorter = PageFileOptionsSorterBinding.inflate(this.activity().getLayoutInflater());
        final SharedPreferences saved = this.activity().getSharedPreferences("page_file_options_sorter", Context.MODE_PRIVATE);
        final AtomicReference<FileInformationGetter.Order> orderPolicy = new AtomicReference<>();
        final AtomicReference<OrderDirection> orderDirection = new AtomicReference<>();
        if (!saved.getBoolean("advanced", false)) {
            orderPolicy.set(Objects.requireNonNullElse(FileInformationGetter.Order.of(saved.getString("policy", "")), FileInformationGetter.Order.Name));
            sorter.pageFileOptionsSorterPolicy.check(switch (orderPolicy.get()) {
                case Size -> R.id.page_file_options_sorter_size;
                case UpdateTime -> R.id.page_file_options_sorter_time;
                default -> R.id.page_file_options_sorter_name;
            });
            orderDirection.set(Objects.requireNonNullElse(OrderDirection.of(saved.getString("direction", "")), OrderDirection.ASCEND));
            sorter.pageFileOptionsSorterDirection.check(switch (orderDirection.get()) {
                case ASCEND -> R.id.page_file_options_sorter_ascend;
                case DESCEND -> R.id.page_file_options_sorter_descend;
            });
        }
        sorter.pageFileOptionsSorterName.setOnClickListener(r -> orderPolicy.set(FileInformationGetter.Order.Name));
        sorter.pageFileOptionsSorterSize.setOnClickListener(r -> orderPolicy.set(FileInformationGetter.Order.Size));
        sorter.pageFileOptionsSorterTime.setOnClickListener(r -> orderPolicy.set(FileInformationGetter.Order.UpdateTime));
        sorter.pageFileOptionsSorterAscend.setOnClickListener(r -> orderDirection.set(OrderDirection.ASCEND));
        sorter.pageFileOptionsSorterDescend.setOnClickListener(r -> orderDirection.set(OrderDirection.DESCEND));
        new AlertDialog.Builder(this.activity())
                .setTitle(R.string.page_file_options_sorter)
                .setView(sorter.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (d, h) -> Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
                    final FileInformationGetter.Order policy = orderPolicy.get();
                    final OrderDirection direction = orderDirection.get();
                    if (policy == null || direction == null) {
                        Toaster.show(R.string.page_file_options_sorter_not_chose);
                        Main.runOnUiThread(this.activity(), this::sort);
                        return;
                    }
                    saved.edit().putBoolean("advanced", false).putString("policy", policy.name()).putString("direction", direction.name()).apply();
                    final LinkedHashMap<VisibleFileInformation.Order, OrderDirection> orders = new LinkedHashMap<>(4);
                    orders.put(FileInformationGetter.Order.Directory.order(), OrderDirection.DESCEND);
                    orders.put(policy.order(), direction);
                    orders.putIfAbsent(FileInformationGetter.Order.Name.order(), direction);
                    orders.putIfAbsent(FileInformationGetter.Order.CreateTime.order(), direction);
                    orders.putIfAbsent(FileInformationGetter.Order.Id.order(), direction);
                    final ClientConfiguration old = ClientConfigurationSupporter.get();
                    ClientConfigurationSupporter.set(new ClientConfiguration(
                            ClientConfigurationSupporter.threadCount(old),
                            ClientConfigurationSupporter.progressStartDelay(old),
                            ClientConfigurationSupporter.progressInterval(old),
                            ClientConfigurationSupporter.limitPerPage(old),
                            ClientConfigurationSupporter.filterPolicy(old),
                            orders,
                            ClientConfigurationSupporter.duplicatePolicy(old),
                            ClientConfigurationSupporter.userOrders(old),
                            ClientConfigurationSupporter.userGroupOrders(old),
                            ClientConfigurationSupporter.copyNoTempFile(old)));
                    PartList.comparator.uninitializeNullable();
                    this.fragment.partList().clearCurrentPosition();
                }))).setNeutralButton(R.string.advance, (d, h) -> this.sortAdvance()).show();
    }

    @UiThread
    protected void sortAdvance() {
        Main.runOnBackgroundThread(this.activity(), () -> {throw new RuntimeException("WIP");}); // TODO
    }

    @UiThread
    protected void filter() {
        final PageFileOptionsFilterBinding filter = PageFileOptionsFilterBinding.inflate(this.activity().getLayoutInflater());
        final SharedPreferences saved = this.activity().getSharedPreferences("page_file_options_filter", Context.MODE_PRIVATE);
        //noinspection NumericCastThatLosesPrecision
        final AtomicReference<FilterPolicy> filterPolicy = new AtomicReference<>(Objects.requireNonNullElse(FilterPolicy.of(
                (byte) saved.getInt("policy", FilterPolicy.Both.ordinal())), FilterPolicy.Both));
        filter.pageFileOptionsFilterPolicy.check(switch (filterPolicy.get()) {
            case Both -> R.id.page_file_options_filter_both;
            case OnlyDirectories -> R.id.page_file_options_filter_directory;
            case OnlyFiles -> R.id.page_file_options_filter_file;
        });
        filter.pageFileOptionsFilterBoth.setOnClickListener(r -> filterPolicy.set(FilterPolicy.Both));
        filter.pageFileOptionsFilterDirectory.setOnClickListener(r -> filterPolicy.set(FilterPolicy.OnlyDirectories));
        filter.pageFileOptionsFilterFile.setOnClickListener(r -> filterPolicy.set(FilterPolicy.OnlyFiles));
        new AlertDialog.Builder(this.activity())
                .setTitle(R.string.page_file_options_filter)
                .setView(filter.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (d, h) -> Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
                    final FilterPolicy policy = filterPolicy.get();
                    saved.edit().putInt("policy", policy.ordinal()).apply();
                    final ClientConfiguration old = ClientConfigurationSupporter.get();
                    ClientConfigurationSupporter.set(new ClientConfiguration(
                            ClientConfigurationSupporter.threadCount(old),
                            ClientConfigurationSupporter.progressStartDelay(old),
                            ClientConfigurationSupporter.progressInterval(old),
                            ClientConfigurationSupporter.limitPerPage(old),
                            policy,
                            ClientConfigurationSupporter.fileOrders(old),
                            ClientConfigurationSupporter.duplicatePolicy(old),
                            ClientConfigurationSupporter.userOrders(old),
                            ClientConfigurationSupporter.userGroupOrders(old),
                            ClientConfigurationSupporter.copyNoTempFile(old)));
                    this.fragment.partList().clearCurrentPosition();
                }))).show();
    }
}
