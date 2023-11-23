package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.UiThread;
import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.AndroidSupports.FailureReasonGetter;
import com.xuxiaocheng.WList.AndroidSupports.InstantaneousProgressStateGetter;
import com.xuxiaocheng.WList.Commons.Beans.InstantaneousProgressState;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.DownloadTasksManager;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListDownloadFailureCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListDownloadSuccessCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListDownloadWorkingCellBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class TaskDownload extends SPageTaskFragment {
    public TaskDownload() {
        super(PageTaskAdapter.Types.Download);
    }

    @Override
    protected @NotNull SPageTaskStateFragment<?, DownloadTasksManager.DownloadTask, ?> createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new DownloadFailureTaskStateFragment();
            case Working -> new DownloadWorkingTaskStateFragment();
            case Success -> new DownloadSuccessTaskStateFragment();
        };
    }

    public static class DownloadFailureTaskStateFragment extends FailureTaskStateFragment<PageTaskListDownloadFailureCellBinding, DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadFailure> {
        public DownloadFailureTaskStateFragment() {
            super(PageTaskListDownloadFailureCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<DownloadTasksManager.DownloadTask, ?, ?, DownloadTasksManager.DownloadFailure> getManager() {
            return DownloadTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListDownloadFailureCellBinding cell, final DownloadTasksManager.@NotNull DownloadTask task, final DownloadTasksManager.@NotNull DownloadFailure data) {
            ViewUtil.setFileImage(cell.pageTaskListDownloadFailureCellImage, false, task.getFilename());
            cell.pageTaskListDownloadFailureCellName.setText(task.getFilename());
            cell.pageTaskListDownloadFailureCellReason.setText(FailureReasonGetter.toString(data.getReason()));
        }
    }

    public static class DownloadWorkingTaskStateFragment extends WorkingTaskStateFragment<DownloadWorkingTaskStateFragment.WrappedPageTaskListDownloadWorkingCellBinding, DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadWorking> {
        protected static final class WrappedPageTaskListDownloadWorkingCellBinding implements ViewBinding {
            private final @NotNull PageTaskListDownloadWorkingCellBinding cell;
            private DownloadTasksManager.@Nullable DownloadWorking working;
            private long lastW = 0;
            private @Nullable LocalDateTime lastT = null;

            private WrappedPageTaskListDownloadWorkingCellBinding(final @NotNull LayoutInflater inflater, final @Nullable ViewGroup group, final boolean attachRoot) {
                super();
                this.cell = PageTaskListDownloadWorkingCellBinding.inflate(inflater, group, attachRoot);
            }

            @Override
            public @NotNull View getRoot() {
                return this.cell.getRoot();
            }

            @Override
            public @NotNull String toString() {
                return "WrappedPageTaskListDownloadWorkingCellBinding{" +
                        "cell=" + this.cell +
                        ", working=" + this.working +
                        '}';
            }
        }

        public DownloadWorkingTaskStateFragment() {
            super(WrappedPageTaskListDownloadWorkingCellBinding::new);
        }

        @Override
        protected @NotNull AbstractTasksManager<DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadWorking, ?, ?> getManager() {
            return DownloadTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull WrappedPageTaskListDownloadWorkingCellBinding cell, final DownloadTasksManager.@NotNull DownloadTask task, final DownloadTasksManager.@NotNull DownloadWorking data) {
            ViewUtil.setFileImage(cell.cell.pageTaskListDownloadWorkingCellImage, false, task.getFilename());
            cell.cell.pageTaskListDownloadWorkingCellName.setText(task.getFilename());
            cell.working = data;
            cell.cell.pageTaskListDownloadWorkingCellProgress.setMin(0);
            cell.cell.pageTaskListDownloadWorkingCellProgress.setMax(1000);
            this.resetProgress(cell, false);
            data.getUpdateCallbacks().registerNamedForce("DownloadWorkingTaskStateFragment", () -> Main.runOnUiThread(this.activity(), () ->
                    this.resetProgress(cell, true)));
        }

        protected float calculateProgress(final @NotNull Iterable<? extends Pair.@NotNull ImmutablePair<@NotNull AtomicLong, @NotNull Long>> progress) {
            long current = 0, total = 0;
            for (final Pair.ImmutablePair<AtomicLong, Long> pair: progress) {
                current += pair.getFirst().longValue();
                total += pair.getSecond().longValue();
            }
            return 1.0f * current / total;
        }

        @UiThread
        protected void resetProgress(final @NotNull WrappedPageTaskListDownloadWorkingCellBinding cell, final boolean animate) {
            assert cell.working != null;
            final List<Pair.ImmutablePair<AtomicLong, Long>> progress = cell.working.getProgress();
            final InstantaneousProgressState state = cell.working.getState();
            final Context context = cell.getRoot().getContext();
            if (progress == null || state == null) {
                cell.cell.pageTaskListDownloadWorkingCellProcessText.setText(MessageFormat.format(context.getString(R.string.page_task_process), 0));
                cell.cell.pageTaskListDownloadWorkingCellProgress.setIndeterminate(true);
                cell.cell.pageTaskListDownloadWorkingCellSize.setText(R.string.page_task_download_working_preparing);
                cell.cell.pageTaskListDownloadWorkingCellTime.setText(MessageFormat.format(context.getString(R.string.page_task_time), context.getString(R.string.unknown)));
                return;
            }
            final Pair.ImmutablePair<Long, Long> m = InstantaneousProgressStateGetter.merge(state);
            if (m.getFirst().longValue() == m.getSecond().longValue()) {
                cell.cell.pageTaskListDownloadWorkingCellProcessText.setText(MessageFormat.format(context.getString(R.string.page_task_process), 1));
                cell.cell.pageTaskListDownloadWorkingCellProgress.setIndeterminate(true);
                cell.cell.pageTaskListDownloadWorkingCellSize.setText(R.string.page_task_download_working_finishing);
                cell.cell.pageTaskListDownloadWorkingCellTime.setText(MessageFormat.format(context.getString(R.string.page_task_time), "0s"));
                return;
            }
            float readPercent = this.calculateProgress(progress);
            final float writePercent = 1.0f * m.getFirst().longValue() / m.getSecond().longValue();
            if (readPercent < writePercent) readPercent = writePercent;
            cell.cell.pageTaskListDownloadWorkingCellProcessText.setText(MessageFormat.format(context.getString(R.string.page_task_process), writePercent));
            cell.cell.pageTaskListDownloadWorkingCellProgress.setIndeterminate(false);
            //noinspection NumericCastThatLosesPrecision
            cell.cell.pageTaskListDownloadWorkingCellProgress.setProgress((int) (writePercent * 1000), animate);
            //noinspection NumericCastThatLosesPrecision
            cell.cell.pageTaskListDownloadWorkingCellProgress.setSecondaryProgress((int) (readPercent * 1000));
            final String unknown = context.getString(R.string.unknown);
            final LocalDateTime now = LocalDateTime.now();
            final long interval = cell.lastT == null ? 1000 : Duration.between(cell.lastT, now).toMillis();
            //noinspection NumericCastThatLosesPrecision
            cell.cell.pageTaskListDownloadWorkingCellSize.setText(MessageFormat.format(context.getString(R.string.page_task_size_speed),
                    ViewUtil.formatSize(m.getFirst().longValue(), unknown),
                    ViewUtil.formatSize(m.getSecond().longValue(), unknown),
                    ViewUtil.formatSize((long) (1.0f * (m.getFirst().longValue() - cell.lastW) / (interval == 0 ? 1000 : interval)) * 1000, unknown)));
            cell.lastW = m.getFirst().longValue();
            cell.lastT = now;
        }

        @Override
        protected void onUnbind(final @NotNull WrappedPageTaskListDownloadWorkingCellBinding cell) {
            super.onUnbind(cell);
            if (cell.working != null)
                cell.working.getUpdateCallbacks().unregisterNamed("DownloadWorkingTaskStateFragment");
            cell.working = null;
            cell.lastW = 0;
            cell.lastT = null;
        }
    }

    public static class DownloadSuccessTaskStateFragment extends SuccessTaskStateFragment<PageTaskListDownloadSuccessCellBinding, DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadSuccess> {
        public DownloadSuccessTaskStateFragment() {
            super(PageTaskListDownloadSuccessCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<DownloadTasksManager.DownloadTask, ?, DownloadTasksManager.DownloadSuccess, ?> getManager() {
            return DownloadTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListDownloadSuccessCellBinding cell, final DownloadTasksManager.@NotNull DownloadTask task, final DownloadTasksManager.@NotNull DownloadSuccess data) {
            ViewUtil.setFileImage(cell.pageTaskListDownloadSuccessCellImage, false, task.getFilename());
            cell.pageTaskListDownloadSuccessCellName.setText(task.getFilename());
            final Context context = cell.getRoot().getContext();
            final File saved = task.getSavePath();
            cell.pageTaskListDownloadSuccessCellPath.setText(MessageFormat.format(context.getString(R.string.page_task_download_success_path), saved.getAbsolutePath()));
            if (saved.isFile()) {
                cell.pageTaskListDownloadSuccessCellSize.setText(ViewUtil.formatSize(saved.length(), context.getString(R.string.unknown)));
                cell.pageTaskListDownloadSuccessCellPath.getPaint().reset();
                cell.pageTaskListDownloadSuccessCellName.getPaint().reset();
                cell.pageTaskListDownloadSuccessCellName.setTextColor(context.getColor(R.color.text_normal));
                cell.pageTaskListDownloadSuccessCellImage.setImageAlpha(255);
            } else {
                cell.pageTaskListDownloadSuccessCellSize.setText(R.string.page_task_download_success_deleted);
                cell.pageTaskListDownloadSuccessCellPath.getPaint().setFlags(Paint.ANTI_ALIAS_FLAG | Paint.STRIKE_THRU_TEXT_FLAG);
                cell.pageTaskListDownloadSuccessCellName.getPaint().setFlags(Paint.ANTI_ALIAS_FLAG | Paint.STRIKE_THRU_TEXT_FLAG);
                cell.pageTaskListDownloadSuccessCellName.setTextColor(context.getColor(R.color.text_hint));
                cell.pageTaskListDownloadSuccessCellImage.setImageAlpha(128);
            }
         }
    }
}
