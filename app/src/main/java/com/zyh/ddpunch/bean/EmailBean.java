package com.zyh.ddpunch.bean;

/**
 * Author: zyh
 * Create time: 2020/5/18 19:43
 */
public class EmailBean {
    private String title;
    private String content;

    public EmailBean(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
