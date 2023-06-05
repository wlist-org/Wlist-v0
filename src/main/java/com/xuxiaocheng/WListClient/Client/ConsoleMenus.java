package com.xuxiaocheng.WListClient.Client;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateUserHelper;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.WrongStateException;
import com.xuxiaocheng.WListClient.Server.Operation;
import com.xuxiaocheng.WListClient.Server.Options;
import com.xuxiaocheng.WListClient.Server.VisibleUserInformation;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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
            .addBody(List.of("1", "Login", ""))
            .addBody(List.of("2", "Register", ""))
            .addBody(List.of("3", "Change password", ""))
            .addBody(List.of("4", "Logoff", ""))
            .addBody(List.of("5", "List users", ""));
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static boolean chooseMenu(final @NotNull WListClient client, final @NotNull AtomicReference<String> token) {
        ConsoleMenus.menu.print();
        try {
            System.out.print("Please enter operation id: ");
            final int mode = Integer.parseInt(ConsoleMenus.Scanner.nextLine());
            switch (mode) {
                case 0 -> {return false;}
                case 1 -> {
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
                case 2 -> {
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
                case 3 -> {
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
                case 4 -> {
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
                case 5 -> {
                    System.out.println("Listing users...");
                    if (token.get() == null)
                        System.out.println("Currently not logged in, please login and try again.");
                    else {
                        int i = 0;
                        do {
                            final Pair.ImmutablePair<Long, List<VisibleUserInformation>> page = OperateUserHelper.listUsers(client, token.get(),
                                    GlobalConfiguration.getInstance().limit(), i, Options.OrderDirection.ASCEND);
                            if (page.getSecond().isEmpty() && i > 0)
                                break;
                            System.out.printf("Total: %d, Page: %d %n", page.getFirst().longValue(), i);
                            final PrintTable table = PrintTable.create();
                            if (GlobalConfiguration.getInstance().showPermissions()) {
                                table.setHeader(List.of("id", "username", "permissions"));
                                for (final VisibleUserInformation information : page.getSecond())
                                    table.addBody(List.of(String.valueOf(information.id()),
                                            information.username(), information.permissions().toString()));
                            } else {
                                table.setHeader(List.of("id", "username"));
                                for (final VisibleUserInformation information : page.getSecond())
                                    table.addBody(List.of(String.valueOf(information.id()), information.username()));
                            }
                            table.print();
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
