package com.zyh.ddpunch.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;


import com.zyh.ddpunch.constant.Constant;
import com.zyh.ddpunch.email.EmailSender;
import com.zyh.ddpunch.util.LogUtil;
import com.zyh.ddpunch.util.TimeUtils;

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
            LogUtil.D("通知时间-->" + postTime);
            LogUtil.D("通知-->tikeText:" + tikeText);
            LogUtil.D("通知-->标题:" + notTitle + "--摘要--" + subText + "--正文--" + text);

            //首先判断通知时间是不是当前时间
            String nowTime = new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis());

            //如果是当天
            if (nowTime.equals(postTime)) {
                if (text.contains("上班打卡成功")) {
                    sendEmail(TimeUtils.millis2String(System.currentTimeMillis()) + "上班打卡成功", "服务通知2:\n" + text);
                }
                if (text.contains("下班打卡成功")) {
                    sendEmail(TimeUtils.millis2String(System.currentTimeMillis()) + "下班打卡成功", "服务通知2:\n" + text);
                }
            }
            cancelAllNotifications();
        }
    }

    private void sendEmail(final String title, final String content) {
        //耗时操作要起子线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EmailSender sender = new EmailSender();
                    //设置服务器地址和端口，可以查询网络
                    sender.setProperties(Constant.HOST, Constant.PORT);
                    //分别设置发件人，邮件标题和文本内容
                    sender.setMessage(Constant.sendEmailInfo, title, content);
                    //设置收件人
                    sender.setReceiver(new String[]{Constant.receiveEmailInfo});
                    //添加附件换成你手机里正确的路径
                    // sender.addAttachment("/sdcard/emil/emil.txt");
                    //发送邮件
                    //sender.setMessage("你的163邮箱账号", "EmailS//ender", "Java Mail ！");这里面两个邮箱账号要一致
                    sender.sendEmail(Constant.HOST, Constant.sendEmailInfo, Constant.emailPassWord);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
