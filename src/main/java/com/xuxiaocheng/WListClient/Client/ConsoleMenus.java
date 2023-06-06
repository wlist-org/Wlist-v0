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
import com.xuxiaocheng.WListClient.Server.VisibleUserInformation;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

public final class ConsoleMenus {
    private ConsoleMenus() {
        super();
    }

    private static final @NotNull Scanner Scanner = new Scanner(System.in);

    private static final PrintTable menu = PrintTable.create()
            .setHeader(List.of("ID", "Operation", "Detail"))
            .addBody(List.of("0", "Exit", ""))
            .addBody(List.of("1", "Close server", ""))
            .addBody(List.of("10", "Register", ""))
            .addBody(List.of("11", "Login", ""))
            .addBody(List.of("12", "Change password", ""))
            .addBody(List.of("13", "Logoff", ""))
            .addBody(List.of("20", "List users", ""))
            .addBody(List.of("21", "Delete user", ""))
            // TODO
            .addBody(List.of("40", "List files", ""))
            ;
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static boolean chooseMenu(final @NotNull WListClient client, final @NotNull AtomicReference<String> token) {
        System.out.println("Current login status: " + (token.get() != null));
        ConsoleMenus.menu.print();
        try {
            System.out.print("Please enter operation id: ");
            final int mode = Integer.parseInt(ConsoleMenus.Scanner.nextLine());
            switch (mode) {
                case 0 -> {return false;}
                case 1 -> {
                    System.out.println("Closing server... WARNING!");
                    if (token.get() == null)
                        System.out.println("Currently not logged in, please login and try again.");
                    else {
                        System.out.print("WARNING! Close server. Enter to confirm.");
                        if (ConsoleMenus.Scanner.nextLine().isBlank()) {
                            OperateServerHelper.closeServer(client, token.get());
                            return false;
                        }
                    }
                }
                case 10 -> {
                    System.out.println("Registering...");
                    System.out.print("Please enter username: ");
                    final String username = ConsoleMenus.Scanner.nextLine();
                    System.out.print("Please enter password: ");
                    final String password = ConsoleMenus.Scanner.nextLine();
                    final boolean success = OperateUserHelper.register(client, username, password);
                    if (success)
                        System.out.println("Success, then re login!");
                    else
                        System.out.println("Username already exists.");
                }
                case 11 -> {
                    System.out.println("Logging in...");
                    System.out.print("Please enter username: ");
                    final String username = ConsoleMenus.Scanner.nextLine();
                    System.out.print("Please enter password: ");
                    final String password = ConsoleMenus.Scanner.nextLine();
                    token.set(OperateUserHelper.login(client, username, password));
                    if (token.get() == null)
                        System.out.println("Wrong username or password.");
                    else
                        System.out.println("Success!");
                }
                case 12 -> {
                    System.out.println("Changing password...");
                    if (token.get() == null)
                        System.out.println("Currently not logged in, please login and try again.");
                    else {
                        System.out.print("Please enter old password: ");
                        final String oldPassword = ConsoleMenus.Scanner.nextLine();
                        System.out.print("Please enter new password: ");
                        final String newPassword = ConsoleMenus.Scanner.nextLine();
                        final boolean success = OperateUserHelper.changePassword(client, token.get(), oldPassword, newPassword);
                        if (success) {
                            System.out.println("Success, then re login!");
                            token.set(null);
                        } else
                            System.out.println("Wrong password.");
                    }
                }
                case 13 -> {
                    System.out.println("Logging off... WARNING!");
                    if (token.get() == null)
                        System.out.println("Currently not logged in, please login and try again.");
                    else {
                        System.out.print("Please confirm password: ");
                        final String password = ConsoleMenus.Scanner.nextLine();
                        final boolean success = OperateUserHelper.logoff(client, token.get(), password);
                        if (success) {
                            System.out.println("Success!");
                            token.set(null);
                        } else
                            System.out.println("Error, unknown reason.");
                    }
                }
                case 20 -> {
                    System.out.println("Listing users...");
                    if (token.get() == null)
                        System.out.println("Currently not logged in, please login and try again.");
                    else {
                        System.out.print("Please enter page number, or enter to page 1 (limit: " + GlobalConfiguration.getInstance().limit() + "): ");
                        final String choosePage = ConsoleMenus.Scanner.nextLine();
                        int i = 0;
                        if (!choosePage.isBlank())
                            i = Integer.parseInt(choosePage) - 1;
                        do {
                            final Pair.ImmutablePair<Long, List<VisibleUserInformation>> page = OperateUserHelper.listUsers(client, token.get(),
                                    GlobalConfiguration.getInstance().limit(), i, Options.OrderDirection.ASCEND);
                            if (page.getSecond().isEmpty() && i > 0)
                                break;
                            System.out.printf("Total: %d, Page: %d %n", page.getFirst().longValue(), i + 1);
                            final PrintTable table = PrintTable.create().setHeader(List.of("id", "username", "group"));
                            for (final VisibleUserInformation information : page.getSecond())
                                table.addBody(List.of(String.valueOf(information.id()), information.username(), information.group()));
                            table.print();
                            if ((long) (i + 1) * GlobalConfiguration.getInstance().limit() >= page.getFirst().longValue())
                                break;
                            System.out.print("Enter to continue, or any other char to exit: ");
                            if (!ConsoleMenus.Scanner.nextLine().isBlank())
                                break;
                            ++i;
                        } while (true);
                    }
                }
                case 21 -> {
                    System.out.println("Deleting user... WARNING!");
                    if (token.get() == null)
                        System.out.println("Currently not logged in, please login and try again.");
                    else {
                        System.out.print("Please enter username to delete: ");
                        final String username = ConsoleMenus.Scanner.nextLine();
                        // TODO check if equal to LOGOFF
                        final boolean success = OperateUserHelper.deleteUser(client, token.get(), username);
                        if (success)
                            System.out.println("Success!");
                        else
                            System.out.println("Error, unknown reason.");
                    }
                }
                // TODO
                case 40 -> {
                    System.out.println("Listing files...");
                    if (token.get() == null)
                        System.out.println("Currently not logged in, please login and try again.");
                    else {
                        System.out.print("Please enter directory path: ");
                        final DrivePath path = new DrivePath(ConsoleMenus.Scanner.nextLine());
                        System.out.print("Please enter page number, or enter to page 1 (limit: " + GlobalConfiguration.getInstance().limit() + "): ");
                        final String choosePage = ConsoleMenus.Scanner.nextLine();
                        int i = 0;
                        if (!choosePage.isBlank())
                            i = Integer.parseInt(choosePage) - 1;
                        do {
                            final Pair.ImmutablePair<Long, List<VisibleFileInformation>> page = OperateFileHelper.listFiles(client, token.get(),
                                    path, GlobalConfiguration.getInstance().limit(), i, Options.OrderPolicy.FileName, Options.OrderDirection.ASCEND);
                            if (page == null) {
                                System.out.print("Path is not exist.");
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
                            System.out.print("Enter to continue, or any other char to exit: ");
                            if (!ConsoleMenus.Scanner.nextLine().isBlank())
                                break;
                            ++i;
                        } while (true);
                    }
                }
                default -> {
                }
            }
        } catch (final NumberFormatException exception) {
            System.out.printf("NumberFormatException: %s %n", exception.getMessage());
            return true;
        } catch (final WrongStateException exception) {
            if (exception.getState() == Operation.State.NoPermission) {
                System.out.println("No permission!");
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
}
