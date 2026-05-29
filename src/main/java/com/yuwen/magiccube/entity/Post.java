package com.yuwen.magiccube.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.Transient;
@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(nullable = false, length = 1000)
    private String content;

    private Integer likes = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Transient
    private List<Comment> comments;
    
    @Transient
    private String username;
    
    @Transient
    private String role;

    @Transient
    private boolean likedByCurrentUser;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }


    @Transient
    private String likedUsersStr;

    public String getLikedUsersStr() {
        return likedUsersStr;
    }

    public void setLikedUsersStr(String likedUsersStr) {
        this.likedUsersStr = likedUsersStr;
    }


    // 在 Post.java 中找个位置加上这个字段
    @Column(columnDefinition = "boolean default false")
    private Boolean isPinned = false;

    // 加上 Getter 和 Setter
    public Boolean getIsPinned() {
        return isPinned;
    }

    public void setIsPinned(Boolean isPinned) {
        this.isPinned = isPinned;
    }


    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getLikes() {
        return likes;
    }

    public void setLikes(Integer likes) {
        this.likes = likes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isLikedByCurrentUser() {
        return likedByCurrentUser;
    }

    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        this.likedByCurrentUser = likedByCurrentUser;
    }

}