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

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import ch.bubendorf.locusaddon.gsakdatabase.DetailActivity;
import ch.bubendorf.locusaddon.gsakdatabase.R;
import locus.api.android.objects.PackPoints;
import locus.api.objects.extra.Location;
import locus.api.objects.geoData.Point;
import locus.api.objects.geocaching.GeocachingAttribute;
import locus.api.objects.geocaching.GeocachingData;
import locus.api.objects.geocaching.GeocachingLog;
import locus.api.objects.geocaching.GeocachingWaypoint;

import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;

/**
 * @author Markus Bubendorf <gsakforlocus@bubendorf.net>
 */
public class GsakReader {

    private static final String TAG = "GsakReader";

    public static SQLiteDatabase openDatabase(final Context context, final String dbId, final boolean ignorePrefs) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (ignorePrefs || sharedPreferences.getBoolean("pref_use_" + dbId, false)) {
            final String path = sharedPreferences.getString(dbId, "");
            if (Gsak.isReadableGsakDatabase(path)) {
                return SQLiteDatabase.openDatabase(path,
                        null, SQLiteDatabase.NO_LOCALIZED_COLLATORS + SQLiteDatabase.OPEN_READONLY);
            }
        }
        return null;
    }


    private static final Set<String> columnBlackList = new HashSet<>();

    static {
        columnBlackList.add("Code:1");
        columnBlackList.add("CacheType");
        columnBlackList.add("Container");
        columnBlackList.add("Difficulty");
        columnBlackList.add("Latitude");
        columnBlackList.add("LOCK");
        columnBlackList.add("LongHtm");
        columnBlackList.add("Longitude");
        columnBlackList.add("LongDescription");
        columnBlackList.add("ShortHtm");
        columnBlackList.add("ShortDescription");
        columnBlackList.add("Terrain");
        columnBlackList.add("LatOriginal");
        columnBlackList.add("LonOriginal");
        columnBlackList.add("Status");
        columnBlackList.add("GCV_AverageVote");
        columnBlackList.add("GCV_MedianVote");
        columnBlackList.add("GCV_UserVote");
        columnBlackList.add("GCV_VoteCount");
        columnBlackList.add("GCV_VoteDistribution");
        columnBlackList.add("GCV_VoteDistributionFull");
        columnBlackList.add("GCV_AverageQualified");
        columnBlackList.add("GCV_MedianQualified");
        columnBlackList.add("v2Owned");
        columnBlackList.add("ErUpdater");
        columnBlackList.add("cCode");
        columnBlackList.add("rowid");
    }

    public static final Collection<String> preselectList = new ArrayList<>();

    static {
        preselectList.add("AnzahlInGemeinde"); // Only MB
        preselectList.add("AnzahlInOrtschaft"); // Only MB
        preselectList.add("Country");
        preselectList.add("County");
        preselectList.add("DNFDate");
        preselectList.add("FavPoints");
        preselectList.add("FavRatio");
        preselectList.add("GefundenVon"); // Only MB
        preselectList.add("LastFoundDate");
        preselectList.add("LastLog");
        preselectList.add("Ortschaft"); // Only MB
        preselectList.add("UserNote");
        preselectList.add("User2");
        preselectList.add("User3");
        preselectList.add("User4");
    }

    /**
     * Returns the column names of the database table
     *
     * @param db Database connection
     * @param tableName Name of the table
     * @return Unsorted list of column names
     */
    @SuppressLint("Range")
    public static List<ColumnMetaData> getColumns(final SQLiteDatabase db, final String tableName) {
        final String sql = "pragma table_info(" + tableName + ")";

        final List<ColumnMetaData> columns = new ArrayList<>();
        final Cursor c = db.rawQuery(sql, null);
        while (c.moveToNext()) {
            final String columnName = c.getString(c.getColumnIndex("name"));
            if (!columnBlackList.contains(columnName)) {
                final String type = c.getString(c.getColumnIndex("type"));
                columns.add(new ColumnMetaData(tableName, columnName, type));
            }
        }
        c.close();
        return columns;
    }

    public static Collection<ColumnMetaData> getColumns(final SQLiteDatabase database) {
        final TreeSet<ColumnMetaData> allColumnNames = new TreeSet<>(ColumnMetaData.COMPARATOR);
        for (final String tableName : new String[]{"Caches", "CacheMemo", "Custom"}) {
            final List<ColumnMetaData> columnMetaDatas = getColumns(database, tableName);
            allColumnNames.addAll(columnMetaDatas);
        }
        return allColumnNames;
    }

    public static Collection<ColumnMetaData> getAllColumns(final Context context) {
        final TreeSet<ColumnMetaData> allColumnNames = new TreeSet<>(ColumnMetaData.COMPARATOR);
        for (final String dbId : new String[]{"db", "db2", "db3"}) {

            final String error = Gsak.checkDatabase(context, dbId);
            if (error != null) {
                throw new RuntimeException(error);
            }

            final SQLiteDatabase database = openDatabase(context, dbId, true);
            if (database != null) {
                allColumnNames.addAll(getColumns(database));
                database.close();
            }
        }
        return allColumnNames;
    }

    @NonNull
    public static List<CacheWrapper> readGCCodes(final Context context, final GeocacheAsyncTask asyncTask,
                                                 final SQLiteDatabase db, final SQLiteDatabase db2, final SQLiteDatabase db3,
                                                 final Location centerLocation, final Location topLeftLocation,
                                                 final Location bottomRightLocation) {
        final Map<String, CacheWrapper> gcCodes = new ConcurrentHashMap<>(256);

        final List<SQLiteDatabase> databases = new ArrayList<>();
        if (db != null) {
            databases.add(db);
        }
        if (db2 != null) {
            databases.add(db2);
        }
        if (db3 != null) {
            databases.add(db3);
        }
        final AtomicInteger count = new AtomicInteger(0);
        final int total = databases.size();
        databases.stream().parallel().forEach(database -> {
            GsakReader.loadGCCodes(context, asyncTask, database, gcCodes, centerLocation, topLeftLocation, bottomRightLocation);
            asyncTask.myPublishProgress(count.incrementAndGet(), total);
        });

/*        int count = 0;
        final int total = (db != null ? 1 : 0) + (db2 != null ? 1 : 0) + (db3 != null ? 1 : 0);
        if (db != null && !asyncTask.isCancelled()) {
            asyncTask.myPublishProgress(++count, total);
            GsakReader.loadGCCodes(context, asyncTask, db, gcCodes, centerLocation, topLeftLocation, bottomRightLocation);
        }
        if (db2 != null && !asyncTask.isCancelled()) {
            asyncTask.myPublishProgress(++count, total);
            GsakReader.loadGCCodes(context, asyncTask, db2, gcCodes, centerLocation, topLeftLocation, bottomRightLocation);
        }
        if (db3 != null && !asyncTask.isCancelled()) {
            asyncTask.myPublishProgress(++count, total);
            GsakReader.loadGCCodes(context, asyncTask, db3, gcCodes, centerLocation, topLeftLocation, bottomRightLocation);
        }*/

        final int limit = parseInt(getDefaultSharedPreferences(context).getString("limit", "100"));

        List<CacheWrapper> cacheWrappers = new ArrayList<>(gcCodes.values());
        cacheWrappers.sort((p1, p2) -> Float.compare(p1.distance, p2.distance));
        if (limit > 0 && cacheWrappers.size() > limit) {
            cacheWrappers = cacheWrappers.subList(0, limit);
        }
        return cacheWrappers;
    }

    @NonNull
    public static PackPoints readGeocaches(final Context context, final GeocacheAsyncTask asyncTask, final List<CacheWrapper> gcCodes) throws ParseException {
        int count = 0;
        final int reportStepSize = gcCodes.size() >= 500 ? 50 : 10;
        final PackPoints packPoints = new PackPoints("GSAK data");
        for (final CacheWrapper cacheWrapper : gcCodes) {
            if (asyncTask.isCancelled()) {
                return packPoints;
            }
            if (count % reportStepSize == 0) {
                asyncTask.myPublishProgress(count, gcCodes.size());
            }
            final Point p = GsakReader.readGeocache(context, cacheWrapper.db, cacheWrapper.gcCode, false, null);
            if (p != null) {
                count++;
                packPoints.addPoint(p);
            }
        }
        return packPoints;
    }

    @SuppressLint("Range")
    public static void loadGCCodes(@NonNull final Context context,
                                   @NonNull final GeocacheAsyncTask asyncTask,
                                   @NonNull final SQLiteDatabase database,
                                   @NonNull final Map<String, CacheWrapper> gcCodes,
                                   @NonNull final Location centerLocation,
                                   @Nullable final Location topLeftLocation,
                                   @Nullable final Location bottomRightLocation) {
        Log.d(TAG, "loadGCCodes(" + centerLocation + ", " + database.getPath() + ")");
        final long startTime = System.currentTimeMillis();

        String sql = buildGCCodesSQL(context);

        double latFrom = -Double.MAX_VALUE;
        double latTo = Double.MAX_VALUE;
        double lonFrom = -Double.MAX_VALUE;
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
        Log.d(TAG, sql);
        final Cursor c = database.rawQuery(sql, null);
        int count = 0;
        while (c.moveToNext()) {
            count++;
            if (asyncTask.isCancelled()) {
                c.close();
                return;
            }
            final String code = c.getString(c.getColumnIndexOrThrow("Code"));
            loc.setLatitude(c.getDouble(c.getColumnIndexOrThrow("Latitude")));
            loc.setLongitude(c.getDouble(c.getColumnIndexOrThrow("Longitude")));
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
        final long duration = System.currentTimeMillis() - startTime;
        Log.d(TAG, "loadGCCodes() Number of Caches = " + count + ", Duration = " + duration);
    }

    private static String buildGCCodesSQL(final Context context) {
        final SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        final boolean considerWayPoints = sharedPreferences.getBoolean("consider_wps", true);
        final StringBuilder sql = new StringBuilder(256);

        sql.append("SELECT c.Latitude as Latitude, c.Longitude as Longitude, c.Code as Code ");
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

        final String nick = sharedPreferences.getString("nick", "");
        if (!sharedPreferences.getBoolean("own", false) && nick.length() > 0) {
            sql.append(" AND c.PlacedBy != '");
            sql.append(nick);
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

    @SuppressLint("Range")
    public static String getNonNullString(final Cursor cacheCursor, final String fieldName) {
        final String value = cacheCursor.getString(cacheCursor.getColumnIndexOrThrow(fieldName));
        return value == null ? "" : value;
    }

    @Nullable
    @SuppressLint("Range")
    public static Point readGeocache(final Context context, final SQLiteDatabase database,
                                     final String gcCode, final boolean withDetails, final String logLimit) throws ParseException {
        final String table = withDetails ? "CachesAll" : "Caches";
        final Cursor cacheCursor = database.rawQuery("SELECT * FROM " + table + " WHERE Code = ?", new String[]{gcCode});
        if (!cacheCursor.moveToNext()) {
            return null;
        }
        final Location loc = new Location(cacheCursor.getDouble(cacheCursor.getColumnIndex("Latitude")), cacheCursor.getDouble(cacheCursor.getColumnIndex("Longitude")));
        final Point point = new Point(getNonNullString(cacheCursor,"Name"), loc);

        final GeocachingData gcData = new GeocachingData();
        gcData.setCacheID(getNonNullString(cacheCursor,"Code"));
        gcData.setName(getNonNullString(cacheCursor,"Name"));
        gcData.setOwner(getNonNullString(cacheCursor,"OwnerName"));
        gcData.setPlacedBy(getNonNullString(cacheCursor,"PlacedBy"));
        gcData.setDifficulty(cacheCursor.getFloat(cacheCursor.getColumnIndexOrThrow("Difficulty")));
        gcData.setTerrain(cacheCursor.getFloat(cacheCursor.getColumnIndexOrThrow("Terrain")));
        gcData.setCountry(getNonNullString(cacheCursor,"Country"));
        gcData.setState(getNonNullString(cacheCursor,"State"));
        gcData.setContainer(Gsak.convertContainer(cacheCursor.getString(cacheCursor.getColumnIndexOrThrow("Container"))));
        gcData.setType(Gsak.convertCacheType(cacheCursor.getString(cacheCursor.getColumnIndexOrThrow("CacheType"))));
        gcData.setAvailable(Gsak.isAvailable(cacheCursor.getString(cacheCursor.getColumnIndexOrThrow("Status"))));
        gcData.setArchived(Gsak.isArchived(cacheCursor.getString(cacheCursor.getColumnIndexOrThrow("Status"))));
        gcData.setFound(Gsak.isFound(cacheCursor.getInt(cacheCursor.getColumnIndexOrThrow("Found"))));
        gcData.setPremiumOnly(Gsak.isPremium(cacheCursor.getInt(cacheCursor.getColumnIndexOrThrow("IsPremium"))));
        gcData.setComputed(Gsak.isCorrected(cacheCursor.getInt(cacheCursor.getColumnIndexOrThrow("HasCorrected"))));

        gcData.setLatOriginal(cacheCursor.getDouble(cacheCursor.getColumnIndexOrThrow("LatOriginal")));
        gcData.setLonOriginal(cacheCursor.getDouble(cacheCursor.getColumnIndexOrThrow("LonOriginal")));
//        gcData.setCacheUrl();
        gcData.setFavoritePoints(cacheCursor.getInt(cacheCursor.getColumnIndexOrThrow("FavPoints")));
//        gcData.setGcVoteNumOfVotes(cacheCursor.getInt(cacheCursor.getColumnIndex("??")));
//        gcData.setGcVoteAverage(cacheCursor.getFloat(cacheCursor.getColumnIndex("??")));
//        gcData.setGcVoteUserVote(cacheCursor.getFloat(cacheCursor.getColumnIndex("??")));

        //gcData.setexported = dateFormat.format(date);
        gcData.setDateUpdated(getDate(cacheCursor, "LastUserDate"));
        gcData.setDateHidden(getDate(cacheCursor, "Created"));
        gcData.setDatePublished(getDate(cacheCursor, "PlacedDate"));

        if (withDetails) {
            // More!
            gcData.setNotesExternal(getNonNullString(cacheCursor,"UserNote"));
            gcData.setEncodedHints(getNonNullString(cacheCursor,"Hints"));
            gcData.setDescriptions(getNonNullString(cacheCursor,"ShortDescription"),
                    cacheCursor.getInt(cacheCursor.getColumnIndexOrThrow("ShortHtm")) == 1,
                    getNonNullString(cacheCursor,"LongDescription"),
                    cacheCursor.getInt(cacheCursor.getColumnIndexOrThrow("LongHtm")) == 1);

            // TB & GC
            gcData.setTrackables(Gsak.parseTravelBug(cacheCursor.getString(cacheCursor.getColumnIndexOrThrow("TravelBugs"))));
        }

        /* Add waypoints to Geocache */
        final ArrayList<GeocachingWaypoint> pgdws = new ArrayList<>();

        final Cursor wpCursor = database.rawQuery("SELECT * FROM WayAll WHERE cParent = ?", new String[]{gcData.getCacheID()});
        while (wpCursor.moveToNext()) {

            final GeocachingWaypoint waypoint = new GeocachingWaypoint();
            waypoint.setLat(wpCursor.getDouble(wpCursor.getColumnIndexOrThrow("cLat")));
            waypoint.setLon(wpCursor.getDouble(wpCursor.getColumnIndexOrThrow("cLon")));
            waypoint.setName(getNonNullString(wpCursor,"cName"));
            waypoint.setType(Gsak.convertWaypointType(wpCursor.getString(wpCursor.getColumnIndexOrThrow("cType"))));
            waypoint.setDesc(getNonNullString(wpCursor, "cComment"));
            waypoint.setCode(getNonNullString(wpCursor,"cCode"));
//            waypoint.setDescModified();
            pgdws.add(waypoint);
        }
        wpCursor.close();
        gcData.setWaypoints(pgdws);

        if (withDetails && logLimit != null) {
            final ArrayList<GeocachingLog> pgdls = new ArrayList<>();

            // Add the additional columns
            final List<ColumnMetaData> columns = new ArrayList<>(getColumns(database));
            columns.sort(ColumnMetaData.COMPARATOR);
            final SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
            columns.removeIf(column -> !sharedPreferences.getBoolean("column_" + column.getColumnName(), false));

            if (columns.size() > 0) {
                final GeocachingLog logEntry = new GeocachingLog();
                logEntry.setDate(System.currentTimeMillis());
                logEntry.setFinder(context.getText(R.string.app_name).toString());
                final StringBuilder sb = new StringBuilder(64);
                for (final ColumnMetaData column : columns) {
                    final String text = cacheCursor.getString(cacheCursor.getColumnIndex(column.getColumnName()));
                    if (text != null && text.length() > 0 /*&& !"0".equals(text)*/) {
                        sb.append(deCamelize(column.getColumnName()));
                        sb.append(": ");
                        sb.append(format(text, column));
                        sb.append("\n");
                    }
                }
                logEntry.setLogText(sb.toString());
                logEntry.setType(GeocachingLog.CACHE_LOG_TYPE_UNKNOWN);
                logEntry.setId(-254);
                pgdls.add(logEntry);
            }

              // Add logsCursor to Geocache
            final Cursor logsCursor = database.rawQuery("SELECT * FROM LogsAll WHERE lParent = ? ORDER BY lDate DESC LIMIT ?",
                    new String[]{gcData.getCacheID(), logLimit});

            while (logsCursor.moveToNext()) {
                final GeocachingLog pgdl = new GeocachingLog();
                pgdl.setDate(getDate(logsCursor, "lDate"));
                pgdl.setFinder(logsCursor.getString(logsCursor.getColumnIndexOrThrow("lBy")));

                final int latIndex = logsCursor.getColumnIndexOrThrow("lLat");
                if (!logsCursor.isNull(latIndex)) {
                    pgdl.setCooLat(logsCursor.getDouble(latIndex));
                }
                final int lonIndex = logsCursor.getColumnIndexOrThrow("lLon");
                if (!logsCursor.isNull(lonIndex)) {
                    pgdl.setCooLon(logsCursor.getDouble(lonIndex));
                }

                pgdl.setLogText(logsCursor.getString(logsCursor.getColumnIndexOrThrow("lText")));
                pgdl.setType(Gsak.convertLogType(logsCursor.getString(logsCursor.getColumnIndexOrThrow("lType"))));
                pgdl.setId(logsCursor.getLong(logsCursor.getColumnIndexOrThrow("lLogId")));
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
        final Package pkg = DetailActivity.class.getPackage();
        point.setExtraOnDisplay(
                pkg == null ? "" : pkg.getName(),
                DetailActivity.class.getName(),
                "cacheId", gcData.getCacheID());
        return point;
    }

    private static String format(final String text, final ColumnMetaData columnMetaData) {
        final String type = columnMetaData.getType().toLowerCase();
        switch (type) {
            case "text":
                final String columnName = columnMetaData.getColumnName().toLowerCase();
                if (columnName.contains("date") || "changed".equals(columnName) ||
                        "created".equals(columnName) || "lastlog".equals(columnName)) {
                    try {
                        final long value = getDate(text);
                        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(value));
                    } catch (final ParseException pe) {
                        return text;
                    }
                }
                return text.trim();
            case "integer":
                return text.trim();
            case "real":
                try {
                    final double value = Double.parseDouble(text);
                    return Double.toString(value);
                } catch (final NumberFormatException nfe) {
                    return text;
                }
        }
        return text.trim();
    }

    /**
     * Inserts a space between all lowercase and uppercase letters
     * @param text Input text
     * @return Text with additional spaces
     */
    public  static String deCamelize(final String text) {
        return text.replaceAll("([a-z])([A-Z0-9])", "$1 $2").replace('_', ' ');
    }

    @SuppressLint("Range")
    private static long getDate(final Cursor c, final String columnName) throws ParseException {
        final String text = c.getString(c.getColumnIndexOrThrow(columnName));
        return getDate(text);
    }

    private static long getDate(final String text) throws ParseException {
        @SuppressLint("SimpleDateFormat") final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if (text.length() == 10) {
            final Date date = dateFormat.parse(text);
            return date == null ? 0L : date.getTime();
        }
        return 0L;
    }


}
