package com.theost.wavenote.models;

public class Keyword {

    String id;
    String word;
    String type;

    public Keyword(String id, String word, String type) {
        this.id = id;
        this.word = word;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getWord() {
        return word;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
