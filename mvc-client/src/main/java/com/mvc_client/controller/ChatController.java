package com.mvc_client.controller;

import com.mvc_client.Mapper.VectorMapper;
import com.mvc_client.RAG.LoadDocument;
import com.mvc_client.RAG.Readers;
import com.mvc_client.domain.Result;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor.FILTER_EXPRESSION;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatController {

    @Qualifier(value = "deepseek-chat")
    @Autowired
    private ChatClient deekseek_chat;

    @Qualifier(value = "deepseek-reasoner")
    @Autowired
    private ChatClient deekseek_reasoner;

    @Qualifier(value = "qwen-omni-turbo")
    @Autowired
    private ChatClient qwen_omni_turbo;

    private final Readers readers;

    private final LoadDocument loadDocument;

    private final VectorMapper vectorMapper;

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @RequestMapping(value = "/{model}/chat", produces = "text/html;charset=utf-8")
    public Flux<String> talk(String chatId, @PathVariable String model, String prompt ,
     @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        // 使用 Flux.defer() 将流的创建逻辑包裹起来

        List<String> fileHashesByChatId = vectorMapper.getFileHashesByChatId(chatId);
        String filterExpression = fileHashesByChatId.isEmpty()
                ? "hash IN ['Never Match']"
                : ("hash IN [" + fileHashesByChatId.stream()
                .map(hash -> "'" + hash + "'")
                .collect(Collectors.joining(", ")) + "]");

        System.out.println("filterExpression = " + filterExpression);
            return Flux.defer(() -> {
                if (model.equals("deepseek-chat")) {
                    return deekseek_chat.prompt()
                            .user(prompt)
                            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                            .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, filterExpression))
                            .stream()
                            .content();
                } else if (model.equals("deepseek-reasoner")) {
                    return deekseek_reasoner.prompt()
                            .user(prompt)
                            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                            .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, filterExpression))
                            .stream()
                            .content();
                } else if (model.equals("qwen-omni-turbo")) {

                    List<Media> medias;
                    if(files!=null) {
                        medias = files.stream()
                                .map(file -> new Media(
                                                MimeType.valueOf(Objects.requireNonNull(file.getContentType())),
                                                file.getResource()
                                        )
                                )
                                .toList();
                    } else {
                        medias = new ArrayList<>();
                    }

                    return qwen_omni_turbo.prompt()
                            .user(p -> p.text(prompt).media(medias.toArray(Media[]::new)))
                            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                            .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, filterExpression))
                            .stream()
                            .content();
                }

                return Flux.error(new IllegalArgumentException("Unsupported model: " + model));
            });
    }

    @RequestMapping("/upload/{chatId}")
    public Result uploadPdf(@PathVariable String chatId, @RequestParam("file") MultipartFile file) {
            String type = readers.getType(file.getResource());
            loadDocument.loadFile(file.getResource() , chatId , type);
            return Result.ok();
    }


}
