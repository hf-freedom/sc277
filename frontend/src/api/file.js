import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

export const uploadFile = (formData) => {
  return api.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const getApprovalChains = () => {
  return api.get('/files/approval-chains')
}

export const updateDraft = (versionId, formData) => {
  return api.put(`/files/draft/${versionId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const submitForApproval = (versionId) => {
  return api.post(`/files/submit/${versionId}`)
}

export const approve = (versionId, approver, comment, isAdditionalReview = false) => {
  return api.post(`/files/approve/${versionId}`, null, {
    params: { approver, comment, isAdditionalReview }
  })
}

export const reject = (versionId, approver, comment) => {
  return api.post(`/files/reject/${versionId}`, null, {
    params: { approver, comment }
  })
}

export const withdraw = (versionId) => {
  return api.post(`/files/withdraw/${versionId}`)
}

export const rollback = (versionId) => {
  return api.post(`/files/rollback/${versionId}`)
}

export const downloadFile = (versionId) => {
  window.open(`/api/files/download/${versionId}`, '_blank')
}

export const getAllFiles = () => {
  return api.get('/files')
}

export const getVersionsByFileId = (fileId) => {
  return api.get(`/files/${fileId}/versions`)
}

export const getVersion = (versionId) => {
  return api.get(`/files/version/${versionId}`)
}

export default api
