package com.yuwen.magiccube.utils;

import cn.hutool.core.collection.CollUtil;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class WordCloudUtil {
    // 停用词（过滤掉"的、了、太"这类无意义的词）
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "的", "了", "太", "很", "真的", "就", "要", "都", "更", "让", "能", "有", "是", "在", "和", "也", "必", "别", "出", "从", "到", "多", "对", "而", "凡", "各", "给", "过", "还", "会", "及", "即", "既", "见", "具", "看", "可", "来", "乐", "类", "么", "每", "们", "那", "哪", "呢", "您", "凭", "其", "且", "却", "仍", "啥", "甚", "生", "使", "似", "所", "同", "为", "位", "我", "无", "勿", "喜", "相", "项", "以", "因", "应", "用", "于", "再", "则", "怎", "曾", "这", "之", "者", "只", "至", "致", "诸", "自", "吧", "啊", "呀", "哦", "嘛", "呗", "全", "啦", "道", "次", "遍", "个", "首", "句", "步", "分", "种"
    ));

    // 最小词长度（过滤掉单个字的无意义词）
    private static final int MIN_WORD_LENGTH = 2;

    /**
     * 统计所有帖子内容的关键词词频
     * @param contentList 数据库里所有帖子的content列表
     * @return 词频Map（关键词：出现次数）
     */
    public static Map<String, Integer> countWordFrequency(List<String> contentList) {
        // 存储所有有效关键词
        List<String> allValidWords = new ArrayList<>();

        // 遍历每个帖子内容，拆分+过滤
        for (String content : contentList) {
            if (content == null || content.trim().isEmpty()) { // 跳过空内容
                continue;
            }
            // 使用HanLP进行中文分词
            List<Term> terms = StandardTokenizer.segment(content);
            // 过滤：只留长度≥2的词 + 排除停用词
            List<String> validWords = terms.stream()
                    .map(term -> term.word.trim())
                    .filter(word -> word.length() >= MIN_WORD_LENGTH)
                    .filter(word -> !STOP_WORDS.contains(word))
                    .collect(Collectors.toList());
            allValidWords.addAll(validWords);
        }

        // 统计每个词出现的次数
        Map<String, Integer> wordCount = CollUtil.countMap(allValidWords);
        // 按词频降序排序
        return wordCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
    }
}