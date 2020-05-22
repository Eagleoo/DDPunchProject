package com.zyh.ddpunch.service;

import android.accessibilityservice.AccessibilityService;
import android.app.Instrumentation;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.blankj.utilcode.util.AppUtils;
import com.zyh.ddpunch.bean.EmailBean;
import com.zyh.ddpunch.bean.EventBusBean;
import com.zyh.ddpunch.constant.Constant;
import com.zyh.ddpunch.util.log.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static com.zyh.ddpunch.constant.Constant.main_page_ResId;
import static com.zyh.ddpunch.constant.Constant.search_page_ResId;
import static com.zyh.ddpunch.util.CommonUtils.findResIdById;
import static com.zyh.ddpunch.util.CommonUtils.startApplication;


/**
 * Created by zyh on 2020/5/18.
 * 模拟点击主服务
 */

public class MainAccessService extends AccessibilityService {

    private String TAG = MainAccessService.class.getSimpleName();
    private AccessibilityNodeInfo node;

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d("------MainAccessService------" + "辅助服务启动-------");
        startCSer();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.v(TAG, "------" + event.getPackageName());
    }

    private void startCSer() {
        startApplication(getApplicationContext(), Constant.dingding_PakeName, Constant.dingding_HomeName);
        Disposable subscribe = Observable.timer(15, TimeUnit.SECONDS)//打开钉钉界面有延迟
                .subscribeOn(Schedulers.io())
                .doOnNext(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        Logger.d("打卡倒计时到了");
                        node = refshPage();
                        if (node != null && Constant.dingding_PakeName.equals(node.getPackageName().toString())) {
                            node = refshPage();
                            if (findResIdById(node, main_page_ResId)) {
                                Logger.i("已进入钉钉主页");
                            } else {
                                Logger.e("未进入钉钉主页");
                            }

                            if (findResIdById(node, search_page_ResId)) {
                                Logger.i("找到钉钉搜索节点");
                            } else {
                                Logger.e("未找到钉钉搜索节点");
                            }
                            List<AccessibilityNodeInfo> list = node.findAccessibilityNodeInfosByViewId(search_page_ResId);
                            list.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        Toast.makeText(MainAccessService.this, "等待电脑打卡脚本执行", Toast.LENGTH_SHORT).show();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    //刷新节点
    private AccessibilityNodeInfo refshPage() {
        return getRootInActiveWindow();
    }

    @Subscribe
    public void onMessageEvent(final EventBusBean event) {
        switch (event.getReceiveType()) {
            case Constant.EVENT_BACK:
                Disposable mTimerSubscribe = Observable
                        .timer(5, TimeUnit.SECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(new Consumer<Long>() {
                            @Override
                            public void accept(Long aLong) throws Exception {
                                //先清除活动栈，避免再次拉起钉钉时保持在原界面
                                startApplication(getApplicationContext(), Constant.dingding_PakeName, Constant.dingding_HomeName);
                            }
                        })
                        .delay(3, TimeUnit.SECONDS)
                        .subscribe(new Consumer<Long>() {
                            @Override
                            public void accept(Long aLong) throws Exception {
                                startApplication(MainAccessService.this,Constant.appPackageName, Constant.activityPackageName);
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                throwable.printStackTrace();
                            }
                        });
                break;
            default:
                break;
        }
    }
    @Override
    public void onInterrupt() {

    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
    }
}
