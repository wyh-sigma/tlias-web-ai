package com.wyh;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class EmbeddingSmokeTest {



    @Autowired
    private EmbeddingModel embeddingModel;

    @Test
    void test() {
        float[] vector = embeddingModel.embed("天机学堂一共有几个部门");
        System.out.println("维度=" + vector.length);
    }
}
