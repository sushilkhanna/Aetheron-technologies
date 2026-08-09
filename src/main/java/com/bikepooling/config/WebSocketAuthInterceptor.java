package com.bikepooling.config;

import com.bikepooling.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Missing or invalid Authorization header.");
            }

            String token = header.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                throw new IllegalArgumentException("Invalid or expired JWT token.");
            }

            Long userId = jwtUtil.extractUserId(token);
            accessor.setUser(() -> String.valueOf(userId));
        }

        return message;
    }
}