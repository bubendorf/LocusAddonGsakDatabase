package net.kuratkoo.locusaddon.gsakdatabase.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import net.kuratkoo.locusaddon.gsakdatabase.LoadActivity;

import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import locus.api.objects.extra.Location;
import locus.api.objects.geoData.Point;
import locus.api.objects.geocaching.GeocachingAttribute;
import locus.api.objects.geocaching.GeocachingData;
import locus.api.objects.geocaching.GeocachingLog;
import locus.api.objects.geocaching.GeocachingWaypoint;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static java.lang.Float.parseFloat;

public class GsakReader {

    public static void loadGCCodes(Context context, AsyncTask asyncTask, SQLiteDatabase database,
                                   List<Pair> gcCodes, Set<String> alreadyLoaded, Location curr) {
        String sql = buildCacheSQL(context);

        float radiusMeter = parseFloat(getDefaultSharedPreferences(context).getString("radius", "25")) * 1000;
        float radiusNorthSouth = 360f / (40007863 / radiusMeter);
        float radiusEastWest = 360f / (40075017 / radiusMeter) / (float)Math.cos(curr.getLatitude() / 180 * Math.PI);
        String[] cond = new String[]{
                String.valueOf(curr.getLatitude() - radiusNorthSouth),
                String.valueOf(curr.getLatitude() + radiusNorthSouth),
                String.valueOf(curr.getLongitude() - radiusEastWest),
                String.valueOf(curr.getLongitude() + radiusEastWest)
        };
        /* Load GC codes */
        Location loc = new Location();
        Cursor c = database.rawQuery(sql, cond);
        while (c.moveToNext()) {
            if (asyncTask !=null && asyncTask.isCancelled()) {
                c.close();
                return;
            }
            String code = c.getString(c.getColumnIndex("Code"));
            if (!alreadyLoaded.contains(code)) {
                alreadyLoaded.add(code);
                loc.setLatitude(c.getDouble(c.getColumnIndex("Latitude")));
                loc.setLongitude(c.getDouble(c.getColumnIndex("Longitude")));
                if (loc.distanceTo(curr) <= radiusMeter) {
                    gcCodes.add(new Pair(loc.distanceTo(curr), code, database));
                }
            }
        }
        c.close();
    }

    private static String buildCacheSQL(Context context) {
        StringBuilder sql = new StringBuilder(256);
        sql.append("SELECT Latitude, Longitude, Code FROM Caches WHERE (status = 'A'");

        // Disable geocaches
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean("disable", false)) {
            sql.append(" OR status = 'T'");
        }
        // Archived geocaches
        if (sharedPreferences.getBoolean("archive", false)) {
            sql.append(" OR status = 'X'");
        }
        sql.append(") ");

        // Found and not Found
        boolean found = sharedPreferences.getBoolean("found", false);
        boolean notfound = sharedPreferences.getBoolean("notfound", true);
        if (found || notfound) {
            sql.append(" AND ( 1=0 ");
            if (found) {
                sql.append(" OR Found = 1");
            }
            if (notfound) {
                sql.append(" OR Found = 0");
            }
            sql.append(" ) ");
        }

        if (!sharedPreferences.getBoolean("own", false)) {
            sql.append(" AND PlacedBy != '");
            sql.append(sharedPreferences.getString("nick", ""));
            sql.append("'");
        }

        List<String> geocacheTypes = Gsak.geocacheTypesFromFilter(sharedPreferences);
        boolean first = true;
        StringBuilder sqlType = new StringBuilder(256);
        for (String geocacheType : geocacheTypes) {
            if (first) {
                sqlType.append(geocacheType);
                first = false;
            } else {
                sqlType.append(" OR ").append(geocacheType);
            }
        }
        if (sqlType.length() > 0) {
            sql.append(" AND (");
            sql.append(sqlType);
            sql.append(")");
        }

