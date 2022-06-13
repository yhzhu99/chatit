package com.buaa.chatserver.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Message {
    private String senderName, receiverName, message, date;
    private Status status;
    private String messageType; // text / image / file / music / video
    private String messageName;
}
