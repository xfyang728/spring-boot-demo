package com.example.springbootdemo.websocket;


import org.springframework.stereotype.Controller;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;


@Controller
@ServerEndpoint("/websocket/{code}")
public class WebSocket {
    private Session session;
    public static CopyOnWriteArraySet<WebSocket> webSockets = new CopyOnWriteArraySet<>();
    private static ConcurrentHashMap<String, Session> sessionPool = new ConcurrentHashMap<>();

    /**
     * 创建连接
     *
     * @param session
     * @param code
     */
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "code") String code) {
        this.session = session;
        webSockets.add(this);
        sessionPool.put(code, session);
        sessionPool.forEach((key, value) -> {
        });
        System.out.println("【websocket消息】有新的连接，总数为:" + webSockets.size());
    }

    /**
     * 断开连接
     */
    @OnClose
    public void onClose() {
        webSockets.remove(this);
        System.out.println("【websocket消息】连接断开，总数为:" + webSockets.size());
    }

    /**
     * 收到客户端消息
     *
     * @param message
     */
    @OnMessage
    public void onMessage(String message) {
        System.out.println("【websocket消息】收到客户端消息:" + message);
    }

    /**
     * 广播消息
     */
    public void sendAllMessage(String message) {
        for (WebSocket webSocket : webSockets) {
            try {
                webSocket.session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 给指定人发送单点消息
     *
     * @param code
     * @param message
     */
    public void sendOneMessage(String code, String message) {
        Session session = sessionPool.get(code);
        //在发送数据之前先确认 session是否已经打开 使用session.isOpen() 为true 则发送消息
        if (session != null && session.isOpen()) {
            try {
                session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
