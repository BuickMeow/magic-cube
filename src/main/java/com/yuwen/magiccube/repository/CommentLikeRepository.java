package com.yuwen.magiccube.repository;

import com.yuwen.magiccube.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Integer> {
    Optional<CommentLike> findByCommentIdAndUserId(Integer commentId, Integer userId);
    List<CommentLike> findByCommentId(Integer commentId);
    long countByCommentId(Integer commentId);
    
    // 新增：删除指定评论的所有点赞
    void deleteByCommentId(Integer commentId);
}
