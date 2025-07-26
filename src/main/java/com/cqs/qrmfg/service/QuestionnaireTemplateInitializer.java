package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.QuestionTemplate;
import com.cqs.qrmfg.repository.QuestionTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class QuestionnaireTemplateInitializer implements CommandLineRunner {

    @Autowired
    private QuestionTemplateRepository questionTemplateRepository;

    @Override
    public void run(String... args) throws Exception {
        // Only initialize if no templates exist
        if (questionTemplateRepository.countByIsActiveTrue() == 0) {
            initializeDefaultTemplate();
        }
    }

    private void initializeDefaultTemplate() {
        List<QuestionTemplate> templates = Arrays.asList(
            // Step 1: Basic Information
            createTemplate(1, 1, "Basic Information", "Material Name", "Material Name", "CQS", "input", "materialName", true, null),
            createTemplate(2, 1, "Basic Information", "Material Type", "Material Type", "CQS", "select", "materialType", true, 
                "[{\"value\":\"chemical\",\"label\":\"Chemical\"},{\"value\":\"mixture\",\"label\":\"Mixture\"},{\"value\":\"substance\",\"label\":\"Substance\"},{\"value\":\"preparation\",\"label\":\"Preparation\"}]"),
            createTemplate(3, 1, "Basic Information", "CAS Number", "CAS Number", "CQS", "input", "casNumber", false, null),
            createTemplate(4, 1, "Basic Information", "Supplier Name", "Supplier Name", "Plant", "input", "supplierName", true, null),

            // Step 2: Physical Properties
            createTemplate(5, 2, "Physical Properties", "Physical State", "Physical State", "CQS", "radio", "physicalState", true,
                "[{\"value\":\"solid\",\"label\":\"Solid\"},{\"value\":\"liquid\",\"label\":\"Liquid\"},{\"value\":\"gas\",\"label\":\"Gas\"},{\"value\":\"vapor\",\"label\":\"Vapor\"}]"),
            createTemplate(6, 2, "Physical Properties", "Color", "Color", "Plant", "input", "color", false, null),
            createTemplate(7, 2, "Physical Properties", "Odor", "Odor", "Plant", "input", "odor", false, null),
            createTemplate(8, 2, "Physical Properties", "Boiling Point (°C)", "Boiling Point (°C)", "CQS", "input", "boilingPoint", false, null),
            createTemplate(9, 2, "Physical Properties", "Melting Point (°C)", "Melting Point (°C)", "CQS", "input", "meltingPoint", false, null),

            // Step 3: Hazard Classification
            createTemplate(10, 3, "Hazard Classification", "Hazard Categories", "Hazard Categories", "CQS", "checkbox", "hazardCategories", true,
                "[{\"value\":\"flammable\",\"label\":\"Flammable\"},{\"value\":\"toxic\",\"label\":\"Toxic\"},{\"value\":\"corrosive\",\"label\":\"Corrosive\"},{\"value\":\"irritant\",\"label\":\"Irritant\"},{\"value\":\"oxidizing\",\"label\":\"Oxidizing\"},{\"value\":\"explosive\",\"label\":\"Explosive\"}]"),
            createTemplate(11, 3, "Hazard Classification", "Signal Word", "Signal Word", "CQS", "radio", "signalWord", true,
                "[{\"value\":\"danger\",\"label\":\"DANGER\"},{\"value\":\"warning\",\"label\":\"WARNING\"},{\"value\":\"none\",\"label\":\"None\"}]"),
            createTemplate(12, 3, "Hazard Classification", "Hazard Statements", "Hazard Statements", "CQS", "textarea", "hazardStatements", false, null),

            // Step 4: Safety Measures
            createTemplate(13, 4, "Safety Measures", "Personal Protection Equipment", "Personal Protection Equipment", "Plant", "checkbox", "personalProtection", true,
                "[{\"value\":\"gloves\",\"label\":\"Gloves\"},{\"value\":\"goggles\",\"label\":\"Safety Goggles\"},{\"value\":\"respirator\",\"label\":\"Respirator\"},{\"value\":\"apron\",\"label\":\"Chemical Apron\"},{\"value\":\"boots\",\"label\":\"Safety Boots\"}]"),
            createTemplate(14, 4, "Safety Measures", "First Aid Measures", "First Aid Measures", "Plant", "textarea", "firstAidMeasures", true, null),
            createTemplate(15, 4, "Safety Measures", "Storage Conditions", "Storage Conditions", "Plant", "textarea", "storageConditions", true, null),
            createTemplate(16, 4, "Safety Measures", "Handling Precautions", "Handling Precautions", "Plant", "textarea", "handlingPrecautions", true, null),
            createTemplate(17, 4, "Safety Measures", "Disposal Methods", "Disposal Methods", "Plant", "textarea", "disposalMethods", true, null),
            createTemplate(18, 4, "Safety Measures", "Spill Cleanup Procedures", "Spill Cleanup Procedures", "Plant", "textarea", "spillCleanup", true, null),

            // Step 5: Environmental Information
            createTemplate(19, 5, "Environmental Information", "Environmental Hazards", "Environmental Hazards", "CQS", "checkbox", "environmentalHazards", false,
                "[{\"value\":\"aquatic_acute\",\"label\":\"Acute Aquatic Toxicity\"},{\"value\":\"aquatic_chronic\",\"label\":\"Chronic Aquatic Toxicity\"},{\"value\":\"ozone_depletion\",\"label\":\"Ozone Depletion\"},{\"value\":\"bioaccumulation\",\"label\":\"Bioaccumulation\"}]"),
            createTemplate(20, 5, "Environmental Information", "Waste Treatment Methods", "Waste Treatment Methods", "Plant", "textarea", "wasteTreatment", false, null)
        );

        questionTemplateRepository.saveAll(templates);
        System.out.println("Initialized " + templates.size() + " questionnaire templates");
    }

    private QuestionTemplate createTemplate(int srNo, int stepNumber, String category, String questionText, 
                                          String comments, String responsible, String questionType, 
                                          String fieldName, boolean isRequired, String options) {
        QuestionTemplate template = new QuestionTemplate();
        template.setSrNo(srNo);
        template.setStepNumber(stepNumber);
        template.setCategory(category);
        template.setQuestionText(questionText);
        template.setComments(comments);
        template.setResponsible(responsible);
        template.setQuestionType(questionType.toUpperCase());
        template.setFieldName(fieldName);
        template.setIsRequired(isRequired);
        template.setOptions(options);
        template.setOrderIndex(srNo);
        template.setIsActive(true);
        template.setVersion(1);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        template.setCreatedBy("SYSTEM");
        template.setUpdatedBy("SYSTEM");

        // Set help text based on field
        switch (fieldName) {
            case "materialName":
                template.setHelpText("Material name from project item master");
                break;
            case "casNumber":
                template.setHelpText("Chemical Abstracts Service number - unique identifier for chemical substances");
                break;
            case "supplierName":
                template.setHelpText("Enter the name of the supplier company");
                break;
            case "physicalState":
                template.setHelpText("Physical state at room temperature (20°C)");
                break;
            case "color":
                template.setHelpText("Describe the color and appearance of the material");
                break;
            case "odor":
                template.setHelpText("Describe the odor characteristics");
                break;
            case "boilingPoint":
                template.setHelpText("Temperature at which the material changes from liquid to gas");
                break;
            case "meltingPoint":
                template.setHelpText("Temperature at which the material changes from solid to liquid");
                break;
            case "hazardCategories":
                template.setHelpText("Select all applicable hazard classifications according to GHS");
                break;
            case "signalWord":
                template.setHelpText("GHS signal word based on the most severe hazard category");
                break;
            case "personalProtection":
                template.setHelpText("Required PPE based on hazard assessment");
                break;
            case "firstAidMeasures":
                template.setHelpText("Describe first aid procedures for different exposure routes");
                break;
            case "storageConditions":
                template.setHelpText("Specify temperature, humidity, and other storage requirements");
                break;
            case "handlingPrecautions":
                template.setHelpText("Describe safe handling procedures and precautions");
                break;
            case "disposalMethods":
                template.setHelpText("Describe proper disposal methods and regulatory requirements");
                break;
            case "spillCleanup":
                template.setHelpText("Describe procedures for cleaning up spills and leaks");
                break;
            case "environmentalHazards":
                template.setHelpText("Environmental impact classifications according to GHS");
                break;
            case "wasteTreatment":
                template.setHelpText("Describe waste treatment and disposal methods");
                break;
            default:
                template.setHelpText("Please provide the required information");
        }

        return template;
    }
}