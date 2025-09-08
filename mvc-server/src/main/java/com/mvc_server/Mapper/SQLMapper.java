package com.mvc_server.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Objects;

@Mapper
public interface SQLMapper {

    //@Select("SELECT DISTINCT conversation_id FROM spring_ai_chat_memory")
    public List<Object> runSQL(@Param("sql") String sql);

}
