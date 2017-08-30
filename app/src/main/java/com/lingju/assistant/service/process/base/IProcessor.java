package com.lingju.assistant.service.process.base;

import com.lingju.context.entity.Command;
import com.lingju.context.entity.base.IChatResult;
import com.lingju.model.SmsInfo;

/**
 * Robot回复指令的处理器接口
 */
public interface IProcessor {

    /**
     * 获取Robot输出的最后一个{@linkplain IChatResult IChatResult}
     *
     * @return
     */
    public IChatResult getCurrentChatResult();

    /**
     * 将指定{@linkplain IChatResult IChatResult}绑定到调用者的线程变量中
     *
     * @param chatResult
     * @return
     */
    public IProcessor bind2CurrentThread(IChatResult chatResult);

    /**
     * 处理器的目标指令（针对一级指令而言）
     *
     * @return
     */
    public int aimCmd();

    /**
     * 处理Robot返回的指令
     *
     * @param cmd       输出指令
     * @param text      输出文本
     * @param inputType 输入类型
     */
    public void handle(Command cmd, String text, int inputType);

    /**
     * 处理未读短信
     **/
    void smsMsgHandle();

    /**
     * 接收短信
     *
     * @param sms 短信信息对象
     * @param number 发件人号码
     **/
    void receiveSms(SmsInfo sms, StringBuilder number);

    void cancelTingTask();
}
