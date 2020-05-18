package com.zyh.ddpunch.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.zyh.ddpunch.constant.Constant;
import com.zyh.ddpunch.util.LogUtil;

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

    private AccessibilityNodeInfo node;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.E("------MainAccessService------" + "辅助服务启动-------");
        startCSer();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        LogUtil.E("------" + event.getPackageName());
    }

    private void startCSer() {
        startApplication(getApplicationContext(), Constant.dingding_PakeName);
        Disposable subscribe = Observable.timer(15, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .doOnNext(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        LogUtil.E("倒计时到了--------");
                        node = refshPage();
                        if (node != null && Constant.dingding_PakeName.equals(node.getPackageName().toString())) {
                            LogUtil.E("已进入app" + node);
                            node = refshPage();
                            if (findResIdById(node, main_page_ResId)) {
                                LogUtil.E("已进入app主页");
                            } else {
                                LogUtil.E("未进入app主页");
                            }

                            if (findResIdById(node, search_page_ResId)) {
                                LogUtil.E("找到搜索节点");
                            } else {
                                LogUtil.E("未找到搜索节点");
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

    @Override
    public void onInterrupt() {

    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
    }
}
