package com.yuwen.magiccube.repository;

import com.yuwen.magiccube.entity.InterventionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterventionRecordRepository extends JpaRepository<InterventionRecord, Integer> {

    List<InterventionRecord> findByStudentIdOrderByCreatedAtDesc(Integer studentId);

    List<InterventionRecord> findByTeacherIdOrderByCreatedAtDesc(Integer teacherId);

    List<InterventionRecord> findByStudentIdAndTeacherIdOrderByCreatedAtDesc(Integer studentId, Integer teacherId);
}
