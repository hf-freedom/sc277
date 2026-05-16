<template>
  <div id="app">
    <el-container>
      <el-header style="background: #409EFF; color: white; font-size: 20px; font-weight: bold; display: flex; align-items: center;">
        <el-icon style="margin-right: 10px;"><Document /></el-icon>
        文件审批发布系统
      </el-header>
      <el-main style="padding: 20px;">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { Document } from '@element-plus/icons-vue'
</script>

<style>
#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  min-height: 100vh;
}
body {
  margin: 0;
  padding: 0;
}
</style>
