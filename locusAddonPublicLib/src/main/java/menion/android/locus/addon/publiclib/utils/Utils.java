package menion.android.locus.addon.publiclib.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class Utils {

	public static boolean isAppAvailable(Context context, String packageName, int version) {
		//Log.i(TAG, "isAppAvailable(" + context + ", " + packageName + ", " + version + ")");
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
			if (info == null)
				return false;
			return info.versionCode >= version;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}
}
