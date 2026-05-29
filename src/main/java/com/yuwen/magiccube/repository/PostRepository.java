package com.yuwen.magiccube.repository;

import com.yuwen.magiccube.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {
    // 按发布时间倒序查询所有留言
    List<Post> findAllByOrderByCreatedAtDesc();

    // 按点赞数倒序查询所有留言
    List<Post> findAllByOrderByLikesDesc();

    // 按用户 ID 查询留言
    List<Post> findByUserId(Integer userId);

    // 按用户 ID 和创建时间查询（用于干预前后对比）
    List<Post> findByUserIdAndCreatedAtBefore(Integer userId, LocalDateTime dateTime);
    List<Post> findByUserIdAndCreatedAtAfter(Integer userId, LocalDateTime dateTime);

    // ================== 修改后的置顶排序方法 ==================
    // 1. 按时间：先按是否置顶降序，再按时间降序
    List<Post> findAllByOrderByIsPinnedDescCreatedAtDesc();

    // 2. 按点赞：先按是否置顶降序，再按点赞数降序，如果点赞数相同再按时间降序
    List<Post> findAllByOrderByIsPinnedDescLikesDescCreatedAtDesc();


}