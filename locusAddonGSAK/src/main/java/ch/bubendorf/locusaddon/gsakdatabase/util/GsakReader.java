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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.bubendorf.locusaddon.gsakdatabase.DetailActivity;
import locus.api.android.objects.PackPoints;
import locus.api.objects.extra.Location;
import locus.api.objects.geoData.Point;
import locus.api.objects.geocaching.GeocachingAttribute;
import locus.api.objects.geocaching.GeocachingData;
import locus.api.objects.geocaching.GeocachingLog;
import locus.api.objects.geocaching.GeocachingWaypoint;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;

/**
 * @author Markus Bubendorf <gsakforlocus@bubendorf.net>
 */
public class GsakReader {

    public static SQLiteDatabase openDatabase(final Context context, final String dbId) {
        final SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean("pref_use_" + dbId, false)) {
            final String path = sharedPreferences.getString(dbId, "");
            if (Gsak.isGsakDatabase(path)) {
                return SQLiteDatabase.openDatabase(path,
                        null, SQLiteDatabase.NO_LOCALIZED_COLLATORS + SQLiteDatabase.OPEN_READONLY);
            }
        }
        return null;
    }

    @NonNull
    public static List<CacheWrapper> readGCCodes(final Context context, final GeocacheAsyncTask asyncTask,
                                                       final SQLiteDatabase db, final SQLiteDatabase db2, final SQLiteDatabase db3,
                                                       final Location centerLocation, final Location topLeftLocation,
                                                       final Location bottomRightLocation) {
        final Map<String, CacheWrapper> gcCodes = new HashMap<>(256);

        if (db != null && !asyncTask.isCancelled()) {
            GsakReader.loadGCCodes(context, asyncTask, db, gcCodes, centerLocation, topLeftLocation, bottomRightLocation);
        }
        if (db2 != null && !asyncTask.isCancelled()) {
            GsakReader.loadGCCodes(context, asyncTask, db2, gcCodes, centerLocation, topLeftLocation, bottomRightLocation);
        }
        if (db3 != null && !asyncTask.isCancelled()) {
            GsakReader.loadGCCodes(context, asyncTask, db3, gcCodes, centerLocation, topLeftLocation, bottomRightLocation);
        }

        final int limit = parseInt(getDefaultSharedPreferences(context).getString("limit", "100"));

        List<CacheWrapper> cacheWrappers = new ArrayList<>(gcCodes.values());
        cacheWrappers.sort((p1, p2) -> Float.compare(p1.distance, p2.distance));
        if (limit > 0 && cacheWrappers.size() > limit) {
            cacheWrappers = cacheWrappers.subList(0, limit);
        }
        return cacheWrappers;
    }

    @NonNull
    public static PackPoints readGeocaches(final GeocacheAsyncTask asyncTask, final List<CacheWrapper> gcCodes) throws ParseException {
        int count = 0;
        final int reportStepSize = gcCodes.size() >= 500 ? 50 : 10;
        final PackPoints packPoints = new PackPoints("GSAK data");
        for (final CacheWrapper cacheWrapper : gcCodes) {
            if (asyncTask.isCancelled()) {
                return packPoints;
            }
            if (count % reportStepSize == 0) {
                asyncTask.myPublishProgress(count);
            }
            final Point p = GsakReader.readGeocache(cacheWrapper.db, cacheWrapper.gcCode, false, null);
            if (p != null) {
                count++;
                packPoints.addPoint(p);
            }
        }
        return packPoints;
    }

    public static void loadGCCodes(@NonNull final Context context,
                                   @NonNull final GeocacheAsyncTask asyncTask,
                                   @NonNull final SQLiteDatabase database,
                                   @NonNull final Map<String, CacheWrapper> gcCodes,
                                   @NonNull final Location centerLocation,
                                   @Nullable final Location topLeftLocation,
                                   @Nullable final Location bottomRightLocation) {
        String sql = buildGCCodesSQL(context);

        double latFrom = Double.MIN_VALUE;
        double latTo = Double.MAX_VALUE;
        double lonFrom = Double.MIN_VALUE;
        double lonTo = Double.MAX_VALUE;

        final float radiusMeter = parseFloat(getDefaultSharedPreferences(context).getString("radius", "25")) * 1000;
        if (topLeftLocation != null && bottomRightLocation != null) {
            latFrom = Math.max(latFrom, bottomRightLocation.getLatitude());
            latTo = Math.min(latTo, topLeftLocation.getLatitude());
            lonFrom = Math.max(lonFrom, topLeftLocation.getLongitude());
            lonTo = Math.min(lonTo, bottomRightLocation.getLongitude());
        }

        final float radiusNorthSouth = 360f / (40007863 / radiusMeter);
        final float radiusEastWest = 360f / (40075017 / radiusMeter) / (float) Math.cos(centerLocation.getLatitude() / 180 * Math.PI);
        latFrom = Math.max(latFrom, centerLocation.getLatitude() - radiusNorthSouth);
        latTo = Math.min(latTo, centerLocation.getLatitude() + radiusNorthSouth);
        lonFrom = Math.max(lonFrom, centerLocation.getLongitude() - radiusEastWest);
        lonTo = Math.min(lonTo, centerLocation.getLongitude() + radiusEastWest);

        sql = sql.replace(":latFrom", Double.toString(latFrom));
        sql = sql.replace(":latTo", Double.toString(latTo));
        sql = sql.replace(":lonFrom", Double.toString(lonFrom));
        sql = sql.replace(":lonTo", Double.toString(lonTo));

        /* Load GC codes */
        final Location loc = new Location();
        final Cursor c = database.rawQuery(sql, null);
        while (c.moveToNext()) {
            if (asyncTask.isCancelled()) {
                c.close();
                return;
            }
            final String code = c.getString(c.getColumnIndex("Code"));
            loc.setLatitude(c.getDouble(c.getColumnIndex("Latitude")));
            loc.setLongitude(c.getDouble(c.getColumnIndex("Longitude")));
            final CacheWrapper cacheWrapper = gcCodes.get(code);
            final float distance = loc.distanceTo(centerLocation);
            if (cacheWrapper == null) {
                if (distance <= radiusMeter) {
                    gcCodes.put(code, new CacheWrapper(distance, code, database));
                }
            } else {
                // Update the entry if the distance is less
                if (distance < cacheWrapper.distance) {
                    cacheWrapper.distance = distance;
                    cacheWrapper.db = database;
                }
            }
        }
        c.close();
    }

    private static String buildGCCodesSQL(final Context context) {
        final SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        final boolean considerWayPoints = sharedPreferences.getBoolean("consider_wps", true);
        final StringBuilder sql = new StringBuilder(256);

        sql.append("SELECT c.Latitude, c.Longitude, c.Code ");
        sql.append("FROM Caches c ");
        appendWhereClause(sharedPreferences, sql);
        sql.append(" AND (");
        sql.append("CAST(c.Latitude AS REAL) >= :latFrom AND CAST(c.Latitude AS REAL) <= :latTo AND CAST(c.Longitude AS REAL) >= :lonFrom AND CAST(c.Longitude AS REAL) <= :lonTo");
        sql.append(" )");

        if (considerWayPoints) {
                sql.append("UNION ");
                sql.append("SELECT w.cLat as Latitude, w.cLon as Longitude, c.Code ");
                sql.append("FROM Caches c ");
                sql.append("LEFT JOIN Waypoints w on w.cParent = c.Code ");
                appendWhereClause(sharedPreferences, sql);
                sql.append(" AND ( (");
                sql.append("CAST(c.LatOriginal AS REAL) >= :latFrom AND CAST(c.LatOriginal AS REAL) <= :latTo AND CAST(c.LonOriginal AS REAL) >= :lonFrom AND CAST(c.LonOriginal AS REAL) <= :lonTo");
                sql.append(" ) OR (");
                sql.append("CAST(w.cLat AS REAL) >= :latFrom AND CAST(w.cLat AS REAL) <= :latTo AND CAST(w.cLon AS REAL) >= :lonFrom AND CAST(w.cLon AS REAL) <= :lonTo");
            sql.append(") )");
        }

        return sql.toString();
    }

    private static void appendWhereClause(final SharedPreferences sharedPreferences, final StringBuilder sql) {
        sql.append("WHERE (c.status = 'A'");
        // Disable geocaches
        if (sharedPreferences.getBoolean("disable", false)) {
            sql.append(" OR c.status = 'T'");
        }
        // Archived geocaches
        if (sharedPreferences.getBoolean("archive", false)) {
            sql.append(" OR c.status = 'X'");
        }
        sql.append(") ");

        // Found and not Found
        final boolean found = sharedPreferences.getBoolean("found", false);
        final boolean notfound = sharedPreferences.getBoolean("notfound", true);
        if (found || notfound) {
            sql.append(" AND ( 1=0 ");
            if (found) {
                sql.append(" OR c.Found = 1");
            }
            if (notfound) {
                sql.append(" OR c.Found = 0");
            }
            sql.append(" ) ");
        }

        if (!sharedPreferences.getBoolean("own", false)) {
            sql.append(" AND c.PlacedBy != '");
            sql.append(sharedPreferences.getString("nick", ""));
            sql.append("'");
        }

        final List<String> geocacheTypes = Gsak.geocacheTypesFromFilter(sharedPreferences);
        boolean first = true;
        final StringBuilder sqlType = new StringBuilder(256);
        for (final String geocacheType : geocacheTypes) {
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
    }

    @Nullable
    public static Point readGeocache(final SQLiteDatabase database, final String gcCode, final boolean withDetails, final String logLimit) throws ParseException {
        final String table = withDetails ? "CachesAll" : "Caches";
        final Cursor cacheCursor = database.rawQuery("SELECT * FROM " + table + " WHERE Code = ?", new String[]{gcCode});
        if (!cacheCursor.moveToNext()) {
            return null;
        }
        final Location loc = new Location(cacheCursor.getDouble(cacheCursor.getColumnIndex("Latitude")), cacheCursor.getDouble(cacheCursor.getColumnIndex("Longitude")));
        final Point point = new Point(cacheCursor.getString(cacheCursor.getColumnIndex("Name")), loc);

        final GeocachingData gcData = new GeocachingData();
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
        gcData.setDateHidden(getDate(cacheCursor, "Created"));
        gcData.setDatePublished(getDate(cacheCursor, "PlacedDate"));

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

        /* Add waypoints to Geocache */
        final ArrayList<GeocachingWaypoint> pgdws = new ArrayList<>();

        final Cursor wpCursor = database.rawQuery("SELECT * FROM WayAll WHERE cParent = ?", new String[]{gcData.getCacheID()});
        while (wpCursor.moveToNext()) {
/*            if (this.isCancelled()) {
                wpCursor.close();
                return null;
            }*/
            final GeocachingWaypoint waypoint = new GeocachingWaypoint();
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

        if (withDetails && logLimit != null) {
            final ArrayList<GeocachingLog> pgdls = new ArrayList<>();

            // Add the whole Custom table
           /* final GeocachingLog logEntry = new GeocachingLog();
            logEntry.setDate(System.currentTimeMillis());
            logEntry.setFinder("GSAK for Locus"); // TODO: übersetzen
            final StringBuilder sb = new StringBuilder();
            for (int colNum = 76; colNum < cacheCursor.getColumnCount(); colNum++) { // TODO Magic Number vermeiden
             sb.append(cacheCursor.getColumnName(colNum));
             sb.append(" = ");
             sb.append(cacheCursor.getString(colNum));
             sb.append("\n");
            }
            logEntry.setLogText(sb.toString());
            logEntry.setType(GeocachingLog.CACHE_LOG_TYPE_UNKNOWN);
            pgdls.add(logEntry);*/

            // Add logsCursor to Geocache
            final Cursor logsCursor = database.rawQuery("SELECT * FROM LogsAll WHERE lParent = ? ORDER BY lDate DESC LIMIT ?",
                    new String[]{gcData.getCacheID(), logLimit});

            while (logsCursor.moveToNext()) {
                final GeocachingLog pgdl = new GeocachingLog();
                pgdl.setDate(getDate(logsCursor, "lDate"));
                pgdl.setFinder(logsCursor.getString(logsCursor.getColumnIndex("lBy")));
                pgdl.setLogText(logsCursor.getString(logsCursor.getColumnIndex("lText")));
                pgdl.setType(Gsak.convertLogType(logsCursor.getString(logsCursor.getColumnIndex("lType"))));
                pgdls.add(pgdl);
            }
            logsCursor.close();
            gcData.setLogs(pgdls);
        }

        cacheCursor.close();

        if (withDetails) {
            // Add attributes to Geocache
            final Cursor at = database.rawQuery("SELECT * FROM Attributes WHERE aCode = ?", new String[]{gcData.getCacheID()});
            final List<GeocachingAttribute> pgas = new ArrayList<>();

            while (at.moveToNext()) {
                final boolean isPositive = at.getInt(at.getColumnIndex("aInc")) == 1;
                final GeocachingAttribute pga = new GeocachingAttribute(at.getInt(at.getColumnIndex("aId")), isPositive);
                pgas.add(pga);
            }
            at.close();
            gcData.setAttributes(pgas);
        }

        point.setGcData(gcData);
        point.setExtraOnDisplay(
                DetailActivity.class.getPackage().getName(),
                DetailActivity.class.getName(),
                "cacheId", gcData.getCacheID());
        return point;
    }

    private static long getDate(final Cursor c, final String columnName) throws ParseException {
        @SuppressLint("SimpleDateFormat") final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        final String text = c.getString(c.getColumnIndex(columnName));
        if (text.length() == 10) {
            return dateFormat.parse(text).getTime();
        }
        return 0L;
    }


}
