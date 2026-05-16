<template>
  <div class="file-detail">
    <el-button @click="goBack" style="margin-bottom: 20px;">
      <el-icon><ArrowLeft /></el-icon>
      返回
    </el-button>

    <el-card>
      <template #header>
        <span>文件详情 - {{ fileInfo?.fileName }}</span>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="文件名">{{ fileInfo?.fileName }}</el-descriptions-item>
        <el-descriptions-item label="文件类型">
          <el-tag :type="fileInfo?.type === 'SENSITIVE' ? 'danger' : 'info'" size="small">
            {{ getFileTypeLabel(fileInfo?.type) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="敏感文件">
          <el-tag v-if="fileInfo?.isSensitive" type="danger" size="small">是</el-tag>
          <span v-else>否</span>
        </el-descriptions-item>
        <el-descriptions-item label="下载限制" v-if="fileInfo?.isSensitive">
          {{ fileInfo?.downloadCount || 0 }} / {{ fileInfo?.downloadLimit || 3 }} 次
        </el-descriptions-item>
        <el-descriptions-item label="需要额外复核" v-if="fileInfo?.isSensitive">
          <el-tag v-if="fileInfo?.requiresAdditionalReview" type="warning" size="small">需要</el-tag>
          <span v-else>不需要</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建者">{{ fileInfo?.creator }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(fileInfo?.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-top: 20px;">
      <template #header>
        <span>版本列表</span>
      </template>

      <el-table :data="versions" border stripe style="width: 100%">
        <el-table-column prop="version" label="版本号" width="100" />
        <el-table-column prop="fileName" label="文件名" />
        <el-table-column label="文件类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 'SENSITIVE' ? 'danger' : 'info'" size="small">
              {{ getFileTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="文件大小" width="120">
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="uploader" label="上传者" width="120" />
        <el-table-column prop="uploadTime" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.uploadTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前审批" width="150">
          <template #default="{ row }">
            {{ row.currentApprovalChain || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="敏感文件" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.isSensitive" type="danger" size="small">是</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="下载限制" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.isSensitive && row.downloadLimit">
              {{ row.downloadCount || 0 }} / {{ row.downloadLimit }}
            </span>
            <span v-else style="color: #909399;">无限制</span>
          </template>
        </el-table-column>
        <el-table-column label="额外复核" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isSensitive && row.requiresAdditionalReview" type="warning" size="small">需要</el-tag>
            <span v-else style="color: #909399;">不需要</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="400">
          <template #default="{ row }">
            <el-button-group size="small">
              <el-button
                v-if="row.status === 'DRAFT' || row.status === 'REJECTED'"
                type="primary"
                @click="submitApproval(row.id)"
              >
                提交审批
              </el-button>
              <el-button
                v-if="row.status === 'PENDING_APPROVAL' || row.status === 'APPROVING'"
                type="success"
                @click="showApproveDialog(row)"
              >
                审批
              </el-button>
              <el-button
                v-if="row.status === 'PUBLISHED'"
                type="warning"
                @click="handleWithdraw(row.id)"
              >
                撤回
              </el-button>
              <el-button
                v-if="row.status === 'PUBLISHED'"
                type="danger"
                @click="handleRollback(row.id)"
              >
                回滚
              </el-button>
              <el-button
                v-if="row.status === 'PUBLISHED'"
                type="success"
                @click="handleDownload(row.id)"
              >
                下载
              </el-button>
              <el-button
                v-if="row.status === 'DRAFT' || row.status === 'REJECTED'"
                type="info"
                @click="showEditDialog(row)"
              >
                修改
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="approveDialogVisible" title="审批" width="600px">
      <div v-if="currentVersion?.approvalRecords && currentVersion.approvalRecords.length > 0" style="margin-bottom: 20px;">
        <div style="font-weight: bold; margin-bottom: 10px;">审批流程：</div>
        <el-steps direction="vertical" :active="currentVersion.currentApprovalIndex" finish-status="success">
          <el-step
            v-for="(record, index) in currentVersion.approvalRecords"
            :key="index"
            :title="record.approvalChainName"
            :description="getApprovalRecordDesc(record)"
          />
        </el-steps>
      </div>
      <el-form :model="approveForm" label-width="80px">
        <el-form-item label="审批人">
          <el-input v-model="approveForm.approver" />
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.comment" type="textarea" />
        </el-form-item>
        <el-form-item v-if="currentVersion?.isSensitive" label="额外复核">
          <el-checkbox v-model="approveForm.isAdditionalReview">是（敏感文件需要）</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReject" :loading="approving">驳回</el-button>
        <el-button type="success" @click="handleApprove" :loading="approving">通过</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="修改草稿" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="选择文件">
          <el-upload
            :auto-upload="false"
            :on-change="handleEditFileChange"
            :limit="1"
            accept="*"
          >
            <el-button type="primary">选择文件（可选）</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="文件描述">
          <el-input v-model="editForm.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit" :loading="editing">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  submitForApproval,
  approve,
  reject,
  withdraw,
  rollback,
  downloadFile,
  updateDraft,
  getVersionsByFileId
} from '../api/file'

const route = useRoute()
const router = useRouter()
const fileId = route.params.fileId

const fileInfo = ref(null)
const versions = ref([])
const approveDialogVisible = ref(false)
const editDialogVisible = ref(false)
const approving = ref(false)
const editing = ref(false)
const currentVersion = ref(null)
const approveForm = ref({
  approver: '',
  comment: '',
  isAdditionalReview: false
})
const editForm = ref({
  file: null,
  description: ''
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

const getApprovalRecordDesc = (record) => {
  if (record.status === 'PENDING') {
    return '待审批'
  } else if (record.status === 'APPROVED') {
    return `已通过 - ${record.approver} - ${formatTime(record.approvalTime)}`
  } else if (record.status === 'REJECTED') {
    return `已驳回 - ${record.approver} - ${formatTime(record.approvalTime)}`
  }
  return ''
}

const loadVersions = async () => {
  try {
    const response = await getVersionsByFileId(fileId)
    if (response.data.code === 200) {
      versions.value = response.data.data
      if (versions.value.length > 0) {
        fileInfo.value = {
          fileName: versions.value[0].fileName,
          type: versions.value[0].type,
          creator: versions.value[0].uploader,
          createTime: versions.value[0].uploadTime
        }
      }
    }
  } catch (error) {
    ElMessage.error('加载版本列表失败')
  }
}

const submitApproval = async (versionId) => {
  try {
    const response = await submitForApproval(versionId)
    if (response.data.code === 200) {
      ElMessage.success('提交审批成功')
      loadVersions()
    } else {
      ElMessage.error(response.data.message || '提交失败')
    }
  } catch (error) {
    ElMessage.error('提交失败')
  }
}

const showApproveDialog = (row) => {
  currentVersion.value = row
  approveForm.value = {
    approver: '',
    comment: '',
    isAdditionalReview: false
  }
  approveDialogVisible.value = true
}

const handleApprove = async () => {
  if (!approveForm.value.approver) {
    ElMessage.warning('请输入审批人')
    return
  }

  approving.value = true
  try {
    const response = await approve(
      currentVersion.value.id,
      approveForm.value.approver,
      approveForm.value.comment,
      approveForm.value.isAdditionalReview
    )
    if (response.data.code === 200) {
      ElMessage.success('审批通过')
      approveDialogVisible.value = false
      loadVersions()
    } else {
      ElMessage.error(response.data.message || '审批失败')
    }
  } catch (error) {
    ElMessage.error('审批失败')
  } finally {
    approving.value = false
  }
}

const handleReject = async () => {
  if (!approveForm.value.approver) {
    ElMessage.warning('请输入审批人')
    return
  }

  approving.value = true
  try {
    const response = await reject(
      currentVersion.value.id,
      approveForm.value.approver,
      approveForm.value.comment
    )
    if (response.data.code === 200) {
      ElMessage.success('已驳回')
      approveDialogVisible.value = false
      loadVersions()
    } else {
      ElMessage.error(response.data.message || '驳回失败')
    }
  } catch (error) {
    ElMessage.error('驳回失败')
  } finally {
    approving.value = false
  }
}

const handleWithdraw = async (versionId) => {
  try {
    await ElMessageBox.confirm('确定要撤回该文件吗？如果文件已被其他流程引用则无法撤回', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const response = await withdraw(versionId)
    if (response.data.code === 200) {
      ElMessage.success('撤回成功')
      loadVersions()
    } else {
      ElMessage.error(response.data.message || '撤回失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('撤回失败')
    }
  }
}

const handleRollback = async (versionId) => {
  try {
    await ElMessageBox.confirm('确定要回滚该版本吗？会恢复上一版本为发布状态', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const response = await rollback(versionId)
    if (response.data.code === 200) {
      ElMessage.success('回滚成功')
      loadVersions()
    } else {
      ElMessage.error(response.data.message || '回滚失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('回滚失败')
    }
  }
}

const handleDownload = (versionId) => {
  downloadFile(versionId)
  ElMessage.success('开始下载')
}

const showEditDialog = (row) => {
  currentVersion.value = row
  editForm.value = {
    file: null,
    description: row.description || ''
  }
  editDialogVisible.value = true
}

const handleEditFileChange = (file) => {
  editForm.value.file = file.raw
}

const submitEdit = async () => {
  editing.value = true
  try {
    const formData = new FormData()
    if (editForm.value.file) {
      formData.append('file', editForm.value.file)
    }
    if (editForm.value.description) {
      formData.append('description', editForm.value.description)
    }

    const response = await updateDraft(currentVersion.value.id, formData)
    if (response.data.code === 200) {
      ElMessage.success('修改成功')
      editDialogVisible.value = false
      loadVersions()
    } else {
      ElMessage.error(response.data.message || '修改失败')
    }
  } catch (error) {
    ElMessage.error('修改失败')
  } finally {
    editing.value = false
  }
}

const goBack = () => {
  router.push('/')
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

const getStatusTagType = (status) => {
  const map = {
    DRAFT: 'info',
    PENDING_APPROVAL: 'warning',
    APPROVING: 'warning',
    APPROVED: '',
    PUBLISHED: 'success',
    REJECTED: 'danger',
    ARCHIVED: 'info',
    WITHDRAWN: 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    DRAFT: '草稿',
    PENDING_APPROVAL: '待审批',
    APPROVING: '审批中',
    APPROVED: '已批准',
    PUBLISHED: '已发布',
    REJECTED: '已驳回',
    ARCHIVED: '已归档',
    WITHDRAWN: '已撤回'
  }
  return map[status] || status
}

onMounted(() => {
  loadVersions()
})
</script>

<style scoped>
.file-detail {
  padding: 0;
}
</style>
