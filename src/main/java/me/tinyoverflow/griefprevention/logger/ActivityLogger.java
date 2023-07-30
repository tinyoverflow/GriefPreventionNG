/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2015 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.tinyoverflow.griefprevention.logger;

import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.configurations.LoggerConfiguration;
import me.tinyoverflow.griefprevention.tasks.PruneLogsTask;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

public class ActivityLogger
{
    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("HH:mm");
    private final SimpleDateFormat filenameFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final StringBuffer messageBuffer = new StringBuffer();

    private final java.util.logging.Logger serverLogger;
    private final File logDirectory;
    private final LoggerConfiguration configuration;

    public ActivityLogger(java.util.logging.Logger serverLogger, File logDirectory, LoggerConfiguration configuration)
    {
        this.serverLogger = serverLogger;
        this.logDirectory = logDirectory;
        this.configuration = configuration;

        if (!this.logDirectory.exists() && !this.logDirectory.mkdirs())
        {
            serverLogger.log(Level.SEVERE, "Could not create logging directory: " + this.logDirectory.getPath());
        }
    }

    public void log(ActivityType type, String message)
    {
        // Skip if the given log type is not enabled.
        if (!switch (type)
        {
            case SOCIAL -> configuration.isSocialEnabled();
            case SUSPICIOUS -> configuration.isSuspiciousEnabled();
            case ADMIN -> configuration.isAdminEnabled();
            case DEBUG -> configuration.isDebugEnabled();
            default -> true;
        })
        {
            return;
        }

        // Write message to the memory buffer.
        String timestamp = timestampFormat.format(new Date());
        messageBuffer.append(timestamp).append(' ').append(message).append('\n');
    }

    public void flush()
    {
        // Skip if nothing is inside the buffer.
        if (messageBuffer.length() == 0) return;

        // Determine filename based on the current date.
        String fileName = filenameFormat.format(new Date()) + ".log";
        File logFile = Path.of(logDirectory.toString(), fileName).toFile();

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(logFile, StandardCharsets.UTF_8, true)))
        {
            bufferedWriter.write(messageBuffer.toString());
            messageBuffer.setLength(0);
        } catch (IOException e)
        {
            serverLogger.severe("Could not write to log file " + logFile + ": " + e.getMessage());
        }
    }

    public void registerTasks(GriefPrevention plugin, BukkitScheduler scheduler)
    {
        // Flush messages every 60 seconds.
        scheduler.scheduleSyncRepeatingTask(plugin, this::flush, 0, 60 * 20);

        // Prune old logs once a day, if enabled.
        if (configuration.isLogPruningEnabled())
        {
            scheduler.scheduleSyncRepeatingTask(
                    plugin,
                    new PruneLogsTask(serverLogger, logDirectory, configuration),
                    0,
                    24 * 60 * 60 * 20
            );
        }
    }
}
