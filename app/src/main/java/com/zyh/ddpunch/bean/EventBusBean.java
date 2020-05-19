package com.zyh.ddpunch.bean;

public class EventBusBean {
    private String receiveType;
    private int type;
    private Object content;

    public EventBusBean(String receiveType, int type, Object content) {
        this.receiveType = receiveType;
        this.type = type;
        this.content = content;
    }

    public String getReceiveType() {
        return receiveType;
    }

    public void setReceiveType(String receiveType) {
        this.receiveType = receiveType;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }
}
