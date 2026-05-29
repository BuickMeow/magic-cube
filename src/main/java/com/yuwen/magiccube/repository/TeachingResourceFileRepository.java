package com.yuwen.magiccube.repository;

import com.yuwen.magiccube.entity.TeachingResourceFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TeachingResourceFileRepository extends JpaRepository<TeachingResourceFile, Integer> {
    List<TeachingResourceFile> findByResourceIdOrderBySortOrderAsc(Integer resourceId);

    // 🌟 必须加上这两个注解，否则执行删除时会报错
    @Modifying
    @Transactional
    void deleteByResourceId(Integer resourceId);
    // 🌟 新增：根据 课文ID 和 父文件夹ID 查询文件（用于分层显示）
    List<TeachingResourceFile> findByResourceIdAndParentIdOrderBySortOrderAsc(Integer resourceId, Integer parentId);
}
