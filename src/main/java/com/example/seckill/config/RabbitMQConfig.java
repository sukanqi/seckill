package com.example.seckill.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    @Value("${seckill.rabbitmq.queue}")
    private String queueName;

    @Value("${seckill.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${seckill.rabbitmq.routing-key}")
    private String routingKey;

    private static final String DLX_EXCHANGE = "seckill.dlx.exchange";
    private static final String DLX_QUEUE = "seckill.dlx.queue";
    private static final String DLX_ROUTING_KEY = "seckill.dlx";

    @Bean
    public Queue seckillQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", DLX_ROUTING_KEY);
        return new Queue(queueName, true, false, false, args);
    }

    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue())
                .to(seckillExchange())
                .with(routingKey);
    }

    // === 死信队列配置 ===

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DLX_QUEUE, true);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DLX_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setMandatory(true);
        return rabbitTemplate;
    }
}
