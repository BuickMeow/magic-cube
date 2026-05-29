package com.yuwen.magiccube.controller;

import com.yuwen.magiccube.entity.Comment;
import com.yuwen.magiccube.entity.Post;
import com.yuwen.magiccube.entity.User;
import com.yuwen.magiccube.service.PostService;
import com.yuwen.magiccube.repository.UserRepository;
import com.yuwen.magiccube.repository.CommentRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/forum")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    // 留言板首页
    @GetMapping
    public String forum(
            Model model,
            HttpSession session,
            @RequestParam(value = "sortBy", defaultValue = "time") String sortBy) {
        User user = (User) session.getAttribute("currentUser");
        Integer currentUserId = user != null ? user.getId() : null;
        List<Post> posts = postService.getAllPostsForUser(currentUserId, sortBy);
        model.addAttribute("posts", posts);
        model.addAttribute("user", user);
        model.addAttribute("currentSort", sortBy);
        return "forum";
    }

    // 发布留言
    @PostMapping("/post")
    public String createPost(@RequestParam String content, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user != null) {
            Post newPost = postService.createPost(user.getId(), content);
            return "redirect:/forum#post-" + newPost.getId();
        }
        return "redirect:/forum";
    }

    // 点赞（支持取消）
    @PostMapping("/like/{postId}")
    @ResponseBody
    public Map<String, Object> likePost(@PathVariable Integer postId, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return Map.of("success", false, "message", "请先登录");
        }
        return postService.toggleLikePost(postId, user.getId());
    }

    // 点赞评论
    @PostMapping("/comment-like/{commentId}")
    @ResponseBody
    public Map<String, Object> likeComment(@PathVariable Integer commentId, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return Map.of("success", false, "message", "请先登录");
        }
        return postService.toggleLikeComment(commentId, user.getId());
    }

    // 添加评论
    @PostMapping("/comment/{postId}")
    public String addComment(@PathVariable Integer postId, @RequestParam String content, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user != null) {
            postService.addComment(postId, user.getId(), content);
        }
        return "redirect:/forum#post-" + postId;
    }

    // 添加回复
    @PostMapping("/reply/{postId}")
    @ResponseBody
    public Map<String, Object> addReply(
            @PathVariable Integer postId,
            @RequestParam String content,
            @RequestParam Integer parentId,
            HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return Map.of("success", false, "message", "请先登录");
        }

        try {
            Optional<Comment> parentCommentOpt = commentRepository.findById(parentId);
            if (parentCommentOpt.isEmpty()) {
                return Map.of("success", false, "message", "评论不存在");
            }

            Comment parentComment = parentCommentOpt.get();
            String replyToUsername = null;

            Optional<User> replyTargetUserOpt = userRepository.findById(parentComment.getUserId());
            if (replyTargetUserOpt.isPresent()) {
                replyToUsername = replyTargetUserOpt.get().getUsername();
            }

            Comment comment = postService.addReply(postId, user.getId(), content, parentId);

            Optional<User> userOpt = userRepository.findById(user.getId());
            if (userOpt.isPresent()) {
                comment.setUsername(userOpt.get().getUsername());
                comment.setRole(userOpt.get().getRole());
            }

            comment.setParentUsername(replyToUsername);

            return Map.of(
                    "success", true,
                    "message", "回复成功",
                    "commentId", comment.getId(),
                    "username", comment.getUsername(),
                    "role", comment.getRole(),
                    "content", content,
                    "createdAt", comment.getCreatedAt() != null ?
                            comment.getCreatedAt().toString().replace("T", " ") : "",
                    "parentUsername", replyToUsername
            );
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "回复失败");
        }
    }

    @GetMapping("/delete/{postId}")
    public String deletePost(@PathVariable Integer postId, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            System.out.println("删除帖子失败：用户未登录");
            return "redirect:/forum";
        }

        try {
            System.out.println("开始删除帖子: " + postId + ", 用户: " + user.getUsername() + " (ID: " + user.getId() + ")");
            boolean deleted = postService.deletePost(postId, user.getId());
            if (!deleted) {
                System.out.println("删除帖子失败：权限不足或帖子不存在，帖子ID: " + postId);
            } else {
                System.out.println("成功删除帖子: " + postId);
            }
        } catch (Exception e) {
            System.err.println("删除帖子时发生异常: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/forum";
    }

    @GetMapping("/delete-comment/{commentId}")
    public String deleteComment(@PathVariable Integer commentId, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            System.out.println("删除评论失败：用户未登录");
            return "redirect:/forum";
        }

        try {
            Optional<Comment> commentOpt = commentRepository.findById(commentId);
            Integer postId = null;
            if (commentOpt.isPresent()) {
                postId = commentOpt.get().getPostId();
            }
            
            boolean deleted = postService.deleteComment(commentId, user.getId());
            if (!deleted) {
                System.out.println("删除评论失败：权限不足或评论不存在，评论ID: " + commentId + ", 用户ID: " + user.getId());
            } else {
                System.out.println("成功删除评论: " + commentId + ", 操作用户: " + user.getUsername());
            }
            
            if (postId != null) {
                return "redirect:/forum#post-" + postId;
            }
        } catch (Exception e) {
            System.err.println("删除评论时发生异常: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/forum";
    }

    // 置顶/取消置顶留言（仅管理员可用）
    @GetMapping("/pin/{postId}")
    public String pinPost(@PathVariable Integer postId, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user != null && "admin".equals(user.getRole())) {
            postService.togglePinPost(postId, user.getId());
        }
        return "redirect:/forum";
    }
}