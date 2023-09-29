package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFilesListInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * @see VisibleFilesListInformation
 */
public final class FilesListInformationGetter {
    public long total(final @NotNull VisibleFilesListInformation information) {
        return information.total();
    }

    public long filtered(final @NotNull VisibleFilesListInformation information) {
        return information.filtered();
    }

    public @NotNull @Unmodifiable List<@NotNull VisibleFileInformation> informationList(final @NotNull VisibleFilesListInformation information) {
        return information.informationList();
    }
}
