package com.theost.wavenote.models;

import java.util.Date;

public class Track {

    private int id;
    private String name;
    private String fileName;
    private Date createTime;

    public Track(int id, String name, String fileName, Date createTime) {
        this.id = id;
        this.name = name;
        this.fileName = fileName;
        this.createTime = createTime;
    }

    public Track() { }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}