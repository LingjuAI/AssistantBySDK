package com.lingju.assistant.entity.action;

/**
 * Created by Ken on 2017/5/12.
 */
public class DialogEntity {

    private int id = 310;
    private String label;
    private String[] data;
    private String type;

    public DialogEntity() {
    }

    public String[] getData() {
        return data;
    }

    public void setData(String[] data) {
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public enum DialogType {
        SINGLE, MULTIPLE
    }
}
