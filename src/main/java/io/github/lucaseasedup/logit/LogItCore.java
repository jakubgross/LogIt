/*
 * LogItCore.java
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
package io.github.lucaseasedup.logit;

import static io.github.lucaseasedup.logit.LogItPlugin.getMessage;
import static io.github.lucaseasedup.logit.hash.HashGenerator.getBCrypt;
import static io.github.lucaseasedup.logit.hash.HashGenerator.getMd2;
import static io.github.lucaseasedup.logit.hash.HashGenerator.getMd5;
import static io.github.lucaseasedup.logit.hash.HashGenerator.getSha1;
import static io.github.lucaseasedup.logit.hash.HashGenerator.getSha256;
import static io.github.lucaseasedup.logit.hash.HashGenerator.getSha384;
import static io.github.lucaseasedup.logit.hash.HashGenerator.getSha512;
import static io.github.lucaseasedup.logit.hash.HashGenerator.getWhirlpool;
import static io.github.lucaseasedup.logit.util.CollectionUtils.containsIgnoreCase;
import io.github.lucaseasedup.logit.account.AccountKeys;
import io.github.lucaseasedup.logit.account.AccountManager;
import io.github.lucaseasedup.logit.account.AccountWatcher;
import io.github.lucaseasedup.logit.backup.BackupManager;
import io.github.lucaseasedup.logit.command.AcclockCommand;
import io.github.lucaseasedup.logit.command.AccunlockCommand;
import io.github.lucaseasedup.logit.command.ChangeEmailCommand;
import io.github.lucaseasedup.logit.command.ChangePassCommand;
import io.github.lucaseasedup.logit.command.DisabledCommandExecutor;
import io.github.lucaseasedup.logit.command.LogItCommand;
import io.github.lucaseasedup.logit.command.LoginCommand;
import io.github.lucaseasedup.logit.command.LogoutCommand;
import io.github.lucaseasedup.logit.command.NopCommandExecutor;
import io.github.lucaseasedup.logit.command.ProfileCommand;
import io.github.lucaseasedup.logit.command.RecoverPassCommand;
import io.github.lucaseasedup.logit.command.RegisterCommand;
import io.github.lucaseasedup.logit.command.RememberCommand;
import io.github.lucaseasedup.logit.command.UnregisterCommand;
import io.github.lucaseasedup.logit.config.InvalidPropertyValueException;
import io.github.lucaseasedup.logit.config.LogItConfiguration;
import io.github.lucaseasedup.logit.hash.BCrypt;
import io.github.lucaseasedup.logit.hash.HashGenerator;
import io.github.lucaseasedup.logit.listener.BlockEventListener;
import io.github.lucaseasedup.logit.listener.EntityEventListener;
import io.github.lucaseasedup.logit.listener.InventoryEventListener;
import io.github.lucaseasedup.logit.listener.PlayerEventListener;
import io.github.lucaseasedup.logit.listener.ServerEventListener;
import io.github.lucaseasedup.logit.listener.SessionEventListener;
import io.github.lucaseasedup.logit.listener.TickEventListener;
import io.github.lucaseasedup.logit.locale.EnglishLocale;
import io.github.lucaseasedup.logit.locale.GermanLocale;
import io.github.lucaseasedup.logit.locale.Locale;
import io.github.lucaseasedup.logit.locale.LocaleManager;
import io.github.lucaseasedup.logit.locale.PolishLocale;
import io.github.lucaseasedup.logit.mail.MailSender;
import io.github.lucaseasedup.logit.persistence.AirBarSerializer;
import io.github.lucaseasedup.logit.persistence.ExperienceSerializer;
import io.github.lucaseasedup.logit.persistence.HealthBarSerializer;
import io.github.lucaseasedup.logit.persistence.HungerBarSerializer;
import io.github.lucaseasedup.logit.persistence.LocationSerializer;
import io.github.lucaseasedup.logit.persistence.PersistenceManager;
import io.github.lucaseasedup.logit.persistence.PersistenceSerializer;
import io.github.lucaseasedup.logit.profile.ProfileManager;
import io.github.lucaseasedup.logit.session.SessionManager;
import io.github.lucaseasedup.logit.storage.CacheType;
import io.github.lucaseasedup.logit.storage.CsvStorage;
import io.github.lucaseasedup.logit.storage.H2Storage;
import io.github.lucaseasedup.logit.storage.MySqlStorage;
import io.github.lucaseasedup.logit.storage.NullStorage;
import io.github.lucaseasedup.logit.storage.PostgreSqlStorage;
import io.github.lucaseasedup.logit.storage.SqliteStorage;
import io.github.lucaseasedup.logit.storage.Storage;
import io.github.lucaseasedup.logit.storage.Storage.Type;
import io.github.lucaseasedup.logit.storage.WrapperStorage;
import io.github.lucaseasedup.logit.util.HashtableBuilder;
import io.github.lucaseasedup.logit.util.IoUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * The central part of LogIt.
 */
public final class LogItCore
{
    private LogItCore(LogItPlugin plugin)
    {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        this.firstRun = !getDataFile("config.yml").exists();
    }
    
