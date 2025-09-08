package com.mvc_client.RAG;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Readers {

    public List<Document> getDocuments(Resource resource , String type) {
        switch (type) {
            case "pdf":
                return PdfReader(resource);
            case "xlsx":
                return XlsxReader(resource);
            case "md":
                return MdReader(resource);
            default:
                return TextReader(resource);
        }
    }

    public String getType(Resource resource) {
        String type = "text";
        String filename = resource.getFilename();
        if (filename != null && filename.endsWith(".pdf")) {
            type = "pdf";
        }
        if (filename != null && filename.endsWith(".xlsx")) {
            type = "xlsx";
        }
        if (filename != null && filename.endsWith(".md")) {
            type = "md";
        }
        return type;
    }

    private List<Document> PdfReader(Resource resource){
        return new PagePdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0)
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0)
                                .build())
                        .withPagesPerDocument(1)
                        .build()).read();
    }

    private List<Document> XlsxReader(Resource resource) {
        return new TikaDocumentReader(resource).read();
    }

    private List<Document> MdReader(Resource resource) {
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(true)
                .withIncludeBlockquote(true)
                .build();

        MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
        return reader.get();

    }

    List<Document> TextReader(Resource resource) {
        TextReader textReader = new TextReader(resource);
        return textReader.read();
    }



}
