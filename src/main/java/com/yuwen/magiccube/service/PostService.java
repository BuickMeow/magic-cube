package com.yuwen.magiccube.service;

import com.yuwen.magiccube.entity.Comment;
import com.yuwen.magiccube.entity.Post;
import com.yuwen.magiccube.entity.User;
import com.yuwen.magiccube.entity.PostLike;
import com.yuwen.magiccube.entity.CommentLike;
import com.yuwen.magiccube.repository.CommentRepository;
import com.yuwen.magiccube.repository.PostRepository;
import com.yuwen.magiccube.repository.UserRepository;
import com.yuwen.magiccube.repository.PostLikeRepository;
import com.yuwen.magiccube.repository.CommentLikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    public Comment addComment(Integer postId, Integer userId, String content) {
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        return commentRepository.save(comment);
    }

    public Comment addReply(Integer postId, Integer userId, String content, Integer parentId) {
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        return commentRepository.save(comment);
    }

    public List<Post> getAllPosts() {
        return getAllPostsForUser(null, "time");
    }

    public List<Post> getAllPostsForUser(Integer currentUserId) {
        return getAllPostsForUser(currentUserId, "time");
    }

    public List<Post> getAllPostsForUser(Integer currentUserId, String sortBy) {
        List<Post> posts;

        // ========== 修改点 1：使用带有严格二级排序（时间托底）的查询 ==========
        if ("likes".equals(sortBy)) {
            // 先置顶，再按点赞数，点赞一样则按时间
            posts = postRepository.findAllByOrderByIsPinnedDescLikesDescCreatedAtDesc();
        } else if ("comments".equals(sortBy)) {
            // 评论数是在内存中计算的，所以数据库层面先给个基础排序（置顶 + 时间）托底
            posts = postRepository.findAllByOrderByIsPinnedDescCreatedAtDesc();
        } else {
            // 默认：先置顶，再按时间
            posts = postRepository.findAllByOrderByIsPinnedDescCreatedAtDesc();
        }

        for (Post post : posts) {
            Optional<User> userOptional = userRepository.findById(post.getUserId());
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                post.setUsername(user.getUsername());
                post.setRole(user.getRole());
            }

            List<PostLike> pLikes = postLikeRepository.findByPostId(post.getId());
            post.setLikes(pLikes.size());

            String pLikedStr = pLikes.stream()
                    .map(pl -> userRepository.findById(pl.getUserId()).map(User::getUsername).orElse("未知用户"))
                    .collect(Collectors.joining(", "));
            post.setLikedUsersStr(pLikedStr);

            if (currentUserId != null) {
                try {
                    Optional<PostLike> userLike = postLikeRepository.findByPostIdAndUserId(post.getId(), currentUserId);
                    post.setLikedByCurrentUser(userLike.isPresent());
                } catch (Exception e) {
                    post.setLikedByCurrentUser(false);
                }
            }

            List<Comment> allComments = commentRepository.findByPostIdOrderByCreatedAtAsc(post.getId());
            Map<Integer, Comment> commentMap = allComments.stream()
                    .collect(Collectors.toMap(Comment::getId, c -> c));

            for (Comment comment : allComments) {
                Optional<User> commentUserOptional = userRepository.findById(comment.getUserId());
                if (commentUserOptional.isPresent()) {
                    User commentUser = commentUserOptional.get();
                    comment.setUsername(commentUser.getUsername());
                    comment.setRole(commentUser.getRole());
                }

                List<CommentLike> cLikes = commentLikeRepository.findByCommentId(comment.getId());
                comment.setLikes(cLikes.size());

                String cLikedStr = cLikes.stream()
                        .map(cl -> userRepository.findById(cl.getUserId()).map(User::getUsername).orElse("未知用户"))
                        .collect(Collectors.joining(", "));
                comment.setLikedUsersStr(cLikedStr);

                if (currentUserId != null) {
                    try {
                        Optional<CommentLike> commentUserLike = commentLikeRepository.findByCommentIdAndUserId(comment.getId(), currentUserId);
                        comment.setLikedByCurrentUser(commentUserLike.isPresent());
                    } catch (Exception e) {
                        comment.setLikedByCurrentUser(false);
                    }
                } else {
                    comment.setLikedByCurrentUser(false);
                }
            }

            List<Comment> topLevelComments = allComments.stream()
                    .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                    .collect(Collectors.toList());

            for (Comment topLevel : topLevelComments) {
                topLevel.setComments(new ArrayList<>());
            }

            for (Comment reply : allComments) {
                if (reply.getParentId() != null && reply.getParentId() != 0) {
                    Comment directParent = commentMap.get(reply.getParentId());
                    if (directParent != null) {
                        reply.setParentUsername(directParent.getUsername());
                        Comment current = directParent;
                        while (current.getParentId() != null && current.getParentId() != 0) {
                            Comment nextParent = commentMap.get(current.getParentId());
                            if (nextParent == null) break;
                            current = nextParent;
                        }
                        final Integer rootId = current.getId();
                        topLevelComments.stream()
                                .filter(t -> t.getId().equals(rootId))
                                .findFirst()
                                .ifPresent(t -> t.getComments().add(reply));
                    }
                }
            }
            post.setComments(topLevelComments);
        }

        // ========== 修改点 2：内存排序时，加入时间作为第二条件 ==========
        if ("comments".equals(sortBy)) {
            posts.sort((p1, p2) -> {
                boolean p1Pinned = p1.getIsPinned() != null ? p1.getIsPinned() : false;
                boolean p2Pinned = p2.getIsPinned() != null ? p2.getIsPinned() : false;

                // 第一梯队：是否置顶
                if (p1Pinned != p2Pinned) {
                    return p1Pinned ? -1 : 1;
                }

                // 第二梯队：比较评论数
                int commentCompare = Integer.compare(p2.getComments().size(), p1.getComments().size());
                if (commentCompare != 0) {
                    return commentCompare;
                }

                // 第三梯队：如果评论数也一样，按时间倒序（新的在上）
                if (p1.getCreatedAt() != null && p2.getCreatedAt() != null) {
                    return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                }
                return 0;
            });
        }
        return posts;
    }
    public Post createPost(Integer userId, String content) {
        Post post = new Post();
        post.setUserId(userId);
        post.setContent(content);
        return postRepository.save(post);
    }

    // ========== 修复点：帖子点赞实时校准数量与名单 ==========
    @Transactional
    public Map<String, Object> toggleLikePost(Integer postId, Integer userId) {
        Optional<Post> optionalPost = postRepository.findById(postId);
        if (optionalPost.isEmpty()) {
            return Map.of("success", false, "message", "帖子不存在");
        }

        boolean isLiked;
        try {
            Optional<PostLike> userLike = postLikeRepository.findByPostIdAndUserId(postId, userId);
            Post post = optionalPost.get();

            if (userLike.isPresent()) {
                postLikeRepository.delete(userLike.get());
                isLiked = false;
            } else {
                PostLike postLike = new PostLike();
                postLike.setPostId(postId);
                postLike.setUserId(userId);
                postLikeRepository.save(postLike);
                isLiked = true;
            }

            // 解决事务延迟：拿取最新的列表后在内存中进行二次校准
            List<PostLike> updatedLikes = postLikeRepository.findByPostId(postId);
            if (!isLiked) {
                updatedLikes.removeIf(pl -> pl.getUserId().equals(userId));
            } else {
                if (updatedLikes.stream().noneMatch(pl -> pl.getUserId().equals(userId))) {
                    PostLike newPl = new PostLike();
                    newPl.setUserId(userId);
                    updatedLikes.add(newPl);
                }
            }

            // 校准真实点赞数，不依赖 getLikes()
            int realLikeCount = updatedLikes.size();
            post.setLikes(realLikeCount);
            postRepository.save(post);

            String likedUsersStr = updatedLikes.stream()
                    .map(pl -> userRepository.findById(pl.getUserId()).map(User::getUsername).orElse("未知用户"))
                    .collect(Collectors.joining(", "));

            return Map.of("success", true, "liked", isLiked, "likes", realLikeCount, "likedUsersStr", likedUsersStr);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "点赞失败：" + e.getMessage());
        }
    }

    // ========== 修复点：评论点赞实时校准数量与名单 ==========
    @Transactional
    public Map<String, Object> toggleLikeComment(Integer commentId, Integer userId) {
        Optional<Comment> optionalComment = commentRepository.findById(commentId);
        if (optionalComment.isEmpty()) {
            return Map.of("success", false, "message", "评论不存在");
        }

        boolean isLiked;
        try {
            Optional<CommentLike> userLike = commentLikeRepository.findByCommentIdAndUserId(commentId, userId);
            Comment comment = optionalComment.get();

            if (userLike.isPresent()) {
                commentLikeRepository.delete(userLike.get());
                isLiked = false;
            } else {
                CommentLike commentLike = new CommentLike();
                commentLike.setCommentId(commentId);
                commentLike.setUserId(userId);
                commentLikeRepository.save(commentLike);
                isLiked = true;
            }

            // 解决事务延迟：拿取最新的列表后在内存中进行二次校准
            List<CommentLike> updatedLikes = commentLikeRepository.findByCommentId(commentId);
            if (!isLiked) {
                updatedLikes.removeIf(cl -> cl.getUserId().equals(userId));
            } else {
                if (updatedLikes.stream().noneMatch(cl -> cl.getUserId().equals(userId))) {
                    CommentLike newCl = new CommentLike();
                    newCl.setUserId(userId);
                    updatedLikes.add(newCl);
                }
            }

            // 校准真实点赞数，不依赖 getLikes()
            int realLikeCount = updatedLikes.size();
            comment.setLikes(realLikeCount);
            commentRepository.save(comment);

            String likedUsersStr = updatedLikes.stream()
                    .map(cl -> userRepository.findById(cl.getUserId()).map(User::getUsername).orElse("未知用户"))
                    .collect(Collectors.joining(", "));

            return Map.of("success", true, "liked", isLiked, "likes", realLikeCount, "likedUsersStr", likedUsersStr);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "点赞失败：" + e.getMessage());
        }
    }

    @Transactional
    public boolean deletePost(Integer postId, Integer currentUserId) {
        try {
            Optional<Post> optionalPost = postRepository.findById(postId);
            if (optionalPost.isEmpty()) {
                System.out.println("帖子不存在: " + postId);
                return false;
            }

            Post post = optionalPost.get();
            Optional<User> currentUserOpt = userRepository.findById(currentUserId);
            if (currentUserOpt.isEmpty()) {
                System.out.println("当前用户不存在: " + currentUserId);
                return false;
            }

            User currentUser = currentUserOpt.get();
            boolean canDelete = false;

            if (post.getUserId().equals(currentUserId)) {
                canDelete = true;
            } else if ("admin".equals(currentUser.getRole())) {
                Optional<User> postAuthorOpt = userRepository.findById(post.getUserId());
                if (postAuthorOpt.isPresent()) {
                    User postAuthor = postAuthorOpt.get();
                    if (!"admin".equals(postAuthor.getRole())) {
                        canDelete = true;
                    }
                }
            }

            if (!canDelete) {
                System.out.println("权限不足，无法删除帖子: " + postId);
                return false;
            }

            // 第一步：获取该帖子的所有评论
            List<Comment> allComments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
            System.out.println("找到 " + allComments.size() + " 条评论，开始删除...");

            // 第二步：先删除所有评论的点赞记录
            for (Comment comment : allComments) {
                try {
                    commentLikeRepository.deleteByCommentId(comment.getId());
                } catch (Exception e) {
                    System.err.println("删除评论点赞失败: " + comment.getId() + ", 错误: " + e.getMessage());
                }
            }

            // 第三步：删除帖子的点赞记录
            try {
                postLikeRepository.deleteByPostId(postId);
            } catch (Exception e) {
                System.err.println("删除帖子点赞失败: " + e.getMessage());
            }

            // 第四步：删除所有评论（使用逐个删除避免级联问题）
            for (Comment comment : allComments) {
                try {
                    commentRepository.deleteById(comment.getId());
                } catch (Exception e) {
                    System.err.println("删除评论失败: " + comment.getId() + ", 错误: " + e.getMessage());
                }
            }

            // 第五步：删除帖子本身
            postRepository.deleteById(postId);
            System.out.println("成功删除帖子: " + postId);

            return true;
        } catch (Exception e) {
            System.err.println("删除帖子时发生严重错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Transactional
    public boolean deleteComment(Integer commentId, Integer currentUserId) {
        try {
            Optional<Comment> optionalComment = commentRepository.findById(commentId);
            if (optionalComment.isEmpty()) {
                System.out.println("评论不存在: " + commentId);
                return false;
            }

            Comment comment = optionalComment.get();
            Optional<User> currentUserOpt = userRepository.findById(currentUserId);
            if (currentUserOpt.isEmpty()) {
                System.out.println("当前用户不存在: " + currentUserId);
                return false;
            }

            User currentUser = currentUserOpt.get();
            Optional<Post> parentPostOpt = postRepository.findById(comment.getPostId());
            boolean isPostOwner = parentPostOpt.isPresent() && parentPostOpt.get().getUserId().equals(currentUserId);

            boolean canDelete = false;
            if (comment.getUserId().equals(currentUserId)) {
                canDelete = true;
            } else if (isPostOwner) {
                canDelete = true;
            } else if ("admin".equals(currentUser.getRole())) {
                Optional<User> commentAuthorOpt = userRepository.findById(comment.getUserId());
                if (commentAuthorOpt.isPresent()) {
                    User commentAuthor = commentAuthorOpt.get();
                    if (!"admin".equals(commentAuthor.getRole())) {
                        canDelete = true;
                    }
                }
            }

            if (!canDelete) {
                System.out.println("权限不足，无法删除评论: " + commentId);
                return false;
            }

            // 查找所有子孙评论（包括直接回复和间接回复）
            List<Comment> allComments = commentRepository.findByPostIdOrderByCreatedAtAsc(comment.getPostId());
            List<Comment> descendantsToDelete = new ArrayList<>();
            findDescendants(commentId, allComments, descendantsToDelete);

            // 按层级从深到浅排序，确保先删除最深层的子评论
            descendantsToDelete.sort((c1, c2) -> {
                int depth1 = getDepth(c1.getId(), allComments);
                int depth2 = getDepth(c2.getId(), allComments);
                return Integer.compare(depth2, depth1);
            });

            // 第一步：删除所有子评论的点赞记录
            for (Comment child : descendantsToDelete) {
                commentLikeRepository.deleteByCommentId(child.getId());
            }

            // 第二步：删除所有子评论
            for (Comment child : descendantsToDelete) {
                commentRepository.deleteById(child.getId());
            }

            // 第三步：删除父评论的点赞记录
            commentLikeRepository.deleteByCommentId(commentId);

            // 第四步：删除父评论
            commentRepository.deleteById(commentId);

            System.out.println("成功删除评论: " + commentId + " 及其 " + descendantsToDelete.size() + " 个子评论");
            return true;
        } catch (Exception e) {
            System.err.println("删除评论时发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void findDescendants(Integer parentId, List<Comment> allComments, List<Comment> descendantsToDelete) {
        for (Comment c : allComments) {
            if (parentId.equals(c.getParentId())) {
                descendantsToDelete.add(c);
                findDescendants(c.getId(), allComments, descendantsToDelete);
            }
        }
    }

    private int getDepth(Integer commentId, List<Comment> allComments) {
        int depth = 0;
        Integer currentId = commentId;
        while (currentId != null) {
            final Integer searchId = currentId;
            Optional<Comment> parent = allComments.stream()
                    .filter(c -> c.getId().equals(searchId))
                    .findFirst();
            if (parent.isPresent() && parent.get().getParentId() != null) {
                currentId = parent.get().getParentId();
                depth++;
            } else {
                break;
            }
        }
        return depth;
    }

    public List<String> getAllPostContents() {
        return postRepository.findAll().stream()
                .map(Post::getContent)
                .collect(Collectors.toList());
    }
    // 切换帖子的置顶状态
    @Transactional
    public boolean togglePinPost(Integer postId, Integer currentUserId) {
        Optional<Post> optionalPost = postRepository.findById(postId);
        Optional<User> currentUserOpt = userRepository.findById(currentUserId);

        if (optionalPost.isPresent() && currentUserOpt.isPresent()) {
            User currentUser = currentUserOpt.get();
            // 只有老师有权限置顶
            if ("admin".equals(currentUser.getRole())) {
                Post post = optionalPost.get();
                // 切换状态：如果是 true 就变 false，如果是 false 或 null 就变 true
                post.setIsPinned(post.getIsPinned() != null && post.getIsPinned() ? false : true);
                postRepository.save(post);
                return true;
            }
        }
        return false;
    }
}