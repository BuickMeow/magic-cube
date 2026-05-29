package com.yuwen.magiccube.repository;

import com.yuwen.magiccube.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    List<Question> findByQuestionnaireId(Integer questionnaireId);
    List<Question> findByQuestionnaireIdAndDimension(Integer questionnaireId, String dimension);
}
