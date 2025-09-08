package com.mvc_client.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MemoryMapper {

    //@Select("SELECT DISTINCT conversation_id FROM spring_ai_chat_memory")
    public List<String> getHistoryId();

}
