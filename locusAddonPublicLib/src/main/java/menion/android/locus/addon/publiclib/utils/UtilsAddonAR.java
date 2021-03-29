package menion.android.locus.addon.publiclib.utils;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import menion.android.locus.addon.publiclib.DisplayData;
import menion.android.locus.addon.publiclib.LocusConst;
import menion.android.locus.addon.publiclib.geoData.PointsData;
import menion.android.locus.addon.publiclib.geoData.Track;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

public class UtilsAddonAR {

	private static final String TAG = "UtilsAddonAR";
	
	// value that point have no altitude
	public static final float NO_ALTITUDE = Float.MIN_VALUE;
	
	// intent call to view AR
	public static final String INTENT = "menion.android.locus.addon.ar.VIEW";
	
	// id used for recognizing if add-on is closed or not. Register receiver in your 
	// starting activity on this number
	public static final int REQUEST_ADDON_AR = 30001;
	
	// extra ident for intent - location
	public static final String EXTRA_LOCATION = "EXTRA_LOCATION";
	// identificator of actual guiding item
	public static final String EXTRA_GUIDING_ID = "EXTRA_GUIDING_ID";
	// event to kill process
	public static final String EXTRA_END_AR = "EXTRA_END_AR";
	
	// result id of selected point
	public static final String RESULT_WPT_ID = "RESULT_WPT_ID";

	// broadcast identificator
	public final static String BROADCAST_DATA = "menion.android.locus.addon.ar.NEW_DATA";
	
	// last used location
	private static Location lastLocation;
	
	public static boolean isInstalled(Context context) {
		// check if exist new version of AR add-on. Older version used
		// different and deprecated API, so not usable anymore
		return Utils.isAppAvailable(context, "menion.android.locus.addon.ar", 3);
	}
	
	public static boolean showPoints(Activity context, ArrayList<PointsData> data,
			Location yourLoc, long guidedWptId) {
		if (!isInstalled(context)) {
			Log.i(TAG, "missing required version 3");
			return false;
		}
		
		// prepare intent and start it
		Intent intent = new Intent(INTENT);
		intent.putExtra(LocusConst.EXTRA_POINTS_DATA_ARRAY, data);
		intent.putExtra(EXTRA_LOCATION, yourLoc);
		intent.putExtra(EXTRA_GUIDING_ID, guidedWptId);
		
		// check intent firstly
		if (!DisplayData.hasData(intent)) {
			Log.w(TAG, "Intent 'null' or not contain any data");
			return false;
		}

		// store location
		lastLocation = yourLoc;

		// finally start activity
		context.startActivityForResult(intent, REQUEST_ADDON_AR);
		return true;
	}
	
	public static void updateLocation(Context context, Location loc) {
		// do some tests if is really need to send new location
		if ((loc.getTime() - lastLocation.getTime()) < 5000 ||
			(loc.distanceTo(lastLocation) < 5 &&
					Math.abs(loc.getAltitude() - lastLocation.getAltitude()) < 10)) {
			return;
		} else {
			lastLocation = loc;
			Intent intent = new Intent(BROADCAST_DATA);
			intent.putExtra(EXTRA_LOCATION, lastLocation);
			context.sendBroadcast(intent);
		}
	}
	
	public static void showTracks(Context context, ArrayList<Track> tracks)
			throws NoSuchAlgorithmException {
		throw new NoSuchAlgorithmException("Not yet implemented");
	}
}
