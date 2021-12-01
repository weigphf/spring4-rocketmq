/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mq;

import org.apache.rocketmq.client.AccessChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import utils.ConfigUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class ListenerContainerConfiguration implements ApplicationContextAware, SmartInitializingSingleton {
    private final static Logger log = LoggerFactory.getLogger(ListenerContainerConfiguration.class);

    public static final String FILE_NAME = "api";
    public static final String ROCKETMQ_NAMESERVER = ConfigUtil.getPropVal2String(FILE_NAME, "rocketmq.nameserver");
    public static final String ROCKETMQ_CONSUMER_GROUP = ConfigUtil.getPropVal2String(FILE_NAME, "rocketmq.consumer.group");

    private ConfigurableApplicationContext applicationContext;

    private AtomicLong counter = new AtomicLong(0);



    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
//        GenericApplicationContext ctx = new GenericApplicationContext(applicationContext);
//        //使用XmlBeanDefinitionReader
//        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
//        //加载ClassPathResource
//        xmlReader.loadBeanDefinitions(new ClassPathResource("rootContext.xml"));
        //调用Refresh方法,只能调用一次
//        ctx.refresh();


        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, Object> beans = this.applicationContext.getBeansWithAnnotation(RocketMQMessageListener.class);

        if (Objects.nonNull(beans)) {
            beans.forEach(this::registerContainer);
        }
    }

    private void registerContainer(String beanName, Object bean) {
        Class<?> clazz = AopProxyUtils.ultimateTargetClass(bean);

        if (!RocketMQListener.class.isAssignableFrom(bean.getClass())) {
            throw new IllegalStateException(clazz + " is not instance of " + RocketMQListener.class.getName());
        }

        RocketMQMessageListener annotation = clazz.getAnnotation(RocketMQMessageListener.class);

        String consumerGroup = annotation.consumerGroup();

        consumerGroup = StringUtils.isEmpty(consumerGroup) ? ROCKETMQ_CONSUMER_GROUP : consumerGroup;


        String topic = annotation.topic();


        validate(annotation);

        String containerBeanName = String.format("%s_%s", DefaultRocketMQListenerContainer.class.getName(),
                counter.incrementAndGet());
//        GenericApplicationContext  genericApplicationContext = (GenericApplicationContext)applicationContext;

//        genericApplicationContext.registerBean(containerBeanName, DefaultRocketMQListenerContainer.class,
//            () -> createRocketMQListenerContainer(containerBeanName, bean, annotation));

        DefaultRocketMQListenerContainer createContainer = createRocketMQListenerContainer(containerBeanName, bean, annotation);

        MutablePropertyValues pvs = new MutablePropertyValues();

        ReflectionUtils.doWithFields(DefaultRocketMQListenerContainer.class, new ReflectionUtils.FieldCallback() {
            public void doWith(Field field) throws IllegalArgumentException,
                    IllegalAccessException {
                int modify = field.getModifiers();
                field.setAccessible(true); // 设置些属性是可以访问的
                //final修饰的基本类型不可修改
                if (field.getType().isPrimitive() && Modifier.isFinal(modify)) {
                    return;
                }
                //static final同时修饰
                boolean  removeFinal = Modifier.isStatic(modify) && Modifier.isFinal(modify);
                if (removeFinal) {
                    return;
                }
                pvs.addPropertyValue(field.getName(),field.get(createContainer));
            }
        });


        ClassDerivedBeanDefinition classDerivedBeanDefinition = new ClassDerivedBeanDefinition(DefaultRocketMQListenerContainer.class,null,pvs);
//        genericApplicationContext.registerBeanDefinition(containerBeanName,classDerivedBeanDefinition);

        // 获取bean工厂并转换为DefaultListableBeanFactory
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
        defaultListableBeanFactory.registerBeanDefinition(containerBeanName, classDerivedBeanDefinition);

//        genericApplicationContext.refresh();

//        genericApplicationContext.registerBeanDefinition(containerBeanName,bean);

        DefaultRocketMQListenerContainer container = applicationContext.getBean(containerBeanName,
                DefaultRocketMQListenerContainer.class);


        if (!container.isRunning()) {
            try {
                container.start();
            } catch (Exception e) {
                log.error("Started container failed. {}", container, e);
                throw new RuntimeException(e);
            }
        }

        log.info("Register the listener to container, listenerBeanName:{}, containerBeanName:{}", beanName, containerBeanName);
    }



    @SuppressWarnings("serial")
    private static class ClassDerivedBeanDefinition extends RootBeanDefinition {

        public ClassDerivedBeanDefinition(ClassDerivedBeanDefinition original) {
            super(original);
        }

        public ClassDerivedBeanDefinition(Class<?> beanClass,ConstructorArgumentValues cargs, MutablePropertyValues pvs) {
            super(beanClass,cargs,pvs);
        }

        public Constructor<?>[] getPreferredConstructors() {
            Class<?> clazz = getBeanClass();
            Constructor<?>[] publicCtors = clazz.getConstructors();
            if (publicCtors.length > 0) {
                return publicCtors;
            }
            return null;
        }

        @Override
        public RootBeanDefinition cloneBeanDefinition() {
            return new ClassDerivedBeanDefinition(this);
        }
    }



    private DefaultRocketMQListenerContainer createRocketMQListenerContainer(String name, Object bean,
                                                                             RocketMQMessageListener annotation) {
        DefaultRocketMQListenerContainer container = new DefaultRocketMQListenerContainer();

        container.setRocketMQMessageListener(annotation);

        String nameServer = annotation.nameServer();
        nameServer = StringUtils.isEmpty(nameServer) ? ROCKETMQ_NAMESERVER : nameServer;
        container.setNameServer(nameServer);

        String accessChannel = annotation.accessChannel();
        if (!StringUtils.isEmpty(accessChannel)) {
            container.setAccessChannel(AccessChannel.valueOf(accessChannel));
        }else {
            container.setAccessChannel(AccessChannel.LOCAL);    //todo 华为云是否要改成CLOUD？
        }
        container.setTopic(annotation.topic());
        String tags = annotation.selectorExpression();
        if (!StringUtils.isEmpty(tags)) {
            container.setSelectorExpression(tags);
        }
        container.setConsumerGroup(annotation.consumerGroup());
        container.setRocketMQMessageListener(annotation);
        container.setRocketMQListener((RocketMQListener)bean);
//        container.setObjectMapper(objectMapper);
        container.setName(name);  // REVIEW ME, use the same clientId or multiple?

        return container;
    }

    private void validate(RocketMQMessageListener annotation) {
        if (annotation.consumeMode() == ConsumeMode.ORDERLY &&
                annotation.messageModel() == MessageModel.BROADCASTING) {
            throw new BeanDefinitionValidationException(
                    "Bad annotation definition in @RocketMQMessageListener, messageModel BROADCASTING does not support ORDERLY message!");
        }
    }
}
