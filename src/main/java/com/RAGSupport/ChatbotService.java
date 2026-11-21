package com.RAGSupport;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    @Value("classpath*:knowledge/*.pdf")
    private Resource[] policyFiles;

    public ChatbotService(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    @PostConstruct
    public void loadKnowledgeBase() {
        try {
            List<Document> allChunks = new ArrayList<>();
            TokenTextSplitter splitter = TokenTextSplitter.builder().withChunkSize(300).build();

            // Loop through each PDF file
            if (policyFiles != null && policyFiles.length > 0) {
                for (Resource resource : policyFiles) {
                    PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
                    List<Document> pages = reader.read();
                    List<Document> chunks = splitter.apply(pages);
                    allChunks.addAll(chunks);
                }
                vectorStore.add(allChunks);
                log.info("Knowledge base loaded successfully: {} chunks indexed into vector store.", allChunks.size());
            }
        } catch (Exception e) {
            log.error("Could not index documents into vector store on startup: {}", e.getMessage(), e);
        }
    }

    public String answerUserQuery(String question) {
        try {
            List<Document> relevantChunks = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(question)
                    .topK(3)
                    .build()
            );
            StringBuilder context = new StringBuilder();
            if (relevantChunks != null) {
                for (Document document : relevantChunks) {
                    context.append(document.getText())
                            .append("\n\n");
                }
            }
            String finalContext = context.toString();

            String SYSTEM_PROMPT = """
                     You are an AI customer support assistant for our e-commerce company.
                    
                                    Answer the customer using ONLY the company information provided below.
                                    If the answer is not available in the provided information, say:
                                    "I don't have that information in the company documents."
                                    COMPANY INFORMATION
                                    %s
                    """.formatted(finalContext);

            return chatClient
                    .prompt(SYSTEM_PROMPT)
                    .user(question)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Error answering query: {}", e.getMessage(), e);
            return "An error occurred while processing your request: " + e.getMessage();
        }
    }
}
