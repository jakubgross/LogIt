/*
 * RegisterCommand.java
 *
 * Copyright (C) 2012-2013 LucasEasedUp
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.lucaseasedup.logit.command;

import static io.github.lucaseasedup.logit.LogItPlugin.getMessage;
import static io.github.lucaseasedup.logit.util.MessageUtils.sendMessage;
import static io.github.lucaseasedup.logit.util.PlayerUtils.getPlayer;
import static io.github.lucaseasedup.logit.util.PlayerUtils.getPlayerIp;
import static io.github.lucaseasedup.logit.util.PlayerUtils.isPlayerOnline;
import io.github.lucaseasedup.logit.LogItCore;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author LucasEasedUp
 */
public class RegisterCommand extends AbstractCommandExecutor
{
    public RegisterCommand(LogItCore core)
    {
        super(core);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        Player p = null;
        
        try
        {
            p = (Player) sender;
        }
        catch (ClassCastException ex)
        {
        }
        
        if (args.length > 0 && args[0].equals("-x") && args.length <= 3)
        {
            if (p != null && ((core.isPlayerForcedToLogin(p) && !core.getSessionManager().isSessionAlive(p))
                    || !p.hasPermission("logit.register.others")))
            {
                sender.sendMessage(getMessage("NO_PERMS"));
            }
            else if (args.length < 2)
            {
                sender.sendMessage(getMessage("PARAM_MISSING").replace("%param%", "player"));
            }
            else if (args.length < 3)
            {
                sender.sendMessage(getMessage("PARAM_MISSING").replace("%param%", "password"));
            }
            else if (core.getAccountManager().isRegistered(args[1]))
            {
                sender.sendMessage(getMessage("CREATE_ACCOUNT_ALREADY_OTHERS").replace("%player%", args[1]));
            }
            else if (args[2].length() < core.getConfig().getInt("password.min-length"))
            {
                sender.sendMessage(getMessage("PASSWORD_TOO_SHORT").replace("%min-length%",
                        String.valueOf(core.getConfig().getInt("password.min-length"))));
            }
            else if (args[2].length() > core.getConfig().getInt("password.max-length"))
            {
                sender.sendMessage(getMessage("PASSWORD_TOO_LONG").replace("%max-length%",
                        String.valueOf(core.getConfig().getInt("password.max-length"))));
            }
            else
            {
                try
                {
                    if (core.getAccountManager().createAccount(args[1], args[2]))
                    {
                        sendMessage(args[1], getMessage("CREATE_ACCOUNT_SUCCESS_SELF"));
                        sender.sendMessage(getMessage("CREATE_ACCOUNT_SUCCESS_OTHERS").replace("%player%", args[1]));
                        
                        if (isPlayerOnline(args[1]))
                        {
                            core.getAccountManager().attachIp(args[1], getPlayerIp(getPlayer(args[1])));
                            
                            core.getSessionManager().startSession(args[1]);
                            sendMessage(args[1], getMessage("START_SESSION_SUCCESS_SELF"));
                            sender.sendMessage(getMessage("START_SESSION_SUCCESS_SELF"));
                            
                            if (core.getConfig().getBoolean("waiting-room.enabled")
                                && core.getConfig().getBoolean("waiting-room.newbie-teleport.enabled"))
                            {
                                getPlayer(args[1]).teleport(core.getWaitingRoom().getNewbieTeleportLocation());
                            }
                        }
                    }
                }
                catch (SQLException ex)
                {
                    sender.sendMessage(getMessage("CREATE_ACCOUNT_FAIL_OTHERS").replace("%player%", args[1]));
                }
            }
        }
        else if (args.length <= 2)
        {
            if (p == null)
            {
                sender.sendMessage(getMessage("ONLY_PLAYERS"));
            }
            else if (!p.hasPermission("logit.register.self"))
            {
                p.sendMessage(getMessage("NO_PERMS"));
            }
            else if (args.length < 1)
            {
                p.sendMessage(getMessage("PARAM_MISSING").replace("%param%", "password"));
            }
            else if (args.length < 2)
            {
                p.sendMessage(getMessage("PARAM_MISSING").replace("%param%", "confirmpassword"));
            }
            else if (core.getAccountManager().isRegistered(p.getName()))
            {
                p.sendMessage(getMessage("CREATE_ACCOUNT_ALREADY_SELF"));
            }
            else if (args[0].length() < core.getConfig().getInt("password.min-length"))
            {
                p.sendMessage(getMessage("PASSWORD_TOO_SHORT").replace("%min-length%",
                        String.valueOf(core.getConfig().getInt("password.min-length"))));
            }
            else if (args[0].length() > core.getConfig().getInt("password.max-length"))
            {
                p.sendMessage(getMessage("PASSWORD_TOO_LONG").replace("%max-length%",
                        String.valueOf(core.getConfig().getInt("password.max-length"))));
            }
            else if (!args[0].equals(args[1]))
            {
                p.sendMessage(getMessage("PASSWORDS_DO_NOT_MATCH"));
            }
            else if (core.getAccountManager().countAccountsWithIp(getPlayerIp(p)) >= core.getConfig().getInt("crowd-control.accounts-per-ip.amount")
                && !core.getConfig().getStringList("crowd-control.accounts-per-ip.unrestricted-ips").contains(getPlayerIp(p)))
            {
                p.sendMessage(getMessage("ACCOUNTS_PER_IP_LIMIT"));
            }
            else
            {
                try
                {
                    if (core.getAccountManager().createAccount(p.getName(), args[0]))
                    {
                        sender.sendMessage(getMessage("CREATE_ACCOUNT_SUCCESS_SELF"));
                        
                        core.getAccountManager().attachIp(p.getName(), getPlayerIp(p));
                        
                        core.getSessionManager().startSession(p.getName());
                        sender.sendMessage(getMessage("START_SESSION_SUCCESS_SELF"));
                        
                        if (core.getConfig().getBoolean("waiting-room.enabled")
                            && core.getConfig().getBoolean("waiting-room.newbie-teleport.enabled"))
                        {
                            p.teleport(core.getWaitingRoom().getNewbieTeleportLocation());
                        }
                    }
                }
                catch (SQLException ex)
                {
                    sender.sendMessage(getMessage("CREATE_ACCOUNT_FAIL_SELF"));
                }
            }
        }
        else
        {
            sender.sendMessage(getMessage("INCORRECT_PARAMETER_COMBINATION"));
        }
        
        return true;
    }
}
