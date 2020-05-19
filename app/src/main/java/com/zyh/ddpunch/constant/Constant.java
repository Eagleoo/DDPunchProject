package com.zyh.ddpunch.constant;

import android.os.Environment;

/**
 * Created by zyh on 2020/5/18.
 */

public class Constant {

    public static final int REQUEST_MEDIA_PROJECTION = 18;
    public static final int REQUEST_WINDOW_GRANT = 201;

    public static final String HOST = "smtp.qq.com";
    public static final String PORT = "465";

    //发送方邮箱
    public static final String sendEmailInfo ="714948055@qq.com";

    //接收方邮箱
    public static final String receiveEmailInfo ="zhangyuanhang@mayifengbao.com";

    public static final String emailPassWord = "nqmlrfgwtsrybccc";

    //默认上班打卡时间
    public static final String upJobTime="07:49";

    //默认下班打卡时间
    public static final String downJobTime="20:10";

    //钉钉包名
    public static final String dingding_PakeName="com.alibaba.android.rimet";

    //手机启动页
    public static final String launcher_PakeName="com.android.launcher3";
    //next 手机桌面
    public static final String launcher_PakeName2="com.google.android.googlequicksearchbox";

    //考勤页面判定
    public static String webview_page_ResId = "com.alibaba.android.rimet:id/webview_frame";

    //主页
    public static String main_page_ResId = "com.alibaba.android.rimet:id/home_bottom_tab_root";

    public static String work_page_ResId = "com.alibaba.android.rimet:id/home_bottom_tab_button_work";

    //搜索框
    public static String search_page_ResId = "com.alibaba.android.rimet:id/search_btn";

    //打卡搜索框
    public static String tv_page_ResId = "com.alibaba.android.rimet:id/item_name_tv";

    /**
     * EventBus截取图片完成标记
     */
    public static final String EVENT_PIC = "picture";

    public static String sdCardDir = Environment.getExternalStorageDirectory().getPath() + "/dingding/";

    public static final String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };

}
