package com.mvc_client.Mapper;

import com.mvc_client.domain.VectorDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface VectorMapper {

    public List<VectorDto> getVectorsByFileName(String fileName);

    public List<VectorDto> getVectorsByFileHash(String hash);

    public List<String> getFileNamesByChatId(String chatId);

    public List<String> getFileHashesByChatId(String chatId);

    public Integer addFileVector(VectorDto vectorDto);

}
