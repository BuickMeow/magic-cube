package com.yuwen.magiccube.repository;

import com.yuwen.magiccube.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {
    // 按帖子 ID 查询评论，按时间正序
    List<Comment> findByPostIdOrderByCreatedAtAsc(Integer postId);
    
    // 按帖子 ID 和父评论 ID 查询（楼中楼）
    List<Comment> findByPostIdAndParentIdOrderByCreatedAtAsc(Integer postId, Integer parentId);
    
    // 查询某条评论的所有回复（用于级联删除）
    List<Comment> findByParentIdOrderByCreatedAtAsc(Integer parentId);
    
    // 🌟 新增：按用户 ID 查询该用户的所有评论
    List<Comment> findByUserIdOrderByCreatedAtDesc(Integer userId);
}