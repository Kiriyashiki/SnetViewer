package xyz.hainaut.snetviewer.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Stores the parsed timestamp information from targetTimes.json.
 */
public class TargetTimeInfo {
    private static final DateTimeFormatter PARSER_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private static final DateTimeFormatter FULL_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static final ZoneId JST_ZONE = ZoneId.of("Asia/Tokyo");

    private final String basetime;
    private final String validtime;
    private final ZonedDateTime basetimeUtc;
    private final ZonedDateTime basetimeJst;

    public TargetTimeInfo(String basetime, String validtime) {
        this.basetime = Objects.requireNonNull(basetime, "basetime must not be null");
        this.validtime = Objects.requireNonNull(validtime, "validtime must not be null");

        LocalDateTime local = LocalDateTime.parse(basetime, PARSER_FORMATTER);
        this.basetimeUtc = local.atZone(ZoneOffset.UTC);
        this.basetimeJst = basetimeUtc.withZoneSameInstant(JST_ZONE);
    }

    public String getBasetime() {
        return basetime;
    }

    public String getValidtime() {
        return validtime;
    }

    public ZonedDateTime getBasetimeUtc() {
        return basetimeUtc;
    }

    public ZonedDateTime getBasetimeJst() {
        return basetimeJst;
    }

    /**
     * Formats the basetime in JST timezone as "YYYY/MM/DD HH:mm".
     */
    public String getFormattedJstTime() {
        return basetimeJst.format(DISPLAY_FORMATTER);
    }

    /**
     * Formats the basetime in JST timezone with seconds as "YYYY/MM/DD HH:mm:ss".
     */
    public String getFullFormattedJstTime() {
        return basetimeJst.format(FULL_DISPLAY_FORMATTER);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetTimeInfo that = (TargetTimeInfo) o;
        return Objects.equals(basetime, that.basetime) &&
               Objects.equals(validtime, that.validtime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(basetime, validtime);
    }

    @Override
    public String toString() {
        return "TargetTimeInfo{" +
                "basetime='" + basetime + '\'' +
                ", validtime='" + validtime + '\'' +
                ", JST=" + getFormattedJstTime() +
                '}';
    }
}
