package com.yuwen.magiccube.repository;

import com.yuwen.magiccube.entity.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OptionRepository extends JpaRepository<Option, Integer> {
    List<Option> findByQuestionId(Integer questionId);
}
