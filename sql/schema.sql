CREATE DATABASE IF NOT EXISTS community_health DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE community_health;

CREATE TABLE IF NOT EXISTS sys_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(120) NOT NULL,
    role VARCHAR(50) NOT NULL,
    enabled BIT NOT NULL
);

CREATE TABLE IF NOT EXISTS resident_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    archive_no VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    gender VARCHAR(10),
    birth_date DATE,
    id_card VARCHAR(30) UNIQUE,
    phone VARCHAR(30),
    address VARCHAR(255),
    occupation VARCHAR(50),
    archive_date DATE,
    archive_org VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS medical_histories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    resident_id BIGINT NOT NULL,
    disease_name VARCHAR(80),
    diagnosed_date DATE,
    treatment_status VARCHAR(255),
    family_disease VARCHAR(120),
    family_relation VARCHAR(120),
    allergen VARCHAR(120),
    allergic_reaction VARCHAR(255),
    smoking_habit VARCHAR(120),
    drinking_habit VARCHAR(120),
    diet_habit VARCHAR(255),
    exercise_habit VARCHAR(255),
    source_type VARCHAR(30)
);

CREATE TABLE IF NOT EXISTS medical_history_diseases (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    medical_history_id BIGINT NOT NULL,
    disease_name VARCHAR(80) NOT NULL,
    treatment_status VARCHAR(255),
    checked_at DATE
);

CREATE TABLE IF NOT EXISTS medical_history_genetic_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    medical_history_id BIGINT NOT NULL,
    disease_name VARCHAR(80) NOT NULL,
    relation_to_resident VARCHAR(80) NOT NULL
);

CREATE TABLE IF NOT EXISTS medical_history_allergy_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    medical_history_id BIGINT NOT NULL,
    allergen VARCHAR(120) NOT NULL,
    allergic_reaction VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS health_measurements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    resident_id BIGINT NOT NULL,
    measured_at DATETIME NOT NULL,
    location VARCHAR(120),
    height_cm DECIMAL(6,2),
    weight_kg DECIMAL(6,2),
    systolic_bp INT,
    diastolic_bp INT,
    blood_sugar DECIMAL(6,2),
    blood_lipid DECIMAL(6,2),
    heart_rate INT,
    source_type VARCHAR(30),
    alert_flag BIT NOT NULL
);

CREATE TABLE IF NOT EXISTS visit_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    resident_id BIGINT NOT NULL,
    visit_time DATETIME NOT NULL,
    organization VARCHAR(120),
    diagnosis VARCHAR(255),
    prescription TEXT,
    exam_report TEXT
);

CREATE TABLE IF NOT EXISTS medication_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    resident_id BIGINT NOT NULL,
    drug_name VARCHAR(120) NOT NULL,
    start_date DATE,
    end_date DATE,
    dosage VARCHAR(120),
    usage_method VARCHAR(120),
    reason VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS vaccination_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    resident_id BIGINT NOT NULL,
    vaccine_name VARCHAR(120) NOT NULL,
    vaccinated_at DATE,
    institution VARCHAR(120),
    batch_no VARCHAR(80)
);

CREATE TABLE IF NOT EXISTS follow_up_plans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    resident_id BIGINT NOT NULL,
    disease_type VARCHAR(120) NOT NULL,
    period_days INT NOT NULL,
    next_follow_up_date DATE,
    content VARCHAR(255),
    active BIT NOT NULL
);

CREATE TABLE IF NOT EXISTS follow_up_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    plan_id BIGINT NOT NULL,
    resident_id BIGINT NOT NULL,
    follow_up_time DATETIME NOT NULL,
    doctor_name VARCHAR(50),
    symptoms VARCHAR(255),
    physical_signs VARCHAR(255),
    guidance VARCHAR(255),
    medication_adjustment VARCHAR(255),
    next_reminder_date DATE
);

CREATE TABLE IF NOT EXISTS alert_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    resident_id BIGINT NOT NULL,
    alert_type VARCHAR(80) NOT NULL,
    level VARCHAR(30) NOT NULL,
    message VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    related_type VARCHAR(80),
    related_id BIGINT,
    notified_resident BIT NOT NULL,
    notified_doctor BIT NOT NULL
);

CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    username VARCHAR(50),
    module_name VARCHAR(80),
    action_name VARCHAR(80),
    http_method VARCHAR(20),
    request_path VARCHAR(255),
    ip_address VARCHAR(80)
);

CREATE TABLE IF NOT EXISTS health_assessments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    resident_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    assessment_date DATETIME NOT NULL,
    health_score INT,
    health_level VARCHAR(20),
    disease_type VARCHAR(80),
    evaluation VARCHAR(1000),
    guidance VARCHAR(1000),
    doctor_name VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS intervention_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    resident_id BIGINT,
    doctor_id BIGINT,
    doctor_name VARCHAR(50),
    intervention_type VARCHAR(80) NOT NULL,
    target_metric VARCHAR(80) NOT NULL,
    before_value DECIMAL(10,2),
    after_value DECIMAL(10,2),
    intervention_date DATETIME NOT NULL,
    notes VARCHAR(1000),
    effect_level VARCHAR(30),
    auto_evaluation VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS backup_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    file_name VARCHAR(100) NOT NULL,
    file_path VARCHAR(255),
    file_size BIGINT NOT NULL,
    status VARCHAR(20),
    operator VARCHAR(50),
    remark VARCHAR(255)
);

ALTER TABLE medical_histories ADD INDEX idx_medical_resident (resident_id);
ALTER TABLE medical_histories ADD UNIQUE INDEX uk_medical_resident (resident_id);
ALTER TABLE medical_history_diseases ADD INDEX idx_history_disease_history (medical_history_id);
ALTER TABLE medical_history_genetic_records ADD INDEX idx_history_genetic_history (medical_history_id);
ALTER TABLE medical_history_allergy_records ADD INDEX idx_history_allergy_history (medical_history_id);
ALTER TABLE health_measurements ADD INDEX idx_measure_resident (resident_id);
ALTER TABLE visit_records ADD INDEX idx_visit_resident (resident_id);
ALTER TABLE medication_records ADD INDEX idx_medication_resident (resident_id);
ALTER TABLE vaccination_records ADD INDEX idx_vaccine_resident (resident_id);
ALTER TABLE follow_up_plans ADD INDEX idx_plan_resident (resident_id);
ALTER TABLE follow_up_records ADD INDEX idx_record_resident (resident_id);
ALTER TABLE alert_records ADD INDEX idx_alert_resident (resident_id);
ALTER TABLE health_assessments ADD INDEX idx_assessment_resident (resident_id);
ALTER TABLE intervention_records ADD INDEX idx_intervention_resident (resident_id);
ALTER TABLE backup_records ADD INDEX idx_backup_created (created_at);
