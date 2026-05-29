package com.yuwen.magiccube.repository;

import com.yuwen.magiccube.entity.TeachingResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TeachingResourceRepository extends JpaRepository<TeachingResource, Integer> {
    List<TeachingResource> findAllByOrderBySortOrderAsc();
}
