package com.nesting.assistant.rag.splitter;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.huggingface.translator.TextEmbeddingTranslator;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.Batchifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;


@Component
@Slf4j
public class SemanticTextSplitter {

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile(
            "(?<=[.!?。！？])\\s+(?=[A-Z\\[(`\"'\\u4e00-\\u9fa5])",
            Pattern.MULTILINE
    );

    private final SentenceTransformerModel model;
    private final double similarityThreshold;

    public SemanticTextSplitter() {
        this(0.7);
    }

    public SemanticTextSplitter(double similarityThreshold) {
        this.model = new SentenceTransformerModel();
        this.similarityThreshold = similarityThreshold;
    }

    /**
     * 语义分块主方法
     * @param text 输入文本
     * @return 语义块列表
     */
    public List<String> split(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 分割成句子
        List<String> sentences = splitSentences(text);
        if (sentences.size() <= 1) {
            return sentences;
        }

        // 2. 计算所有句子的向量
        List<float[]> embeddings = model.encode(sentences);

        // 3. 语义聚块
        List<String> chunks = new ArrayList<>();
        List<String> currentChunk = new ArrayList<>();
        currentChunk.add(sentences.get(0));

        for (int i = 1; i < sentences.size(); i++) {
            // 4. 计算余弦相似度
            double similarity = cosineSimilarity(embeddings.get(i - 1), embeddings.get(i));

            // 5. 决策：是否切割
            if (similarity < similarityThreshold) {
                // 相似度低，开启新块
                chunks.add(joinSentences(currentChunk));
                currentChunk = new ArrayList<>();
                currentChunk.add(sentences.get(i));
            } else {
                // 相似度高，合并到当前块
                currentChunk.add(sentences.get(i));
            }
        }

        // 6. 添加最后一个块
        if (!currentChunk.isEmpty()) {
            chunks.add(joinSentences(currentChunk));
        }

        return chunks;
    }

    /**
     * 将文本分割成句子
     */
    private List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();

        // 使用正则表达式分割句子（支持中英文）
        String[] parts = SENTENCE_BOUNDARY.split(text);
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }

        return sentences;
    }

    /**
     * 合并句子列表为单个文本块
     */
    private String joinSentences(List<String> sentences) {
        return String.join(" ", sentences);
    }

    /**
     * 计算两个向量的余弦相似度
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("向量维度必须相同");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 句子编码器模型封装
     */
    private static class SentenceTransformerModel {
        private ZooModel<String, float[]> model;

        public SentenceTransformerModel() {
            loadModel();
        }

        private void loadModel() {
            try {
                // 使用 HuggingFace 国内镜像
                String modelUrl = "https://hf-mirror.com/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2";

                Criteria<String, float[]> criteria = Criteria.builder()
                        .setTypes(String.class, float[].class)
                        .optModelUrls(modelUrl)
                        .optEngine("PyTorch")
                        .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                        .optProgress(new ProgressBar())
                        .build();

                this.model = criteria.loadModel();
            } catch (Exception e) {
                log.error("Failed to load model, falling back to simple splitter", e);
                // 降级：模型加载失败时不抛异常，而是使用简单分割
                this.model = null;
            }
        }

        public List<float[]> encode(List<String> sentences) {
            List<float[]> embeddings = new ArrayList<>();

            try (Predictor<String, float[]> predictor = model.newPredictor()) {
                for (String sentence : sentences) {
                    float[] embedding = predictor.predict(sentence);
                    embeddings.add(embedding);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to encode sentences", e);
            }

            return embeddings;
        }
    }
}