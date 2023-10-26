package com.xuxiaocheng.WListAndroid.UIs.Pages.File;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.AndroidSupports.ClientConfigurationSupporter;
import com.xuxiaocheng.WList.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WList.AndroidSupports.FileLocationGetter;
import com.xuxiaocheng.WList.AndroidSupports.InstantaneousProgressStateGetter;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WList.Client.ClientConfiguration;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Options.OrderDirection;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.databinding.PageFileOptionsRefreshBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileOptionsSorterBinding;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class PageFilePartOptions {
    protected final @NotNull PageFile pageFile;

    public PageFilePartOptions(final @NotNull PageFile pageFile) {
        super();
        this.pageFile = pageFile;
    }

    private @NotNull ActivityMain activity() {
        return this.pageFile.getMainActivity();
    }

    
    @UiThread
    protected void refresh() {
        final PageFileOptionsRefreshBinding refresh = PageFileOptionsRefreshBinding.inflate(this.activity().getLayoutInflater());
        new AlertDialog.Builder(this.activity())
                .setTitle(R.string.page_file_options_refresh)
                .setView(refresh.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (d, h) -> {
                    final FileLocation location = this.pageFile.partList.currentLocation();
                    final AtomicLong max = new AtomicLong(0);
                    Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
                        this.pageFile.partList.listLoadingAnimation(true, 0, 0);
                        if (this.pageFile.partList.isOnRoot()) return;
                        FilesAssistant.refresh(this.activity().address(), this.activity().username(), location, Main.ClientExecutors, state -> {
                            final Pair.ImmutablePair<Long, Long> pair = InstantaneousProgressStateGetter.merge(state);
                            max.set(pair.getSecond().longValue());
                            this.pageFile.partList.listLoadingAnimation(true, pair.getFirst().longValue(), pair.getSecond().longValue());
                        });
                    }, () -> this.pageFile.partList.listLoadingAnimation(false, max.get(), max.get())));
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
                    final FileLocation location = this.pageFile.partList.currentLocation();
                    if (IdentifierNames.RootSelector.equals(FileLocationGetter.storage(location)))
                        Main.runOnUiThread(this.activity(), () -> this.pageFile.partList.onRootPage(this.pageFile.partList.getCurrentPosition()));
                    else
                        Main.runOnUiThread(this.activity(), () -> this.pageFile.partList.onInsidePage(this.pageFile.getPage().pageFileName.getText(), location, this.pageFile.partList.getCurrentPosition()));
                }))).setNegativeButton(R.string.advance, (d, h) -> {
                    this.sortAdvance();
                }).show();
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
