package com.zyh.ddpunch.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.zyh.ddpunch.constant.Constant.sdCardDir;

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

    public static Bitmap buffer2Bitmap(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        // 每个像素的间距
        int pixelStride = planes[0].getPixelStride();
        final ByteBuffer buffer = planes[0].getBuffer();
        // 总的间距
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);

        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }

    public static void saveBitmap(Bitmap bm) {
        try {
            File dirFile = new File(sdCardDir);
            if (!dirFile.exists()) {
                dirFile.mkdirs();
            }
            File file = new File(sdCardDir, "tmplName" + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
