package com.lingju.assistant;

/**
 * Created by Administrator on 2016/11/4.
 */
public interface IView<T extends IPresenter> {

    public void setPresenter(T presenter);

}
