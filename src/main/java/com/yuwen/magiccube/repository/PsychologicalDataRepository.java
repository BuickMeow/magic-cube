package com.yuwen.magiccube.repository;

import com.yuwen.magiccube.entity.PsychologicalData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PsychologicalDataRepository extends JpaRepository<PsychologicalData, Integer> {
    List<PsychologicalData> findByUserIdOrderByTestDateAsc(Integer userId);
    List<PsychologicalData> findByUserIdOrderByTestDateDesc(Integer userId);
    List<PsychologicalData> findAllByOrderByTestDateDesc();
    
    // 🌟 新增：按创建时间倒序查询所有数据（用于教师端按提交时间排序）
    List<PsychologicalData> findAllByOrderByCreatedAtDesc();
    
    // 🌟 新增：按创建时间倒序查询单个用户的数据（用于获取最新问卷）
    List<PsychologicalData> findByUserIdOrderByCreatedAtDesc(Integer userId);
    
    // 🌟 修改：添加倒序排序，确保最新的数据在前面
    List<PsychologicalData> findByUserIdAndCdRiscScoreIsNotNullOrderByCreatedAtDesc(Integer userId);
    
    List<PsychologicalData> findByUserIdAndTestDateBeforeOrderByTestDateAsc(Integer userId, LocalDate date);
    List<PsychologicalData> findByUserIdAndTestDateAfterOrderByTestDateAsc(Integer userId, LocalDate date);
    
    List<PsychologicalData> findByScoreBetween(Integer min, Integer max);
}