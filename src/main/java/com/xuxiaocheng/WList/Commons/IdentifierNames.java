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
    }
}
