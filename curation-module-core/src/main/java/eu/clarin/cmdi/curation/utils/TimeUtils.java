/**
 *
 */
package eu.clarin.cmdi.curation.utils;

import org.apache.log4j.Logger;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * This class makes millisecond time measurements human readable into date and time
 */
public class TimeUtils {

    public static String humanizeToTime(long millis) {
        if (millis < 1000)
            return millis + " ms";

        if (millis < 1000 * 60)
            return TimeUnit.MILLISECONDS.toSeconds(millis) + " sec (" + millis + " ms)";

        else
            return String.format("%02d min, %02d sec (%d ms)",
                    TimeUnit.MILLISECONDS.toMinutes(millis),
                    TimeUnit.MILLISECONDS.toSeconds(millis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)),
                    millis
            );
    }

    public static String humanizeToDate(long millis) {
        Instant instant = Instant.ofEpochMilli(millis);
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, zoneId);

        String timestamp = zdt.toString();
        timestamp = timestamp.replaceFirst("\\[", " [");
        timestamp = timestamp.replaceFirst("T", " ");
        return timestamp;
    }

    public static String humanizeToDate(Timestamp timestamp){
        return humanizeToDate(timestamp.getTime());
    }

}
