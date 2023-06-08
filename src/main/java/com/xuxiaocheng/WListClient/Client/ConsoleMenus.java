package com.xuxiaocheng.WListClient.Client;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateFileHelper;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateServerHelper;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateUserHelper;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.WrongStateException;
import com.xuxiaocheng.WListClient.Server.DrivePath;
import com.xuxiaocheng.WListClient.Server.Operation;
import com.xuxiaocheng.WListClient.Server.Options;
import com.xuxiaocheng.WListClient.Server.VisibleFileInformation;
import com.xuxiaocheng.WListClient.Server.VisibleUserGroupInformation;
import com.xuxiaocheng.WListClient.Server.VisibleUserInformation;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public final class ConsoleMenus {
    private ConsoleMenus() {
        super();
    }

    private static final @NotNull Scanner Scanner = new Scanner(System.in);

    public static class TokenPair {
        protected String token;
        protected String username;

        @Override
        public @NotNull String toString() {
            return "TokenPair{" +
                    "token='" + this.token + '\'' +
                    ", username='" + this.username + '\'' +
                    '}';
        }
    }

    @FunctionalInterface
    private interface MenuHandler {
        boolean handle(final @NotNull WListClient client, final @NotNull TokenPair token) throws WrongStateException, IOException, InterruptedException;
    }

    private static final @NotNull PrintTable Menu = PrintTable.create()
            .setHeader(List.of("ID", "Operation", "Permission", "Detail"))
            .addBody(List.of("0", "Exit", "any", "Exit client directly."))
            .addBody(List.of("1", "Close server", "admin", "Try close server, and then exit client."))
            .addBody(List.of("10", "Register", "any", "Register a new user."))
            .addBody(List.of("11", "Login", "any", "Login with username and password."))
            .addBody(List.of("12", "Get permissions", "user", "Display the current user group."))
            .addBody(List.of("13", "Change username", "user", "Change username. Don't need login again."))
            .addBody(List.of("14", "Change password", "user", "Change password after verifying the old one. All accounts need to be logged in again."))
            .addBody(List.of("15", "Logoff", "user", "Logoff after verifying old password. WARNING: Please operate with caution."))
            .addBody(List.of("20", "List users", "admin", "Get registered users list."))
            .addBody(List.of("21", "Delete user", "admin", "Delete any of registered user."))
            .addBody(List.of("22", "List groups", "admin", "Get user groups list."))
            .addBody(List.of("23", "Add group", "admin", "Add a new empty user group."))
            .addBody(List.of("24", "Delete group", "admin", "Delete a user group without users in it."))
            .addBody(List.of("25", "Change group", "admin", "Move the user to the specified user group."))
            .addBody(List.of("26", "Add permissions", "admin", "Add permissions to user group."))
            .addBody(List.of("27", "Remove permissions", "admin", "Remove permissions from user group."))
            // TODO
            .addBody(List.of("40", "List files", "user", "Get files list in explicit directory."))
            .addBody(List.of("41", "Make directory", "user", "Making new directories recursively."))
            ;
    public static boolean chooseMenu(final @NotNull WListClient client, final @NotNull TokenPair token) {
        //noinspection VariableNotUsedInsideIf
        System.out.println("Current login status: " + (token.token == null ? "false" : "true  username: '" + token.username + '\''));
        ConsoleMenus.Menu.print();
        try {
            System.out.print("Please enter operation id: ");
            final int mode = Integer.parseInt(ConsoleMenus.Scanner.nextLine());
            final MenuHandler handler = switch (mode) {
                case 1 -> ConsoleMenus.closeServer;
                case 10 -> ConsoleMenus.register;
                case 11 -> ConsoleMenus.login;
                case 12 -> ConsoleMenus.getPermissions;
                case 13 -> ConsoleMenus.changeUsername;
                case 14 -> ConsoleMenus.changePassword;
                case 15 -> ConsoleMenus.logoff;
                case 20 -> ConsoleMenus.listUsers;
                case 21 -> ConsoleMenus.deleteUser;
                case 22 -> ConsoleMenus.listGroups;
                case 23 -> ConsoleMenus.addGroup;
                case 24 -> ConsoleMenus.deleteGroup;
                case 25 -> ConsoleMenus.changeGroup;
                case 26 -> ConsoleMenus.addPermissions;
                case 27 -> ConsoleMenus.removePermissions;
                // TODO
                case 40 -> ConsoleMenus.listFiles;
                case 41 -> ConsoleMenus.makeDirectories;
                default -> (c, t) -> mode == 0;
            };
            if (handler.handle(client, token))
                return false;
        } catch (final NumberFormatException exception) {
            System.out.printf("NumberFormatException: %s %n", exception.getMessage());
            return true;
        } catch (final WrongStateException exception) {
            if (exception.getState() == Operation.State.NoPermission) {
                System.out.println("No permission! It is also possible that the password or permissions have been changed and a new login is required.");
                return true;
            }
            System.out.println("Wrong response state. Please check if the client version matches.");
            HLog.DefaultLogger.log(HLogLevel.WARN, exception);
        } catch (final IllegalArgumentException exception) {
            System.out.println("Wrong network argument. Please check if the client version matches.");
        } catch (final IOException | InterruptedException exception) {
            HLog.DefaultLogger.log(HLogLevel.FAULT, exception);
            return false;
        } finally {
            System.out.println("Enter to continue...");
            ConsoleMenus.Scanner.nextLine();
        }
        return true;
    }

    private static boolean checkToken(final @NotNull TokenPair token) {
        if (token.token == null) {
            System.out.println("Currently not logged in, please login and try again.");
            return true;
        }
        return false;
    }

    private static final @NotNull MenuHandler closeServer = (client, token) -> {
        System.out.println("Closing server... WARNING!");
        if (ConsoleMenus.checkToken(token))
            return false;
        System.out.print("WARNING! Close server. Enter to confirm.");
        if (ConsoleMenus.Scanner.nextLine().isBlank()) {
            if (OperateServerHelper.closeServer(client, token.token)) {
                System.out.println("Success!");
                return true;
            } else
                System.out.println("Failure, unknown reason.");
        }
        return false;
    };

    private static final @NotNull MenuHandler register = (client, token) -> {
        System.out.println("Registering...");
        System.out.print("Please enter username: ");
        final String username = ConsoleMenus.Scanner.nextLine();
        System.out.print("Please enter password: ");
        final String password = ConsoleMenus.Scanner.nextLine();
        if (OperateUserHelper.register(client, username, password))
            System.out.println("Success, then login again!");
        else
            System.out.println("Username already exists.");
        return false;
    };
    private static final @NotNull MenuHandler login = (client, token) -> {
        System.out.println("Logging in...");
        System.out.print("Please enter username: ");
        final String username = ConsoleMenus.Scanner.nextLine();
        System.out.print("Please enter password: ");
        final String password = ConsoleMenus.Scanner.nextLine();
        final String t = OperateUserHelper.login(client, username, password);
        if (t != null) {
            System.out.println("Success!");
            token.token = t;
            token.username = username;
        } else
            System.out.println("Wrong username or password.");
        return false;
    };
    private static final @NotNull MenuHandler getPermissions = (client, token) -> {
        System.out.println("Getting permissions...");
        if (ConsoleMenus.checkToken(token))
            return false;
        final VisibleUserGroupInformation t = OperateUserHelper.getPermissions(client, token.token);
        if (t != null) {
            System.out.println("Success!");
            System.out.println("Group name: " + t.name());
            final Map<String, Boolean> map = new LinkedHashMap<>();
            for (final Operation.Permission permission: Operation.Permission.values()) {
                if (permission == Operation.Permission.Undefined)
                    continue;
                map.put(permission.name(), t.permissions().contains(permission));
            }
            PrintTable.create()
                    .setHeader(map.keySet().stream().toList())
                    .addBody(map.values().stream().map(String::valueOf).toList())
                    .print();
        } else
            System.out.println("Failure, unknown reason.");
        return false;
    };
    private static final @NotNull MenuHandler changeUsername = (client, token) -> {
        System.out.println("Changing username...");
        if (ConsoleMenus.checkToken(token))
            return false;
        System.out.print("Please enter new username: ");
        final String newUsername = ConsoleMenus.Scanner.nextLine();
        if (OperateUserHelper.changeUsername(client, token.token, newUsername)) {
            System.out.println("Success!");
            token.username = newUsername;
        } else
            System.out.println("Expired token or denied operation.");
        return false;
    };
    private static final @NotNull MenuHandler changePassword = (client, token) -> {
        System.out.println("Changing password...");
        if (ConsoleMenus.checkToken(token))
            return false;
        System.out.print("Please enter old password: ");
        final String oldPassword = ConsoleMenus.Scanner.nextLine();
        System.out.print("Please enter new password: ");
        final String newPassword = ConsoleMenus.Scanner.nextLine();
        if (OperateUserHelper.changePassword(client, token.token, oldPassword, newPassword)) {
            System.out.println("Success, then login again!");
            token.token = null;
            token.username = null;
        } else
            System.out.println("Wrong password or expired token.");
        return false;
    };
    private static final @NotNull MenuHandler logoff = (client, token) -> {
        System.out.println("Logging off... WARNING!");
        if (ConsoleMenus.checkToken(token))
            return false;
        else {
            System.out.print("Please confirm password: ");
            final String password = ConsoleMenus.Scanner.nextLine();
            if (OperateUserHelper.logoff(client, token.token, password)) {
                System.out.println("Success!");
                token.token = null;
                token.username = null;
            } else
                System.out.println("Failure, unknown reason. Token may have expired.");
        }
        return false;
    };

    private static final @NotNull MenuHandler listUsers = (client, token) -> {
        System.out.println("Listing users...");
        if (ConsoleMenus.checkToken(token))
            return false;
        System.out.print("Please enter page number, or enter to page 1 (limit: " + GlobalConfiguration.getInstance().limit() + "): ");
        final String choosePage = ConsoleMenus.Scanner.nextLine();
        int i = 0;
        if (!choosePage.isBlank())
            i = Integer.parseInt(choosePage) - 1;
        do {
            final Pair.ImmutablePair<Long, List<VisibleUserInformation>> page = OperateUserHelper.listUsers(client, token.token,
                    GlobalConfiguration.getInstance().limit(), i, Options.OrderDirection.ASCEND);
            if (page.getSecond().isEmpty() && i > 0)
                break;
            System.out.printf("Total: %d, Page: %d %n", page.getFirst().longValue(), i + 1);
            final PrintTable table = PrintTable.create().setHeader(List.of("id", "username", "group"));
            for (final VisibleUserInformation information: page.getSecond())
                table.addBody(List.of(String.valueOf(information.id()), information.username(), information.group()));
            table.print();
            if ((long) (i + 1) * GlobalConfiguration.getInstance().limit() >= page.getFirst().longValue())
                break;
            System.out.print("Enter to continue, or other key to exit: ");
            if (!ConsoleMenus.Scanner.nextLine().isBlank())
                break;
            ++i;
        } while (true);
        return false;
    };
    private static final @NotNull MenuHandler deleteUser = (client, token) -> {
        System.out.println("Deleting user... WARNING!");
        if (ConsoleMenus.checkToken(token))
            return false;
        System.out.print("Please enter username to delete: ");
        final String username = ConsoleMenus.Scanner.nextLine();
        if (token.username.equals(username)) {
            System.out.print("WARNING! Deleting yourself. Enter to confirm.");
            if (!ConsoleMenus.Scanner.nextLine().isBlank())
                return false;
        }
        if (OperateUserHelper.deleteUser(client, token.token, username)) {
            System.out.println("Success!");
            if (token.username.equals(username)) {
                token.token = null;
                token.username = null;
            }
        } else
            System.out.println("No such user.");
        return false;
    };
    private static final @NotNull MenuHandler listGroups = (client, token) -> {
        System.out.println("Listing user groups...");
        if (ConsoleMenus.checkToken(token))
            return false;
        System.out.print("Please enter page number, or enter to page 1 (limit: " + GlobalConfiguration.getInstance().limit() + "): ");
        final String choosePage = ConsoleMenus.Scanner.nextLine();
        int i = 0;
        if (!choosePage.isBlank())
            i = Integer.parseInt(choosePage) - 1;
        do {
            final Pair.ImmutablePair<Long, List<VisibleUserGroupInformation>> page = OperateUserHelper.listGroups(client, token.token,
                    GlobalConfiguration.getInstance().limit(), i, Options.OrderDirection.ASCEND);
            if (page.getSecond().isEmpty() && i > 0)
                break;
            System.out.printf("Total: %d, Page: %d %n", page.getFirst().longValue(), i + 1);
            final PrintTable table = PrintTable.create().setHeader(List.of("id", "name", "permissions"));
            for (final VisibleUserGroupInformation information: page.getSecond())
                table.addBody(List.of(String.valueOf(information.id()), information.name(), information.permissions().toString()));
            table.print();
            if ((long) (i + 1) * GlobalConfiguration.getInstance().limit() >= page.getFirst().longValue())
                break;
            System.out.print("Enter to continue, or other key to exit: ");
            if (!ConsoleMenus.Scanner.nextLine().isBlank())
                break;
            ++i;
        } while (true);
        return false;
    };
    private static final @NotNull MenuHandler addGroup = (client, token) -> {
        System.out.println("Adding user group...");
        if (ConsoleMenus.checkToken(token))
            return false;
        System.out.print("Please enter new group name: ");
        final String groupName = ConsoleMenus.Scanner.nextLine();
        if (OperateUserHelper.addGroup(client, token.token, groupName))
            System.out.println("Success!");
        else
            System.out.println("Group name already exists.");
        return false;
    };
    private static final @NotNull MenuHandler deleteGroup = (client, token) -> {
        System.out.println("Deleting user group...");
        if (ConsoleMenus.checkToken(token))
            return false;
        System.out.print("Please enter group name: ");
        final String groupName = ConsoleMenus.Scanner.nextLine();
        final Boolean success = OperateUserHelper.deleteGroup(client, token.token, groupName);
        if (success == null)
            System.out.println("No such group.");
        else if (success.booleanValue())
            System.out.println("Success!");
        else
            System.out.println("Failure, some users are still in this group.");
        return false;
    };
    private static final @NotNull MenuHandler changeGroup = (client, token) -> {
        System.out.println("Changing user into explicit group...");
        if (ConsoleMenus.checkToken(token))
            return false;
        System.out.print("Please enter username: ");
        final String username = ConsoleMenus.Scanner.nextLine();
        System.out.print("Please enter group name: ");
        final String groupName = ConsoleMenus.Scanner.nextLine();
        final Boolean success = OperateUserHelper.changeGroup(client, token.token, username, groupName);
        if (success == null)
            System.out.println("No such group.");
        else if (success.booleanValue())
            System.out.println("Success!");
        else
            System.out.println("No such user.");
        return false;
    };
    private static final @NotNull MenuHandler addPermissions =(client, token) -> {
        System.out.println("Adding permissions for user group...");
        if (ConsoleMenus.checkToken(token))
            return false;
        System.out.print("Please enter group name: ");
        final String groupName = ConsoleMenus.Scanner.nextLine();
        final EnumSet<Operation.Permission> permissions = ConsoleMenus.getPermissions();
        if (OperateUserHelper.changePermission(client, token.token, groupName, true, permissions))
            System.out.println("Success!");
        else
            System.out.println("No such group.");
        return false;
    };
    private static final @NotNull MenuHandler removePermissions =(client, token) -> {
        System.out.println("Removing permissions for user group...");
        if (ConsoleMenus.checkToken(token))
            return false;
        System.out.print("Please enter group name: ");
        final String groupName = ConsoleMenus.Scanner.nextLine();
        final EnumSet<Operation.Permission> permissions = ConsoleMenus.getPermissions();
        if (OperateUserHelper.changePermission(client, token.token, groupName, false, permissions))
            System.out.println("Success!");
        else
            System.out.println("No such group.");
        return false;
    };

    private static final @NotNull MenuHandler listFiles = (client, token) -> {
        System.out.println("Listing files...");
        if (ConsoleMenus.checkToken(token))
            return false;
        System.out.print("Please enter directory path: ");
        final DrivePath path = new DrivePath(ConsoleMenus.Scanner.nextLine());
        System.out.print("Please enter page number, or enter to page 1 (limit: " + GlobalConfiguration.getInstance().limit() + "): ");
        final String choosePage = ConsoleMenus.Scanner.nextLine();
        int i = 0;
        if (!choosePage.isBlank())
            i = Integer.parseInt(choosePage) - 1;
        do {
            final Pair.ImmutablePair<Long, List<VisibleFileInformation>> page = OperateFileHelper.listFiles(client, token.token,
                    path, GlobalConfiguration.getInstance().limit(), i, Options.OrderPolicy.FileName, Options.OrderDirection.ASCEND);
            if (page == null) {
                System.out.println("No such directory.");
                break;
            }
            if (page.getSecond().isEmpty() && i > 0)
                break;
            System.out.printf("Total: %d, Page: %d %n", page.getFirst().longValue(), i + 1);
            final PrintTable table = PrintTable.create().setHeader(List.of("name", "dir", "size", "create_time", "update_time", "md5"));
            for (final VisibleFileInformation information : page.getSecond())
                table.addBody(List.of(information.path().getName(), String.valueOf(information.is_dir()), String.valueOf(information.size()),
                        information.createTime() == null ? "Unknown" : information.createTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        information.updateTime() == null ? "Unknown" : information.updateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        information.md5()));
            table.print();
            if ((long) (i + 1) * GlobalConfiguration.getInstance().limit() >= page.getFirst().longValue())
                break;
            System.out.print("Enter to continue, or other key to exit: ");
            if (!ConsoleMenus.Scanner.nextLine().isBlank())
                break;
            ++i;
        } while (true);
        return false;
    };
    private static final @NotNull MenuHandler makeDirectories =(client, token) -> {
        System.out.println("Making directory...");
        if (ConsoleMenus.checkToken(token))
            return false;
        System.out.print("Please enter directory path: ");
        final DrivePath path = new DrivePath(ConsoleMenus.Scanner.nextLine());
        final Options.DuplicatePolicy policy = ConsoleMenus.getDuplicatePolicy();
        final VisibleFileInformation information = OperateFileHelper.makeDirectories(client, token.token, path, policy);
        if (information != null) {
            System.out.println("Success!");
            PrintTable.create().setHeader(List.of("name", "create_time", "update_time"))
                    .addBody(List.of(information.path().getName(),
                            information.createTime() == null ? "Unknown" : information.createTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            information.updateTime() == null ? "Unknown" : information.updateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                    .print();
        } else
            System.out.println("Invalid filename.");
        return false;
    };


    private static final @NotNull PrintTable DuplicatePolicyTable = PrintTable.create().setHeader(List.of("id", "policy", "detail"))
            .addBody(List.of("1", "ERROR", "Only attempt to response the same file."))
            .addBody(List.of("2", "OVER", "Force replace existing file."))
            .addBody(List.of("3", "KEEP", "Automatically rename and retry."))
            .addBody(List.of("", "Notice: ", "For directories, will response the original one."));
    private static Options.@NotNull DuplicatePolicy getDuplicatePolicy() {
        ConsoleMenus.DuplicatePolicyTable.print();
        System.out.print("Please enter duplicate policy id: ");
        while (true) {
            final int id = Integer.parseInt(ConsoleMenus.Scanner.nextLine());
            final Options.DuplicatePolicy policy = switch (id) {
                case 1 -> Options.DuplicatePolicy.ERROR;
                case 2 -> Options.DuplicatePolicy.OVER;
                case 3 -> Options.DuplicatePolicy.KEEP;
                default -> null;
            };
            if (policy != null)
                return policy;
            System.out.print("Invalid id. Please enter valid duplicate policy id again: ");
        }
    }

    private static final @NotNull PrintTable PermissionsTable = PrintTable.create().setHeader(List.of("id", "policy", "detail"))
            .addBody(List.of("1", "ServerOperate", "Operate server state. DANGEROUS!"))
            .addBody(List.of("2", "Broadcast", "Send broadcast to other connections."))
            .addBody(List.of("3", "UsersList", "Get users and user groups list."))
            .addBody(List.of("4", "UsersOperate", "Modify users and user groups. DANGEROUS!"))
            .addBody(List.of("5", "FilesList", "Get files list."))
            .addBody(List.of("6", "FileDownload", "Download explicit file."))
            .addBody(List.of("7", "FileUpload", "Upload file to explicit path."))
            .addBody(List.of("8", "FileDelete", "Delete explicit file."));
    private static @NotNull EnumSet<Operation.@NotNull Permission> getPermissions() {
        ConsoleMenus.PermissionsTable.print();
        System.out.print("Please enter the selected permission ids: ");
        while (true) {
            final EnumSet<Operation.Permission> permissions = EnumSet.noneOf(Operation.Permission.class);
            final List<Integer> ids = Stream.of(ConsoleMenus.Scanner.nextLine().split(" ")).map(Integer::parseInt).toList();
            boolean flag = true;
            for (final Integer id: ids) {
                final Operation.Permission permission = switch (id.intValue()) {
                    case 1 -> Operation.Permission.ServerOperate;
                    case 2 -> Operation.Permission.Broadcast;
                    case 3 -> Operation.Permission.UsersList;
                    case 4 -> Operation.Permission.UsersOperate;
                    case 5 -> Operation.Permission.FilesList;
                    case 6 -> Operation.Permission.FileDownload;
                    case 7 -> Operation.Permission.FileUpload;
                    case 8 -> Operation.Permission.FileDelete;
                    default -> null;
                };
                if (permission == null) {
                    System.out.printf("Invalid id (%d). Please enter valid permission id again: ", id);
                    flag = false;
                    break;
                }
                permissions.add(permission);
            }
            if (flag)
                return permissions;
        }
    }
}
