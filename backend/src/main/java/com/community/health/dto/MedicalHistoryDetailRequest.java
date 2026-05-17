package com.community.health.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class MedicalHistoryDetailRequest {
    private Long residentId;
    private String smokingHabit;
    private String drinkingHabit;
    private String dietHabit;
    private String exerciseHabit;
    private String sourceType;
    private List<DiseaseItem> diseases;
    private List<GeneticItem> geneticHistories;
    private List<AllergyItem> allergies;

    @Getter
    @Setter
    public static class DiseaseItem {
        private String diseaseName;
        private String treatmentStatus;
        private LocalDate checkedAt;
    }

    @Getter
    @Setter
    public static class GeneticItem {
        private String diseaseName;
        private String relationToResident;
    }

    @Getter
    @Setter
    public static class AllergyItem {
        private String allergen;
        private String allergicReaction;
    }
}
