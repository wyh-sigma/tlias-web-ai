package com.wyh.loadtest;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * 压测专用的假向量模型：把文本按词哈希成固定维度向量，不发网络请求。
 *
 * <p>它不追求语义质量，只保证：同一段文本向量稳定、相似文本向量相邻，
 * 这样 SimpleVectorStore 的检索链路（相似度计算、topK、阈值过滤）能被真实执行到，
 * 压测时就不会被 DashScope 的 embedding 接口卡住。
 */
public class MockEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSIONS = 128;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> inputs = request.getInstructions();
        List<Embedding> embeddings = new ArrayList<>(inputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            embeddings.add(new Embedding(vector(inputs.get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return vector(document.getText());
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    private float[] vector(String text) {
        float[] vector = new float[DIMENSIONS];
        if (text == null || text.isEmpty()) {
            vector[0] = 1f;
            return vector;
        }
        for (String token : text.toLowerCase().split("[^\\p{L}\\p{N}]+")) {
            if (token.isEmpty()) {
                continue;
            }
            vector[Math.floorMod(token.hashCode(), DIMENSIONS)] += 1f;
        }
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }
        return vector;
    }
}
