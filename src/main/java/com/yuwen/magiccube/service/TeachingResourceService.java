package com.yuwen.magiccube.service;

import org.springframework.transaction.annotation.Transactional;
import com.yuwen.magiccube.entity.TeachingResource;
import com.yuwen.magiccube.entity.TeachingResourceFile;
import com.yuwen.magiccube.repository.TeachingResourceFileRepository;
import com.yuwen.magiccube.repository.TeachingResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class TeachingResourceService {

    @Autowired
    private TeachingResourceRepository teachingResourceRepository;

    @Autowired
    private TeachingResourceFileRepository teachingResourceFileRepository;

    private static final String RESOURCE_BASE_PATH = "teaching-resources";

    public List<TeachingResource> getAllResources() {
        return teachingResourceRepository.findAllByOrderBySortOrderAsc();
    }

    public TeachingResource getResourceById(Integer id) {
        return teachingResourceRepository.findById(id).orElse(null);
    }

    public List<TeachingResourceFile> getResourceFiles(Integer resourceId) {
        return teachingResourceFileRepository.findByResourceIdOrderBySortOrderAsc(resourceId);
    }

    public TeachingResourceFile getFileById(Integer fileId) {
        return teachingResourceFileRepository.findById(fileId).orElse(null);
    }

    // 🌟 新增：扫描单个资源的文件夹
    @Transactional // 🌟 新增事务注解
    public void scanSingleResource(Integer resourceId) {
        TeachingResource resource = teachingResourceRepository.findById(resourceId).orElse(null);
        if (resource == null) {
            System.err.println("资源不存在：" + resourceId);
            return;
        }

        String folderPath = "magic-cube/src/main/resources/static/" + RESOURCE_BASE_PATH + "/" + resource.getFolderName();
        File folder = new File(folderPath);
        System.out.println("Java 正在寻找的绝对路径是: " + folder.getAbsolutePath());
        System.out.println("扫描单个资源：" + resource.getTitle() + ", 路径：" + folderPath);

        if (folder.exists() && folder.isDirectory()) {
            int fileCount = scanFolderAndSaveFiles(resource.getId(), folder, resource.getFolderName());
            System.out.println("  ✓ 扫描到 " + fileCount + " 个文件/文件夹");
        } else {
            System.out.println("  ⚠️ 文件夹不存在或不是目录");
        }
    }

    // 🌟 修改：扫描文件夹并初始化资源文件信息（保留批量扫描功能）
    public void scanAndInitResources() {
        System.out.println("========== 开始扫描教学资源 ==========");

        List<TeachingResource> resources = teachingResourceRepository.findAll();
        System.out.println("找到 " + resources.size() + " 篇课文");

        for (TeachingResource resource : resources) {
            // 🌟 使用相对路径
            String folderPath = "magic-cube/src/main/resources/static/" + RESOURCE_BASE_PATH + "/" + resource.getFolderName();
            File folder = new File(folderPath);

            System.out.println("检查课文：" + resource.getTitle() + ", 路径：" + folderPath);
            System.out.println("  文件夹存在：" + folder.exists() + ", 是目录：" + folder.isDirectory());

            if (folder.exists() && folder.isDirectory()) {
                int fileCount = scanFolderAndSaveFiles(resource.getId(), folder, resource.getFolderName());
                System.out.println("  ✓ 扫描到 " + fileCount + " 个文件/文件夹");
            } else {
                System.out.println("  ⚠️ 文件夹不存在或不是目录");
            }
        }

        System.out.println("========== 扫描完成 ========== ");
    }

    // 🌟 核心修复：支持真正文件夹层级的扫描方法
    private int scanFolderAndSaveFiles(Integer resourceId, File folder, String folderName) {
        // 先删除旧数据，避免重复
        teachingResourceFileRepository.deleteByResourceId(resourceId);

        int count = 0;
        File[] fileList = folder.listFiles();

        if (fileList != null) {
            for (File file : fileList) {
                if (file.isFile()) {
                    // 1. 如果是普通文件，直接放在根目录 (parentId = 0)
                    System.out.println("    发现文件：" + file.getName());
                    TeachingResourceFile resourceFile = new TeachingResourceFile();
                    resourceFile.setResourceId(resourceId);
                    resourceFile.setParentId(0);
                    resourceFile.setFileName(file.getName());
                    resourceFile.setFilePath(RESOURCE_BASE_PATH + "/" + folderName + "/" + file.getName());
                    resourceFile.setFileSize(file.length());
                    resourceFile.setFileType(determineFileType(file.getName()));
                    resourceFile.setDescription(getFileDescription(file.getName()));
                    teachingResourceFileRepository.save(resourceFile);
                    count++;

                } else if (file.isDirectory()) {
                    // 2. 🌟 如果发现是文件夹！先在数据库建一个“文件夹”记录
                    System.out.println("    发现子文件夹：" + file.getName());
                    TeachingResourceFile folderRecord = new TeachingResourceFile();
                    folderRecord.setResourceId(resourceId);
                    folderRecord.setParentId(0); // 文件夹本身在根目录
                    folderRecord.setFileName(file.getName()); // 比如："配套教学视频"
                    folderRecord.setFilePath(RESOURCE_BASE_PATH + "/" + folderName + "/" + file.getName());
                    folderRecord.setFileType("folder"); // 关键：标记为文件夹
                    folderRecord.setDescription("文件夹");

                    // 先保存文件夹，才能拿到数据库自动生成的 ID
                    folderRecord = teachingResourceFileRepository.save(folderRecord);
                    count++;

                    // 3. 扫描这个文件夹里面的文件
                    File[] subFiles = file.listFiles();
                    if (subFiles != null) {
                        for (File subFile : subFiles) {
                            if (subFile.isFile()) {
                                System.out.println("      发现文件夹内文件：" + subFile.getName());
                                TeachingResourceFile subResourceFile = new TeachingResourceFile();
                                subResourceFile.setResourceId(resourceId);
                                // 🌟 关键：认贼作父！把 parentId 指向刚才建好的文件夹 ID
                                subResourceFile.setParentId(folderRecord.getId());
                                subResourceFile.setFileName(subFile.getName()); // 真正的文件名，不带斜杠前缀了
                                subResourceFile.setFilePath(RESOURCE_BASE_PATH + "/" + folderName + "/" + file.getName() + "/" + subFile.getName());
                                subResourceFile.setFileSize(subFile.length());
                                subResourceFile.setFileType(determineFileType(subFile.getName()));
                                subResourceFile.setDescription(getFileDescription(subFile.getName()));
                                teachingResourceFileRepository.save(subResourceFile);
                                count++;
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    private String determineFileType(String fileName) {
        if (fileName.contains("教案") || fileName.contains("教学方案")) {
            return "lesson_plan";
        } else if (fileName.contains("目标") || fileName.contains("建议")) {
            return "teaching_guide";
        } else if (fileName.contains("剧本")) {
            return "script";
        } else if (fileName.endsWith(".mp4") || fileName.endsWith(".avi")) {
            return "video";
        }
        return "document"; // 默认改成 document
    }

    private String getFileDescription(String fileName) {
        if (fileName.contains("教案") || fileName.contains("教学方案")) {
            return "教案";
        } else if (fileName.contains("思维能力")) {
            return "思维能力教学目标与引导建议";
        } else if (fileName.contains("心理健康")) {
            return "心理健康教学目标与引导建议";
        } else if (fileName.contains("剧本")) {
            return "教学剧本";
        } else if (fileName.endsWith(".mp4")) {
            return "教学视频";
        }
        return fileName;
    }


    // 🌟 相对路径常量
    private static final String DYNAMIC_BASE_PATH = "magic-cube/src/main/resources/static/teaching-resources/";

    // 添加新课文
    @Transactional
    public void addLesson(String title) {
        // 1. 保存到数据库
        TeachingResource resource = new TeachingResource();
        resource.setTitle(title);
        resource.setFolderName(title); // 文件夹名字和标题一致
        resource.setSortOrder(99);
        // 必须先保存 resource，这样才能生成 ID
        resource = teachingResourceRepository.save(resource);

        // 2. 在电脑硬盘上创建真实文件夹
        File folder = new File(DYNAMIC_BASE_PATH + title);
        if (!folder.exists()) {
            folder.mkdirs();
            System.out.println("✅ 成功创建物理文件夹：" + folder.getAbsolutePath());
        }
    }

    // 删除课文
    @Transactional
    public void deleteLesson(Integer id) {
        TeachingResource resource = teachingResourceRepository.findById(id).orElse(null);
        if (resource != null) {
            // 1. 删除硬盘上的真实文件夹及其内部所有文件
            File folder = new File(DYNAMIC_BASE_PATH + resource.getFolderName());
            deleteDirectoryRecursively(folder);
            System.out.println("🗑️ 成功删除物理文件夹：" + folder.getAbsolutePath());

            // 2. 删除数据库记录
            teachingResourceRepository.delete(resource);
        }
    }

    // 辅助方法：递归删除文件夹里的所有东西
    private void deleteDirectoryRecursively(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectoryRecursively(file);
            }
        }
        directoryToBeDeleted.delete();
    }

    // 🌟 1. 获取特定层级的文件和文件夹
    public List<TeachingResourceFile> getFilesByParentId(Integer resourceId, Integer parentId) {
        return teachingResourceFileRepository.findByResourceIdAndParentIdOrderBySortOrderAsc(resourceId, parentId);
    }

    // 🌟 2. 新建子文件夹（同步创建数据库和硬盘文件夹）
    @Transactional
    public void createSubFolder(Integer resourceId, Integer parentId, String folderName) {
        TeachingResource resource = getResourceById(resourceId);
        if(resource == null) return;

        String relativePath = "";
        if (parentId == 0) {
            relativePath = "teaching-resources/" + resource.getFolderName() + "/" + folderName;
        } else {
            TeachingResourceFile parentFolder = getFileById(parentId);
            relativePath = parentFolder.getFilePath() + "/" + folderName;
        }

        // 物理路径
        File physicalFolder = new File("magic-cube/src/main/resources/static/" + relativePath);
        if (!physicalFolder.exists()) {
            physicalFolder.mkdirs();
        }

        // 存入数据库
        TeachingResourceFile newFolder = new TeachingResourceFile();
        newFolder.setResourceId(resourceId);
        newFolder.setParentId(parentId);
        newFolder.setFileName(folderName);
        newFolder.setFileType("folder"); // 关键：标记这是一个文件夹
        newFolder.setFilePath(relativePath);
        newFolder.setDescription("文件夹");
        teachingResourceFileRepository.save(newFolder);
    }

    // 🌟 3. 上传真实文件（保存到硬盘并记录到数据库）
    @Transactional
    public void uploadFile(Integer resourceId, Integer parentId, org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        TeachingResource resource = getResourceById(resourceId);
        if(resource == null || file.isEmpty()) return;

        String originalFilename = file.getOriginalFilename();
        String relativePath = "";

        if (parentId == 0) {
            relativePath = "teaching-resources/" + resource.getFolderName() + "/" + originalFilename;
        } else {
            TeachingResourceFile parentFolder = getFileById(parentId);
            relativePath = parentFolder.getFilePath() + "/" + originalFilename;
        }

        File dest = new File(new File("magic-cube/src/main/resources/static/" + relativePath).getAbsolutePath());
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs(); // 确保父目录存在
        }
        file.transferTo(dest); // 把网页传来的文件写进硬盘

        // 保存数据库
        TeachingResourceFile newFile = new TeachingResourceFile();
        newFile.setResourceId(resourceId);
        newFile.setParentId(parentId);
        newFile.setFileName(originalFilename);

        // 简单判断文件类型
        if(originalFilename.endsWith(".mp4") || originalFilename.endsWith(".avi")) {
            newFile.setFileType("video");
        } else {
            newFile.setFileType("document");
        }

        newFile.setFilePath(relativePath);
        newFile.setFileSize(file.getSize());
        newFile.setDescription(originalFilename);
        teachingResourceFileRepository.save(newFile);
    }

    // 🌟 4. 删除单个文件或文件夹（递归删除里面的所有内容）
    @Transactional
    public void deleteFileItem(Integer itemId) {
        TeachingResourceFile file = getFileById(itemId);
        if (file != null) {
            deleteDbAndPhysicalRecursively(file);
        }
    }

    // 辅助方法：连环套娃删除数据库记录和物理文件
    private void deleteDbAndPhysicalRecursively(TeachingResourceFile file) {
        // 先查出它里面包着的所有子文件
        List<TeachingResourceFile> children = getFilesByParentId(file.getResourceId(), file.getId());
        for (TeachingResourceFile child : children) {
            deleteDbAndPhysicalRecursively(child); // 递归删除子文件
        }

        // 删硬盘文件
        File physicalFile = new File("magic-cube/src/main/resources/static/" + file.getFilePath());
        if (physicalFile.exists()) {
            if(physicalFile.isDirectory()) {
                deleteDirectoryRecursively(physicalFile); // 这个方法你之前已经加过了
            } else {
                physicalFile.delete();
            }
        }
        // 最后删数据库记录
        teachingResourceFileRepository.delete(file);
    }
}