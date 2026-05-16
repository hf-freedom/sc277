<template>
  <div class="file-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>文件列表</span>
          <el-button type="primary" @click="showUploadDialog">
            <el-icon><Upload /></el-icon>
            上传文件
          </el-button>
        </div>
      </template>

      <el-table :data="files" border stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="fileName" label="文件名" min-width="150" />
        <el-table-column label="敏感标记" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isSensitive" type="danger" size="small" effect="dark">
              🔒 敏感
            </el-tag>
            <span v-else style="color: #909399;">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="文件类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 'SENSITIVE' ? 'danger' : 'info'" size="small">
              {{ getFileTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creator" label="创建者" width="100" />
        <el-table-column label="下载限制" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.downloadLimit">
              {{ row.downloadCount || 0 }} / {{ row.downloadLimit }}
            </span>
            <span v-else style="color: #909399;">无限制</span>
          </template>
        </el-table-column>
        <el-table-column label="额外复核" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.requiresAdditionalReview" type="warning" size="small">需要</el-tag>
            <span v-else style="color: #909399;">不需要</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="当前状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row)" size="small">
              {{ getStatusText(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="goToDetail(row.id)">
              <el-icon><View /></el-icon>
              管理版本
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && files.length === 0" description="暂无文件，请点击上方按钮上传文件" />
    </el-card>

    <el-dialog v-model="uploadDialogVisible" title="上传文件" width="600px">
      <el-form :model="uploadForm" label-width="100px">
        <el-form-item label="选择文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
            accept="*"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">只能上传一个文件</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="文件类型">
          <el-select v-model="uploadForm.fileType" placeholder="请选择文件类型" style="width: 100%">
            <el-option
              v-for="(chain, type) in approvalChains"
              :key="type"
              :label="getFileTypeLabel(type)"
              :value="type"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="uploadForm.fileType" label="审批流程">
          <el-steps direction="vertical" :active="0" finish-status="process">
            <el-step
              v-for="(approver, index) in approvalChains[uploadForm.fileType]"
              :key="index"
              :title="approver"
              :description="`第${index + 1}级审批`"
            />
          </el-steps>
        </el-form-item>
        <el-form-item label="敏感文件">
          <el-switch v-model="uploadForm.isSensitive" />
          <el-alert
            v-if="uploadForm.isSensitive"
            title="敏感文件标记"
            type="warning"
            description="标记为敏感文件后，需要额外复核，可设置下载次数限制"
            show-icon
            style="margin-top: 8px"
          />
        </el-form-item>
        <el-form-item v-if="uploadForm.isSensitive" label="下载次数限制">
          <el-input-number v-model="uploadForm.downloadLimit" :min="1" :max="100" style="width: 100%" />
          <div style="color: #909399; font-size: 12px; margin-top: 4px;">
            限制该文件最多可被下载的次数（默认3次）
          </div>
        </el-form-item>
        <el-form-item v-if="uploadForm.isSensitive" label="额外复核">
          <el-switch v-model="uploadForm.requiresAdditionalReview" />
          <div style="color: #909399; font-size: 12px; margin-top: 4px;">
            开启后，所有审批完成后还需要安全人员额外复核才能发布
          </div>
        </el-form-item>
        <el-form-item label="文件描述">
          <el-input v-model="uploadForm.description" type="textarea" />
        </el-form-item>
        <el-form-item label="上传者">
          <el-input v-model="uploadForm.uploader" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUpload" :loading="uploading">确认上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Upload, View } from '@element-plus/icons-vue'
import { uploadFile, getAllFiles, getApprovalChains } from '../api/file'

const router = useRouter()
const files = ref([])
const approvalChains = ref({})
const uploadDialogVisible = ref(false)
const uploading = ref(false)
const uploadRef = ref(null)
const uploadForm = ref({
  file: null,
  description: '',
  uploader: '匿名用户',
  fileType: '',
  isSensitive: false,
  downloadLimit: 3,
  requiresAdditionalReview: false
})

const fileTypeLabels = {
  DOCUMENT: '文档',
  IMAGE: '图片',
  VIDEO: '视频',
  CODE: '代码',
  CONFIG: '配置',
  SENSITIVE: '敏感文件'
}

const getFileTypeLabel = (type) => {
  return fileTypeLabels[type] || type
}

const loading = ref(false)

const loadFiles = async () => {
  loading.value = true
  try {
    const response = await getAllFiles()
    if (response.data.code === 200) {
      files.value = response.data.data
    } else {
      ElMessage.error(response.data.message || '加载文件列表失败')
    }
  } catch (error) {
    ElMessage.error('加载文件列表失败，请重试')
  } finally {
    loading.value = false
  }
}

const loadApprovalChains = async () => {
  try {
    const response = await getApprovalChains()
    if (response.data.code === 200) {
      approvalChains.value = response.data.data
    }
  } catch (error) {
    ElMessage.error('加载审批链配置失败')
  }
}

const showUploadDialog = () => {
  uploadForm.value = {
    file: null,
    description: '',
    uploader: '匿名用户',
    fileType: '',
    isSensitive: false,
    downloadLimit: 3,
    requiresAdditionalReview: false
  }
  uploadDialogVisible.value = true
}

const handleFileChange = (file) => {
  uploadForm.value.file = file.raw
}

const submitUpload = async () => {
  if (!uploadForm.value.file) {
    ElMessage.warning('请选择文件')
    return
  }

  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadForm.value.file)
    formData.append('description', uploadForm.value.description)
    formData.append('uploader', uploadForm.value.uploader)
    if (uploadForm.value.fileType) {
      formData.append('fileType', uploadForm.value.fileType)
    }
    formData.append('isSensitive', uploadForm.value.isSensitive)
    if (uploadForm.value.isSensitive) {
      formData.append('downloadLimit', uploadForm.value.downloadLimit)
      formData.append('requiresAdditionalReview', uploadForm.value.requiresAdditionalReview)
    }

    const response = await uploadFile(formData)
    if (response.data.code === 200) {
      ElMessage.success('上传成功')
      uploadDialogVisible.value = false
      loadFiles()
    } else {
      ElMessage.error(response.data.message || '上传失败')
    }
  } catch (error) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

const goToDetail = (fileId) => {
  router.push(`/detail/${fileId}`)
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const getStatusType = (row) => {
  if (row.currentPublishedVersionId) return 'success'
  if (row.currentDraftVersionId) return 'info'
  return 'info'
}

const getStatusText = (row) => {
  if (row.currentPublishedVersionId) return '已发布'
  if (row.currentDraftVersionId) return '草稿中'
  return '未知'
}

onMounted(() => {
  loadFiles()
  loadApprovalChains()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
