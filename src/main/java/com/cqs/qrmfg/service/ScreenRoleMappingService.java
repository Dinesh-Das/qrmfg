package com.cqs.qrmfg.service;

import java.util.List;
import java.util.Map;

public interface ScreenRoleMappingService {
    Map<String, List<Long>> getAllMappings();
    void updateMapping(String route, List<Long> roleIds);
    List<String> getAllowedRoutesForRoles(List<Long> roleIds);
} 