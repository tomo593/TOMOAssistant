<template>
  <div class="app-wrapper">
    <div class="app-content">
      <el-container class="app-container">
        <el-aside width="260px" class="app-aside">
          <Sidebar />
        </el-aside>
        <el-main class="app-main">
          <router-view />
        </el-main>
      </el-container>
    </div>
  </div>
</template>

<script setup>
import Sidebar from './components/common/Sidebar.vue'
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.app-wrapper {
  min-height: 100vh;
  background: #e8e0d8;
  display: flex;
  justify-content: center;
  padding: 20px;
}

.app-content {
  width: 70%;
  min-width: 900px;
  max-width: 1400px;
  height: calc(100vh - 40px);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.app-container {
  height: 100%;
}

.app-aside {
  background: #f5f0eb;
  color: #333;
  border-right: 1px solid #e0d5ca;
  overflow-y: auto;
}

.app-aside::-webkit-scrollbar {
  display: none;
}

.app-aside {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.app-main {
  background: #f5f0eb;
  padding: 0;
  overflow: hidden;
}
</style>
