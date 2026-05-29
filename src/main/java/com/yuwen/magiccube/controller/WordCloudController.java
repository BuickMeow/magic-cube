package com.yuwen.magiccube.controller;

import com.yuwen.magiccube.service.PostService;
import com.yuwen.magiccube.utils.WordCloudUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 词云接口控制器：提供词频数据给前端
 */
@RestController
@RequestMapping("/wordcloud")
@CrossOrigin // 解决前端跨域问题（必加，否则前端调接口会报错）
public class WordCloudController {

    @Autowired
    private PostService postService;

    /**
     * 获取词云的词频数据
     * 访问地址：http://localhost:8080/wordcloud/data
     */
    @GetMapping("/data")
    public Map<String, Integer> getWordCloudData() {
        // 1. 从数据库获取所有帖子的内容列表
        List<String> allPostContents = postService.getAllPostContents();
        // 2. 调用工具类统计关键词词频
        Map<String, Integer> wordFrequency = WordCloudUtil.countWordFrequency(allPostContents);
        // 3. 返回词频数据（JSON格式）
        return wordFrequency;
    }
}