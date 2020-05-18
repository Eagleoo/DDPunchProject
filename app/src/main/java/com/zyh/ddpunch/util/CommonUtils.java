package com.zyh.ddpunch.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author: zyh
 * Create time: 2020/5/11 19:13
 */
public class CommonUtils {

    //启动第三方app
    public static boolean startApplication(Context context, String packageName) {
        boolean rlt = false;

        PackageManager pkgMgr = context.getPackageManager();
        if (null != pkgMgr) {
            Intent intent = pkgMgr.getLaunchIntentForPackage(packageName);
            if (null != intent) {
                context.startActivity(intent);
                rlt = true;
            }
        }
        return rlt;
    }

    //通过控件id找到对应元素
    public static boolean findResIdById(AccessibilityNodeInfo info, String resId) {
        if (info == null) {
            return false;
        }
        List<AccessibilityNodeInfo> list = info.findAccessibilityNodeInfosByViewId(resId);

        if (list == null || list.size() == 0) {
            return false;
        }
        return true;
    }

    //判断email格式是否正确
    public boolean isEmail(String email) {
        String str = "^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))" +
                "([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$";
        Pattern p = Pattern.compile(str);
        Matcher m = p.matcher(email);

        return m.matches();
    }

    /**
     * 获取本地软件版本号名称
     */
    public static String getLocalVersionName(Context ctx) {
        String localVersion = "";
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            localVersion = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersion;
    }
}
