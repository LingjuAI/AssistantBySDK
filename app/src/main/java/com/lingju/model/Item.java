package com.lingju.model;


import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@Entity(nameInDb = "t_item")
public class Item {
    /** GreenDao设置自增长时，主键必须为Long型，而long型仍不起作用 **/
    @Id(autoincrement = true)
    private Long id;
    private String item;
    /**
     * 0:收入  1：支出
     **/
    private int expense;

    public Item() {
    }

    public Item(String item) {
        this.item = item;
    }


    public Item(String item, int expense) {
        this.item = item;
        this.expense = expense;
    }

    @Generated(hash = 1652768961)
    public Item(Long id, String item, int expense) {
        this.id = id;
        this.item = item;
        this.expense = expense;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public int getExpense() {
        return expense;
    }

    public void setExpense(int expense) {
        this.expense = expense;
    }


}
