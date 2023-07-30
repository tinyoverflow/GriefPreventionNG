package me.tinyoverflow.griefprevention.tasks;

import me.tinyoverflow.griefprevention.configurations.LoggerConfiguration;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PruneLogsTask implements Runnable
{
    private final Logger serverLogger;
    private final File logDirectory;
    private final LoggerConfiguration loggerConfiguration;

    public PruneLogsTask(Logger serverLogger, File logDirectory, LoggerConfiguration loggerConfiguration)
    {
        this.serverLogger = serverLogger;
        this.logDirectory = logDirectory;
        this.loggerConfiguration = loggerConfiguration;
    }

    @Override
    public void run()
    {
        // Get all files and skip this run if there are none.
        File[] files = logDirectory.listFiles();
        if (files == null || files.length == 0)
        {
            return;
        }

        // Calculate the deadline for file deletion.
        Calendar deadline = Calendar.getInstance();
        deadline.add(
                Calendar.DATE,
                Math.negateExact(loggerConfiguration.getDaysToKeep())
        );

        // Go through each file inside the folder and handle it.
        for (File file : files)
        {
            // Do not care about directories.
            if (file.isDirectory())
                continue;

            // Prepare variables used inside the catch block.
            try
            {
                // Get date string from filename using regex.
                Pattern fileNamePattern = Pattern.compile("(?<date>\\d{4}-\\d{2}-\\d{2}).log");
                Matcher fileNameMatcher = fileNamePattern.matcher(file.getName());

                // If the pattern doesn't match, it's not a file we want to care about.
                if (!fileNameMatcher.matches())
                    return;

                LocalDate fileDate = LocalDate.parse(fileNameMatcher.group("date"));
                if (deadline.before(fileDate) && !file.delete())
                {
                    serverLogger.log(Level.WARNING, "Could not delete log file: " + file.getPath());
                }
            }
            catch (PatternSyntaxException pse)
            {
                serverLogger.log(Level.SEVERE, "Could not compile log file pattern. Please report this error.");
            }
            catch (DateTimeParseException dtpe)
            {
                serverLogger.log(Level.WARNING, "Log file with invalid date found in directory: " + logDirectory.getPath());
            }
            catch (SecurityException se)
            {
                serverLogger.log(Level.SEVERE, "Security manager denied access to log file: " + file.getPath());
            }
        }
    }
}
