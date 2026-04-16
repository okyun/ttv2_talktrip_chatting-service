package org.example.talktripchattingservice.chat.dto.request;

import lombok.Data;

@Data
public class ChatRoomRequestDto {
    private String buyerAccountEmail;
    private String sellerAccountEmail;
    private int productId;
}

