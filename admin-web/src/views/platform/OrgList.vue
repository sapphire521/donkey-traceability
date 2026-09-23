<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>组织管理</h3>
        <p class="page-subtitle">联盟成员组织与 Fabric MSP 映射，平台管理员可准入新组织</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openAdd">新增组织</el-button>
    </div>

    <el-card class="card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="orgCode" label="组织编码" width="120" />
        <el-table-column prop="orgName" label="组织名称" min-width="200" />
        <el-table-column prop="orgType" label="类型" width="110">
          <template #default="{ row }"><el-tag size="small">{{ typeLabel(row.orgType) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="mspId" label="MSP ID" min-width="150" />
        <el-table-column prop="contact" label="联系人" width="110" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'info'" size="small">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.status === 1" @click="setStatus(row, 1)">启用</el-button>
            <el-button link type="danger" :disabled="row.status === 2" @click="setStatus(row, 2)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" title="新增组织" width="460px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="组织名称"><el-input v-model="form.orgName" placeholder="如：XX养殖场" /></el-form-item>
        <el-form-item label="组织类型">
          <el-select v-model="form.orgType" style="width: 100%">
            <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="MSP ID"><el-input v-model="form.mspId" placeholder="如：OrgFarmMSP" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contact" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { http } from '@/utils/request'
import { ElMessage } from 'element-plus'

const list = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const form = ref({ orgName: '', orgType: 'FARM', mspId: '', contact: '', phone: '' })
const types = [
  { value: 'PLATFORM', label: '平台运营方' }, { value: 'FARM', label: '养殖场' },
  { value: 'PLANT', label: '屠宰加工厂' }, { value: 'LOGISTICS', label: '物流商' },
  { value: 'SHOP', label: '门店' }, { value: 'REGULATOR', label: '监管部门' }
]
const typeLabel = (t: string) => types.find(x => x.value === t)?.label || t
const statusLabel = (s: number) => (s === 1 ? '正常' : s === 2 ? '停用' : '待启用')

async function load() { loading.value = true; const r = await http.get('/orgs', { page: 1, size: 50 }); list.value = r.records; loading.value = false }
function openAdd() { form.value = { orgName: '', orgType: 'FARM', mspId: '', contact: '', phone: '' }; dialog.value = true }
async function submit() { await http.post('/orgs', form.value); ElMessage.success('组织已创建（模拟 Fabric CA 注册）'); dialog.value = false; load() }
async function setStatus(row: any, s: number) { await http.put(`/orgs/${row.id}/status`, { status: s }); ElMessage.success('状态已更新'); load() }

onMounted(load)
</script>
