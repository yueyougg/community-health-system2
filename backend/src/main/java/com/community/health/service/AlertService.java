package com.community.health.service;

import com.community.health.model.AlertRecord;
import com.community.health.repository.AlertRecordRepository;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    private final AlertRecordRepository alertRecordRepository;

    public AlertService(AlertRecordRepository alertRecordRepository) {
        this.alertRecordRepository = alertRecordRepository;
    }

    public AlertRecord createAlert(Long residentId,
                                   String alertType,
                                   String level,
                                   String message,
                                   String relatedType,
                                   Long relatedId) {
        AlertRecord alert = new AlertRecord();
        alert.setResidentId(residentId);
        alert.setAlertType(alertType);
        alert.setLevel(level);
        alert.setMessage(message);
        alert.setRelatedType(relatedType);
        alert.setRelatedId(relatedId);
        return alertRecordRepository.save(alert);
    }
}
