package com.mvc_client.RAG;

import com.mvc_client.Mapper.VectorMapper;
import com.mvc_client.domain.VectorDto;
import com.mvc_client.util.HashTool;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class LoadDocument {

    private final VectorStore redisVectorStore;

    private final VectorMapper vectorMapper;

    private static final Logger logger = LoggerFactory.getLogger(LoadDocument.class);

    private final  Readers readers;

    @PostConstruct
    void initLoad() throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = null;
        try {
            resources = resolver.getResources("classpath:/initFiles/**/*.*");
        } catch (IOException e) {
            logger.error("读取初始目录文件出错：" + e.getMessage());
            return;
        }
        for (Resource resource : resources) {
            String fileName = resource.getFilename();
            System.out.println("加载初始文档: " + fileName);
            String hash;
            try {
                hash = HashTool.getResourceHash(resource);
            } catch (IOException e) {
                logger.warn("初始文档错误：" + e.getMessage());
                throw new RuntimeException(e);
            }

            List<VectorDto> vectorsByFileName = vectorMapper.getVectorsByFileHash(hash);
            if (!vectorsByFileName.isEmpty()) {
                System.out.println("初始文档已加载过: " + fileName);
            } else {
                String type = readers.getType(resource);
                List<Document> rawDocs  = readers.getDocuments(resource,type);

                List<Document> docs = new ArrayList<>();
                for (int i = 0; i < rawDocs.size(); i++) {
                    Document d = rawDocs.get(i);
                    Map<String, Object> md = new HashMap<>(d.getMetadata());
                    md.put("hash", hash);
                    md.put("file_name", fileName);
                    Document doc = Document.builder()
                            .metadata(md)
                            .text(d.getText())
                            .media(d.getMedia())
                            .id(d.getId())
                            .score(d.getScore())
                            .build();
                    docs.add(doc);
                }
                VectorDto vectorDto = VectorDto.builder()
                        .type(type)
                        .fileName(fileName)
                        .chatId("0")
                        .hash(hash)
                        .build();
                vectorMapper.addFileVector(vectorDto);
                redisVectorStore.add(docs);
            }
        }
        System.out.println("向量库已经添加全部初始文件");
    }

    public void loadFile(Resource resource , String chatId , String type) {
        String fileName = resource.getFilename();
        System.out.println("加载用户文档: " + fileName);
        String hash;
        try {
            hash = HashTool.getResourceHash(resource);
        } catch (IOException e) {
            logger.warn("用户文档错误：" + e.getMessage());
            throw new RuntimeException(e);
        }
        List<VectorDto> vectorsByFileHash = vectorMapper.getVectorsByFileHash(hash);

        if(!vectorsByFileHash.isEmpty()){
            System.out.println("用户文档已加载过: " + fileName);
        }else {
            List<Document> rawDocs = readers.getDocuments(resource,type);

            List<Document> docs = new ArrayList<>();
            for (int i = 0; i < rawDocs.size(); i++) {
                Document d = rawDocs.get(i);
                Map<String, Object> md = new HashMap<>(d.getMetadata());
                md.put("hash", hash);
                md.put("file_name", fileName);
                Document doc = Document.builder()
                        .metadata(md)
                        .text(d.getText())
                        .media(d.getMedia())
                        .id(d.getId())
                        .score(d.getScore())
                        .build();
                docs.add(doc);
            }


            VectorDto vectorDto = VectorDto.builder()
                    .type(type)
                    .fileName(fileName)
                    .chatId(chatId)
                    .hash(hash)
                    .build();
            vectorMapper.addFileVector(vectorDto);
            redisVectorStore.add(docs);
        }
        List<String> fileHashesByChatId = vectorMapper.getFileHashesByChatId(chatId);
        if(! fileHashesByChatId.contains(hash)) {
            logger.info("添加到chat文档: {}" , fileName);
            VectorDto vectorDto = VectorDto.builder()
                    .type(type)
                    .fileName(fileName)
                    .chatId(chatId)
                    .hash(hash)
                    .build();
            vectorMapper.addFileVector(vectorDto);
        }

        System.out.println("用户文档已加载");
    }


}
