package com.yuwen.magiccube.service;

import com.yuwen.magiccube.entity.PsychologicalData;
import com.yuwen.magiccube.entity.User;
import com.yuwen.magiccube.repository.PsychologicalDataRepository;
import com.yuwen.magiccube.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PsychologicalWarningService {

    @Autowired
    private PsychologicalDataRepository psychologicalDataRepository;

    @Autowired
    private UserRepository userRepository;

    // 预警阈值
    private static final double ANXIETY_THRESHOLD = 3.5;  // 焦虑维度阈值
    private static final double OVERALL_THRESHOLD = 2.5;  // 整体状态阈值

    // 检查是否需要预警
    public boolean checkWarningNeeded(Integer userId) {
        List<PsychologicalData> recentData = psychologicalDataRepository
                .findByUserIdOrderByTestDateDesc(userId)
                .stream()
                .limit(3) // 最近3次数据
                .toList();

        if (recentData.isEmpty()) return false;

        // 检查最近一次数据
        PsychologicalData latest = recentData.get(0);

        // 焦虑维度过高或整体状态过低触发预警
        return (latest.getAnxietyLevel() != null && latest.getAnxietyLevel() > ANXIETY_THRESHOLD) ||
                (latest.getScore() != null && latest.getScore() < OVERALL_THRESHOLD);
    }

    // 获取需要预警的学生列表
    public List<User> getStudentsNeedWarning() {
        List<User> students = userRepository.findByRole("student");
        return students.stream()
                .filter(student -> checkWarningNeeded(student.getId()))
                .toList();
    }
}
