package org.mitre.thor.logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * The loggerFormatter class determines the color and format of the CoreLogger output. As it is currently set up,
 * The color of the log string is determined by level/type of the log where SEVERE ERRORS are red, WARNINGS are yellow,
 * and INFO are grey. Furthermore, the structure is as follows: date, hour-minute-second, thread number, origen class,
 * origen function, string message.
 */
public class loggerFormatter extends Formatter {

    public final String ANSI_RED = "\u001B[31m";
    public final String ANSI_YELLOW = "\u001B[33m";
    public final String ANSI_WHITE = "\u001B[37m";

    public SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd");
    public SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    /**
     * Returns a formatted string which is to be written to the console and log files based on a logging request.
     * This function is called everytime the logger is told to log.
     *
     * @param record instance of the logging request which contains the logging message and level
     * @return the string that is to written to the console and log file
     */
    @Override
    public String format(LogRecord record) {

        StringBuilder builder = new StringBuilder();

        Level level = record.getLevel();
        if (Level.INFO.equals(level)) {
            builder.append(ANSI_WHITE);
        }
        if (Level.WARNING.equals(level)) {
            builder.append(ANSI_YELLOW);
        }
        if (Level.SEVERE.equals(level)) {
            builder.append(ANSI_RED);
        }

        builder.append("[").append(dateFormat.format(new Date())).append("]");
        builder.append("[").append(timeFormat.format(new Date())).append("]");
        builder.append(" [").append(record.getLevel().getName()).append("]");
        builder.append(" [").append("thread ").append(record.getThreadID()).append("]");
        builder.append(" [").append(record.getSourceClassName()).append("]");
        builder.append(" [").append(record.getSourceMethodName()).append("]");
        builder.append(" : ").append(record.getMessage());
        builder.append("\n");

        return builder.toString();
    }
}
