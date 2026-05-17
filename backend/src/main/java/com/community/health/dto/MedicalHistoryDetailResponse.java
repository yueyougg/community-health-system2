package com.community.health.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MedicalHistoryDetailResponse {
    private Long id;
    private Long residentId;
    private String smokingHabit;
    private String drinkingHabit;
    private String dietHabit;
    private String exerciseHabit;
    private String sourceType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DiseaseItem> diseases = new ArrayList<>();
    private List<GeneticItem> geneticHistories = new ArrayList<>();
    private List<AllergyItem> allergies = new ArrayList<>();

    @Getter
    @Setter
    public static class DiseaseItem {
        private Long id;
        private String diseaseName;
        private String treatmentStatus;
        private LocalDate checkedAt;
    }

    @Getter
    @Setter
    public static class GeneticItem {
        private Long id;
        private String diseaseName;
        private String relationToResident;
    }

    @Getter
    @Setter
    public static class AllergyItem {
        private Long id;
        private String allergen;
        private String allergicReaction;
    }
}
