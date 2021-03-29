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

    private static final String TAG = "LocusAddonGsakDatabase|GsakUtils";

    public static Boolean isGsakDatabase(File f) {
        if (f.exists() && f.canRead() && f.isFile() && f.getName().endsWith("db3")) {
            return true;
        } else {
            return false;
        }
    }

    public static int convertContainer(String size) {
        if (size.equals("Small")) {
            return PointGeocachingData.CACHE_SIZE_SMALL;
        } else if (size.equals("Large")) {
            return PointGeocachingData.CACHE_SIZE_LARGE;
        } else if (size.equals("Micro")) {
            return PointGeocachingData.CACHE_SIZE_MICRO;
        } else if (size.equals("Not chosen")) {
            return PointGeocachingData.CACHE_SIZE_NOT_CHOSEN;
        } else if (size.equals("Other")) {
            return PointGeocachingData.CACHE_SIZE_OTHER;
        } else if (size.equals("Regular")) {
            return PointGeocachingData.CACHE_SIZE_REGULAR;
        } else {
            return PointGeocachingData.CACHE_SIZE_NOT_CHOSEN;
        }
    }

    public static int convertCacheType(String type) {
        if (type.equals("C")) {
            return PointGeocachingData.CACHE_TYPE_CACHE_IN_TRASH_OUT;
        } else if (type.equals("R")) {
            return PointGeocachingData.CACHE_TYPE_EARTH;
        } else if (type.equals("E")) {
            return PointGeocachingData.CACHE_TYPE_EVENT;
        } else if (type.equals("B")) {
            return PointGeocachingData.CACHE_TYPE_LETTERBOX;
        } else if (type.equals("Z")) {
            return PointGeocachingData.CACHE_TYPE_MEGA_EVENT;
        } else if (type.equals("M")) {
            return PointGeocachingData.CACHE_TYPE_MULTI;
        } else if (type.equals("T")) {
            return PointGeocachingData.CACHE_TYPE_TRADITIONAL;
        } else if (type.equals("U")) {
            return PointGeocachingData.CACHE_TYPE_MYSTERY;
        } else if (type.equals("V")) {
            return PointGeocachingData.CACHE_TYPE_VIRTUAL;
        } else if (type.equals("W")) {
            return PointGeocachingData.CACHE_TYPE_WEBCAM;
        } else if (type.equals("I")) {
            return PointGeocachingData.CACHE_TYPE_WHERIGO;
        } else if (type.equals("A")) {
            return PointGeocachingData.CACHE_TYPE_PROJECT_APE;
        } else if (type.equals("L")) {
            return PointGeocachingData.CACHE_TYPE_LOCATIONLESS;
        } else if (type.equals("G")) {
            return PointGeocachingData.CACHE_TYPE_BENCHMARK; 
        } else if (type.equals("H")) {
            return PointGeocachingData.CACHE_TYPE_GROUNDSPEAK;                
        } else if (type.equals("X")) {
            return PointGeocachingData.CACHE_TYPE_MAZE_EXHIBIT;     
        } else if (type.equals("Y")) {
            return PointGeocachingData.CACHE_TYPE_WAYMARK;             
        } else if (type.equals("F")) {
            return PointGeocachingData.CACHE_TYPE_LF_EVENT;                         
        } else if (type.equals("D")) {
            return PointGeocachingData.CACHE_TYPE_LF_CELEBRATION;            
        } else {
            return PointGeocachingData.CACHE_TYPE_TRADITIONAL;
        }
    }

    public static boolean isAvailable(String status) {
        if (status.equals("A")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isArchived(String status) {
        if (status.equals("X")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isFound(int found) {
        if (found == 1) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isPremium(int premium) {
        if (premium == 1) {
            return true;
        } else {
            return false;
        }
    }

    public static String convertWaypointType(String waypointType) {
        if (waypointType.equals("Final Location")) {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_FINAL;
        } else if (waypointType.equals("Parking Area")) {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_PARKING;
        } else if (waypointType.equals("Question to Answer")) {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_QUESTION;
        } else if (waypointType.equals("Reference Point")) {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_REFERENCE;
        } else if (waypointType.equals("Stages of a Multicache")) {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_STAGES;
        } else if (waypointType.equals("Trailhead")) {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_TRAILHEAD;
        } else {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_REFERENCE;
        }
    }

    public static int convertLogType(String logType) {
        if (logType.equals("Announcement")) {
            return PointGeocachingData.CACHE_LOG_TYPE_ANNOUNCEMENT;
        } else if (logType.equals("Attended")) {
            return PointGeocachingData.CACHE_LOG_TYPE_ATTENDED;
        } else if (logType.equals("Didn't find it")) {
            return PointGeocachingData.CACHE_LOG_TYPE_NOT_FOUNDED;
        } else if (logType.equals("Enable Listing")) {
            return PointGeocachingData.CACHE_LOG_TYPE_ENABLE_LISTING;
        } else if (logType.equals("Found it")) {
            return PointGeocachingData.CACHE_LOG_TYPE_FOUNDED;
        } else if (logType.equals("Needs Archived")) {
            return PointGeocachingData.CACHE_LOG_TYPE_NEEDS_ARCHIVED;
        } else if (logType.equals("Needs Maintenance")) {
            return PointGeocachingData.CACHE_LOG_TYPE_NEEDS_MAINTENANCE;
        } else if (logType.equals("Owner Maintenance")) {
            return PointGeocachingData.CACHE_LOG_TYPE_OWNER_MAINTENANCE;
        } else if (logType.equals("Post Reviewer Note")) {
            return PointGeocachingData.CACHE_LOG_TYPE_POST_REVIEWER_NOTE;
        } else if (logType.equals("Publish Listing")) {
            return PointGeocachingData.CACHE_LOG_TYPE_PUBLISH_LISTING;
        } else if (logType.equals("Temporarily Disable Listing")) {
            return PointGeocachingData.CACHE_LOG_TYPE_TEMPORARILY_DISABLE_LISTING;
        } else if (logType.equals("Update Coordinates")) {
            return PointGeocachingData.CACHE_LOG_TYPE_UPDATE_COORDINATES;
        } else if (logType.equals("Webcam Photo Taken")) {
            return PointGeocachingData.CACHE_LOG_TYPE_WEBCAM_PHOTO_TAKEN;
        } else if (logType.equals("Will Attend")) {
            return PointGeocachingData.CACHE_LOG_TYPE_WILL_ATTEND;
        } else if (logType.equals("Write note")) {
            return PointGeocachingData.CACHE_LOG_TYPE_WRITE_NOTE;
        } else {
            return PointGeocachingData.CACHE_LOG_TYPE_UNKNOWN;
        }
    }

    public static boolean isCorrected(int correction) {
        if (correction == 1) {
            return true;
        } else {
            return false;
        }
    }

    public static ArrayList<PointGeocachingDataTravelBug> parseTravelBug(String tb) {
        ArrayList<PointGeocachingDataTravelBug> pgdtbl = new ArrayList<PointGeocachingDataTravelBug>();
        Pattern p = Pattern.compile("<BR>([^\\(]+)\\(id = ([0-9]+), ref = ([A-Z0-9]+)\\)");
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
        List<String> geocacheTypes = new ArrayList<String>();

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
            geocacheTypes.add("CacheType = \"E\"");
        }
        if (sharedPref.getBoolean("gc_type_cito", false)) {
            geocacheTypes.add("CacheType = \"C\"");
        }
        if (sharedPref.getBoolean("gc_type_mega", false)) {
            geocacheTypes.add("CacheType = \"Z\"");
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
