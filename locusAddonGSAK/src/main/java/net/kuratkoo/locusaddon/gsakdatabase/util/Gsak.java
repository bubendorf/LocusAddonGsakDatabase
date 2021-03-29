package net.kuratkoo.locusaddon.gsakdatabase.util;

import android.content.SharedPreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingDataTravelBug;

/**
 * Gsak
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 */
public class Gsak {

    public static Boolean isGsakDatabase(File f) {
        return f.exists() && f.canRead() && f.isFile() && f.getName().endsWith("db3");
    }

    public static int convertContainer(String size) {
        switch (size) {
            case "Small":
                return PointGeocachingData.CACHE_SIZE_SMALL;
            case "Large":
                return PointGeocachingData.CACHE_SIZE_LARGE;
            case "Micro":
                return PointGeocachingData.CACHE_SIZE_MICRO;
            case "Not chosen":
            case "Unknown":
                return PointGeocachingData.CACHE_SIZE_NOT_CHOSEN;
            case "Other":
                return PointGeocachingData.CACHE_SIZE_OTHER;
            case "Regular":
                return PointGeocachingData.CACHE_SIZE_REGULAR;
            default:
                return PointGeocachingData.CACHE_SIZE_NOT_CHOSEN;
        }
    }

    public static int convertCacheType(String type) {
        switch (type) {
            case "C":
                return PointGeocachingData.CACHE_TYPE_CACHE_IN_TRASH_OUT;
            case "R":
                return PointGeocachingData.CACHE_TYPE_EARTH;
            case "E":
                return PointGeocachingData.CACHE_TYPE_EVENT;
            case "B":
                return PointGeocachingData.CACHE_TYPE_LETTERBOX;
            case "Q":
                return PointGeocachingData.CACHE_TYPE_ADVENTURE_LAB;
            case "Z":
                return PointGeocachingData.CACHE_TYPE_MEGA_EVENT;
            case "J":
                return PointGeocachingData.CACHE_TYPE_GIGA_EVENT;
            case "M":
                return PointGeocachingData.CACHE_TYPE_MULTI;
            case "T":
                return PointGeocachingData.CACHE_TYPE_TRADITIONAL;
            case "U":
                return PointGeocachingData.CACHE_TYPE_MYSTERY;
            case "V":
                return PointGeocachingData.CACHE_TYPE_VIRTUAL;
            case "W":
                return PointGeocachingData.CACHE_TYPE_WEBCAM;
            case "I":
                return PointGeocachingData.CACHE_TYPE_WHERIGO;
            case "A":
                return PointGeocachingData.CACHE_TYPE_PROJECT_APE;
            case "L":
                return PointGeocachingData.CACHE_TYPE_LOCATIONLESS;
            case "G":
                return PointGeocachingData.CACHE_TYPE_BENCHMARK;
            case "H":
                return PointGeocachingData.CACHE_TYPE_GROUNDSPEAK;
            case "X":
                return PointGeocachingData.CACHE_TYPE_MAZE_EXHIBIT;
            case "Y":
                return PointGeocachingData.CACHE_TYPE_WAYMARK;
            case "F":
                return PointGeocachingData.CACHE_TYPE_LF_EVENT;
            case "D":
                return PointGeocachingData.CACHE_TYPE_LF_CELEBRATION;
            default:
                return PointGeocachingData.CACHE_TYPE_TRADITIONAL;
        }
    }

    public static boolean isAvailable(String status) {
        return status.equals("A");
    }

    public static boolean isArchived(String status) {
        return status.equals("X");
    }

    public static boolean isFound(int found) {
        return found == 1;
    }

    public static boolean isPremium(int premium) {
        return premium == 1;
    }

    public static String convertWaypointType(String waypointType) {
        switch (waypointType) {
            case "Final Location":
                return PointGeocachingData.CACHE_WAYPOINT_TYPE_FINAL;
            case "Parking Area":
                return PointGeocachingData.CACHE_WAYPOINT_TYPE_PARKING;
            case "Question to Answer":
                return PointGeocachingData.CACHE_WAYPOINT_TYPE_QUESTION;
            case "Reference Point":
                return PointGeocachingData.CACHE_WAYPOINT_TYPE_REFERENCE;
            case "Stages of a Multicache":
                return PointGeocachingData.CACHE_WAYPOINT_TYPE_STAGES;
            case "Trailhead":
                return PointGeocachingData.CACHE_WAYPOINT_TYPE_TRAILHEAD;
            case "Physical Stage":
                return PointGeocachingData.CACHE_WAYPOINT_TYPE_PHYSICAL_STAGE;
            case "Virtual Stage":
                return PointGeocachingData.CACHE_WAYPOINT_TYPE_VIRTUAL_STAGE;
            case "Original Coordinates":
                return PointGeocachingData.CACHE_WAYPOINT_TYPE_REFERENCE;
            default:
                return PointGeocachingData.CACHE_WAYPOINT_TYPE_REFERENCE;
        }
    }

