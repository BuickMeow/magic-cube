-- magic-cube database schema and initial data
-- Target database: MySQL 8.0+
-- Encoding: UTF-8

CREATE DATABASE IF NOT EXISTS magic_cube
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE magic_cube;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS comment_likes;
DROP TABLE IF EXISTS post_likes;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS posts;
DROP TABLE IF EXISTS intervention_records;
DROP TABLE IF EXISTS psychological_data;
DROP TABLE IF EXISTS user_answers;
DROP TABLE IF EXISTS options;
DROP TABLE IF EXISTS questions;
DROP TABLE IF EXISTS questionnaires;
DROP TABLE IF EXISTS teaching_resource_file;
DROP TABLE IF EXISTS teaching_resource;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
  id INT NOT NULL AUTO_INCREMENT,
  username VARCHAR(255) NOT NULL,
  password VARCHAR(255),
  class_id VARCHAR(255),
  role VARCHAR(50) DEFAULT 'student',
  gender VARCHAR(20),
  age INT,
  grade VARCHAR(50),
  name VARCHAR(100),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  KEY idx_users_class_role (class_id, role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE questionnaires (
  id INT NOT NULL AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  description VARCHAR(1000),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE questions (
  id INT NOT NULL AUTO_INCREMENT,
  questionnaire_id INT NOT NULL,
  content VARCHAR(500) NOT NULL,
  question_type VARCHAR(50) NOT NULL,
  dimension VARCHAR(100),
  PRIMARY KEY (id),
  KEY idx_questions_questionnaire_id (questionnaire_id),
  CONSTRAINT fk_questions_questionnaire
    FOREIGN KEY (questionnaire_id) REFERENCES questionnaires(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE options (
  id INT NOT NULL AUTO_INCREMENT,
  question_id INT NOT NULL,
  content VARCHAR(200) NOT NULL,
  score_value INT,
  PRIMARY KEY (id),
  KEY idx_options_question_id (question_id),
  CONSTRAINT fk_options_question
    FOREIGN KEY (question_id) REFERENCES questions(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_answers (
  id INT NOT NULL AUTO_INCREMENT,
  user_id INT NOT NULL,
  questionnaire_id INT NOT NULL,
  question_id INT NOT NULL,
  option_id INT,
  scale_score INT,
  answer_text VARCHAR(1000),
  submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  intervention_status VARCHAR(50) DEFAULT 'PRE_INTERVENTION',
  PRIMARY KEY (id),
  KEY idx_user_answers_user_questionnaire (user_id, questionnaire_id),
  KEY idx_user_answers_question_id (question_id),
  KEY idx_user_answers_submitted_at (submitted_at),
  CONSTRAINT fk_user_answers_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_user_answers_questionnaire
    FOREIGN KEY (questionnaire_id) REFERENCES questionnaires(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_user_answers_question
    FOREIGN KEY (question_id) REFERENCES questions(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_user_answers_option
    FOREIGN KEY (option_id) REFERENCES options(id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE psychological_data (
  id INT NOT NULL AUTO_INCREMENT,
  user_id INT NOT NULL,
  score INT NOT NULL,
  test_date DATE NOT NULL,
  anxiety_level INT,
  motivation_level INT,
  focus_level INT,
  intervention_status VARCHAR(50) DEFAULT 'PRE_INTERVENTION',
  cd_risc_score INT,
  student_name VARCHAR(100),
  student_gender VARCHAR(20),
  student_age INT,
  student_grade VARCHAR(50),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_psychological_data_user_created (user_id, created_at),
  KEY idx_psychological_data_test_date (test_date),
  CONSTRAINT fk_psychological_data_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE intervention_records (
  id INT NOT NULL AUTO_INCREMENT,
  student_id INT NOT NULL,
  teacher_id INT NOT NULL,
  student_name VARCHAR(100),
  teacher_name VARCHAR(100),
  title VARCHAR(255),
  content VARCHAR(2000),
  intervention_type VARCHAR(100),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_intervention_student (student_id),
  KEY idx_intervention_teacher (teacher_id),
  CONSTRAINT fk_intervention_student
    FOREIGN KEY (student_id) REFERENCES users(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_intervention_teacher
    FOREIGN KEY (teacher_id) REFERENCES users(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE posts (
  id INT NOT NULL AUTO_INCREMENT,
  user_id INT NOT NULL,
  content VARCHAR(1000) NOT NULL,
  likes INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_pinned BOOLEAN DEFAULT FALSE,
  PRIMARY KEY (id),
  KEY idx_posts_user_id (user_id),
  KEY idx_posts_pinned_time (is_pinned, created_at),
  CONSTRAINT fk_posts_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comments (
  id INT NOT NULL AUTO_INCREMENT,
  post_id INT NOT NULL,
  user_id INT NOT NULL,
  content VARCHAR(500) NOT NULL,
  parent_id INT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_comments_post_id (post_id),
  KEY idx_comments_user_id (user_id),
  KEY idx_comments_parent_id (parent_id),
  CONSTRAINT fk_comments_post
    FOREIGN KEY (post_id) REFERENCES posts(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_comments_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_comments_parent
    FOREIGN KEY (parent_id) REFERENCES comments(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE post_likes (
  id INT NOT NULL AUTO_INCREMENT,
  post_id INT NOT NULL,
  user_id INT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_post_likes_post_user (post_id, user_id),
  KEY idx_post_likes_user_id (user_id),
  CONSTRAINT fk_post_likes_post
    FOREIGN KEY (post_id) REFERENCES posts(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_post_likes_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comment_likes (
  id INT NOT NULL AUTO_INCREMENT,
  comment_id INT NOT NULL,
  user_id INT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_comment_likes_comment_user (comment_id, user_id),
  KEY idx_comment_likes_user_id (user_id),
  CONSTRAINT fk_comment_likes_comment
    FOREIGN KEY (comment_id) REFERENCES comments(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_comment_likes_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE teaching_resource (
  id INT NOT NULL AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  folder_name VARCHAR(200),
  sort_order INT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_teaching_resource_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE teaching_resource_file (
  id INT NOT NULL AUTO_INCREMENT,
  resource_id INT NOT NULL,
  file_name VARCHAR(200) NOT NULL,
  file_type VARCHAR(50) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  file_size BIGINT,
  description VARCHAR(500),
  sort_order INT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  parent_id INT DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_resource_file_resource_parent (resource_id, parent_id),
  CONSTRAINT fk_resource_file_resource
    FOREIGN KEY (resource_id) REFERENCES teaching_resource(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Initial users. Passwords match the current project implementation.
INSERT INTO users (id, username, password, class_id, role, gender, age, grade, name, created_at) VALUES
(1, 'admin', 'admin123', 'class01', 'admin', 'male', 30, 'teacher', '张老师', NOW()),
(2, 'student', '123456', 'class01', 'student', 'female', 13, '七年级', '李明', NOW()),
(3, 'student02', '123456', 'class01', 'student', 'male', 14, '七年级', '王同学', NOW());

INSERT INTO questionnaires (id, title, description, created_at) VALUES
(1, 'CD-RISC 心理弹性评估问卷（25 道题）', 'Connor-Davidson 心理弹性量表，用于评估学生心理弹性与压力应对能力。', NOW());

INSERT INTO questions (id, questionnaire_id, content, question_type, dimension) VALUES
(1, 1, '我能适应变化', 'SCALE', 'RESILIENCE'),
(2, 1, '我有亲密、安全的关系', 'SCALE', 'RESILIENCE'),
(3, 1, '无论发生什么我都能应付', 'SCALE', 'RESILIENCE'),
(4, 1, '过去的成功让我有信心面对挑战', 'SCALE', 'RESILIENCE'),
(5, 1, '我能看到事情积极的一面', 'SCALE', 'RESILIENCE'),
(6, 1, '经历困难后我通常能恢复过来', 'SCALE', 'RESILIENCE'),
(7, 1, '我知道去哪里寻求帮助', 'SCALE', 'RESILIENCE'),
(8, 1, '在压力下我能集中注意力并清晰思考', 'SCALE', 'RESILIENCE'),
(9, 1, '我不会因失败而气馁', 'SCALE', 'RESILIENCE'),
(10, 1, '我有明确的目标感', 'SCALE', 'RESILIENCE'),
(11, 1, '我能实现自己的目标', 'SCALE', 'RESILIENCE'),
(12, 1, '当事情看起来没有希望时，我不会轻易放弃', 'SCALE', 'RESILIENCE'),
(13, 1, '我喜欢在解决问题时起带头作用', 'SCALE', 'RESILIENCE'),
(14, 1, '我认为自己是一个坚强的人', 'SCALE', 'RESILIENCE'),
(15, 1, '我能做出不寻常或艰难的决定', 'SCALE', 'RESILIENCE'),
(16, 1, '我能处理不愉快的情绪', 'SCALE', 'RESILIENCE'),
(17, 1, '我感觉能够掌控自己的生活', 'SCALE', 'RESILIENCE'),
(18, 1, '我喜欢挑战', 'SCALE', 'RESILIENCE'),
(19, 1, '我会努力工作以达到目标', 'SCALE', 'RESILIENCE'),
(20, 1, '我对自己的成就感到骄傲', 'SCALE', 'RESILIENCE'),
(21, 1, '我能够从压力经历中获得力量', 'SCALE', 'RESILIENCE'),
(22, 1, '我能够保持幽默感', 'SCALE', 'RESILIENCE'),
(23, 1, '我能够在困难中保持冷静', 'SCALE', 'RESILIENCE'),
(24, 1, '我能够接受事情并不总按计划发展的事实', 'SCALE', 'RESILIENCE'),
(25, 1, '我相信自己能够应对未来的挑战', 'SCALE', 'RESILIENCE');

INSERT INTO options (question_id, content, score_value)
SELECT q.id, o.content, o.score_value
FROM questions q
JOIN (
  SELECT '从来不' AS content, 0 AS score_value UNION ALL
  SELECT '很少', 1 UNION ALL
  SELECT '有时', 2 UNION ALL
  SELECT '经常', 3 UNION ALL
  SELECT '一直如此', 4
) o
WHERE q.questionnaire_id = 1;

INSERT INTO psychological_data
(id, user_id, score, test_date, anxiety_level, motivation_level, focus_level, intervention_status, cd_risc_score, student_name, student_gender, student_age, student_grade, created_at)
VALUES
(1, 2, 68, CURDATE() - INTERVAL 7 DAY, NULL, NULL, NULL, 'PRE_INTERVENTION', 68, '李明', 'female', 13, '七年级', NOW() - INTERVAL 7 DAY),
(2, 2, 76, CURDATE(), NULL, NULL, NULL, 'POST_INTERVENTION', 76, '李明', 'female', 13, '七年级', NOW()),
(3, 3, 58, CURDATE(), NULL, NULL, NULL, 'PRE_INTERVENTION', 58, '王同学', 'male', 14, '七年级', NOW());

INSERT INTO user_answers
(user_id, questionnaire_id, question_id, scale_score, submitted_at, intervention_status)
VALUES
(2, 1, 1, 3, NOW(), 'POST_INTERVENTION'),
(2, 1, 2, 3, NOW(), 'POST_INTERVENTION'),
(2, 1, 3, 4, NOW(), 'POST_INTERVENTION'),
(2, 1, 4, 4, NOW(), 'POST_INTERVENTION'),
(2, 1, 5, 3, NOW(), 'POST_INTERVENTION'),
(2, 1, 6, 3, NOW(), 'POST_INTERVENTION'),
(2, 1, 7, 4, NOW(), 'POST_INTERVENTION'),
(2, 1, 8, 3, NOW(), 'POST_INTERVENTION'),
(2, 1, 9, 4, NOW(), 'POST_INTERVENTION'),
(2, 1, 10, 4, NOW(), 'POST_INTERVENTION'),
(2, 1, 11, 3, NOW(), 'POST_INTERVENTION'),
(2, 1, 12, 3, NOW(), 'POST_INTERVENTION'),
(2, 1, 13, 3, NOW(), 'POST_INTERVENTION'),
(2, 1, 14, 3, NOW(), 'POST_INTERVENTION'),
(2, 1, 15, 3, NOW(), 'POST_INTERVENTION'),
(2, 1, 16, 3, NOW(), 'POST_INTERVENTION'),
(2, 1, 17, 3, NOW(), 'POST_INTERVENTION'),
(2, 1, 18, 3, NOW(), 'POST_INTERVENTION'),
(2, 1, 19, 3, NOW(), 'POST_INTERVENTION'),
(2, 1, 20, 3, NOW(), 'POST_INTERVENTION'),
(2, 1, 21, 3, NOW(), 'POST_INTERVENTION'),
(2, 1, 22, 2, NOW(), 'POST_INTERVENTION'),
(2, 1, 23, 2, NOW(), 'POST_INTERVENTION'),
(2, 1, 24, 2, NOW(), 'POST_INTERVENTION'),
(2, 1, 25, 2, NOW(), 'POST_INTERVENTION');

INSERT INTO intervention_records
(student_id, teacher_id, student_name, teacher_name, title, content, intervention_type, created_at)
VALUES
(2, 1, '李明', '张老师', '学习压力疏导记录', '学生近期学习压力较大，建议通过时间规划、适度运动和同伴沟通缓解压力。', '个别谈话', NOW());

INSERT INTO posts (id, user_id, content, likes, created_at, is_pinned) VALUES
(1, 1, '欢迎使用班级留言板，请同学们积极分享学习与生活中的想法。', 1, NOW() - INTERVAL 2 DAY, TRUE),
(2, 2, '今天完成了心理能量测评，感觉能更清楚地了解自己的状态。', 0, NOW() - INTERVAL 1 DAY, FALSE);

INSERT INTO comments (id, post_id, user_id, content, parent_id, created_at) VALUES
(1, 1, 2, '收到老师的提醒。', NULL, NOW() - INTERVAL 1 DAY),
(2, 2, 1, '保持记录，后续可以观察干预前后的变化。', NULL, NOW());

INSERT INTO post_likes (post_id, user_id, created_at) VALUES
(1, 2, NOW());

INSERT INTO teaching_resource (id, title, folder_name, sort_order, created_at) VALUES
(1, '《春》', '《春》', 1, NOW()),
(2, '《走一步，再走一步》', '《走一步，再走一步》', 2, NOW()),
(3, '五年级下册《田忌赛马》', '五年级下册《田忌赛马》', 3, NOW());

INSERT INTO teaching_resource_file
(resource_id, file_name, file_type, file_path, file_size, description, sort_order, created_at, parent_id)
VALUES
(1, '教案.docx', 'lesson_plan', 'teaching-resources/《春》/教案.docx', NULL, '教案', 1, NOW(), 0),
(1, '思维能力教学目标与引导建议.docx', 'teaching_guide', 'teaching-resources/《春》/思维能力教学目标与引导建议.docx', NULL, '思维能力教学目标与引导建议', 2, NOW(), 0),
(2, '教案.doc', 'lesson_plan', 'teaching-resources/《走一步，再走一步》/教案.doc', NULL, '教案', 1, NOW(), 0),
(3, '教案.docx', 'lesson_plan', 'teaching-resources/五年级下册《田忌赛马》/教案.docx', NULL, '教案', 1, NOW(), 0);
