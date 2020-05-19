package com.zyh.ddpunch.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;


import com.zyh.ddpunch.bean.EmailBean;
import com.zyh.ddpunch.bean.EventBusBean;
import com.zyh.ddpunch.constant.Constant;
import com.zyh.ddpunch.email.EmailSender;
import com.zyh.ddpunch.util.TimeUtils;
import com.zyh.ddpunch.util.log.Logger;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.MessagingException;

/**
 * Created by zyh on 2020/5/18.
 */

@SuppressLint("OverrideAbstract")
public class NotificationService extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        if (sbn.getPackageName().equals(Constant.dingding_PakeName)) {
            Notification notification = sbn.getNotification();
            if (notification == null) {
                return;
            }
            String tikeText = notification.tickerText == null ? "" : notification.tickerText.toString();
            String notTitle = notification.extras.getString("android.title") == null ? "" : notification.extras
                    .getString("android.title");//标题
            String subText = notification.extras.getString("android.subText") == null ? "" : notification.extras
                    .getString("android.subText");//摘要
            String text = notification.extras.getString("android.text") == null ? "" : notification.extras
                    .getString("android.text");  //正文
            String postTime = new SimpleDateFormat("yyyy-MM-dd").format(new Date(notification.when));   //通知时间
            Logger.d("通知时间-->" + postTime);
            Logger.d("通知-->tikeText:" + tikeText);
            Logger.d("通知-->标题:" + notTitle + "--摘要--" + subText + "--正文--" + text);

            //首先判断通知时间是不是当前时间
            String nowTime = new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis());

            //如果是当天
            if (nowTime.equals(postTime)) {
                if (text.contains("上班打卡成功")) {
                    EventBus.getDefault().post(new EventBusBean(Constant.EVENT_PIC, 0, new EmailBean(
                            TimeUtils.millis2String(System.currentTimeMillis()) + "上班打卡成功", "服务通知2:\n" + text)));
                }
                if (text.contains("下班打卡成功")) {
                    EventBus.getDefault().post(new EventBusBean(Constant.EVENT_PIC, 0, new EmailBean(
                            TimeUtils.millis2String(System.currentTimeMillis()) + "下班打卡成功", "服务通知2:\n" + text)));
                }
            }
            cancelAllNotifications();
        }
    }


}