    public static int convertLogType(String logType) {
        switch (logType) {
            case "Announcement":
                return PointGeocachingData.CACHE_LOG_TYPE_ANNOUNCEMENT;
            case "Attended":
                return PointGeocachingData.CACHE_LOG_TYPE_ATTENDED;
            case "Didn't find it":
                return PointGeocachingData.CACHE_LOG_TYPE_NOT_FOUNDED;
            case "Enable Listing":
                return PointGeocachingData.CACHE_LOG_TYPE_ENABLE_LISTING;
            case "Found it":
                return PointGeocachingData.CACHE_LOG_TYPE_FOUNDED;
            case "Needs Archived":
                return PointGeocachingData.CACHE_LOG_TYPE_NEEDS_ARCHIVED;
            case "Needs Maintenance":
                return PointGeocachingData.CACHE_LOG_TYPE_NEEDS_MAINTENANCE;
            case "Owner Maintenance":
                return PointGeocachingData.CACHE_LOG_TYPE_OWNER_MAINTENANCE;
            case "Post Reviewer Note":
                return PointGeocachingData.CACHE_LOG_TYPE_POST_REVIEWER_NOTE;
            case "Publish Listing":
                return PointGeocachingData.CACHE_LOG_TYPE_PUBLISH_LISTING;
            case "Temporarily Disable Listing":
                return PointGeocachingData.CACHE_LOG_TYPE_TEMPORARILY_DISABLE_LISTING;
            case "Update Coordinates":
                return PointGeocachingData.CACHE_LOG_TYPE_UPDATE_COORDINATES;
            case "Webcam Photo Taken":
                return PointGeocachingData.CACHE_LOG_TYPE_WEBCAM_PHOTO_TAKEN;
            case "Will Attend":
                return PointGeocachingData.CACHE_LOG_TYPE_WILL_ATTEND;
            case "Write note":
                return PointGeocachingData.CACHE_LOG_TYPE_WRITE_NOTE;
            default:
                return PointGeocachingData.CACHE_LOG_TYPE_UNKNOWN;
        }
    }

    public static boolean isCorrected(int correction) {
        return correction == 1;
    }

    public static ArrayList<PointGeocachingDataTravelBug> parseTravelBug(String tb) {
        ArrayList<PointGeocachingDataTravelBug> pgdtbl = new ArrayList<>();
        Pattern p = Pattern.compile("<BR>([^(]+)\\(id = ([0-9]+), ref = ([A-Z0-9]+)\\)");
        Matcher m = p.matcher(tb);
        while (m.find()) {
            MatchResult mr = m.toMatchResult();
            PointGeocachingDataTravelBug pgdtb = new PointGeocachingDataTravelBug();
            pgdtb.name = mr.group(1);
            pgdtb.srcDetails = "http://www.geocaching.com/track/details.aspx?tracker=" + mr.group(3);
            pgdtbl.add(pgdtb);
        }
        return pgdtbl;
    }

    public static List<String> geocacheTypesFromFilter(SharedPreferences sharedPref) {
        List<String> geocacheTypes = new ArrayList<>();

        if (sharedPref.getBoolean("gc_type_tradi", false)) {
            geocacheTypes.add("CacheType = \"T\"");
        }
        if (sharedPref.getBoolean("gc_type_multi", false)) {
            geocacheTypes.add("CacheType = \"M\"");
        }
        if (sharedPref.getBoolean("gc_type_mystery", false)) {
            geocacheTypes.add("CacheType = \"U\"");
        }
        if (sharedPref.getBoolean("gc_type_earth", false)) {
            geocacheTypes.add("CacheType = \"R\"");
        }
        if (sharedPref.getBoolean("gc_type_letter", false)) {
            geocacheTypes.add("CacheType = \"B\"");
        }
        if (sharedPref.getBoolean("gc_type_event", false)) {
            geocacheTypes.add("CacheType = \"E\""); // Event
            geocacheTypes.add("CacheType = \"Z\""); // Mega Event
            geocacheTypes.add("CacheType = \"J\""); // Giga Event
        }
        if (sharedPref.getBoolean("gc_type_cito", false)) {
            geocacheTypes.add("CacheType = \"C\"");
        }
        if (sharedPref.getBoolean("gc_type_lab", false)) {
            geocacheTypes.add("CacheType = \"Q\"");
        }
        if (sharedPref.getBoolean("gc_type_wig", false)) {
            geocacheTypes.add("CacheType = \"I\"");
        }
        if (sharedPref.getBoolean("gc_type_virtual", false)) {
            geocacheTypes.add("CacheType = \"V\"");
        }
        if (sharedPref.getBoolean("gc_type_webcam", false)) {
            geocacheTypes.add("CacheType = \"W\"");
        }
        if (sharedPref.getBoolean("gc_type_loc", false)) {
            geocacheTypes.add("CacheType = \"L\"");
        }
        if (sharedPref.getBoolean("gc_type_hq", false)) {
            geocacheTypes.add("CacheType = \"H\"");
        }
        if (sharedPref.getBoolean("gc_type_gps", false)) {
            geocacheTypes.add("CacheType = \"X\"");
        }
        if (sharedPref.getBoolean("gc_type_10years", false)) {
            geocacheTypes.add("CacheType = \"F\"");
        }
        if (sharedPref.getBoolean("gc_type_benchmark", false)) {
            geocacheTypes.add("CacheType = \"G\"");
        }
        if (sharedPref.getBoolean("gc_type_ape", false)) {
            geocacheTypes.add("CacheType = \"A\"");
        }
        if (sharedPref.getBoolean("gc_type_corrected", false)) {
            geocacheTypes.add("HasCorrected = 1");
        }

        return geocacheTypes;
    }
}
