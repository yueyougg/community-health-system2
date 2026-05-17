<template>
  <div class="placeholder-wrap">
    <h1>{{ title }}</h1>
    <p>该功能正在开发中...</p>
    <el-empty description="Coming Soon" />
  </div>
</template>

<script setup>
defineProps({
  title: String
});
</script>

<style scoped>
.placeholder-wrap {
  padding: 40px;
  background: rgb(255,250,235);
  border-radius: 8px;
  text-align: center;
}
</style>
