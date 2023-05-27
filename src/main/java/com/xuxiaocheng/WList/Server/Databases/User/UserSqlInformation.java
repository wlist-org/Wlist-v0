package com.xuxiaocheng.WList.Server.Databases.User;

import com.xuxiaocheng.WList.Server.Operation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.SortedSet;

// TODO back to record.
public final class UserSqlInformation {
    private final long id;
    private @NotNull String username;
    private @NotNull String password;
    private @NotNull SortedSet<Operation.@NotNull Permission> permissions;
    private @NotNull LocalDateTime modifyTime;

    public UserSqlInformation(final long id, final @NotNull String username, final @NotNull String password,
                              final @NotNull SortedSet<Operation.@NotNull Permission> permissions, final @NotNull LocalDateTime modifyTime) {
        super();
        this.id = id;
        this.username = username;
        this.password = password;
        this.permissions = permissions;
        this.modifyTime = modifyTime;
    }

    public long getId() {
        return this.id;
    }

    public @NotNull String getUsername() {
        return this.username;
    }

    public void setUsername(final @NotNull String username) {
        this.username = username;
    }

    public @NotNull String getPassword() {
        return this.password;
    }

    public void setPassword(final @NotNull String password) {
        this.password = password;
    }

    public @NotNull SortedSet<Operation.@NotNull Permission> getPermissions() {
        return this.permissions;
    }

    public void setPermissions(final @NotNull SortedSet<Operation.@NotNull Permission> permissions) {
        this.permissions = permissions;
    }

    public @NotNull LocalDateTime getModifyTime() {
        return this.modifyTime;
    }

    public void setModifyTime(final @NotNull LocalDateTime modifyTime) {
        this.modifyTime = modifyTime;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof UserSqlInformation that)) return false;
        return this.id == that.id && this.username.equals(that.username) && this.password.equals(that.password) && this.permissions.equals(that.permissions) && this.modifyTime.equals(that.modifyTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.password);
    }

    @Override
    public @NotNull String toString() {
        return "UserSqlInformation{" +
                "id=" + this.id +
                ", username='" + this.username + '\'' +
                ", password='" + this.password + '\'' +
                ", permissions=" + this.permissions +
                ", modifyTime=" + this.modifyTime +
                '}';
    }
}
