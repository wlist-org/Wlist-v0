package com.xuxiaocheng.WListAndroid.UIs.Fragments.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.SimpleAdapter;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.ListPopupWindow;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.AndroidSupports.ClientConfigurationSupporter;
import com.xuxiaocheng.WList.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WList.Client.ClientConfiguration;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Options.OrderDirection;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.FragmentsAdapter;
import com.xuxiaocheng.WListAndroid.UIs.IFragmentPart;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileOptionsRefreshBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileOptionsSorterBinding;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class PartOptions extends IFragmentPart<PageFileBinding, FragmentFile> {
    protected PartOptions(final @NotNull FragmentFile fragment) {
        super(fragment);
    }

    @Override
    protected void onPositionChanged(@NotNull final ActivityMain activity, final FragmentsAdapter.@NotNull FragmentTypes position) {
        super.onPositionChanged(activity, position);
        if (position == FragmentsAdapter.FragmentTypes.File)
            activity.getContent().activityMainOptions.setVisibility(View.VISIBLE);
        else
            activity.getContent().activityMainOptions.setVisibility(View.GONE);
    }

    @Override
    protected void onBuild(@NotNull final PageFileBinding page) {
        super.onBuild(page);
        this.activity().getContent().activityMainOptions.setOnClickListener(v -> {
            if (this.activity().currentChoice() != FragmentsAdapter.FragmentTypes.File) return;
            final ListPopupWindow popup = new ListPopupWindow(this.activity());
            popup.setWidth(this.page().getRoot().getWidth() >> 1);
            popup.setAnchorView(this.activity().getContent().activityMainOptions);
            final List<Map<String, Object>> list = new ArrayList<>();
            list.add(Map.of("pos", 1, "image", R.drawable.page_file_options_sorter, "name", this.activity().getString(R.string.page_file_options_sorter)));
            list.add(Map.of("pos", 2, "image", R.drawable.page_file_options_filter, "name", this.activity().getString(R.string.page_file_options_filter)));
            if (this.isConnected()) {
                list.add(Map.of("pos", 3, "image", R.drawable.page_file_options_refresh, "name", this.activity().getString(R.string.page_file_options_refresh)));
                list.add(Map.of("pos", 4, "image", R.mipmap.app_logo, "name", this.activity().getString(R.string.page_file_options_disconnect)));
                list.add(Map.of("pos", 5, "image", R.mipmap.app_logo, "name", this.activity().getString(R.string.page_file_options_close_server)));
            }
            popup.setAdapter(new SimpleAdapter(this.activity(), list, R.layout.page_file_options_cell, new String[]{"image", "name"}, new int[]{R.id.activity_main_options_cell_image, R.id.activity_main_options_cell_name}));
            final AtomicBoolean clickable = new AtomicBoolean(true);
            popup.setOnItemClickListener((p, w, pos, i) -> {
                if (!clickable.compareAndSet(true, false)) return;
                popup.dismiss();
                if (!(list.get(pos).get("pos") instanceof Integer position)) return;
                switch (position.intValue()) {
                    case 1 -> this.sort();
                    case 2 -> this.filter();
                    case 3 -> this.refresh();
                    case 4 -> this.activity().disconnect();
                    case 5 -> this.fragment.partConnect.tryDisconnect();
                }
            });
            popup.show();
        });
    }

    @UiThread
    protected void refresh() {
        final PageFileOptionsRefreshBinding refresh = PageFileOptionsRefreshBinding.inflate(this.activity().getLayoutInflater());
        new AlertDialog.Builder(this.activity())
                .setTitle(R.string.page_file_options_refresh)
                .setView(refresh.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (d, h) -> {
//                    if (this.pageFile.partList.isOnRoot()) return;
//                    final FileLocation location = this.pageFile.partList.currentLocation();
//                    final AtomicLong max = new AtomicLong(0);
//                    Main.runOnBackgroundThread(this.pageFile.activity(), HExceptionWrapper.wrapRunnable(() -> {
//                        final VisibleFileInformation information;
//                        try (final WListClientInterface client = this.pageFile.client()) {
//                            information = OperateFilesHelper.getFileOrDirectory(client, this.pageFile.token(), location, true);
//                            if (information == null) return;
//                        }
//                        final String title = MessageFormat.format(this.pageFile.activity().getString(R.string.page_file_options_refresh_title), FileInformationGetter.name(information));
//                        try {
//                            this.pageFile.partList.listLoadingAnimation(title, true, 0, 0);
//                            if (this.pageFile.partList.isOnRoot()) return;
//                            FilesAssistant.refresh(this.pageFile.address(), this.pageFile.username(), location, Main.ClientExecutors, state -> {
//                                final Pair.ImmutablePair<Long, Long> pair = InstantaneousProgressStateGetter.merge(state);
//                                max.set(pair.getSecond().longValue());
//                                this.pageFile.partList.listLoadingAnimation(title, true, pair.getFirst().longValue(), pair.getSecond().longValue());
//                            });
//                        } finally {
//                            this.pageFile.partList.listLoadingAnimation(title, false, max.get(), max.get());
//                        }
//                    }));
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
                        Main.showToast(this.activity(), R.string.page_file_options_sorter_not_chose);
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
                    this.fragment.partList.clearCurrentPosition();
                }))).setNeutralButton(R.string.advance, (d, h) -> this.sortAdvance()).show();
    }

    @UiThread
    protected void sortAdvance() {
        Main.runOnBackgroundThread(this.activity(), () -> {throw new RuntimeException("WIP");}); // TODO
    }

    @UiThread
    protected void filter() {
        Main.runOnBackgroundThread(this.activity(), () -> {throw new RuntimeException("WIP");}); // TODO
    }
}
