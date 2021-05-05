/*
 * DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 * Version 2, December 2004
 *
 * Copyright (C) 2004 Sam Hocevar <sam@hocevar.net>
 *
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this license document, and changing it is allowed as long
 * as the name is changed.
 *
 * DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 * TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 * 0. You just DO WHAT THE FUCK YOU WANT TO.
 */

package ch.bubendorf.locusaddon.gsakdatabase.util;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.bubendorf.locusaddon.gsakdatabase.R;
import locus.api.objects.geocaching.GeocachingData;
import locus.api.objects.geocaching.GeocachingLog;
import locus.api.objects.geocaching.GeocachingTrackable;
import locus.api.objects.geocaching.GeocachingWaypoint;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Gsak
 *
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 * @author Markus Bubendorf <gsakforlocus@bubendorf.net>
 */
public class Gsak {

    public static boolean isReadableGsakDatabase(final String file) {
        return isReadableGsakDatabase(new File(file));
    }

    public static boolean isReadableGsakDatabase(final File file) {
        return file.exists() && file.canRead() && file.isFile() && file.getName().endsWith("db3");
    }

    public static boolean isNotAGsakDatabase(final String file) {
        return !isGsakDatabase(new File(file));
    }

    public static boolean isGsakDatabase(final File file) {
        return file.exists() && file.isFile() && file.getName().endsWith("db3");
    }

    public static boolean hasSQLiteMagic(final File file) {
        if (!file.exists() || file.length() < 16) {
            return false;
        }

        try (final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")){
            final byte[] b = new byte[16];
            randomAccessFile.readFully(b);
            return b[0] == 'S' &&
                    b[1] == 'Q'&&
                    b[2] == 'L'&&
                    b[3] == 'i'&&
                    b[4] == 't'&&
                    b[5] == 'e';
        } catch (final IOException e) {
            return false;
        }
    }

