package com.yuwen.magiccube.controller;

import com.yuwen.magiccube.entity.TeachingResource;
import com.yuwen.magiccube.entity.TeachingResourceFile;
import com.yuwen.magiccube.entity.User;
import com.yuwen.magiccube.service.TeachingResourceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/teaching-resources")
public class TeachingResourceController {

    @Autowired
    private TeachingResourceService teachingResourceService;

    // 教师端：教学资源库首页
    @GetMapping
    public String teachingResources(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !"admin".equals(user.getRole())) {
            return "redirect:/login";
        }

        List<TeachingResource> resources = teachingResourceService.getAllResources();
        model.addAttribute("resources", resources);
        model.addAttribute("user", user);

        return "teaching-resources/index";
    }

    // 🌟 修改：支持查看具体的子文件夹层级，并找回了自动扫描功能！
    @GetMapping({ "/{id}", "/{id}/folder/{parentId}" })
    public String resourceDetail(@PathVariable Integer id,
                                 @PathVariable(required = false) Integer parentId,
                                 HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !"admin".equals(user.getRole())) {
            return "redirect:/login";
        }

        TeachingResource resource = teachingResourceService.getResourceById(id);
        if (resource == null) return "redirect:/teaching-resources";

        // 如果没传 parentId，说明是在课文根目录（0）
        int currentParentId = (parentId == null) ? 0 : parentId;

        // 获取【当前层级】的所有文件和文件夹
        List<TeachingResourceFile> files = teachingResourceService.getFilesByParentId(id, currentParentId);

        // 🌟🌟🌟 核心修复：把丢失的自动扫描逻辑补回来！(仅在根目录为空时触发)
        if (currentParentId == 0 && (files == null || files.isEmpty())) {
            System.out.println("⚠️ 课文 ID=" + id + " 根目录为空，开始扫描硬盘...");
            teachingResourceService.scanSingleResource(id);
            // 扫描完之后，重新去数据库获取一下最新的文件列表
            files = teachingResourceService.getFilesByParentId(id, currentParentId);
        }

        // 获取当前所在文件夹的详细信息（为了前端做“返回上一级”的面包屑导航）
        TeachingResourceFile currentFolder = null;
        if (currentParentId > 0) {
            currentFolder = teachingResourceService.getFileById(currentParentId);
        }

        boolean hasVideo = files != null && files.stream().anyMatch(file -> "video".equals(file.getFileType()));

        model.addAttribute("resource", resource);
        model.addAttribute("files", files);
        model.addAttribute("currentParentId", currentParentId);
        model.addAttribute("currentFolder", currentFolder);
        model.addAttribute("hasVideo", hasVideo);
        model.addAttribute("user", user);

        return "teaching-resources/detail";
    }

    // 🌟 新增：新建文件夹
    @PostMapping("/{id}/create-folder")
    public String createFolder(@PathVariable Integer id, @RequestParam Integer parentId, @RequestParam String folderName) {
        teachingResourceService.createSubFolder(id, parentId, folderName);
        return parentId == 0 ? "redirect:/teaching-resources/" + id : "redirect:/teaching-resources/" + id + "/folder/" + parentId;
    }

    // 🌟 修复：下载文件接口，添加课文 ID 参数
    @GetMapping("/{id}/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Integer id, @PathVariable Integer fileId, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !"admin".equals(user.getRole())) {
            return ResponseEntity.status(401).build();
        }
        
        TeachingResourceFile file = teachingResourceService.getFileById(fileId);
        
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = Paths.get("magic-cube/src/main/resources/static/" + file.getFilePath()).toAbsolutePath().normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists()) {
                System.err.println("文件不存在：" + filePath);
                System.err.println("尝试的路径：" + filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            String encodedFileName = java.net.URLEncoder.encode(file.getFileName(), "UTF-8").replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=utf-8''" + encodedFileName)
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // 🌟 新增：上传文件（注意参数类型是 MultipartFile）
    @PostMapping("/{id}/upload-file")
    public String uploadFile(@PathVariable Integer id, @RequestParam Integer parentId, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            teachingResourceService.uploadFile(id, parentId, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return parentId == 0 ? "redirect:/teaching-resources/" + id : "redirect:/teaching-resources/" + id + "/folder/" + parentId;
    }

    // 🌟 新增：删除单个文件或子文件夹
    @PostMapping("/{id}/delete-item")
    public String deleteItem(@PathVariable Integer id, @RequestParam Integer itemId, @RequestParam Integer parentId) {
        teachingResourceService.deleteFileItem(itemId);
        return parentId == 0 ? "redirect:/teaching-resources/" + id : "redirect:/teaching-resources/" + id + "/folder/" + parentId;
    }
}
