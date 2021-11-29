package mq;

import org.apache.rocketmq.client.producer.SendResult;

public class MQSendResult{
   private SendResult sendResult;

    public SendResult getSendResult() {
        return sendResult;
    }

    public void setSendResult(SendResult sendResult) {
        this.sendResult = sendResult;
    }
}
