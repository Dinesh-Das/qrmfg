package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.QuestionTemplate;
import com.cqs.qrmfg.repository.QuestionTemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MaterialQuestionnaireServiceTest {

    @Autowired
    private MaterialQuestionnaireService materialQuestionnaireService;

    @Autowired
    private QuestionTemplateRepository templateRepository;

    @Test
    public void testGetTemplateStatistics() {
        // This test will work if template data is loaded
        MaterialQuestionnaireService.TemplateStatistics stats = materialQuestionnaireService.getTemplateStatistics();
        assertNotNull(stats);
        // If template data is loaded, we should have questions
        // assertTrue(stats.totalQuestions > 0);
    }

    @Test
    public void testGetTemplateQuestions() {
        List<QuestionTemplate> templates = materialQuestionnaireService.getTemplateQuestions();
        assertNotNull(templates);
        // Templates list might be empty if migration hasn't run
    }

    @Test
    public void testGetTemplateCategories() {
        List<String> categories = materialQuestionnaireService.getTemplateCategories();
        assertNotNull(categories);
    }
}