package com.yuwen.magiccube.repository;

import com.yuwen.magiccube.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Integer> {
    Optional<PostLike> findByPostIdAndUserId(Integer postId, Integer userId);
    List<PostLike> findByPostId(Integer postId);
    long countByPostId(Integer postId);
    
    // 新增：删除指定帖子的所有点赞
    void deleteByPostId(Integer postId);
}
