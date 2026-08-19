package biz.thonbecker.personal.tankgame.web;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

class TankGameRawWebSocketHandler extends TextWebSocketHandler {

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
        session.sendMessage(new TextMessage("Godot WebSocket connected"));
    }

    @Override
    protected void handleTextMessage(final WebSocketSession session, final TextMessage message) throws Exception {
        session.sendMessage(new TextMessage("server received: " + message.getPayload()));
    }
}
