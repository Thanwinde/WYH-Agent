package com.mvc_client.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VectorDto {

    private Long id;
    private String fileName;
    private String chatId;
    private String type;
    private String hash;

}
