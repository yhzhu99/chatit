package com.buaa.chatserver.controller;


import org.springframework.beans.factory.annotation.Autowired;
import com.buaa.chatserver.model.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.MessageMapping;


// ref: https://blog.csdn.net/yingxiake/article/details/51224929
// @SendTo, @SendToUser, SimpMessagingTemplate

@Controller
public class ChatController {
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;


    @MessageMapping("/message") // url: /app/message
    @SendTo("/chatroom/public")
    public Message receivePublicMessage(@Payload Message message){
        return message;
    }

    @MessageMapping("/private-message")
    public Message receivePrivateMessage(@Payload Message message){
        simpMessagingTemplate.convertAndSendToUser(message.getReceiverName(), "/private", message); // /user/Alex/private
        return message;
    }

}
