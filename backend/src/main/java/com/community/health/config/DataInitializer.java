package com.community.health.config;

import com.community.health.model.UserAccount;
import com.community.health.model.HealthKnowledge;
import com.community.health.model.enums.Role;
import com.community.health.repository.HealthKnowledgeRepository;
import com.community.health.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final HealthKnowledgeRepository healthKnowledgeRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserAccountRepository userAccountRepository,
                           HealthKnowledgeRepository healthKnowledgeRepository,
                           PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.healthKnowledgeRepository = healthKnowledgeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createIfMissing("admin", "admin123", Role.ADMIN);
        createIfMissing("doctor", "doctor123", Role.DOCTOR);
        createIfMissing("resident", "resident123", Role.RESIDENT);
        seedKnowledgeIfEmpty();
    }

    private void createIfMissing(String username, String password, Role role) {
        if (userAccountRepository.existsByUsername(username)) {
            return;
        }
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password));
        account.setRole(role);
        account.setEnabled(true);
        userAccountRepository.save(account);
    }

    private void seedKnowledgeIfEmpty() {
        if (healthKnowledgeRepository.count() > 0) {
            return;
        }

        List<HealthKnowledge> seeds = List.of(
                createKnowledge("高血压日常管理要点", "慢性病",
                        "高血压患者应坚持监测血压、按时服药，减少高盐饮食，每周至少进行中等强度运动150分钟。出现头晕、胸闷等不适需及时就医。",
                        "高血压,慢病管理,血压监测"),
                createKnowledge("糖尿病饮食与血糖控制", "慢性病",
                        "糖尿病患者建议定时定量进餐，减少精制糖和高升糖指数食物，增加蔬菜和优质蛋白摄入，配合规律运动和血糖监测。",
                        "糖尿病,血糖,饮食"),
                createKnowledge("老年人日常护理建议", "日常护理",
                        "老年居民需注意防跌倒、规律作息、充足饮水，定期监测血压和血糖。家属可协助建立就医与用药提醒。",
                        "老年护理,日常管理"),
                createKnowledge("常见慢病人群饮食清单", "饮食健康",
                        "高血压人群宜低盐低脂，糖尿病人群注意碳水总量控制。建议多选全谷物、豆类、深色蔬菜，减少油炸和含糖饮料。",
                        "饮食,低盐,低糖"),
                createKnowledge("社区居民每周运动建议", "运动建议",
                        "建议每周进行3-5次有氧运动（快走、骑行等），每次30分钟以上；并结合2次力量训练，循序渐进，注意心率变化。",
                        "运动,有氧,慢病康复")
        );
        healthKnowledgeRepository.saveAll(seeds);
    }

    private HealthKnowledge createKnowledge(String title, String category, String content, String tags) {
        HealthKnowledge knowledge = new HealthKnowledge();
        knowledge.setTitle(title);
        knowledge.setCategory(category);
        knowledge.setContent(content);
        knowledge.setTags(tags);
        return knowledge;
    }
}
