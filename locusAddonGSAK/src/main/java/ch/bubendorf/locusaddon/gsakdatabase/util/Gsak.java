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

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.content.Context;
import android.content.SharedPreferences;

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
import locus.api.objects.geocaching.GeocachingTrackable;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static locus.api.objects.geocaching.GeocachingData.CACHE_SIZE_LARGE;
import static locus.api.objects.geocaching.GeocachingData.CACHE_SIZE_MICRO;
import static locus.api.objects.geocaching.GeocachingData.CACHE_SIZE_NOT_CHOSEN;
import static locus.api.objects.geocaching.GeocachingData.CACHE_SIZE_OTHER;
import static locus.api.objects.geocaching.GeocachingData.CACHE_SIZE_REGULAR;
import static locus.api.objects.geocaching.GeocachingData.CACHE_SIZE_SMALL;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_BENCHMARK;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_CACHE_IN_TRASH_OUT;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_COMMUNITY_CELEBRATION;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_EARTH;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_EVENT;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_GC_HQ;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_GC_HQ_BLOCK_PARTY;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_GIGA_EVENT;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_LAB_CACHE;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_LETTERBOX;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_LOCATIONLESS;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_MAZE_EXHIBIT;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_MEGA_EVENT;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_MULTI;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_MYSTERY;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_PROJECT_APE;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_TRADITIONAL;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_VIRTUAL;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_WAYMARK;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_WEBCAM;
import static locus.api.objects.geocaching.GeocachingData.CACHE_TYPE_WHERIGO;
import static locus.api.objects.geocaching.GeocachingLog.CACHE_LOG_TYPE_ANNOUNCEMENT;
import static locus.api.objects.geocaching.GeocachingLog.CACHE_LOG_TYPE_ATTENDED;
import static locus.api.objects.geocaching.GeocachingLog.CACHE_LOG_TYPE_ENABLE_LISTING;
import static locus.api.objects.geocaching.GeocachingLog.CACHE_LOG_TYPE_FOUND;
import static locus.api.objects.geocaching.GeocachingLog.CACHE_LOG_TYPE_NEEDS_ARCHIVED;
import static locus.api.objects.geocaching.GeocachingLog.CACHE_LOG_TYPE_NEEDS_MAINTENANCE;
import static locus.api.objects.geocaching.GeocachingLog.CACHE_LOG_TYPE_NOT_FOUND;
import static locus.api.objects.geocaching.GeocachingLog.CACHE_LOG_TYPE_OWNER_MAINTENANCE;
import static locus.api.objects.geocaching.GeocachingLog.CACHE_LOG_TYPE_POST_REVIEWER_NOTE;
import static locus.api.objects.geocaching.GeocachingLog.CACHE_LOG_TYPE_PUBLISH_LISTING;
import static locus.api.objects.geocaching.GeocachingLog.CACHE_LOG_TYPE_TEMPORARILY_DISABLE_LISTING;
import static locus.api.objects.geocaching.GeocachingLog.CACHE_LOG_TYPE_UNKNOWN;
import static locus.api.objects.geocaching.GeocachingLog.CACHE_LOG_TYPE_UPDATE_COORDINATES;
import static locus.api.objects.geocaching.GeocachingLog.CACHE_LOG_TYPE_WEBCAM_PHOTO_TAKEN;
import static locus.api.objects.geocaching.GeocachingLog.CACHE_LOG_TYPE_WILL_ATTEND;
import static locus.api.objects.geocaching.GeocachingLog.CACHE_LOG_TYPE_WRITE_NOTE;
import static locus.api.objects.geocaching.GeocachingWaypoint.CACHE_WAYPOINT_TYPE_FINAL;
import static locus.api.objects.geocaching.GeocachingWaypoint.CACHE_WAYPOINT_TYPE_PARKING;
import static locus.api.objects.geocaching.GeocachingWaypoint.CACHE_WAYPOINT_TYPE_PHYSICAL_STAGE;
import static locus.api.objects.geocaching.GeocachingWaypoint.CACHE_WAYPOINT_TYPE_REFERENCE;
import static locus.api.objects.geocaching.GeocachingWaypoint.CACHE_WAYPOINT_TYPE_TRAILHEAD;
import static locus.api.objects.geocaching.GeocachingWaypoint.CACHE_WAYPOINT_TYPE_VIRTUAL_STAGE;

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
            // No file or the file is too short for a SQLite database ==> No GSAK database
            return false;
        }

        // Read the first 16 bytes of the file and check for the SQLite magic number
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

    /**
     * Checks if the database exists
     * @param context The context
     * @param dbId The Id of the database. "db", "db2" or "db3"
     * @return null if everything is OK; an error text otherwise
     */
    public static String checkDatabase(final Context context, final String dbId) {
        final SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean("pref_use_" + dbId, false)) {
            final String dbPath = sharedPreferences.getString(dbId, "");
            if (dbPath.length() > 0) {
                final File fd = new File(dbPath);
                if (!isReadableGsakDatabase(fd)) {
                    return context.getResources().getString(R.string.no_db_file) + "\n" + dbPath;
                }
                if (ContextCompat.checkSelfPermission(context, READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED &&
                        !hasSQLiteMagic(fd)) {
                    return context.getResources().getString(R.string.no_sqlite_database) + "\n" + dbPath;
                }
            }
        }
        return null;
    }

    public static int convertContainer(final String size) {
        return switch (size) {
            case "Small" -> CACHE_SIZE_SMALL;
            case "Large" -> CACHE_SIZE_LARGE;
            case "Micro" -> CACHE_SIZE_MICRO;
            case "Other" -> CACHE_SIZE_OTHER;
            case "Regular" -> CACHE_SIZE_REGULAR;
            case "Not chosen", "Unknown"-> CACHE_SIZE_NOT_CHOSEN;
            default -> CACHE_SIZE_NOT_CHOSEN;
        };
    }

    public static int convertCacheType(final String type) {
        return switch (type) {
            case "C" -> CACHE_TYPE_CACHE_IN_TRASH_OUT;
            case "R" -> CACHE_TYPE_EARTH;
            case "E" -> CACHE_TYPE_EVENT;
            case "B" -> CACHE_TYPE_LETTERBOX;
            case "Q" -> CACHE_TYPE_LAB_CACHE;
            case "Z" -> CACHE_TYPE_MEGA_EVENT;
            case "J" -> CACHE_TYPE_GIGA_EVENT;
            case "M" -> CACHE_TYPE_MULTI;
            case "U" -> CACHE_TYPE_MYSTERY;
            case "V" -> CACHE_TYPE_VIRTUAL;
            case "W" -> CACHE_TYPE_WEBCAM;
            case "I" -> CACHE_TYPE_WHERIGO;
            case "A" -> CACHE_TYPE_PROJECT_APE;
            case "L" -> CACHE_TYPE_LOCATIONLESS;
            case "G" -> CACHE_TYPE_BENCHMARK;
            case "H" -> CACHE_TYPE_GC_HQ;
            case "X" -> CACHE_TYPE_MAZE_EXHIBIT;
            case "Y" -> CACHE_TYPE_WAYMARK;
            case "F" -> CACHE_TYPE_COMMUNITY_CELEBRATION;
            case "D" -> CACHE_TYPE_GC_HQ_BLOCK_PARTY;
            case "T"-> CACHE_TYPE_TRADITIONAL;
            default -> CACHE_TYPE_TRADITIONAL;
        };
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
        return switch (waypointType) {
            case "Final Location" -> CACHE_WAYPOINT_TYPE_FINAL;
            case "Parking Area" -> CACHE_WAYPOINT_TYPE_PARKING;
            case "Question to Answer", "Virtual Stage" -> CACHE_WAYPOINT_TYPE_VIRTUAL_STAGE;
            case "Stages of a Multicache", "Physical Stage" ->  CACHE_WAYPOINT_TYPE_PHYSICAL_STAGE;
            case "Trailhead" -> CACHE_WAYPOINT_TYPE_TRAILHEAD;
            case "Original Coordinates", "Reference Point"-> CACHE_WAYPOINT_TYPE_REFERENCE;
            default ->  CACHE_WAYPOINT_TYPE_REFERENCE;
        };
    }

    public static int convertLogType(final String logType) {
        return switch (logType) {
            case "Announcement" -> CACHE_LOG_TYPE_ANNOUNCEMENT;
            case "Attended" -> CACHE_LOG_TYPE_ATTENDED;
            case "Didn't find it" -> CACHE_LOG_TYPE_NOT_FOUND;
            case "Enable Listing" -> CACHE_LOG_TYPE_ENABLE_LISTING;
            case "Found it" -> CACHE_LOG_TYPE_FOUND;
            case "Needs Archived" -> CACHE_LOG_TYPE_NEEDS_ARCHIVED;
            case "Needs Maintenance" -> CACHE_LOG_TYPE_NEEDS_MAINTENANCE;
            case "Owner Maintenance" -> CACHE_LOG_TYPE_OWNER_MAINTENANCE;
            case "Post Reviewer Note" -> CACHE_LOG_TYPE_POST_REVIEWER_NOTE;
            case "Publish Listing" -> CACHE_LOG_TYPE_PUBLISH_LISTING;
            case "Temporarily Disable Listing" -> CACHE_LOG_TYPE_TEMPORARILY_DISABLE_LISTING;
            case "Update Coordinates" -> CACHE_LOG_TYPE_UPDATE_COORDINATES;
            case "Webcam Photo Taken" -> CACHE_LOG_TYPE_WEBCAM_PHOTO_TAKEN;
            case "Will Attend" -> CACHE_LOG_TYPE_WILL_ATTEND;
            case "Write note" -> CACHE_LOG_TYPE_WRITE_NOTE;
            default -> CACHE_LOG_TYPE_UNKNOWN;
        };
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
            pgdtb.setSrcDetails("https://www.geocaching.com/track/details.aspx?tracker=" + mr.group(3));
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