        sql.append(" AND CAST(Latitude AS REAL) > ? AND CAST(Latitude AS REAL) < ? AND CAST(Longitude AS REAL) > ? AND CAST(Longitude AS REAL) < ?");
        return sql.toString();
    }

    @Nullable
    public static Point readGeocache(SQLiteDatabase database, String gcCode, boolean withDetails) throws ParseException {
        Cursor cacheCursor = database.rawQuery("SELECT * FROM CachesAll WHERE Code = ?", new String[]{gcCode});
        cacheCursor.moveToNext();
        Location loc = new Location(cacheCursor.getDouble(cacheCursor.getColumnIndex("Latitude")), cacheCursor.getDouble(cacheCursor.getColumnIndex("Longitude")));
        Point point = new Point(cacheCursor.getString(cacheCursor.getColumnIndex("Name")), loc);

        GeocachingData gcData = new GeocachingData();
        gcData.setCacheID(cacheCursor.getString(cacheCursor.getColumnIndex("Code")));
        gcData.setName(cacheCursor.getString(cacheCursor.getColumnIndex("Name")));
        gcData.setOwner(cacheCursor.getString(cacheCursor.getColumnIndex("OwnerName")));
        gcData.setPlacedBy(cacheCursor.getString(cacheCursor.getColumnIndex("PlacedBy")));
        gcData.setDifficulty(cacheCursor.getFloat(cacheCursor.getColumnIndex("Difficulty")));
        gcData.setTerrain(cacheCursor.getFloat(cacheCursor.getColumnIndex("Terrain")));
        gcData.setCountry(cacheCursor.getString(cacheCursor.getColumnIndex("Country")));
        gcData.setState(cacheCursor.getString(cacheCursor.getColumnIndex("State")));
        gcData.setContainer(Gsak.convertContainer(cacheCursor.getString(cacheCursor.getColumnIndex("Container"))));
        gcData.setType(Gsak.convertCacheType(cacheCursor.getString(cacheCursor.getColumnIndex("CacheType"))));
        gcData.setAvailable(Gsak.isAvailable(cacheCursor.getString(cacheCursor.getColumnIndex("Status"))));
        gcData.setArchived(Gsak.isArchived(cacheCursor.getString(cacheCursor.getColumnIndex("Status"))));
        gcData.setFound(Gsak.isFound(cacheCursor.getInt(cacheCursor.getColumnIndex("Found"))));
        gcData.setPremiumOnly(Gsak.isPremium(cacheCursor.getInt(cacheCursor.getColumnIndex("IsPremium"))));
        gcData.setComputed(Gsak.isCorrected(cacheCursor.getInt(cacheCursor.getColumnIndex("HasCorrected"))));

        gcData.setLatOriginal(cacheCursor.getDouble(cacheCursor.getColumnIndex("LatOriginal")));
        gcData.setLonOriginal(cacheCursor.getDouble(cacheCursor.getColumnIndex("LonOriginal")));
//        gcData.setCacheUrl();
        gcData.setFavoritePoints(cacheCursor.getInt(cacheCursor.getColumnIndex("FavPoints")));
//        gcData.setGcVoteNumOfVotes(cacheCursor.getInt(cacheCursor.getColumnIndex("??")));
//        gcData.setGcVoteAverage(cacheCursor.getFloat(cacheCursor.getColumnIndex("??")));
//        gcData.setGcVoteUserVote(cacheCursor.getFloat(cacheCursor.getColumnIndex("??")));

        //gcData.setexported = dateFormat.format(date);
        gcData.setDateUpdated(getDate(cacheCursor, "LastUserDate"));
        gcData.setDateHidden(getDate(cacheCursor,"PlacedDate")); //TODO
        gcData.setDatePublished(getDate(cacheCursor,"PlacedDate")); // TODO

        if (withDetails) {
            // More!
            gcData.setNotes(cacheCursor.getString(cacheCursor.getColumnIndex("UserNote")));
            gcData.setEncodedHints(cacheCursor.getString(cacheCursor.getColumnIndex("Hints")));
            gcData.setDescriptions(cacheCursor.getString(cacheCursor.getColumnIndex("ShortDescription")),
                    cacheCursor.getInt(cacheCursor.getColumnIndex("ShortHtm")) == 1,
                    cacheCursor.getString(cacheCursor.getColumnIndex("LongDescription")),
                    cacheCursor.getInt(cacheCursor.getColumnIndex("LongHtm")) == 1);

            // TB & GC
            gcData.setTrackables(Gsak.parseTravelBug(cacheCursor.getString(cacheCursor.getColumnIndex("TravelBugs"))));
        }

        cacheCursor.close();

        /* Add waypoints to Geocache */
        ArrayList<GeocachingWaypoint> pgdws = new ArrayList<>();

        Cursor wpCursor = database.rawQuery("SELECT * FROM WayAll WHERE cParent = ?", new String[]{gcData.getCacheID()});
        while (wpCursor.moveToNext()) {
/*            if (this.isCancelled()) {
                wpCursor.close();
                return null;
            }*/
            GeocachingWaypoint waypoint = new GeocachingWaypoint();
            waypoint.setLat(wpCursor.getDouble(wpCursor.getColumnIndex("cLat")));
            waypoint.setLon(wpCursor.getDouble(wpCursor.getColumnIndex("cLon")));
            waypoint.setName(wpCursor.getString(wpCursor.getColumnIndex("cName")));
            waypoint.setType(Gsak.convertWaypointType(wpCursor.getString(wpCursor.getColumnIndex("cType"))));
            waypoint.setDesc(wpCursor.getString(wpCursor.getColumnIndex("cComment")));
            waypoint.setCode(wpCursor.getString(wpCursor.getColumnIndex("cCode")));
//            waypoint.setDescModified();
            pgdws.add(waypoint);
        }
        wpCursor.close();
        gcData.setWaypoints(pgdws);

        if (withDetails) {
            // Add logsCursor to Geocache
            //String limit = PreferenceManager.getDefaultSharedPreferences(context).getString("logs_count", "20");
            String limit = "20";
            Cursor logsCursor = database.rawQuery("SELECT * FROM LogsAll WHERE lParent = ? ORDER BY lDate DESC LIMIT ?",
                    new String[]{gcData.getCacheID(), limit});
            ArrayList<GeocachingLog> pgdls = new ArrayList<>();

            while (logsCursor.moveToNext()) {
                GeocachingLog pgdl = new GeocachingLog();
                pgdl.setDate(getDate(logsCursor, "lDate"));
                pgdl.setFinder(logsCursor.getString(logsCursor.getColumnIndex("lBy")));
                pgdl.setLogText(logsCursor.getString(logsCursor.getColumnIndex("lText")));
                pgdl.setType(Gsak.convertLogType(logsCursor.getString(logsCursor.getColumnIndex("lType"))));
                pgdls.add(pgdl);
            }
            logsCursor.close();
            gcData.setLogs(pgdls);
        }

        if (withDetails) {
            // Add attributes to Geocache
            Cursor at = database.rawQuery("SELECT * FROM Attributes WHERE aCode = ?", new String[]{gcData.getCacheID()});
            List<GeocachingAttribute> pgas = new ArrayList<>();

            while (at.moveToNext()) {
                boolean isPositive = at.getInt(at.getColumnIndex("aInc")) == 1;
                GeocachingAttribute pga = new GeocachingAttribute(at.getInt(at.getColumnIndex("aId")), isPositive);
                pgas.add(pga);
            }
            at.close();
            gcData.setAttributes(pgas);
        }

        point.setGcData(gcData);
        point.setExtraOnDisplay("net.kuratkoo.locusaddon.gsakdatabase",
                "net.kuratkoo.locusaddon.gsakdatabase.DetailActivity",
                "cacheId", gcData.getCacheID());
        return point;
    }

    private static long getDate(Cursor c, final String columnName) throws ParseException {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        final String text = c.getString(c.getColumnIndex(columnName));
        if (text.length() == 10) {
            return dateFormat.parse(text).getTime();
        }
        return 0L;
    }


}
