package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import java.util.List;
import java.util.Optional;

/** Data access operations for the resource inventory. */
public interface ResourceDao {

    /** @return the generated resource id */
    int insert(Resource resource);

    Optional<Resource> findById(int id);

    Optional<Resource> findByCode(String code);

    List<Resource> findAll();

    /** Finds resources whose code, name or location contains the text. */
    List<Resource> search(String text);

    List<ResourceCategory> findCategories();

    /** Updates name, category, description, acquisition date and location (the code never changes). */
    boolean update(Resource resource);

    boolean updateStatus(int resourceId, String status);

    boolean existsByCode(String code);
}
