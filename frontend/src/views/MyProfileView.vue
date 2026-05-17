<template>
  <div class="my-profile">
    <el-alert
      title="健康档案提示"
      type="info"
      description="在此您可以查阅您在社区医院登记的所有健康档案信息，包括历史病历、体征测量记录和疫苗接种情况。如需更新关键信息，请联系社区医生。"
      show-icon
      class="mb-20"
    />
    <el-card class="profile-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon"><UserFilled /></el-icon>
            <span class="title">个人健康档案</span>
            <el-tag v-if="profile.archiveNo" type="success" effect="plain" class="archive-no-tag">
              编号: {{ profile.archiveNo }}
            </el-tag>
            <el-tag v-else type="info" effect="plain" class="archive-no-tag">未建档</el-tag>
          </div>
          <div class="header-right">
            <el-button v-if="profile.id && activeTab === 'measurements'" type="primary" @click="showMeasurementDialog = true" class="edit-btn">
              <el-icon><Plus /></el-icon> 新增测量记录
            </el-button>
            <el-button v-else-if="profile.id && activeTab === 'history'" type="primary" @click="openHistoryDialog" class="edit-btn">
              <el-icon><Edit /></el-icon> {{ medicalHistories.length > 0 ? "修改健康史" : "新增健康史" }}
            </el-button>
            <el-button v-else-if="profile.id" type="primary" plain @click="handleEditClick" class="edit-btn">
              <el-icon><Edit /></el-icon> 修改个人信息
            </el-button>
            <el-button v-else type="primary" @click="showCreateDialog = true" class="create-btn">
              <el-icon><Plus /></el-icon> 建立我的档案
            </el-button>
          </div>
        </div>
      </template>

      <div class="profile-content" v-if="profile.id">
        <el-tabs v-model="activeTab" class="custom-tabs">
          <!-- 基本信息 -->
          <el-tab-pane name="basic">
            <div class="tab-content animate-fade-in">
              <el-descriptions :column="2" border class="custom-descriptions">
                <el-descriptions-item label="姓名">
                  <span class="desc-value">{{ profile.name }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="性别">
                  <el-tag :type="profile.gender === 'M' || profile.gender === '男' ? '' : 'danger'" size="small">
                    {{ (profile.gender === 'M' || profile.gender === '男') ? '男' : ((profile.gender === 'F' || profile.gender === '女') ? '女' : '未知') }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="身份证号">{{ profile.idCard }}</el-descriptions-item>
                <el-descriptions-item label="出生日期">{{ profile.birthDate }}</el-descriptions-item>
                <el-descriptions-item label="联系方式">{{ profile.phone || "暂无" }}</el-descriptions-item>
                <el-descriptions-item label="职业">{{ profile.occupation || '暂无' }}</el-descriptions-item>
                <el-descriptions-item label="建档日期">{{ formatFriendlyDate(profile.archiveDate) }}</el-descriptions-item>
                <el-descriptions-item label="家庭住址" :span="2">{{ profile.address }}</el-descriptions-item>
                <el-descriptions-item label="建档机构" :span="2">
                  <el-tag type="info" size="small">{{ profile.archiveOrg }}</el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </el-tab-pane>

          <!-- 健康史 (既往史等) -->
          <el-tab-pane name="history">
            <div class="tab-content animate-fade-in">
              <el-alert
                title="录入说明"
                type="info"
                :closable="false"
                description="健康史支持居民自行补充，也支持社区医生代为录入。系统会记录录入来源。"
                show-icon
                style="margin-bottom: 12px"
              />
              <el-empty v-if="medicalHistories.length === 0" description="暂无健康史记录" :image-size="100" />
              <el-card v-else class="history-card">
                <template #header>
                  <div class="history-card-header">
                    <span>健康史记录</span>
                    <el-tag size="small" type="info">{{ formatSourceType(medicalHistories[0].sourceType) }}</el-tag>
                  </div>
                </template>
                <el-descriptions :column="2" border class="custom-descriptions">
                  <el-descriptions-item label="过往疾病" :span="2">
                    <div v-if="(medicalHistories[0].diseases || []).length > 0" class="family-history-list">
                      <div v-for="(d, i) in medicalHistories[0].diseases" :key="`dis-${i}`" class="family-history-item">
                        <span class="family-relation">{{ d.diseaseName }}</span>
                        <span class="family-sep">|</span>
                        <span class="family-disease">{{ d.treatmentStatus || "治疗情况未填" }}</span>
                        <span class="family-sep">|</span>
                        <span class="family-disease">{{ d.checkedAt || "检查时间未填" }}</span>
                      </div>
                    </div>
                    <span v-else>无</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="遗传病史" :span="2">
                    <div v-if="(medicalHistories[0].geneticHistories || []).length > 0" class="family-history-list">
                      <div v-for="(pair, pairIdx) in medicalHistories[0].geneticHistories" :key="`pair-${pairIdx}`" class="family-history-item">
                        <span class="family-relation">{{ pair.relationToResident || "未知关系" }}</span>
                        <span class="family-sep">:</span>
                        <span class="family-disease">{{ pair.diseaseName || "未填写病症" }}</span>
                      </div>
                    </div>
                    <span v-else>无</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="过敏史" :span="2">
                    <div v-if="(medicalHistories[0].allergies || []).length > 0" class="family-history-list">
                      <div v-for="(a, idx) in medicalHistories[0].allergies" :key="`all-${idx}`" class="family-history-item">
                        <span class="family-relation">{{ a.allergen || "未填过敏物质" }}</span>
                        <span class="family-sep">:</span>
                        <span class="family-disease">{{ a.allergicReaction || "未填过敏反应" }}</span>
                      </div>
                    </div>
                    <span v-else>无</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="吸烟习惯">{{ medicalHistories[0].smokingHabit || "无" }}</el-descriptions-item>
                  <el-descriptions-item label="饮酒习惯">{{ medicalHistories[0].drinkingHabit || "无" }}</el-descriptions-item>
                  <el-descriptions-item label="饮食习惯">{{ medicalHistories[0].dietHabit || "无" }}</el-descriptions-item>
                  <el-descriptions-item label="运动习惯">{{ medicalHistories[0].exerciseHabit || "无" }}</el-descriptions-item>
                </el-descriptions>
              </el-card>
            </div>
          </el-tab-pane>

          <!-- 健康测量记录 -->
          <el-tab-pane name="measurements">
            <div class="tab-content animate-fade-in">
              <el-alert
                title="录入说明"
                type="info"
                :closable="false"
                description="测量记录支持居民自行录入，也支持社区医生代录。系统会自动区分录入来源。"
                show-icon
                style="margin-bottom: 12px"
              />
              <el-table :data="measurements" style="width: 100%" border stripe>
                <el-table-column label="测量时间" width="180">
                  <template #default="{ row }">
                    {{ formatFriendlyDateTime(row.measuredAt) }}
                  </template>
                </el-table-column>
                <el-table-column prop="location" label="测量地点" />
                <el-table-column label="血压 (mmHg)">
                  <template #default="{ row }">
                    <span class="bp-value">
                      <span :class="{ 'text-danger': isSystolicAbnormal(row) }">{{ row.systolicBp }}</span>
                      /
                      <span :class="{ 'text-danger': isDiastolicAbnormal(row) }">{{ row.diastolicBp }}</span>
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="bloodSugar" label="血糖 (mmol/L)">
                  <template #default="{ row }">
                    <span :class="{ 'text-danger': isBloodSugarAbnormal(row) }">{{ row.bloodSugar }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="bloodLipid" label="血脂 (mmol/L)">
                  <template #default="{ row }">
                    <span :class="{ 'text-danger': isBloodLipidAbnormal(row) }">{{ row.bloodLipid }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="heartRate" label="心率 (次/分)">
                  <template #default="{ row }">
                    <span :class="{ 'text-danger': isHeartRateAbnormal(row) }">{{ row.heartRate }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag :type="isMeasurementAbnormal(row) ? 'danger' : 'success'" effect="dark">
                      {{ isMeasurementAbnormal(row) ? "异常" : "正常" }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="sourceType" label="录入来源" width="120">
                  <template #default="{ row }">
                    <el-tag size="small" type="info">{{ formatSourceType(row.sourceType) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="体格">
                  <template #default="{ row }">
                    <el-tooltip :content="'BMI: ' + (row.weightKg / ((row.heightCm/100) * (row.heightCm/100))).toFixed(1)">
                      <span>{{ row.heightCm }}cm / {{ row.weightKg }}kg</span>
                    </el-tooltip>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

          <!-- 就诊记录 -->
          <el-tab-pane name="visits">
            <div class="tab-content animate-fade-in">
              <el-table :data="visits" style="width: 100%" border stripe>
                <el-table-column label="就诊时间" width="180">
                  <template #default="{ row }">
                    {{ formatFriendlyDateTime(row.visitTime) }}
                  </template>
                </el-table-column>
                <el-table-column prop="organization" label="就诊机构" />
                <el-table-column prop="diagnosis" label="诊断结果" />
                <el-table-column prop="prescription" label="处方" show-overflow-tooltip />
                <el-table-column prop="examReport" label="检查报告" show-overflow-tooltip />
              </el-table>
            </div>
          </el-tab-pane>

          <!-- 用药记录 -->
          <el-tab-pane name="medications">
            <div class="tab-content animate-fade-in">
              <el-table :data="medications" style="width: 100%" border stripe>
                <el-table-column prop="drugName" label="药品名称" width="150" />
                <el-table-column label="用药周期" width="220">
                  <template #default="{ row }">
                    <span class="date-range">{{ row.startDate }} <el-icon><Right /></el-icon> {{ row.endDate }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="dosage" label="剂量" />
                <el-table-column prop="usageMethod" label="用法" />
                <el-table-column prop="reason" label="用药原因" />
              </el-table>
            </div>
          </el-tab-pane>

          <!-- 疫苗接种 -->
          <el-tab-pane name="vaccinations">
            <div class="tab-content animate-fade-in">
              <el-table :data="vaccinations" style="width: 100%" border stripe>
                <el-table-column prop="vaccineName" label="疫苗名称" />
                <el-table-column label="接种时间" width="180">
                  <template #default="{ row }">
                    {{ formatFriendlyDate(row.vaccinatedAt) }}
                  </template>
                </el-table-column>
                <el-table-column prop="institution" label="接种机构" />
                <el-table-column prop="batchNo" label="批号" />
              </el-table>
            </div>
          </el-tab-pane>

          <!-- 健康评估 -->
          <el-tab-pane name="assessments">
            <div class="tab-content animate-fade-in">
              <el-table :data="assessments" style="width: 100%" border stripe>
                <el-table-column label="评估时间" width="180">
                  <template #default="{ row }">
                    {{ formatFriendlyDateTime(row.assessmentDate) }}
                  </template>
                </el-table-column>
                <el-table-column label="健康评分" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="getScoreTag(row.healthScore)">{{ row.healthScore }}分</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="健康等级" width="100" align="center">
                  <template #default="{ row }">
                    {{ formatLevel(row.healthLevel) }}
                  </template>
                </el-table-column>
                <el-table-column prop="evaluation" label="评估结论" show-overflow-tooltip />
                <el-table-column prop="guidance" label="指导建议" show-overflow-tooltip />
                <el-table-column prop="doctorName" label="评估人" width="120" />
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
      
      <div v-else class="empty-state animate-fade-in">
        <el-empty description="您尚未建立个人健康档案">
          <template #extra>
            <div class="empty-actions">
              <p class="empty-tip">建立档案后，您可以查看自己的健康数据、就诊记录及体检报告。</p>
              <el-button type="primary" size="large" @click="showCreateDialog = true" class="pulse-btn">
                立即建立健康档案
              </el-button>
            </div>
          </template>
        </el-empty>
      </div>
    </el-card>

    <!-- 建立档案对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="建立个人健康档案"
      width="600px"
      class="create-dialog"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form 
        ref="createFormRef"
        :model="createForm" 
        :rules="createRules" 
        label-width="100px"
        label-position="top"
        class="create-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="createForm.name" placeholder="请输入真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="createForm.idCard" placeholder="18位身份证号（选填，仅用于健康档案）" maxlength="18" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="createForm.gender">
                <el-radio-button label="男">男</el-radio-button>
                <el-radio-button label="女">女</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthDate">
              <el-date-picker
                v-model="createForm.birthDate"
                type="date"
                placeholder="选择日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="联系方式" prop="phone">
          <el-input v-model="createForm.phone" maxlength="30" placeholder="手机号或固定电话（选填）" />
        </el-form-item>

        <el-form-item label="现住址" prop="address">
          <el-input v-model="createForm.address" type="textarea" :rows="2" placeholder="详细居住地址" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="职业" prop="occupation">
              <el-input v-model="createForm.occupation" placeholder="如：工程师、退休等" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="建档机构" prop="archiveOrg">
              <el-input v-model="createForm.archiveOrg" placeholder="默认当前社区" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleCreate">
            确认建立档案
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 修改档案对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="修改个人基本信息"
      width="600px"
      class="create-dialog"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form 
        ref="editFormRef"
        :model="editForm" 
        :rules="createRules" 
        label-width="100px"
        label-position="top"
        class="create-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="editForm.name" placeholder="请输入真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="editForm.idCard" placeholder="18位身份证号（选填，仅用于健康档案）" maxlength="18" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="editForm.gender">
                <el-radio-button label="男">男</el-radio-button>
                <el-radio-button label="女">女</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthDate">
              <el-date-picker
                v-model="editForm.birthDate"
                type="date"
                placeholder="选择日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="联系方式" prop="phone">
          <el-input v-model="editForm.phone" maxlength="30" placeholder="手机号或固定电话（选填）" />
        </el-form-item>

        <el-form-item label="现住址" prop="address">
          <el-input v-model="editForm.address" type="textarea" :rows="2" placeholder="详细居住地址" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="职业" prop="occupation">
              <el-input v-model="editForm.occupation" placeholder="如：工程师、退休等" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="建档机构" prop="archiveOrg">
              <el-input v-model="editForm.archiveOrg" placeholder="所在社区卫生服务中心" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleUpdate">
            确认修改
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showMeasurementDialog"
      title="新增测量记录"
      width="560px"
      destroy-on-close
    >
      <el-form :model="measurementForm" label-width="110px">
        <el-form-item label="测量时间">
          <el-date-picker v-model="measurementForm.measuredAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="测量地点">
          <el-input v-model="measurementForm.location" />
        </el-form-item>
        <el-form-item label="收缩压">
          <el-input-number v-model="measurementForm.systolicBp" :min="40" :max="250" style="width: 100%" />
        </el-form-item>
        <el-form-item label="舒张压">
          <el-input-number v-model="measurementForm.diastolicBp" :min="30" :max="180" style="width: 100%" />
        </el-form-item>
        <el-form-item label="血糖">
          <el-input-number v-model="measurementForm.bloodSugar" :step="0.1" :precision="1" :min="1" :max="40" style="width: 100%" />
        </el-form-item>
        <el-form-item label="心率">
          <el-input-number v-model="measurementForm.heartRate" :min="30" :max="220" style="width: 100%" />
        </el-form-item>
        <el-form-item label="身高(cm)">
          <el-input-number v-model="measurementForm.heightCm" :step="0.1" :precision="1" :min="50" :max="250" style="width: 100%" />
        </el-form-item>
        <el-form-item label="体重(kg)">
          <el-input-number v-model="measurementForm.weightKg" :step="0.1" :precision="1" :min="10" :max="300" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showMeasurementDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreateMeasurement">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showHistoryDialog"
      :title="historyDialogMode === 'edit' ? '修改健康史' : '新增健康史'"
      width="560px"
      destroy-on-close
    >
      <el-form :model="historyForm" label-width="110px">
        <el-form-item label="过往疾病">
          <div class="family-history-editor">
            <div v-for="(item, idx) in diseaseItems" :key="`disease-${idx}`" class="family-history-row">
              <el-input
                v-model="item.diseaseName"
                placeholder="疾病名称（如：高血压）"
                style="width: 32%"
              />
              <el-input
                v-model="item.treatmentStatus"
                placeholder="治疗情况（可选）"
                style="width: 32%"
              />
              <el-date-picker
                v-model="item.checkedAt"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="检查时间"
                style="width: 28%"
              />
              <el-button
                v-if="diseaseItems.length > 1"
                type="danger"
                plain
                @click="removeDiseaseItem(idx)"
              >
                删除
              </el-button>
            </div>
            <el-button type="primary" link @click="addDiseaseItem">+ 新增一项疾病</el-button>
          </div>
        </el-form-item>
        <el-form-item label="遗传病史">
          <div class="family-history-editor">
            <div v-for="(item, idx) in geneticItems" :key="idx" class="family-history-row">
              <el-input
                v-model="item.relationToResident"
                placeholder="家族关系（如：父亲）"
                style="width: 42%"
              />
              <el-input
                v-model="item.diseaseName"
                placeholder="对应病症（如：高血压）"
                style="width: 46%"
              />
              <el-button
                v-if="geneticItems.length > 1"
                type="danger"
                plain
                @click="removeGeneticItem(idx)"
              >
                删除
              </el-button>
            </div>
            <el-button type="primary" link @click="addGeneticItem">+ 新增一条遗传病史</el-button>
          </div>
        </el-form-item>
        <el-form-item label="过敏史">
          <div class="family-history-editor">
            <div v-for="(item, idx) in allergyItems" :key="`allergy-${idx}`" class="family-history-row">
              <el-input
                v-model="item.allergen"
                placeholder="过敏物质（如：花粉）"
                style="width: 42%"
              />
              <el-input
                v-model="item.allergicReaction"
                placeholder="过敏反应（如：皮疹）"
                style="width: 46%"
              />
              <el-button
                v-if="allergyItems.length > 1"
                type="danger"
                plain
                @click="removeAllergyItem(idx)"
              >
                删除
              </el-button>
            </div>
            <el-button type="primary" link @click="addAllergyItem">+ 新增一条过敏史</el-button>
          </div>
        </el-form-item>
        <el-form-item label="吸烟习惯">
          <el-input v-model="historyForm.smokingHabit" placeholder="如：每日10支，已戒烟2年" />
        </el-form-item>
        <el-form-item label="饮酒习惯">
          <el-input v-model="historyForm.drinkingHabit" placeholder="如：每周1-2次少量饮酒" />
        </el-form-item>
        <el-form-item label="饮食习惯">
          <el-input v-model="historyForm.dietHabit" placeholder="如：低盐低脂、高糖饮食等" />
        </el-form-item>
        <el-form-item label="运动习惯">
          <el-input v-model="historyForm.exerciseHabit" placeholder="如：每周快走3次，每次30分钟" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showHistoryDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSaveHistory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from "vue";
import { useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { formatFriendlyDate, formatFriendlyDateTime } from "../utils/datetime";
import { 
  UserFilled, Plus, Right, Edit
} from "@element-plus/icons-vue";
import { 
  residentApi, medicalHistoryApi, measurementApi, 
  visitRecordApi, medicationRecordApi, vaccinationRecordApi, assessmentApi
} from "../api/modules";

const route = useRoute();
const loading = ref(false);
const submitting = ref(false);
const activeTab = ref("basic");
const profile = ref({});
const showCreateDialog = ref(false);
const showEditDialog = ref(false);
const showMeasurementDialog = ref(false);
const showHistoryDialog = ref(false);
const historyDialogMode = ref("create");
const currentHistoryId = ref(null);
const createFormRef = ref(null);
const editFormRef = ref(null);

// 子模块数据
const medicalHistories = ref([]);
const measurements = ref([]);
const visits = ref([]);
const medications = ref([]);
const vaccinations = ref([]);
const assessments = ref([]);

const measurementForm = reactive({
  measuredAt: new Date().toISOString().slice(0, 19),
  location: "家庭",
  systolicBp: 120,
  diastolicBp: 80,
  bloodSugar: 5.2,
  heartRate: 75,
  bloodLipid: 1.5,
  heightCm: 170,
  weightKg: 65
});

const historyForm = reactive({
  smokingHabit: "",
  drinkingHabit: "",
  dietHabit: "",
  exerciseHabit: ""
});
const diseaseItems = ref([{ diseaseName: "", treatmentStatus: "", checkedAt: "" }]);
const geneticItems = ref([{ diseaseName: "", relationToResident: "" }]);
const allergyItems = ref([{ allergen: "", allergicReaction: "" }]);

const resetHistoryForm = () => {
  Object.assign(historyForm, {
    smokingHabit: "",
    drinkingHabit: "",
    dietHabit: "",
    exerciseHabit: ""
  });
  diseaseItems.value = [{ diseaseName: "", treatmentStatus: "", checkedAt: "" }];
  geneticItems.value = [{ diseaseName: "", relationToResident: "" }];
  allergyItems.value = [{ allergen: "", allergicReaction: "" }];
};

const getFamilyHistoryPairs = (item) => item?.geneticHistories || [];
const addDiseaseItem = () => diseaseItems.value.push({ diseaseName: "", treatmentStatus: "", checkedAt: "" });
const removeDiseaseItem = (idx) => {
  diseaseItems.value.splice(idx, 1);
  if (diseaseItems.value.length === 0) {
    diseaseItems.value.push({ diseaseName: "", treatmentStatus: "", checkedAt: "" });
  }
};
const addGeneticItem = () => geneticItems.value.push({ diseaseName: "", relationToResident: "" });
const removeGeneticItem = (idx) => {
  geneticItems.value.splice(idx, 1);
  if (geneticItems.value.length === 0) {
    geneticItems.value.push({ diseaseName: "", relationToResident: "" });
  }
};
const addAllergyItem = () => allergyItems.value.push({ allergen: "", allergicReaction: "" });
const removeAllergyItem = (idx) => {
  allergyItems.value.splice(idx, 1);
  if (allergyItems.value.length === 0) {
    allergyItems.value.push({ allergen: "", allergicReaction: "" });
  }
};

const getScoreTag = (score) => {
  if (score >= 90) return "success";
  if (score >= 70) return "";
  if (score >= 60) return "warning";
  return "danger";
};

const formatLevel = (level) => {
  const map = { EXCELLENT: "优", GOOD: "良", FAIR: "一般", POOR: "较差" };
  return map[level] || level;
};

const formatSourceType = (sourceType) => {
  const map = {
    RESIDENT_SELF: "居民自录",
    DOCTOR_ENTRY: "医生录入",
    MANUAL: "手动录入",
    SYNC: "设备同步"
  };
  return map[sourceType] || "未知来源";
};

const isSystolicAbnormal = (row) => row?.systolicBp != null && (row.systolicBp > 140 || row.systolicBp < 90);
const isDiastolicAbnormal = (row) => row?.diastolicBp != null && (row.diastolicBp > 90 || row.diastolicBp < 60);
const isBloodSugarAbnormal = (row) => row?.bloodSugar != null && (Number(row.bloodSugar) > 6.1 || Number(row.bloodSugar) < 3.9);
const isBloodLipidAbnormal = (row) => row?.bloodLipid != null && Number(row.bloodLipid) > 5.2;
const isHeartRateAbnormal = (row) => row?.heartRate != null && (row.heartRate > 100 || row.heartRate < 60);
const isMeasurementAbnormal = (row) => (
  isSystolicAbnormal(row)
  || isDiastolicAbnormal(row)
  || isBloodSugarAbnormal(row)
  || isBloodLipidAbnormal(row)
  || isHeartRateAbnormal(row)
  || row?.alertFlag === true
);

// 建立档案表单
const createForm = reactive({
  idCard: "",
  phone: "",
  name: "",
  gender: "男",
  birthDate: "",
  address: "",
  occupation: "",
  archiveOrg: "阳光社区卫生服务站",
  archiveDate: new Date().toISOString().split('T')[0]
});

// 修改档案表单
const editForm = reactive({
  name: "",
  idCard: "",
  phone: "",
  gender: "",
  birthDate: "",
  address: "",
  occupation: "",
  archiveOrg: ""
});

const validateIdCardLength = (_rule, value, callback) => {
  if (!value || !String(value).trim()) return callback();
  const idCard = String(value).trim();
  if (idCard.length !== 15 && idCard.length !== 18) {
    return callback(new Error("身份证号长度需为15位或18位"));
  }
  if (!/(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/.test(idCard)) {
    return callback(new Error("身份证号格式不正确"));
  }
  callback();
};

const createRules = {
  idCard: [{ validator: validateIdCardLength, trigger: "blur" }],
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  gender: [{ required: true, message: "请选择性别", trigger: "change" }],
  birthDate: [{ required: true, message: "请选择出生日期", trigger: "change" }],
  address: [{ required: true, message: "请输入现住址", trigger: "blur" }]
};

const fetchProfile = async () => {
  loading.value = true;
  try {
    const res = await residentApi.me();
    profile.value = res.data;
    if (profile.value.id) {
      fetchSubData(activeTab.value);
    }
  } catch (error) {
    // 404 是正常现象，表示未建档
    if (error.response?.status !== 404) {
      console.error("获取档案失败", error);
    }
  } finally {
    loading.value = false;
  }
};

const handleCreate = async () => {
  if (!createFormRef.value) return;
  
  await createFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true;
      try {
        await residentApi.createMe(createForm);
        ElMessage.success("档案建立成功！");
        showCreateDialog.value = false;
        await fetchProfile();
      } catch (error) {
        ElMessage.error(error.message || "建立档案失败");
      } finally {
        submitting.value = false;
      }
    }
  });
};

const handleUpdate = async () => {
  if (!editFormRef.value) return;
  
  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      // 检查是否有空值字段
      const emptyFields = [];
      if (!editForm.name) emptyFields.push('姓名');
      if (!editForm.idCard) emptyFields.push('身份证号');
      if (!editForm.phone) emptyFields.push('联系方式');
      if (!editForm.gender) emptyFields.push('性别');
      if (!editForm.birthDate) emptyFields.push('出生日期');
      if (!editForm.address) emptyFields.push('家庭住址');
      if (!editForm.occupation) emptyFields.push('职业');
      if (!editForm.archiveOrg) emptyFields.push('建档机构');
      
      // 如果有空值字段，向用户确认
      if (emptyFields.length > 0) {
        try {
          await ElMessageBox.confirm(
            `以下字段为空：${emptyFields.join('、')}，是否确认保存？`,
            '确认保存',
            {
              confirmButtonText: '确认',
              cancelButtonText: '取消',
              type: 'warning'
            }
          );
        } catch {
          return; // 用户取消保存
        }
      }
      
      submitting.value = true;
      try {
        const res = await residentApi.updateMe(editForm);
        ElMessage.success("基本信息修改成功！");
        showEditDialog.value = false;
        profile.value = res.data;
      } catch (error) {
        ElMessage.error(error.message || "修改档案失败");
      } finally {
        submitting.value = false;
      }
    }
  });
};

const handleEditClick = () => {
  // 初始化编辑表单数据
  Object.assign(editForm, {
    name: profile.value.name,
    idCard: profile.value.idCard,
    phone: profile.value.phone,
    gender: (profile.value.gender === 'M' || profile.value.gender === '男') ? '男' : (profile.value.gender === 'F' || profile.value.gender === '女' ? '女' : profile.value.gender),
    birthDate: profile.value.birthDate,
    address: profile.value.address,
    occupation: profile.value.occupation,
    archiveOrg: profile.value.archiveOrg
  });
  showEditDialog.value = true;
};

const openHistoryDialog = () => {
  const hasHistory = medicalHistories.value.length > 0;
  historyDialogMode.value = hasHistory ? "edit" : "create";
  if (hasHistory) {
    const current = medicalHistories.value[0];
    currentHistoryId.value = current.id;
    Object.assign(historyForm, {
      smokingHabit: current.smokingHabit || "",
      drinkingHabit: current.drinkingHabit || "",
      dietHabit: current.dietHabit || "",
      exerciseHabit: current.exerciseHabit || ""
    });
    diseaseItems.value = (current.diseases || []).map((d) => ({
      diseaseName: d.diseaseName || "",
      treatmentStatus: d.treatmentStatus || "",
      checkedAt: d.checkedAt || ""
    }));
    geneticItems.value = (current.geneticHistories || []).map((g) => ({
      diseaseName: g.diseaseName || "",
      relationToResident: g.relationToResident || ""
    }));
    allergyItems.value = (current.allergies || []).map((a) => ({
      allergen: a.allergen || "",
      allergicReaction: a.allergicReaction || ""
    }));
  } else {
    currentHistoryId.value = null;
    resetHistoryForm();
  }
  showHistoryDialog.value = true;
};

const handleCreateMeasurement = async () => {
  submitting.value = true;
  try {
    await measurementApi.create({
      ...measurementForm,
      sourceType: "RESIDENT_SELF"
    });
    ElMessage.success("测量记录保存成功");
    showMeasurementDialog.value = false;
    measurements.value = [];
    await fetchSubData("measurements");
  } catch (error) {
    ElMessage.error(error.message || "保存测量记录失败");
  } finally {
    submitting.value = false;
  }
};

const handleSaveHistory = async () => {
  const diseases = diseaseItems.value
    .map((i) => ({
      diseaseName: String(i.diseaseName || "").trim(),
      treatmentStatus: String(i.treatmentStatus || "").trim(),
      checkedAt: i.checkedAt ? i.checkedAt.split('T')[0] : null
    }))
    .filter((i) => i.diseaseName);
  
  // 检查是否所有字段都为空
  const hasNoDiseases = diseases.length === 0;
  const hasNoGeneticHistories = geneticItems.value.every(item => !item.diseaseName || !item.relationToResident);
  const hasNoAllergies = allergyItems.value.every(item => !item.allergen);
  const hasNoHabits = !historyForm.smokingHabit && !historyForm.drinkingHabit && !historyForm.dietHabit && !historyForm.exerciseHabit;
  
  if (hasNoDiseases && hasNoGeneticHistories && hasNoAllergies && hasNoHabits) {
    try {
      await ElMessageBox.confirm(
        '您未填写任何健康史信息，是否确认保存空记录？',
        '确认保存',
        {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
    } catch {
      return; // 用户取消保存
    }
  }
  
  submitting.value = true;
  try {
    const geneticHistories = geneticItems.value
      .map((i) => ({
        diseaseName: String(i.diseaseName || "").trim(),
        relationToResident: String(i.relationToResident || "").trim()
      }))
      .filter((i) => i.diseaseName && i.relationToResident);
    const allergies = allergyItems.value
      .map((i) => ({
        allergen: String(i.allergen || "").trim(),
        allergicReaction: String(i.allergicReaction || "").trim()
      }))
      .filter((i) => i.allergen);
    const payload = {
      ...historyForm,
      diseases,
      geneticHistories,
      allergies,
      sourceType: "RESIDENT_SELF"
    };
    if (historyDialogMode.value === "edit" && currentHistoryId.value) {
      await medicalHistoryApi.updateMe(currentHistoryId.value, payload);
      ElMessage.success("健康史修改成功");
    } else {
      await medicalHistoryApi.create(payload);
      ElMessage.success("健康史保存成功");
    }
    showHistoryDialog.value = false;
    resetHistoryForm();
    medicalHistories.value = [];
    await fetchSubData("history");
  } catch (error) {
    ElMessage.error(error.message || "保存健康史失败");
  } finally {
    submitting.value = false;
  }
};

const fetchSubData = async (tabName) => {
  const residentId = profile.value.id;
  if (!residentId) return;

  try {
    if (tabName === 'history' && medicalHistories.value.length === 0) {
      const res = await medicalHistoryApi.me();
      medicalHistories.value = (res.data || []).slice(0, 1);
    } else if (tabName === 'measurements' && measurements.value.length === 0) {
      const res = await measurementApi.me();
      measurements.value = res.data;
    } else if (tabName === 'visits' && visits.value.length === 0) {
      const res = await visitRecordApi.list(residentId);
      visits.value = res.data;
    } else if (tabName === 'medications' && medications.value.length === 0) {
      const res = await medicationRecordApi.list(residentId);
      medications.value = res.data;
    } else if (tabName === 'vaccinations' && vaccinations.value.length === 0) {
      const res = await vaccinationRecordApi.list(residentId);
      vaccinations.value = res.data;
    } else if (tabName === 'assessments' && assessments.value.length === 0) {
      const res = await assessmentApi.list(residentId);
      assessments.value = res.data;
    }
  } catch (e) {
    console.error(`加载${tabName}数据失败`, e);
  }
};

watch(activeTab, (newVal) => {
  fetchSubData(newVal);
});

// 初始化时处理 URL 参数
onMounted(() => {
  if (route.query.tab) {
    activeTab.value = route.query.tab;
  }
  fetchProfile();
});

// 监听路由参数变化，实现点击侧边栏子项时切换选项卡
watch(() => route.query.tab, (newTab) => {
  activeTab.value = newTab || "basic";
});
</script>

<style scoped>
.my-profile {
  padding: 24px;
  max-width: 1240px;
  margin: 0 auto;
}

.profile-card {
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(47, 64, 86, 0.08); /* 使用主题色阴影 */
  border: 1px solid #edf2f7;
  overflow: hidden;
  background: #ffffff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  font-size: 24px;
  color: #ffffff;
  background: linear-gradient(135deg, #1aa094, #2f4056); /* 主题色渐变 */
  padding: 10px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(26, 160, 148, 0.2);
}

.title {
  font-size: 20px;
  font-weight: 700;
  color: #2f4056;
}

.archive-no-tag {
  font-family: 'JetBrains Mono', monospace;
  font-weight: 600;
  background-color: #f0fdfa;
  border-color: #1aa094;
  color: #1aa094;
}

.custom-tabs :deep(.el-tabs__header) {
  display: none; /* 隐藏标签页头部，由侧边栏控制 */
}

.tab-content {
  padding: 12px 0;
}

.custom-descriptions {
  padding: 12px;
  background: #f8fafc;
  border-radius: 16px;
}

.custom-descriptions :deep(.el-descriptions__label) {
  width: 140px;
  background-color: #f1f5f9;
  color: #475569;
  font-weight: 600;
  padding: 16px 20px;
}

.custom-descriptions :deep(.el-descriptions__content) {
  padding: 16px 20px;
  background-color: #ffffff;
}

.desc-value {
  font-weight: 600;
  color: #1e293b;
}

:deep(.el-table) {
  --el-table-header-bg-color: #f8fafc;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.02);
}

:deep(.el-table th.el-table__cell) {
  font-weight: 700;
  color: #475569;
}

.habit-item {
  margin: 6px 0;
}

.history-card {
  margin-bottom: 12px;
  border-radius: 12px;
}

.history-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #2f4056;
}

.family-history-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.family-history-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.family-relation {
  color: #334155;
  font-weight: 600;
}

.family-sep {
  color: #94a3b8;
}

.family-disease {
  color: #0f172a;
}

.family-history-editor {
  width: 100%;
}
.mr-8 {
  margin-right: 8px;
}
.mb-8 {
  margin-bottom: 8px;
}

.family-history-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.bp-value {
  font-family: monospace;
  font-weight: 700;
  color: #1aa094;
}

.date-range {
  font-size: 13px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 8px;
}

.empty-state {
  padding: 80px 0;
}

.pulse-btn {
  padding: 14px 40px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 14px;
  background-color: #1aa094;
  border: none;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow: 0 6px 20px rgba(26, 160, 148, 0.3);
}

.pulse-btn:hover {
  transform: translateY(-3px) scale(1.02);
  background-color: #168d83;
  box-shadow: 0 8px 25px rgba(26, 160, 148, 0.4);
}

.create-dialog :deep(.el-dialog) {
  border-radius: 24px;
  overflow: hidden;
}

.create-dialog :deep(.el-dialog__header) {
  background: #f8fafc;
  margin-right: 0;
  padding: 24px 32px;
  border-bottom: 1px solid #edf2f7;
}

.create-dialog :deep(.el-dialog__title) {
  font-weight: 700;
  color: #2f4056;
}

.dialog-tip {
  margin-bottom: 24px;
}

.create-form {
  padding: 0 12px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #475569;
}

:deep(.el-input__wrapper), :deep(.el-textarea__wrapper) {
  box-shadow: 0 0 0 1px #e2e8f0 inset;
  border-radius: 10px;
  padding: 4px 12px;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #1aa094 inset !important;
}

.dialog-footer {
  padding: 16px 32px 32px;
  display: flex;
  justify-content: flex-end;
  gap: 16px;
}

:deep(.el-dialog__body) {
  padding-top: 24px;
}

.animate-fade-in {
  animation: fadeIn 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.text-danger {
  color: #ef4444;
  font-weight: 700;
}
</style>