    /**
     * Starts up the {@code LogItCore} if stopped.
     * 
     * @throws FatalReportedException if critical error occured and LogIt could not start.
     * 
     * @see #isStarted()
     * @see #stop()
     */
    public void start() throws FatalReportedException
    {
        if (started)
            return;
        
        dataFolder.mkdir();
        getDataFile("lib").mkdir();
        
        config = new LogItConfiguration();
        
        try
        {
            config.load();
        }
        catch (IOException ex)
        {
            log(Level.SEVERE, "Could not load the configuration file.", ex);
            
            FatalReportedException.throwNew(ex);
        }
        catch (InvalidPropertyValueException ex)
        {
            log(Level.SEVERE, "Invalid configuration property value: " + ex.getPropertyName());
            
            FatalReportedException.throwNew(ex);
        }
        
        if (config.getBoolean("logging.file.enabled"))
        {
            openLogFile(config.getString("logging.file.filename"));
        }
        
        if (firstRun)
        {
            getDataFile("backup").mkdir();
            getDataFile("mail").mkdir();
            getDataFile("lang").mkdir();
            
            File passwordRecoveryTemplateFile = getDataFile("mail/password-recovery.html");
            
            if (!passwordRecoveryTemplateFile.exists())
            {
                try
                {
                    IoUtils.extractResource("password-recovery.html", passwordRecoveryTemplateFile);
                }
                catch (IOException ex)
                {
                    log(Level.WARNING, "Could not copy resource password-recovery.html.", ex);
                }
            }
        }
        
        StorageType leadingStorageType = StorageType.decode(
            plugin.getConfig().getString("storage.accounts.leading.storage-type")
        );
        
        StorageType mirrorStorageType = StorageType.decode(
            plugin.getConfig().getString("storage.accounts.mirror.storage-type")
        );
        
        try
        {
            ReportedException.incrementRequestCount();
            
            if (leadingStorageType.equals(StorageType.H2)
                    || mirrorStorageType.equals(StorageType.H2))
            {
                LogItPlugin.loadLibrary(LIB_H2);
            }
            
            if (leadingStorageType.equals(StorageType.POSTGRESQL)
                    || mirrorStorageType.equals(StorageType.POSTGRESQL))
            {
                LogItPlugin.loadLibrary(LIB_POSTGRESQL);
            }
            
            if (config.getBoolean("password-recovery.enabled"))
            {
                LogItPlugin.loadLibrary(LIB_MAIL);
            }
        }
        catch (ReportedException ex)
        {
            ex.rethrowAsFatal();
        }
        finally
        {
            ReportedException.decrementRequestCount();
        }
        
        Storage leadingAccountStorage = null;
        
        switch (leadingStorageType)
        {
        case NONE:
            leadingAccountStorage = new NullStorage();
            break;
        
        case SQLITE:
            leadingAccountStorage = new SqliteStorage("jdbc:sqlite:" + dataFolder + "/"
                    + config.getString("storage.accounts.leading.sqlite.filename"));
            break;
            
        case MYSQL:
            leadingAccountStorage = new MySqlStorage(
                    config.getString("storage.accounts.leading.mysql.host"),
                    config.getString("storage.accounts.leading.mysql.user"),
                    config.getString("storage.accounts.leading.mysql.password"),
                    config.getString("storage.accounts.leading.mysql.database"));
            break;
            
        case H2:
            leadingAccountStorage = new H2Storage("jdbc:h2:" + dataFolder
                    + "/" + config.getString("storage.accounts.leading.h2.filename"));
            break;
            
        case POSTGRESQL:
            leadingAccountStorage = new PostgreSqlStorage(
                    config.getString("storage.accounts.leading.postgresql.host"),
                    config.getString("storage.accounts.leading.postgresql.user"),
                    config.getString("storage.accounts.leading.postgresql.password"));
            break;
            
        case CSV:
        {
            File dir = new File(dataFolder, config.getString("storage.accounts.leading.csv.dir"));
            
            if (!dir.exists())
                dir.mkdir();
            
            leadingAccountStorage = new CsvStorage(dir);
            
            break;
        }
            
        default:
            FatalReportedException.throwNew();
        }
        
        Storage mirrorAccountStorage = null;
        
        switch (mirrorStorageType)
        {
        case NONE:
            mirrorAccountStorage = new NullStorage();
            break;
        
        case SQLITE:
            mirrorAccountStorage = new SqliteStorage("jdbc:sqlite:" + dataFolder + "/"
                    + config.getString("storage.accounts.mirror.sqlite.filename"));
            break;
            
        case MYSQL:
            mirrorAccountStorage = new MySqlStorage(
                    config.getString("storage.accounts.mirror.mysql.host"),
                    config.getString("storage.accounts.mirror.mysql.user"),
                    config.getString("storage.accounts.mirror.mysql.password"),
                    config.getString("storage.accounts.mirror.mysql.database"));
            break;
            
        case H2:
            mirrorAccountStorage = new H2Storage("jdbc:h2:" + dataFolder
                    + "/" + config.getString("storage.accounts.mirror.h2.filename"));
            break;
            
        case POSTGRESQL:
            mirrorAccountStorage = new PostgreSqlStorage(
                    config.getString("storage.accounts.mirror.postgresql.host"),
                    config.getString("storage.accounts.mirror.postgresql.user"),
                    config.getString("storage.accounts.mirror.postgresql.password"));
            break;
            
        case CSV:
        {
            File dir = new File(dataFolder, config.getString("storage.accounts.mirror.csv.dir"));
            
            if (!dir.exists())
                dir.mkdir();
            
            mirrorAccountStorage = new CsvStorage(dir);
            
            break;
        }
        
        default:
            FatalReportedException.throwNew();
        }
        
        CacheType accountCacheType =
                CacheType.decode(config.getString("storage.accounts.leading.cache"));
        
        @SuppressWarnings("resource")
        WrapperStorage accountStorage = new WrapperStorage(leadingAccountStorage, accountCacheType);
        accountStorage.mirrorStorage(mirrorAccountStorage,
                new HashtableBuilder<String, String>()
                .add(
                    config.getString("storage.accounts.leading.unit"),
                    config.getString("storage.accounts.mirror.unit")
                ).build());
        
        try
        {
            accountStorage.connect();
        }
        catch (IOException ex)
        {
            log(Level.SEVERE, "Could not establish database connection.", ex);
            
            FatalReportedException.throwNew(ex);
        }
        
        String accountsUnit = config.getString("storage.accounts.leading.unit");
        
        AccountKeys accountKeys = new AccountKeys(
            config.getString("storage.accounts.keys.username"),
            config.getString("storage.accounts.keys.salt"),
            config.getString("storage.accounts.keys.password"),
            config.getString("storage.accounts.keys.hashing_algorithm"),
            config.getString("storage.accounts.keys.ip"),
            config.getString("storage.accounts.keys.login_session"),
            config.getString("storage.accounts.keys.email"),
            config.getString("storage.accounts.keys.last_active_date"),
            config.getString("storage.accounts.keys.reg_date"),
            config.getString("storage.accounts.keys.is_locked"),
            config.getString("storage.accounts.keys.persistence")
        );
        
        try
        {
            accountStorage.createUnit(accountsUnit, accountKeys);
        }
        catch (IOException ex)
        {
            log(Level.SEVERE, "Could not create accounts table.", ex);
            
            FatalReportedException.throwNew(ex);
        }
        
        try
        {
            accountStorage.setAutobatchEnabled(true);
            
            Hashtable<String, Type> existingKeys = accountStorage.getKeys(accountsUnit);
            
            for (Map.Entry<String, Type> e : accountKeys.entrySet())
            {
                if (!existingKeys.containsKey(e.getKey()))
                {
                    accountStorage.addKey(accountsUnit, e.getKey(), e.getValue());
                }
            }
            
            accountStorage.executeBatch();
            accountStorage.clearBatch();
            accountStorage.setAutobatchEnabled(false);
        }
        catch (IOException ex)
        {
            log(Level.SEVERE, "Could not update accounts table columns.", ex);
            
            FatalReportedException.throwNew(ex);
        }
        
        if (accountCacheType == CacheType.PRELOADED)
        {
            try
            {
                accountStorage.selectEntries(config.getString("storage.accounts.leading.unit"));
            }
            catch (IOException ex)
            {
                log(Level.SEVERE, "Could not preload accounts.", ex);
            }
        }
        
        accountManager = new AccountManager(accountStorage, accountsUnit, accountKeys);
        
        if (config.getBoolean("profiles.enabled"))
        {
            File profilesPath = getDataFile(config.getString("profiles.path"));
            
            if (!profilesPath.exists())
            {
                profilesPath.mkdir();
            }
            
            profileManager = new ProfileManager(profilesPath,
                    config.getConfigurationSection("profiles.fields"));
        }
        
        persistenceManager = new PersistenceManager();
        
        setSerializerEnabled(LocationSerializer.class,
                config.getBoolean("waiting-room.enabled"));
        setSerializerEnabled(AirBarSerializer.class,
                config.getBoolean("force-login.obfuscate-bars.air"));
        setSerializerEnabled(HealthBarSerializer.class,
                config.getBoolean("force-login.obfuscate-bars.health"));
        setSerializerEnabled(ExperienceSerializer.class,
                config.getBoolean("force-login.obfuscate-bars.experience"));
        setSerializerEnabled(HungerBarSerializer.class,
                config.getBoolean("force-login.obfuscate-bars.hunger"));
        
        accountWatcher = new AccountWatcher();
        backupManager  = new BackupManager();
        sessionManager = new SessionManager();
        tickEventCaller = new TickEventCaller();
        
        localeManager = new LocaleManager();
        localeManager.registerLocale(EnglishLocale.getInstance());
        localeManager.registerLocale(PolishLocale.getInstance());
        localeManager.registerLocale(GermanLocale.getInstance());
        localeManager.setFallbackLocale(EnglishLocale.getInstance());
        
        Locale locale = localeManager.getLocale(getConfig().getString("locale"));
        
        if (locale == null)
        {
            locale = localeManager.getFallbackLocale();
        }
        
        localeManager.switchActiveLocale(locale);
        
        if (config.getBoolean("password-recovery.enabled"))
        {
            mailSender = new MailSender();
            mailSender.configure(
                config.getString("mail.smtp-host"),
                config.getInt("mail.smtp-port"),
                config.getString("mail.smtp-user"),
                config.getString("mail.smtp-password")
            );
        }
        
        accountManagerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,
                accountManager, 0, AccountManager.TASK_PERIOD);
        sessionManagerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,
                sessionManager, 0, SessionManager.TASK_PERIOD);
        tickEventCallerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,
                tickEventCaller, 0, TickEventCaller.TASK_PERIOD);
        accountWatcherTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,
                accountWatcher, 0, AccountWatcher.TASK_PERIOD);
        backupManagerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,
                backupManager, 0, BackupManager.TASK_PERIOD);
        
        if (Bukkit.getPluginManager().isPluginEnabled("Vault"))
        {
            vaultPermissions =
                    Bukkit.getServicesManager().getRegistration(Permission.class).getProvider();
        }
        
        registerEvents();
        setCommandExecutors();
        
        started = true;
        
        log(Level.FINE, getMessage("PLUGIN_START_SUCCESS"));
        
        if (firstRun)
        {
            log(Level.INFO, getMessage("PLUGIN_FIRST_RUN"));
        }
    }
    
    /**
     * Stops the {@code LogItCore} if started.
     * 
     * @see #isStarted()
     * @see #start()
     */
    public void stop()
    {
        if (!started)
            return;
        
        persistenceManager.unregisterSerializer(LocationSerializer.class);
        persistenceManager.unregisterSerializer(AirBarSerializer.class);
        persistenceManager.unregisterSerializer(HealthBarSerializer.class);
        persistenceManager.unregisterSerializer(ExperienceSerializer.class);
        persistenceManager.unregisterSerializer(HungerBarSerializer.class);
        
        try
        {
            accountManager.getStorage().close();
        }
        catch (IOException ex)
        {
            log(Level.WARNING, "Could not close database connection.", ex);
        }
        
        Bukkit.getScheduler().cancelTask(accountManagerTaskId);
        Bukkit.getScheduler().cancelTask(sessionManagerTaskId);
        Bukkit.getScheduler().cancelTask(tickEventCallerTaskId);
        Bukkit.getScheduler().cancelTask(accountWatcherTaskId);
        Bukkit.getScheduler().cancelTask(backupManagerTaskId);
        
        // Unregister all event listeners.
        HandlerList.unregisterAll(plugin);
        
        if (logFileWriter != null)
        {
            try
            {
                logFileWriter.close();
            }
            catch (IOException ex)
            {
                log(Level.WARNING, "Could not close log file.", ex);
            }
            
            logFileWriter = null;
        }
        
        vaultPermissions = null;
        sessionManager = null;
        accountManager = null;
        accountWatcher = null;
        backupManager = null;
        persistenceManager = null;
        profileManager = null;
        mailSender = null;
        localeManager = null;
        tickEventCaller = null;
        
        started = false;
        
        log(Level.FINE, getMessage("PLUGIN_STOP_SUCCESS"));
    }
    
    /**
     * Restarts the {@code LogItCore}, if started,
     * by invoking {@link #stop} and {@link #start}.
     * 
     * @throws FatalReportedException if LogItCore could not be started again.
     * 
     * @see #isStarted()
     */
    public void restart() throws FatalReportedException
    {
        if (!started)
            return;
        
        File sessionFile = getDataFile(config.getString("storage.sessions.filename"));
        
        try
        {
            sessionManager.exportSessions(sessionFile);
        }
        catch (IOException ex)
        {
            log(Level.WARNING, "Could not export sessions.", ex);
        }
        
        stop();
        
        try
        {
            plugin.loadMessages();
        }
        catch (IOException ex)
        {
            log(Level.WARNING, "Could not load messages.", ex);
        }
        
        start();
        
        try
        {
            sessionManager.importSessions(sessionFile);
        }
        catch (IOException ex)
        {
            log(Level.WARNING, "Could not import sessions.", ex);
        }
        
        sessionFile.delete();
        
        log(Level.INFO, getMessage("RELOADED"));
    }
    
    /**
     * Checks if a plain-text password is equal, after hashing, to {@code hashedPassword}.
     * 
     * If "password.use-global-hashing-algorithm" is set to true,
     * the global hashing algorithm will be used instead of {@code hashingAlgorithm}.
     * 
     * @param password         the plain-text password.
     * @param hashedPassword   the hashed password.
     * @param hashingAlgorithm the algorithm used when hashing {@code hashedPassword}.
     * 
     * @return {@code true} if passwords match; {@code false} otherwise.
     * 
     * @see #checkPassword(String, String, String, HashingAlgorithm)
     */
    public boolean checkPassword(String password, String hashedPassword,
                                 HashingAlgorithm hashingAlgorithm)
    {
        if (hashingAlgorithm == null || config.getBoolean("password.use-global-hashing-algorithm"))
        {
            hashingAlgorithm = getDefaultHashingAlgorithm();
        }
        
        if (hashingAlgorithm == HashingAlgorithm.BCRYPT)
        {
            return BCrypt.checkpw(password, hashedPassword);
        }
        else
        {
            return hashedPassword.equals(hash(password, hashingAlgorithm));
        }
    }
    
    /**
     * Checks if a plain-text password with a salt appended
     * is equal, after hashing, to {@code hashedPassword}.
     * 
     * If "password.use-global-hashing-algorithm" is set to true,
     * the global hashing algorithm will be used instead of {@code hashingAlgorithm}.
     * 
     * @param password         the plain-text password.
     * @param hashedPassword   the hashed password.
     * @param salt             the salt for the passwords.
     * @param hashingAlgorithm the algorithm used when hashing {@code hashedPassword}.
     * 
     * @return {@code true} if passwords match; {@code false} otherwise.
     * 
     * @see #checkPassword(String, String, HashingAlgorithm)
     */
    public boolean checkPassword(String password,
                                 String hashedPassword,
                                 String salt,
                                 HashingAlgorithm hashingAlgorithm)
    {
        if (hashedPassword == null || hashedPassword.isEmpty())
            return false;
        
        if (hashingAlgorithm == null || config.getBoolean("password.use-global-hashing-algorithm"))
        {
            hashingAlgorithm = getDefaultHashingAlgorithm();
        }
        
        if (hashingAlgorithm == HashingAlgorithm.BCRYPT)
        {
            try
            {
                return BCrypt.checkpw(password, hashedPassword);
            }
            catch (IllegalArgumentException ex)
            {
                return false;
            }
        }
        else
        {
            if (config.getBoolean("password.use-salt"))
            {
                return hashedPassword.equals(hash(password, salt, hashingAlgorithm));
            }
            else
            {
                return hashedPassword.equals(hash(password, hashingAlgorithm));
            }
        }
    }
    
    /**
     * Checks if a password is equal, after hashing
     * using the default algorithm, to the global password.
     * 
     * @param password the plain-text password.
     * 
     * @return {@code true} if the passwords match; {@code false} otherwise.
     */
    public boolean checkGlobalPassword(String password)
    {
        return checkPassword(password, config.getString("password.global-password.hash"),
            config.getString("password.global-password.salt"), getDefaultHashingAlgorithm());
    }

    /**
     * Changes the global password.
     * 
     * <p> Hashes {@code password} with a random salt using the default algorithm.
     * 
     * @param password the new global password.
     */
    public void changeGlobalPassword(String password)
    {
        String salt = HashGenerator.generateSalt(getDefaultHashingAlgorithm());
        
        config.set("password.global-password.salt", salt);
        config.set("password.global-password.hash",
                hash(password, salt, getDefaultHashingAlgorithm()));
        
        log(Level.INFO, getMessage("GLOBALPASS_SET_SUCCESS"));
    }
    
    public void removeGlobalPassword()
    {
        config.set("password.global-password.hash", "");
        config.set("password.global-password.salt", "");
        
        log(Level.INFO, getMessage("GLOBALPASS_REMOVE_SUCCESS"));
    }
    
    /**
     * Changes a player's password to a randomly generated one,
     * and sends it to the player's e-mail address.
     * 
     * @param username a username of the player whom the mail will be sent.
     */
    public void recoverPassword(String username)
    {
        try
        {
            ReportedException.incrementRequestCount();
            
            if (mailSender == null)
                throw new RuntimeException("MailSender not initialized.");
            
            String to = accountManager.getEmail(username);
            String from = config.getString("mail.email-address");
            String subject = config.getString("password-recovery.subject")
                    .replace("%player%", username);
            
            int passwordLength = config.getInt("password-recovery.password-length");
            String newPassword = generatePassword(passwordLength,
                    config.getString("password-recovery.password-combination"));
            accountManager.changeAccountPassword(username, newPassword);
            
            File bodyTemplateFile =
                    getDataFile(config.getString("password-recovery.body-template"));
            String bodyTemplate;
            
            try (InputStream bodyTemplateInputStream = new FileInputStream(bodyTemplateFile))
            {
                bodyTemplate = IoUtils.toString(bodyTemplateInputStream);
            }
            
            String body = bodyTemplate
                    .replace("%player%", username)
                    .replace("%password%", newPassword);
            
            mailSender.sendMail(Arrays.asList(to), from, subject, body,
                    config.getBoolean("password-recovery.html-enabled"));
            
            log(Level.FINE, getMessage("RECOVER_PASSWORD_SUCCESS_LOG")
                    .replace("%player%", username)
                    .replace("%email%", to));
        }
        catch (ReportedException | IOException ex)
        {
            log(Level.WARNING, getMessage("RECOVER_PASSWORD_FAIL_LOG")
                    .replace("%player%", username), ex);
            
            ReportedException.throwNew(ex);
        }
        finally
        {
            ReportedException.decrementRequestCount();
        }
    }
    
    /**
     * Generates a random password of length equal to {@code length},
     * consisting only of the characters contained in {@code combination}.
     * 
     * <p> If {@code combination} contains more than one occurence of a character,
     * the overall probability of using it in password generation will be higher.
     * 
     * @param length      the desired password length.
     * @param combination the letterset used in the generation process.
     * 
     * @return the generated password.
     */
    public String generatePassword(int length, String combination)
    {
        char[] charArray = combination.toCharArray();
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();
        
        for (int i = 0, n = charArray.length; i < length; i++)
        {
            sb.append(charArray[random.nextInt(n)]);
        }
        
        return sb.toString();
    }
    
    /**
     * Checks if a player is forced to log in.
     * 
     * <p> Returns {@code true} if <i>"force-login.global"</i> is set to <i>true</i>,
     * or the player is in a world with forced login; {@code false} otherwise.
     * 
     * <p> If the player name is contained in <i>"force-login.exempt-players"</i>,
     * it always returns {@code false}.
     * 
     * <p> Note that this method does not check if the player is logged in.
     * For that purpose, use {@link SessionManager#isSessionAlive(Player)}
     * or {@link SessionManager#isSessionAlive(String)}.
     * 
     * @param player the player whom this check will be ran on.
     * 
     * @return {@code true} if the player is forced to log in; {@code false} otherwise.
     */
    public boolean isPlayerForcedToLogIn(Player player)
    {
        String worldName = player.getWorld().getName();
        
        return (config.getBoolean("force-login.global")
                || config.getStringList("force-login.in-worlds").contains(worldName))
             && !containsIgnoreCase(player.getName(),
                     config.getStringList("force-login.exempt-players"));
    }
    
    /**
     * Sends a message to the specified player telling them to either log in or register
     * if it has been enabled in the config.
     * 
     * @param player the player to whom the message will be sent.
     */
    public void sendForceLoginMessage(Player player)
    {
        long minInterval =
                config.getTime("force-login.prompt.min-interval", TimeUnit.MILLISECONDS);
        
        if (minInterval > 0)
        {
            long currentTimeMillis = System.currentTimeMillis();
            Long playerInterval = forceLoginPromptIntervals.get(player);
            
            if (playerInterval != null && currentTimeMillis - playerInterval < minInterval)
                return;
            
            forceLoginPromptIntervals.put(player, currentTimeMillis);
        }
        
        if (accountManager.isRegistered(player.getName()))
        {
            if (config.getBoolean("force-login.prompt.login"))
            {
                if (!config.getBoolean("password.disable-passwords"))
                {
                    player.sendMessage(getMessage("PLEASE_LOGIN"));
                }
                else
                {
                    player.sendMessage(getMessage("PLEASE_LOGIN_NOPASS"));
                }
            }
        }
        else
        {
            if (config.getBoolean("force-login.prompt.register"))
            {
                if (!config.getBoolean("password.disable-passwords"))
                {
                    player.sendMessage(getMessage("PLEASE_REGISTER"));
                }
                else
                {
                    player.sendMessage(getMessage("PLEASE_REGISTER_NOPASS"));
                }
            }
        }
    }
    
    /**
     * Updates permission groups for a player if LogIt is linked to Vault.
     * 
     * <p> Permission groups currently supported: <ul>
     *  <li>Registered</li>
     *  <li>Not registered</li>
     *  <li>Logged in</li>
     *  <li>Logged out</li>
     * </ul>
     * 
     * <p> Exact group names are taken from the LogIt configuration file.
     * 
     * @param player the player whose permission groups should be updated.
     */
    public void updatePlayerGroup(Player player)
    {
        if (!isLinkedToVault())
            return;
        
        if (accountManager.isRegistered(player.getName()))
        {
            vaultPermissions.playerRemoveGroup(player, config.getString("groups.unregistered"));
            vaultPermissions.playerAddGroup(player, config.getString("groups.registered"));
        }
        else
        {
            vaultPermissions.playerRemoveGroup(player, config.getString("groups.registered"));
            vaultPermissions.playerAddGroup(player, config.getString("groups.unregistered"));
        }
        
        if (sessionManager.isSessionAlive(player))
        {
            vaultPermissions.playerRemoveGroup(player, config.getString("groups.logged-out"));
            vaultPermissions.playerAddGroup(player, config.getString("groups.logged-in"));
        }
        else
        {
            vaultPermissions.playerRemoveGroup(player, config.getString("groups.logged-in"));
            vaultPermissions.playerAddGroup(player, config.getString("groups.logged-out"));
        }
    }
    
    /**
     * Checks if LogIt is linked to Vault.
     * 
     * @return {@code true} if LogIt is linked to Vault; {@code false} otherwise.
     */
    public boolean isLinkedToVault()
    {
        return vaultPermissions != null;
    }
    
    /**
     * Hashes a string using the specified algorithm.
     * 
     * @param string           the string to be hashed.
     * @param hashingAlgorithm the hashing algorithm to be used.
     * 
     * @return the resulting hash.
     * 
     * @throws IllegalArgumentException if this method does not support the given algorithm.
     * 
     * @see #hash(String, String, HashingAlgorithm)
     */
    public String hash(String string, HashingAlgorithm hashingAlgorithm)
    {
        switch (hashingAlgorithm)
        {
            case PLAIN:
                return string;
                
            case MD2:
                return getMd2(string);
                
            case MD5:
                return getMd5(string);
                
            case SHA1:
                return getSha1(string);
                
            case SHA256:
                return getSha256(string);
                
            case SHA384:
                return getSha384(string);
                
            case SHA512:
                return getSha512(string);
                
            case WHIRLPOOL:
                return getWhirlpool(string);
                
            case BCRYPT:
                return getBCrypt(string, "");
                
            default:
                throw new IllegalArgumentException("Unknown algorithm: " + hashingAlgorithm);
        }
    }
    
    /**
     * Hashes a string with a salt using the specified algorithm.
     * 
     * @param string           the string to be hashed.
     * @param salt             the salt to be appended to {@code string}.
     * @param hashingAlgorithm the hashing algorithm to be used.
     * 
     * @return resulting hash.
     * 
     * @see #hash(String, HashingAlgorithm)
     */
    public String hash(String string, String salt, HashingAlgorithm hashingAlgorithm)
    {
        String hash;
        
        if (hashingAlgorithm == HashingAlgorithm.BCRYPT)
        {
            hash = getBCrypt(string, salt);
        }
        else if (hashingAlgorithm == HashingAlgorithm.PLAIN)
        {
            hash = hash(string, hashingAlgorithm);
        }
        else
        {
            hash = hash(string + salt, hashingAlgorithm);
        }
        
        return hash;
    }
    
    public HashingAlgorithm getDefaultHashingAlgorithm()
    {
        return HashingAlgorithm.decode(plugin.getConfig().getString("password.hashing-algorithm"));
    }
    
    public IntegrationType getIntegration()
    {
        return IntegrationType.decode(plugin.getConfig().getString("integration"));
    }
    
    /**
     * Logs a message in the name of LogIt.
     * 
     * @param level   the message level.
     * @param message the message to be logged.
     * 
     * @see #log(Level, String, Throwable)
     */
    public void log(Level level, String message)
    {
        if (level == null)
            throw new IllegalArgumentException();
        
        if (config != null && config.isLoaded())
        {
            if (config.getBoolean("logging.file.enabled")
                    && level.intValue() >= config.getInt("logging.file.level"))
            {
                if (logFileWriter == null)
                {
                    openLogFile(config.getString("logging.file.filename"));
                }
                
                try
                {
                    logFileWriter.write(logDateFormat.format(new Date()));
                    logFileWriter.write(" [");
                    logFileWriter.write(level.getName());
                    logFileWriter.write("] ");
                    logFileWriter.write(ChatColor.stripColor(message));
                    logFileWriter.write("\n");
                }
                catch (IOException ex)
                {
                    plugin.getLogger().log(Level.WARNING, "Could not log to file.", ex);
                }
            }
            
            if (config.getBoolean("logging.verbose-console"))
            {
                System.out.println("[" + level + "] " + ChatColor.stripColor(message));
                
                return;
            }
        }
        
        plugin.getLogger().log(level, ChatColor.stripColor(message));
    }
    
    /**
     * Logs a message with a {@code Throwable} in the name of LogIt.
     * 
     * @param level     the message level.
     * @param message   the message to be logged.
     * @param throwable the throwable whose stack trace should be appended to the log.
     * 
     * @see #log(Level, String)
     */
    public void log(Level level, String message, Throwable throwable)
    {
        StringWriter sw = new StringWriter();
        
        try (PrintWriter pw = new PrintWriter(sw))
        {
            throwable.printStackTrace(pw);
        }
        
        log(level, message + " [Exception stack trace:\n" + sw.toString() + "]");
    }
    
    /**
     * Logs a {@code Throwable} in the name of LogIt.
     * 
     * @param level     the logging level.
     * @param throwable the throwable to be logged.
     * 
     * @see #log(Level, String, Throwable)
     */
    public void log(Level level, Throwable throwable)
    {
        StringWriter sw = new StringWriter();
        
        try (PrintWriter pw = new PrintWriter(sw))
        {
            throwable.printStackTrace(pw);
        }
        
        log(level, "Caught exception:\n" + sw.toString());
    }
    
    public boolean isConfigLoaded()
    {
        return config != null && config.isLoaded();
    }
    
    public PersistenceManager getPersistenceManager()
    {
        return persistenceManager;
    }
    
    public ProfileManager getProfileManager()
    {
        return profileManager;
    }
    
    public MailSender getMailSender()
    {
        return mailSender;
    }
    
    public BackupManager getBackupManager()
    {
        return backupManager;
    }
    
    public AccountManager getAccountManager()
    {
        return accountManager;
    }
    
    public SessionManager getSessionManager()
    {
        return sessionManager;
    }
    
    public LocaleManager getLocaleManager()
    {
        return localeManager;
    }
    
    public LogItPlugin getPlugin()
    {
        return plugin;
    }
    
    public LogItConfiguration getConfig()
    {
        return config;
    }
    
    public boolean isFirstRun()
    {
        return firstRun;
    }
    
    public boolean isStarted()
    {
        return started;
    }
    
    public File getDataFolder()
    {
        return plugin.getDataFolder();
    }
    
    public File getDataFile(String path)
    {
        return new File(dataFolder, path);
    }
    
    protected Permission getVaultPermissions()
    {
        return vaultPermissions;
    }
    
    private void setCommandExecutors()
    {
        setCommandExecutor("logit", new LogItCommand(), true);
        setCommandExecutor("logit", new LogItCommand(), true);
        setCommandExecutor("login", new LoginCommand(), true);
        setCommandExecutor("logout", new LogoutCommand(), true);
        setCommandExecutor("remember", new RememberCommand(),
                config.getBoolean("login-sessions.enabled"));
        setCommandExecutor("register", new RegisterCommand(), true);
        setCommandExecutor("unregister", new UnregisterCommand(), true);
        setCommandExecutor("changepass", new ChangePassCommand(),
                !config.getBoolean("password.disable-passwords"));
        setCommandExecutor("changeemail", new ChangeEmailCommand(), true);
        setCommandExecutor("recoverpass", new RecoverPassCommand(),
                config.getBoolean("password-recovery.enabled"));
        setCommandExecutor("profile", new ProfileCommand(), config.getBoolean("profiles.enabled"));
        setCommandExecutor("acclock", new AcclockCommand(), true);
        setCommandExecutor("accunlock", new AccunlockCommand(), true);
        setCommandExecutor("$logit-nop-command", new NopCommandExecutor(), true);
    }
    
    private void setCommandExecutor(String command, CommandExecutor executor, boolean enabled)
    {
        if (enabled)
        {
            plugin.getCommand(command).setExecutor(executor);
        }
        else
        {
            plugin.getCommand(command).setExecutor(new DisabledCommandExecutor());
        }
    }
    
    private void registerEvents()
    {
        plugin.getServer().getPluginManager().registerEvents(new TickEventListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ServerEventListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new BlockEventListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new EntityEventListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PlayerEventListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new InventoryEventListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SessionEventListener(), plugin);
    }
    
    private void openLogFile(String filename)
    {
        File logFile = getDataFile(filename);
        
        if (logFile.length() > 300000)
        {
            int suffix = 0;
            File nextLogFile;
            
            do
            {
                suffix++;
                nextLogFile = getDataFile(filename + "." + suffix);
            }
            while (nextLogFile.exists());
            
            logFile.renameTo(nextLogFile);
        }
        
        try
        {
            logFileWriter = new FileWriter(logFile, true);
        }
        catch (IOException ex)
        {
            plugin.getLogger().log(Level.WARNING, "Could not open log file for writing.", ex);
        }
    }
    
    private void setSerializerEnabled(Class<? extends PersistenceSerializer> clazz, boolean status)
            throws FatalReportedException
    {
        if (status)
        {
            try
            {
                persistenceManager.registerSerializer(clazz);
            }
            catch (ReflectiveOperationException ex)
            {
                log(Level.SEVERE,
                        "Could not register persistence serializer: " + clazz.getSimpleName(), ex);
                
                FatalReportedException.throwNew(ex);
            }
        }
        else for (Player player : Bukkit.getOnlinePlayers())
        {
            try
            {
                persistenceManager.unserializeUsing(player, clazz);
            }
            catch (ReflectiveOperationException | IOException ex)
            {
                log(Level.WARNING,
                        "Could not unserialize persistence for player: " + player.getName(), ex);
            }
        }
    }
    
    /**
     * The preferred way to obtain the instance of {@code LogItCore}.
     * 
     * @return the instance of LogItCore.
     */
    public static LogItCore getInstance()
    {
        if (instance == null)
        {
            instance = new LogItCore(LogItPlugin.getInstance());
        }
        
        return instance;
    }
    
    public static enum StorageType
    {
        UNKNOWN, NONE, SQLITE, MYSQL, H2, POSTGRESQL, CSV;
        
        public static StorageType decode(String s)
        {
            switch (s.toLowerCase())
            {
            case "none":       return NONE;
            case "sqlite":     return SQLITE;
            case "mysql":      return MYSQL;
            case "h2":         return H2;
            case "postgresql": return POSTGRESQL;
            case "csv":        return CSV;
            default:           return UNKNOWN;
            }
        }
        
        /**
         * Converts this {@code StorageType} to a string representation.
         * 
         * @return the string representation of this {@code StorageType},
         *         or {@code null} if no representation for this
         *         {@code StorageType} was implemented.
         */
        public String encode()
        {
            switch (this)
            {
            case NONE:       return "none";
            case SQLITE:     return "sqlite";
            case MYSQL:      return "mysql";
            case H2:         return "h2";
            case POSTGRESQL: return "postgresql";
            case CSV:        return "csv";
            default:         return null;
            }
        }
    }
    
    public static enum HashingAlgorithm
    {
        UNKNOWN, PLAIN, MD2, MD5, SHA1, SHA256, SHA384, SHA512, WHIRLPOOL, BCRYPT;
        
        public static HashingAlgorithm decode(String s)
        {
            switch (s.toLowerCase())
            {
            case "plain":     return PLAIN;
            case "md2":       return MD2;
            case "md5":       return MD5;
            case "sha-1":     return SHA1;
            case "sha-256":   return SHA256;
            case "sha-384":   return SHA384;
            case "sha-512":   return SHA512;
            case "whirlpool": return WHIRLPOOL;
            case "bcrypt":    return BCRYPT;
            default:          return UNKNOWN;
            }
        }
        
        /**
         * Converts this {@code HashingAlgorithm} to a string representation.
         * 
         * @return the string representation of this {@code HashingAlgorithm},
         *         or {@code null} if no representation for this
         *         {@code HashingAlgorithm} was implemented.
         */
        public String encode()
        {
            switch (this)
            {
            case PLAIN:     return "plain";
            case MD2:       return "md2";
            case MD5:       return "md5";
            case SHA1:      return "sha-1";
            case SHA256:    return "sha-256";
            case SHA384:    return "sha-384";
            case SHA512:    return "sha-512";
            case WHIRLPOOL: return "whirlpool";
            case BCRYPT:    return "bcrypt";
            default:        return null;
            }
        }
    }
    
    public static enum IntegrationType
    {
        UNKNOWN, NONE, PHPBB2;
        
        public static IntegrationType decode(String s)
        {
            switch (s.toLowerCase())
            {
            case "none":   return NONE;
            case "phpbb2": return PHPBB2;
            default:       return UNKNOWN;
            }
        }
        
        /**
         * Converts this {@code IntegrationType} to a string representation.
         * 
         * @return the string representation of this {@code IntegrationType},
         *         or {@code null} if no representation for this
         *         {@code IntegrationType} was implemented.
         */
        public String encode()
        {
            switch (this)
            {
            case NONE:   return "plain";
            case PHPBB2: return "phpbb2";
            default:     return null;
            }
        }
    }
    
    public static final Level INTERNAL = new CustomLevel("INTERNAL", -1000);
    
    public static final String LIB_H2 = "h2small-1.3.171.jar";
    public static final String LIB_POSTGRESQL = "postgresql-9.3-1100.jdbc4.jar";
    public static final String LIB_MAIL = "mail-1.4.7.jar";
    
    private static LogItCore instance = null;
    
    private final LogItPlugin plugin;
    private final File dataFolder;
    
    private final boolean firstRun;
    private boolean started = false;
    
    private FileWriter logFileWriter;
    private final SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private LogItConfiguration  config;
    private Permission          vaultPermissions;
    private SessionManager      sessionManager;
    private AccountManager      accountManager;
    private AccountWatcher      accountWatcher;
    private BackupManager       backupManager;
    private PersistenceManager  persistenceManager;
    private ProfileManager      profileManager;
    private MailSender          mailSender;
    private LocaleManager       localeManager;
    private TickEventCaller     tickEventCaller;
    
    private int accountManagerTaskId;
    private int sessionManagerTaskId;
    private int tickEventCallerTaskId;
    private int accountWatcherTaskId;
    private int backupManagerTaskId;
    
    private final Hashtable<Player, Long> forceLoginPromptIntervals = new Hashtable<>();
}
