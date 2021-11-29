package mq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import utils.ConfigUtil;

import java.util.Objects;

@Configuration
public class MQProducerTemplate implements InitializingBean, DisposableBean {

    public static Logger logger = LoggerFactory.getLogger(MQProducerTemplate.class);

    public static final String FILE_NAME = "api";
    public static final String ROCKETMQ_NAMESERVER = ConfigUtil.getPropVal2String(FILE_NAME, "rocketmq.nameserver");
    public static final String ROCKETMQ_PRODUCER_GROUP = ConfigUtil.getPropVal2String(FILE_NAME, "rocketmq.producer.group");

    private static DefaultMQProducer producer;

    static {
        // 实例化消息生产者Producer
        producer = new DefaultMQProducer(ROCKETMQ_PRODUCER_GROUP);
        // 设置NameServer的地址
        producer.setNamesrvAddr(ROCKETMQ_NAMESERVER);
    }


    /**
     * 同步发送
     * @param topic
     * @param tags
     * @param body
     */
    public MQSendResult syncSend(String topic,String tags,String body) throws Exception {

        MQSendResult mqSendResult = new MQSendResult();
        // 创建消息，并指定Topic，Tags和消息体
        Message  msg = new Message(topic /* Topic */,
                tags /* Tags */,
                body.getBytes(RemotingHelper.DEFAULT_CHARSET) /* Message body */
        );
        // 发送消息到一个Broker
        mqSendResult.setSendResult(producer.send(msg));
        return mqSendResult;
    }


    @Override
    public void destroy() throws Exception {
        if (Objects.nonNull(producer)) {
            producer.shutdown();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (producer != null) {
            producer.start();
        }
    }
}
