package com.yuwen.magiccube;

import com.yuwen.magiccube.service.PsychologicalDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class MagicCubeApplication {

    @Autowired
    private PsychologicalDataService psychologicalDataService;

    public static void main(String[] args) {
        SpringApplication.run(MagicCubeApplication.class, args);
    }

    // 应用启动完成后删除旧的星级评分数据
    @EventListener(ApplicationReadyEvent.class)
    public void deleteOldStarRatingData() {
        System.out.println("========== 删除旧的星级评分数据（1-5分） ==========");
        psychologicalDataService.deleteOldStarRatingData();
        System.out.println("========== 旧数据删除完成 ==========");
    }

}
