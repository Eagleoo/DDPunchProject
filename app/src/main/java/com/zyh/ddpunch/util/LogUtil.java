package com.zyh.ddpunch.util;

import android.util.Log;

/**
 * Created by zyh on 2020/5/18.
 */

public class LogUtil {

    private static String TAG = "zyh";
    private static boolean isLog = true;
    public static void D(String str) {
        if (isLog) {
            Log.d(TAG, str);
        }
    }

    public static void E(String str) {
        if (isLog) {
            Log.e(TAG, str);
        }
    }
}
