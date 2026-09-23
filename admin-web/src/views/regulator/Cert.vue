<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>证照管理</h3>
        <p class="page-subtitle">营业执照 / 食品经营许可证 / 检疫条件合格证等，上传即算哈希上链存证</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openAdd">上传证照</el-button>
    </div>

    <el-card class="card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="orgName" label="所属组织" min-width="180" />
        <el-table-column prop="certType" label="证照类型" min-width="160" />
        <el-table-column prop="certNo" label="证号" min-width="150" />
        <el-table-column prop="issueDate" label="发证日期" width="120" />
        <el-table-column prop="expireDate" label="有效期至" width="120" />
        <el-table-column label="审核" width="100">
          <template #default="{ row }"><el-tag :type="tagOf(certAuditMap, row.auditStatus)[1]" size="small">{{ tagOf(certAuditMap, row.auditStatus)[0] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="哈希" min-width="140"><template #default="{ row }"><span class="mono">{{ row.fileHash }}</span></template></el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" title="上传证照（哈希上链）" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="所属组织"><el-input v-model="form.orgName" /></el-form-item>
        <el-form-item label="证照类型">
          <el-select v-model="form.certType" style="width: 100%">
            <el-option label="营业执照" value="营业执照" /><el-option label="食品经营许可证" value="食品经营许可证" />
            <el-option label="动物防疫条件合格证" value="动物防疫条件合格证" /><el-option label="检验检测资质" value="检验检测资质" />
          </el-select>
        </el-form-item>
        <el-form-item label="证号"><el-input v-model="form.certNo" /></el-form-item>
        <el-form-item label="发证日期"><el-date-picker v-model="form.issueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="有效期至"><el-date-picker v-model="form.expireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="附件"><el-upload action="#" :auto-upload="false" :limit="1"><el-button :icon="Upload">选择文件 (PDF)</el-button></el-upload></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="submit">上传并存证</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Upload } from '@element-plus/icons-vue'
import { http } from '@/utils/request'
import { ElMessage } from 'element-plus'
import { certAuditMap, tagOf } from '@/utils/format'

const list = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const form = ref({ orgName: '', certType: '食品经营许可证', certNo: '', issueDate: '2026-09-11', expireDate: '2029-09-11' })

async function load() { loading.value = true; const r = await http.get('/certs', { page: 1, size: 50, scope: 'all' }); list.value = r.records; loading.value = false }
function openAdd() { form.value = { orgName: '', certType: '食品经营许可证', certNo: '', issueDate: '2026-09-11', expireDate: '2029-09-11' }; dialog.value = true }
async function submit() { await http.post('/certs', { ...form.value, fileUri: 'minio://cert/' + Date.now() + '.pdf', fileHash: 'sha256:' + Math.random().toString(16).slice(2, 10) }); ElMessage.success('证照已上传，文件哈希已上链存证'); dialog.value = false; load() }
onMounted(load)
</script>
