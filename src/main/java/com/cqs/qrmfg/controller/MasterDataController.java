package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.model.QrmfgBlockMaster;
import com.cqs.qrmfg.model.QrmfgLocationMaster;
import com.cqs.qrmfg.model.QrmfgProjectItemMaster;
import com.cqs.qrmfg.service.MasterDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/master-data")
@CrossOrigin(origins = "*")
public class MasterDataController {
    
    @Autowired
    private MasterDataService masterDataService;
    
    // Location Master endpoints
    @GetMapping("/locations")
    public ResponseEntity<List<QrmfgLocationMaster>> getAllLocations() {
        List<QrmfgLocationMaster> locations = masterDataService.getAllLocations();
        return ResponseEntity.ok(locations);
    }
    
    @GetMapping("/locations/{locationCode}")
    public ResponseEntity<QrmfgLocationMaster> getLocationByCode(@PathVariable String locationCode) {
        Optional<QrmfgLocationMaster> location = masterDataService.getLocationByCode(locationCode);
        return location.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/locations/search")
    public ResponseEntity<List<QrmfgLocationMaster>> searchLocations(@RequestParam String term) {
        List<QrmfgLocationMaster> locations = masterDataService.searchLocations(term);
        return ResponseEntity.ok(locations);
    }
    
    @PostMapping("/locations")
    public ResponseEntity<QrmfgLocationMaster> createLocation(@RequestBody QrmfgLocationMaster location) {
        QrmfgLocationMaster savedLocation = masterDataService.saveLocation(location);
        return ResponseEntity.ok(savedLocation);
    }
    
    @PutMapping("/locations/{locationCode}")
    public ResponseEntity<QrmfgLocationMaster> updateLocation(@PathVariable String locationCode, 
                                                             @RequestBody QrmfgLocationMaster location) {
        location.setLocationCode(locationCode);
        QrmfgLocationMaster updatedLocation = masterDataService.saveLocation(location);
        return ResponseEntity.ok(updatedLocation);
    }
    
    @DeleteMapping("/locations/{locationCode}")
    public ResponseEntity<Void> deleteLocation(@PathVariable String locationCode) {
        masterDataService.deleteLocation(locationCode);
        return ResponseEntity.noContent().build();
    }
    
    // Block Master endpoints
    @GetMapping("/blocks")
    public ResponseEntity<List<QrmfgBlockMaster>> getAllBlocks() {
        List<QrmfgBlockMaster> blocks = masterDataService.getAllBlocks();
        return ResponseEntity.ok(blocks);
    }
    
    @GetMapping("/blocks/{blockId}")
    public ResponseEntity<QrmfgBlockMaster> getBlockById(@PathVariable String blockId) {
        Optional<QrmfgBlockMaster> block = masterDataService.getBlockById(blockId);
        return block.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/blocks/search")
    public ResponseEntity<List<QrmfgBlockMaster>> searchBlocks(@RequestParam String term) {
        List<QrmfgBlockMaster> blocks = masterDataService.searchBlocks(term);
        return ResponseEntity.ok(blocks);
    }
    
    @PostMapping("/blocks")
    public ResponseEntity<QrmfgBlockMaster> createBlock(@RequestBody QrmfgBlockMaster block) {
        QrmfgBlockMaster savedBlock = masterDataService.saveBlock(block);
        return ResponseEntity.ok(savedBlock);
    }
    
    @PutMapping("/blocks/{blockId}")
    public ResponseEntity<QrmfgBlockMaster> updateBlock(@PathVariable String blockId, 
                                                       @RequestBody QrmfgBlockMaster block) {
        block.setBlockId(blockId);
        QrmfgBlockMaster updatedBlock = masterDataService.saveBlock(block);
        return ResponseEntity.ok(updatedBlock);
    }
    
    @DeleteMapping("/blocks/{blockId}")
    public ResponseEntity<Void> deleteBlock(@PathVariable String blockId) {
        masterDataService.deleteBlock(blockId);
        return ResponseEntity.noContent().build();
    }
    
    // Project Item Master endpoints
    @GetMapping("/project-items")
    public ResponseEntity<List<QrmfgProjectItemMaster>> getAllProjectItems() {
        List<QrmfgProjectItemMaster> projectItems = masterDataService.getAllProjectItems();
        return ResponseEntity.ok(projectItems);
    }
    
    @GetMapping("/project-items/projects/{projectCode}")
    public ResponseEntity<List<QrmfgProjectItemMaster>> getItemsByProject(@PathVariable String projectCode) {
        List<QrmfgProjectItemMaster> items = masterDataService.getItemsByProject(projectCode);
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/project-items/items/{itemCode}")
    public ResponseEntity<List<QrmfgProjectItemMaster>> getProjectsByItem(@PathVariable String itemCode) {
        List<QrmfgProjectItemMaster> projects = masterDataService.getProjectsByItem(itemCode);
        return ResponseEntity.ok(projects);
    }
    
    @GetMapping("/project-codes")
    public ResponseEntity<List<String>> getAllProjectCodes() {
        List<String> projectCodes = masterDataService.getAllProjectCodes();
        return ResponseEntity.ok(projectCodes);
    }
    
    @GetMapping("/item-codes")
    public ResponseEntity<List<String>> getAllItemCodes() {
        List<String> itemCodes = masterDataService.getAllItemCodes();
        return ResponseEntity.ok(itemCodes);
    }
    
    @GetMapping("/project-codes/{projectCode}/items")
    public ResponseEntity<List<String>> getItemCodesByProject(@PathVariable String projectCode) {
        List<String> itemCodes = masterDataService.getItemCodesByProject(projectCode);
        return ResponseEntity.ok(itemCodes);
    }
    
    @PostMapping("/project-items")
    public ResponseEntity<QrmfgProjectItemMaster> createProjectItem(@RequestBody QrmfgProjectItemMaster projectItem) {
        QrmfgProjectItemMaster savedProjectItem = masterDataService.saveProjectItem(projectItem);
        return ResponseEntity.ok(savedProjectItem);
    }
    
    @DeleteMapping("/project-items/{projectCode}/{itemCode}")
    public ResponseEntity<Void> deleteProjectItem(@PathVariable String projectCode, @PathVariable String itemCode) {
        masterDataService.deleteProjectItem(projectCode, itemCode);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/project-items/{projectCode}/{itemCode}/exists")
    public ResponseEntity<Boolean> existsProjectItem(@PathVariable String projectCode, @PathVariable String itemCode) {
        boolean exists = masterDataService.existsProjectItem(projectCode, itemCode);
        return ResponseEntity.ok(exists);
    }
}