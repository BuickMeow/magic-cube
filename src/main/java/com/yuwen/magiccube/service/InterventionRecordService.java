package com.yuwen.magiccube.service;

import com.yuwen.magiccube.entity.InterventionRecord;
import com.yuwen.magiccube.entity.User;
import com.yuwen.magiccube.repository.InterventionRecordRepository;
import com.yuwen.magiccube.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InterventionRecordService {

    @Autowired
    private InterventionRecordRepository interventionRecordRepository;

    @Autowired
    private UserRepository userRepository;

    public InterventionRecord addRecord(Integer studentId, Integer teacherId, String title, String content, String interventionType) {
        InterventionRecord record = new InterventionRecord();
        record.setStudentId(studentId);
        record.setTeacherId(teacherId);
        record.setTitle(title);
        record.setContent(content);
        record.setInterventionType(interventionType);

        Optional<User> studentOpt = userRepository.findById(studentId);
        if (studentOpt.isPresent()) {
            User student = studentOpt.get();
            record.setStudentName(student.getName() != null ? student.getName() : student.getUsername());
        }

        Optional<User> teacherOpt = userRepository.findById(teacherId);
        if (teacherOpt.isPresent()) {
            User teacher = teacherOpt.get();
            record.setTeacherName(teacher.getName() != null ? teacher.getName() : teacher.getUsername());
        }

        return interventionRecordRepository.save(record);
    }

    public List<InterventionRecord> getRecordsByStudent(Integer studentId) {
        return interventionRecordRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    public List<InterventionRecord> getRecordsByTeacher(Integer teacherId) {
        return interventionRecordRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId);
    }

    public List<InterventionRecord> getRecordsByStudentAndTeacher(Integer studentId, Integer teacherId) {
        return interventionRecordRepository.findByStudentIdAndTeacherIdOrderByCreatedAtDesc(studentId, teacherId);
    }

    public void deleteRecord(Integer recordId) {
        interventionRecordRepository.deleteById(recordId);
    }

    public Optional<InterventionRecord> getRecordById(Integer id) {
        return interventionRecordRepository.findById(id);
    }
}
