/*
 * BackupRestoreHubCommand.java
 *
 * Copyright (C) 2012-2014 LucasEasedUp
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
package io.github.lucaseasedup.logit.command.hub;

import static io.github.lucaseasedup.logit.util.MessageHelper._;
import static io.github.lucaseasedup.logit.util.MessageHelper.sendMsg;
import io.github.lucaseasedup.logit.command.CommandHelpLine;
import java.io.File;
import org.bukkit.command.CommandSender;

public final class BackupRestoreHubCommand extends HubCommand
{
    public BackupRestoreHubCommand()
    {
        super("backup restore", new String[] {}, "logit.backup.restore", false, true,
                new CommandHelpLine.Builder()
                        .command("logit backup restore")
                        .descriptionLabel("subCmdDesc.backup.restore.newest")
                        .build());
    }
    
    @Override
    public void execute(CommandSender sender, String[] args)
    {
        File[] backups = getBackupManager().getBackups(true);
        
        if (backups.length == 0)
        {
            sendMsg(sender, _("restoreBackup.noBackups"));
            
            return;
        }
        
        String filename = backups[backups.length - 1].getName();
        
        new BackupRestoreFileHubCommand().execute(sender, new String[] {filename});
    }
}
