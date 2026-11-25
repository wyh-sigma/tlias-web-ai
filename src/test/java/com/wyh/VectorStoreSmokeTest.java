package com.wyh;


import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class VectorStoreSmokeTest {

    @Autowired
    private SimpleVectorStore vectorStore;

    @Test
    void search(){
        var docs = vectorStore.similaritySearch(SearchRequest.builder().query("部门为什么删不掉").topK(3).build());
        docs.forEach(doc -> System.out.println(doc.getScore() + "| " + doc.getText()));
    }
}
