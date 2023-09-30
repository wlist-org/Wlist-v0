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
    private FilesListInformationGetter() {
        super();
    }

    public static long total(final @NotNull VisibleFilesListInformation information) {
        return information.total();
    }

    public static long filtered(final @NotNull VisibleFilesListInformation information) {
        return information.filtered();
    }

    public static @NotNull @Unmodifiable List<@NotNull VisibleFileInformation> informationList(final @NotNull VisibleFilesListInformation information) {
        return information.informationList();
    }
}
