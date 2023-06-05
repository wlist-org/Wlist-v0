package com.xuxiaocheng.WListClient.Client;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PrintTable {
    protected static final String HALF_ROW_LINE = "\u002d";
    protected static final String FULL_ROW_LINE = "\uff0d";
    protected static final char COLUMN_LINE = '|';
    protected static final char CORNER = '+';
    protected static final String HALF_SPACE = "\u0020";
    protected static final String FULL_SPACE = "\u3000";
    protected static final String LF = System.getProperty("line.separator");

    protected final boolean sbcMode;
    protected final @NotNull List<@NotNull String> headers = new ArrayList<>();
    protected final @NotNull List<@NotNull List<@NotNull String>> bodyList = new ArrayList<>();
    protected final @NotNull List<@NotNull Integer> columnCharCount = new ArrayList<>();

    protected PrintTable(final boolean sbcMode) {
        super();
        this.sbcMode = sbcMode;
    }

    public static @NotNull PrintTable create() {
        return new PrintTable(false);
    }

    public static @NotNull PrintTable create(final boolean sbcMode) {
        return new PrintTable(sbcMode);
    }

    public @NotNull PrintTable setHeader(final @NotNull List<String> titles) {
        if (!this.columnCharCount.isEmpty())
            throw new IllegalStateException("Cannot set headers of print table twice.");
        for (int i = 0; i < titles.size(); ++i)
            this.columnCharCount.add(0);
        this.fillColumns(this.headers, titles);
        return this;
    }

    public @NotNull PrintTable addBody(final @NotNull List<String> values) {
        final List<String> bodies = new ArrayList<>();
        this.fillColumns(bodies, values);
        this.bodyList.add(bodies);
        return this;
    }

    protected void fillColumns(final @NotNull Collection<? super @NotNull String> columns, final @NotNull List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            final String column = this.sbcMode ? PrintTable.toSbc(values.get(i)) : values.get(i);
            columns.add(column);
            int width = column.length();
            if (!this.sbcMode) {
                final int sbcCount = PrintTable.nonSbcCount(column);
                width = ((width - sbcCount) << 1) + sbcCount;
            }
            if (width > this.columnCharCount.get(i).intValue())
                this.columnCharCount.set(i, width);
        }
    }

    protected static int nonSbcCount(final @NotNull CharSequence value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++)
            if (value.charAt(i) < '\177')
                ++count;
        return count;
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void print() {
        System.out.print(this);
    }

    @Override
    public @NotNull String toString() {
        final StringBuilder builder = new StringBuilder();
        this.fillBorder(builder);
        this.fillRows(builder, this.headers);
        this.fillBorder(builder);
        for (final List<String> row: this.bodyList)
            this.fillRows(builder, row);
        this.fillBorder(builder);
        return builder.toString();
    }

    protected void fillBorder(final @NotNull StringBuilder builder) {
        builder.append(PrintTable.CORNER);
        for (final Integer width : this.columnCharCount) {
            builder.append((this.sbcMode ? PrintTable.FULL_ROW_LINE : PrintTable.HALF_ROW_LINE).repeat(width.intValue() + 2));
            builder.append(PrintTable.CORNER);
        }
        builder.append(PrintTable.LF);
    }

    protected void fillRows(final @NotNull StringBuilder builder, final @NotNull List<@NotNull String> row) {
        builder.append(PrintTable.COLUMN_LINE);
        final int size = row.size();
        for (int i = 0; i < size; i++) {
            final String value = row.get(i);
            builder.append(this.sbcMode ? PrintTable.FULL_SPACE : PrintTable.HALF_SPACE);
            builder.append(value);
            final int length = value.length();
            final int sbcCount = PrintTable.nonSbcCount(value);
            if (this.sbcMode && (sbcCount & 1) == 1)
                builder.append(PrintTable.HALF_SPACE);
            builder.append(this.sbcMode ? PrintTable.FULL_SPACE : PrintTable.HALF_SPACE);
            final int maxLength = this.columnCharCount.get(i).intValue();
            if (this.sbcMode)
                builder.append(PrintTable.FULL_SPACE.repeat(Math.max(0, (maxLength - length + sbcCount >>> 1))));
            else
                builder.append(PrintTable.HALF_SPACE.repeat(Math.max(0, (maxLength - (((length - sbcCount) << 1) + sbcCount)))));
            builder.append(PrintTable.COLUMN_LINE);
        }
        builder.append(PrintTable.LF);
    }

    public static @NotNull String toSbc(final @NotNull String input) {
        final char[] c = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == ' ') {
                c[i] = '\u3000';
            } else if (c[i] < '\177') {
                c[i] = (char) ((int) c[i] + 65248);
            }
        }
        return new String(c);
    }
}
