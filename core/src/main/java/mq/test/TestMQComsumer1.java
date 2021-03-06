package mq.test;

import mq.MQMessageExt;
import mq.RocketMQListener;
import mq.RocketMQMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@RocketMQMessageListener(
        topic = "appstore_notify_1", 		//topic：和消费者发送的topic相同
        consumerGroup = "appstore_notify_1"         //group：不用和生产者group相同
)
@Component
public class TestMQComsumer1 implements RocketMQListener<MQMessageExt> {
    public static Logger logger = LoggerFactory.getLogger(TestMQComsumer1.class);

    @Override
    public void onMessage(MQMessageExt message) {
        //处理业务逻辑
        logger.info("message==="+message.getMessageExt().toString());
        logger.info("topic==="+message.getMessageExt().getTopic());
        logger.info("body==="+new String(message.getMessageExt().getBody()));
    }

}