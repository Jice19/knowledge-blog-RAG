package com.jice19.blog.rag;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 客户端 BM25 稀疏向量生成：中文按字符二元组(bigram)近似分词、英文按词，统计 TF 词频。
 * 维度 id 用 FNV-1a 32 位 hash（非负）；IDF 由 Qdrant 稀疏向量的 modifier:idf 在查询时修正。
 * 说明：分词器已隔离在本类，生产可替换为 jieba/HanLP 等更精确的分词器。
 */
@Service
public class SparseVectorService {

    /** 稀疏向量：升序去重的维度 id + 对齐的 TF 权重 */
    public record SparseVector(int[] indices, float[] values) {
    }

    public SparseVector encode(String text) {
        TreeMap<Integer, Float> tf = new TreeMap<>();
        for (String token : tokenize(text)) {
            tf.merge(hash(token), 1f, Float::sum);
        }
        int[] indices = new int[tf.size()];
        float[] values = new float[tf.size()];
        int p = 0;
        for (Map.Entry<Integer, Float> e : tf.entrySet()) {
            indices[p] = e.getKey();
            values[p] = e.getValue();
            p++;
        }
        return new SparseVector(indices, values);
    }

    /** 分词：中文连续段 → 相邻二元组；英文/数字连续段 → 整词；空白标点跳过 */
    List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        text = text.toLowerCase();
        int n = text.length();
        int i = 0;
        while (i < n) {
            char c = text.charAt(i);
            if (isCjk(c)) {
                int start = i;
                while (i < n && isCjk(text.charAt(i))) {
                    i++;
                }
                String run = text.substring(start, i);
                if (run.length() == 1) {
                    tokens.add(run);
                } else {
                    for (int k = 0; k + 1 < run.length(); k++) {
                        tokens.add(run.substring(k, k + 2));
                    }
                }
            } else if (Character.isLetterOrDigit(c)) {
                int start = i;
                while (i < n && Character.isLetterOrDigit(text.charAt(i))) {
                    i++;
                }
                tokens.add(text.substring(start, i));
            } else {
                i++;
            }
        }
        return tokens;
    }

    private boolean isCjk(char c) {
        return c >= '\u4e00' && c <= '\u9fff';
    }

    /** FNV-1a 32 位 hash，取非负作为稀疏维度 id（< 2^31，Qdrant u32 合法） */
    private int hash(String s) {
        int h = 0x811c9dc5;
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
            h ^= (b & 0xff);
            h *= 0x01000193;
        }
        return h & 0x7fffffff;
    }
}
