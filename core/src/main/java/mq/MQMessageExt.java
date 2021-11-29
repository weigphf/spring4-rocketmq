package mq;

import org.apache.rocketmq.common.message.MessageExt;

public class MQMessageExt {
    private MessageExt messageExt;

    public MessageExt getMessageExt() {
        return messageExt;
    }

    public void setMessageExt(MessageExt messageExt) {
        this.messageExt = messageExt;
    }
}
