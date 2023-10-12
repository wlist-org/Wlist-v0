package com.xuxiaocheng.WList.Commons;

import org.jetbrains.annotations.NotNull;

public final class IdentifierNames {
    private IdentifierNames() {
        super();
    }

    public enum UserName {
        Admin("admin"),
        ;
        private final @NotNull String identifier;
        UserName(final @NotNull String identifier) {
            this.identifier = identifier;
        }
        public @NotNull String getIdentifier() {
            return this.identifier;
        }
        @Override
        public @NotNull String toString() {
            return "UserName{" +
                    "name='" + this.name() + '\'' +
                    ", identifier='" + this.identifier + '\'' +
                    '}';
        }
        public static boolean contains(final @NotNull String name) {
            return "admin".equals(name);
        }
    }

    public enum UserGroupName {
        Admin("admin"),
        Default("default"),
        ;
        private final @NotNull String identifier;
        UserGroupName(final @NotNull String identifier) {
            this.identifier = identifier;
        }
        public @NotNull String getIdentifier() {
            return this.identifier;
        }
        @Override
        public @NotNull String toString() {
            return "UserGroupName{" +
                    "name='" + this.name() + '\'' +
                    ", identifier='" + this.identifier + '\'' +
                    '}';
        }
        public static boolean contains(final @NotNull String name) {
            return "admin".equals(name) || "default".equals(name);
        }
    }

    public static final @NotNull String RootSelector = "WList#RootSelector";
}
