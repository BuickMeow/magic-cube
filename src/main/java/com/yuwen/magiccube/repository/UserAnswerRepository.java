package com.yuwen.magiccube.repository;

import com.yuwen.magiccube.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Integer> {
    List<UserAnswer> findByUserIdAndQuestionnaireId(Integer userId, Integer questionnaireId);
    void deleteByUserIdAndQuestionnaireId(Integer userId, Integer questionnaireId);
    
    // 根据用户 ID 和提交日期范围查询（用于获取某次测试的所有答案）
    List<UserAnswer> findByUserIdAndSubmittedAtBetweenOrderBySubmittedAtAsc(
        Integer userId, 
        java.time.LocalDateTime startDateTime, 
        java.time.LocalDateTime endDateTime
    );
}