    public static String checkDatabase(final Context context, final String dbId) {
        final SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean("pref_use_" + dbId, false)) {
            final String dbPath = sharedPreferences.getString(dbId, "");
            if (dbPath != null) {
                final File fd = new File(dbPath);
                if (!isReadableGsakDatabase(fd)) {
                    return context.getResources().getString(R.string.no_db_file) + "\n" + dbPath;
                }
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                        !hasSQLiteMagic(fd)) {
                    return context.getResources().getString(R.string.no_sqlite_database) + "\n" + dbPath;
                }
            }
        }
        return null;
    }

    public static int convertContainer(final String size) {
        switch (size) {
            case "Small":
                return GeocachingData.CACHE_SIZE_SMALL;
            case "Large":
                return GeocachingData.CACHE_SIZE_LARGE;
            case "Micro":
                return GeocachingData.CACHE_SIZE_MICRO;
            case "Other":
                return GeocachingData.CACHE_SIZE_OTHER;
            case "Regular":
                return GeocachingData.CACHE_SIZE_REGULAR;
            case "Not chosen":
            case "Unknown":
            default:
                return GeocachingData.CACHE_SIZE_NOT_CHOSEN;
        }
    }

    public static int convertCacheType(final String type) {
        switch (type) {
            case "C":
                return GeocachingData.CACHE_TYPE_CACHE_IN_TRASH_OUT;
            case "R":
                return GeocachingData.CACHE_TYPE_EARTH;
            case "E":
                return GeocachingData.CACHE_TYPE_EVENT;
            case "B":
                return GeocachingData.CACHE_TYPE_LETTERBOX;
            case "Q":
                return GeocachingData.CACHE_TYPE_LAB_CACHE;
            case "Z":
                return GeocachingData.CACHE_TYPE_MEGA_EVENT;
            case "J":
                return GeocachingData.CACHE_TYPE_GIGA_EVENT;
            case "M":
                return GeocachingData.CACHE_TYPE_MULTI;
            case "U":
                return GeocachingData.CACHE_TYPE_MYSTERY;
            case "V":
                return GeocachingData.CACHE_TYPE_VIRTUAL;
            case "W":
                return GeocachingData.CACHE_TYPE_WEBCAM;
            case "I":
                return GeocachingData.CACHE_TYPE_WHERIGO;
            case "A":
                return GeocachingData.CACHE_TYPE_PROJECT_APE;
            case "L":
                return GeocachingData.CACHE_TYPE_LOCATIONLESS;
            case "G":
                return GeocachingData.CACHE_TYPE_BENCHMARK;
            case "H":
                //return GeocachingData.CACHE_TYPE_GROUNDSPEAK;
                return GeocachingData.CACHE_TYPE_GC_HQ;
            case "X":
                return GeocachingData.CACHE_TYPE_MAZE_EXHIBIT;
            case "Y":
                return GeocachingData.CACHE_TYPE_WAYMARK;
            case "F":
                //return GeocachingData.CACHE_TYPE_LF_EVENT;
                return GeocachingData.CACHE_TYPE_COMMUNITY_CELEBRATION;
            case "D":
                return GeocachingData.CACHE_TYPE_GC_HQ_BLOCK_PARTY;
            case "T":
            default:
                return GeocachingData.CACHE_TYPE_TRADITIONAL;
        }
    }

    public static boolean isAvailable(final String status) {
        return status.equals("A");
    }

    public static boolean isArchived(final String status) {
        return status.equals("X");
    }

    public static boolean isFound(final int found) {
        return found == 1;
    }

    public static boolean isPremium(final int premium) {
        return premium == 1;
    }

    public static String convertWaypointType(final String waypointType) {
        switch (waypointType) {
            case "Final Location":
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_FINAL;
            case "Parking Area":
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_PARKING;
            case "Question to Answer":
            case "Virtual Stage":
                //return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_QUESTION;
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_VIRTUAL_STAGE;
            case "Stages of a Multicache":
            case "Physical Stage":
                //return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_STAGES;
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_PHYSICAL_STAGE;
            case "Trailhead":
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_TRAILHEAD;
            case "Original Coordinates":
            case "Reference Point":
            default:
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_REFERENCE;
        }
    }

    public static int convertLogType(final String logType) {
        switch (logType) {
            case "Announcement":
                return GeocachingLog.CACHE_LOG_TYPE_ANNOUNCEMENT;
            case "Attended":
                return GeocachingLog.CACHE_LOG_TYPE_ATTENDED;
            case "Didn't find it":
                return GeocachingLog.CACHE_LOG_TYPE_NOT_FOUND;
            case "Enable Listing":
                return GeocachingLog.CACHE_LOG_TYPE_ENABLE_LISTING;
            case "Found it":
                return GeocachingLog.CACHE_LOG_TYPE_FOUND;
            case "Needs Archived":
                return GeocachingLog.CACHE_LOG_TYPE_NEEDS_ARCHIVED;
            case "Needs Maintenance":
                return GeocachingLog.CACHE_LOG_TYPE_NEEDS_MAINTENANCE;
            case "Owner Maintenance":
                return GeocachingLog.CACHE_LOG_TYPE_OWNER_MAINTENANCE;
            case "Post Reviewer Note":
                return GeocachingLog.CACHE_LOG_TYPE_POST_REVIEWER_NOTE;
            case "Publish Listing":
                return GeocachingLog.CACHE_LOG_TYPE_PUBLISH_LISTING;
            case "Temporarily Disable Listing":
                return GeocachingLog.CACHE_LOG_TYPE_TEMPORARILY_DISABLE_LISTING;
            case "Update Coordinates":
                return GeocachingLog.CACHE_LOG_TYPE_UPDATE_COORDINATES;
            case "Webcam Photo Taken":
                return GeocachingLog.CACHE_LOG_TYPE_WEBCAM_PHOTO_TAKEN;
            case "Will Attend":
                return GeocachingLog.CACHE_LOG_TYPE_WILL_ATTEND;
            case "Write note":
                return GeocachingLog.CACHE_LOG_TYPE_WRITE_NOTE;
            default:
                return GeocachingLog.CACHE_LOG_TYPE_UNKNOWN;
        }
    }

    public static boolean isCorrected(final int correction) {
        return correction == 1;
    }

    public static List<GeocachingTrackable> parseTravelBug(final String tb) {
        final ArrayList<GeocachingTrackable> pgdtbl = new ArrayList<>();
        final Pattern p = Pattern.compile("<BR>([^(]+)\\(id = ([0-9]+), ref = ([A-Z0-9]+)\\)");
        final Matcher m = p.matcher(tb);
        while (m.find()) {
            final MatchResult mr = m.toMatchResult();
            final GeocachingTrackable pgdtb = new GeocachingTrackable();
            pgdtb.setName(mr.group(1));
            pgdtb.setSrcDetails("http://www.geocaching.com/track/details.aspx?tracker=" + mr.group(3));
            pgdtbl.add(pgdtb);
        }
        return pgdtbl;
    }

    public static List<String> geocacheTypesFromFilter(final SharedPreferences sharedPref) {
        final List<String> geocacheTypes = new ArrayList<>();

        if (sharedPref.getBoolean("gc_type_tradi", false)) {
            geocacheTypes.add("CacheType = 'T'");
        }
        if (sharedPref.getBoolean("gc_type_multi", false)) {
            geocacheTypes.add("CacheType = 'M'");
        }
        if (sharedPref.getBoolean("gc_type_mystery", false)) {
            geocacheTypes.add("CacheType = 'U'");
        }
        if (sharedPref.getBoolean("gc_type_earth", false)) {
            geocacheTypes.add("CacheType = 'R'");
        }
        if (sharedPref.getBoolean("gc_type_letter", false)) {
            geocacheTypes.add("CacheType = 'B'");
        }
        if (sharedPref.getBoolean("gc_type_event", false)) {
            geocacheTypes.add("CacheType = 'E'"); // Event
            geocacheTypes.add("CacheType = 'Z'"); // Mega Event
            geocacheTypes.add("CacheType = 'J'"); // Giga Event
            geocacheTypes.add("CacheType = 'P'"); // Block Party
        }
        if (sharedPref.getBoolean("gc_type_cito", false)) {
            geocacheTypes.add("CacheType = 'C'");
        }
        if (sharedPref.getBoolean("gc_type_lab", false)) {
            geocacheTypes.add("CacheType = 'Q'");
        }
        if (sharedPref.getBoolean("gc_type_wig", false)) {
            geocacheTypes.add("CacheType = 'I'");
        }
        if (sharedPref.getBoolean("gc_type_virtual", false)) {
            geocacheTypes.add("CacheType = 'V'");
        }
        if (sharedPref.getBoolean("gc_type_webcam", false)) {
            geocacheTypes.add("CacheType = 'W'");
        }
        if (sharedPref.getBoolean("gc_type_loc", false)) {
            geocacheTypes.add("CacheType = 'L'");
        }
        if (sharedPref.getBoolean("gc_type_hq", false)) {
            geocacheTypes.add("CacheType = 'H'");
        }
        if (sharedPref.getBoolean("gc_type_gps", false)) {
            geocacheTypes.add("CacheType = 'X'");
        }
        if (sharedPref.getBoolean("gc_type_10years", false)) {
            geocacheTypes.add("CacheType = 'F'");
        }
        if (sharedPref.getBoolean("gc_type_benchmark", false)) {
            geocacheTypes.add("CacheType = 'G'");
        }
        if (sharedPref.getBoolean("gc_type_ape", false)) {
            geocacheTypes.add("CacheType = 'A'");
        }
        if (sharedPref.getBoolean("gc_type_corrected", false)) {
            geocacheTypes.add("HasCorrected = 1");
        }

        return geocacheTypes;
    }
}
