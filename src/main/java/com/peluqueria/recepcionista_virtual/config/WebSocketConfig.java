package com.peluqueria.recepcionista_virtual.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                        "https://hairai.netlify.app",
                        "http://localhost:3000",
                        "http://localhost:5173"
                )
                .withSockJS();

        // También sin SockJS para conexiones directas
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                        "https://hairai.netlify.app",
                        "http://localhost:3000",
                        "http://localhost:5173"
                );
    }
}