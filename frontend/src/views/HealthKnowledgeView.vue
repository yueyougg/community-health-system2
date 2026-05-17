<template>
  <div class="health-knowledge-container animate-fade-in" v-loading="loading">
    <el-alert
      title="健康知识说明"
      type="success"
      description="获取专业的健康科普知识，助力科学生活。建议您定期阅读，了解更多日常护理和运动建议。"
      show-icon
      class="mb-20"
    />
    <div class="header-box mb-20">
      <h2 class="page-title">健康知识库</h2>
      <p class="page-desc">获取专业的健康科普知识，助力科学生活。</p>
      <el-button v-if="canEdit" type="primary" class="mt-12" @click="openCreateDialog">创建知识</el-button>
    </div>

    <el-row :gutter="20">
      <el-col :span="24" class="mb-20">
        <div class="filter-bar">
          <el-radio-group v-model="category" @change="loadData">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="慢性病">慢性病</el-radio-button>
            <el-radio-button label="日常护理">日常护理</el-radio-button>
            <el-radio-button label="饮食健康">饮食健康</el-radio-button>
            <el-radio-button label="运动建议">运动建议</el-radio-button>
          </el-radio-group>
        </div>
      </el-col>

      <el-col v-for="item in list" :key="item.id" :xs="24" :sm="12" :md="8" :lg="6" class="mb-20">
        <el-card class="knowledge-card" :body-style="{ padding: '0px' }" shadow="hover">
          <div class="card-cover" :style="{ background: getRandomGradient() }">
            <el-icon class="cover-icon"><Reading /></el-icon>
          </div>
          <div class="card-body">
            <h3 class="article-title">{{ item.title }}</h3>
            <div class="article-meta">
              <el-tag size="small" effect="plain">{{ item.category }}</el-tag>
              <span class="time">{{ formatFriendlyDate(item.createdAt) }}</span>
            </div>
            <p class="article-summary">{{ item.summary || '点击查看详情，了解更多健康小贴士...' }}</p>
            <div class="card-footer">
              <el-button type="primary" link @click="viewDetail(item)">阅读全文 <el-icon><Right /></el-icon></el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="24" v-if="list.length === 0">
        <el-empty description="暂无相关知识文章" />
      </el-col>
    </el-row>

    <!-- 详情对话框 -->
    <el-dialog v-model="showDetail" :title="currentArticle.title" width="800px" destroy-on-close>
      <div class="article-content-box">
        <div class="article-header">
          <el-tag size="small">{{ currentArticle.category }}</el-tag>
          <span class="article-time">发布时间：{{ formatFriendlyDateTime(currentArticle.createdAt) }}</span>
        </div>
        <el-divider />
        <div class="article-body" v-html="currentArticle.content || '内容加载中...'"></div>
      </div>
    </el-dialog>

    <el-dialog v-model="showCreateDialog" title="创建健康知识" width="680px" destroy-on-close>
      <el-form :model="createForm" label-position="top">
        <el-form-item label="标题">
          <el-input v-model="createForm.title" maxlength="120" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="createForm.category" style="width:100%">
            <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="createForm.tags" placeholder="多个标签以逗号分隔" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="createForm.content" type="textarea" :rows="8" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createKnowledge">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { Reading, Right } from "@element-plus/icons-vue";
import { knowledgeApi } from "../api/modules";
import { useAuthStore } from "../store/auth";
import { formatFriendlyDate, formatFriendlyDateTime } from "../utils/datetime";
import { ElMessage } from "element-plus";

const loading = ref(false);
const list = ref([]);
const category = ref("");
const showDetail = ref(false);
const currentArticle = ref({});
const showCreateDialog = ref(false);
const creating = ref(false);
const auth = useAuthStore();
const canEdit = computed(() => auth.role === "DOCTOR" || auth.role === "ADMIN");
const categories = ["慢性病", "日常护理", "饮食健康", "运动建议"];
const createForm = ref({
  title: "",
  category: "慢性病",
  tags: "",
  content: ""
});

const getRandomGradient = () => {
  const gradients = [
    'linear-gradient(135deg, #1aa094 0%, #2f4056 100%)',
    'linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)',
    'linear-gradient(135deg, #10b981 0%, #059669 100%)',
    'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)'
  ];
  return gradients[Math.floor(Math.random() * gradients.length)];
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await knowledgeApi.list({ category: category.value });
    list.value = res.data || [];
  } catch (e) {
    console.error("加载知识库失败", e);
  } finally {
    loading.value = false;
  }
};

const viewDetail = (article) => {
  currentArticle.value = article;
  showDetail.value = true;
};

const openCreateDialog = () => {
  createForm.value = { title: "", category: "慢性病", tags: "", content: "" };
  showCreateDialog.value = true;
};

const createKnowledge = async () => {
  if (!createForm.value.title || !createForm.value.content) {
    return ElMessage.warning("请填写标题和正文");
  }
  creating.value = true;
  try {
    await knowledgeApi.create(createForm.value);
    ElMessage.success("知识条目创建成功");
    showCreateDialog.value = false;
    await loadData();
  } finally {
    creating.value = false;
  }
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.health-knowledge-container { max-width: 1240px; margin: 0 auto; }
.header-box { text-align: center; padding: 20px 0; }
.mt-12 { margin-top: 12px; }
.page-title { color: #2f4056; font-size: 28px; margin-bottom: 8px; }
.page-desc { color: #64748b; font-size: 16px; }
.filter-bar { display: flex; justify-content: center; }
.mb-20 { margin-bottom: 20px; }

.knowledge-card { border-radius: 16px; border: none; transition: all 0.3s; }
.card-cover { height: 120px; display: flex; align-items: center; justify-content: center; color: white; }
.cover-icon { font-size: 48px; opacity: 0.8; }
.card-body { padding: 20px; }
.article-title { font-size: 18px; color: #1e293b; margin-bottom: 12px; line-height: 1.4; height: 50px; overflow: hidden; }
.article-meta { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.time { font-size: 12px; color: #94a3b8; }
.article-summary { font-size: 14px; color: #64748b; line-height: 1.6; margin-bottom: 16px; height: 66px; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; }
.card-footer { border-top: 1px solid #f1f5f9; padding-top: 12px; }

.article-content-box { padding: 0 20px 20px; }
.article-header { display: flex; align-items: center; gap: 15px; color: #94a3b8; font-size: 13px; }
.article-body { line-height: 1.8; color: #334155; font-size: 16px; }

.animate-fade-in { animation: fadeIn 0.5s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>
