<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>用户管理</h3>
        <p class="page-subtitle">组织下用户账号、角色分配与启用停用</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openAdd">新增用户</el-button>
    </div>

    <el-card class="card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="orgName" label="所属组织" min-width="180" />
        <el-table-column label="角色" width="140">
          <template #default="{ row }"><el-tag size="small" type="warning">{{ row.roleCode }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" width="140" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.status === 1" @click="setStatus(row, 1)">启用</el-button>
            <el-button link type="danger" :disabled="row.status === 0" @click="setStatus(row, 0)">禁用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" title="新增用户" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="账号"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="组织">
          <el-select v-model="form.orgId" style="width: 100%" placeholder="选择组织">
            <el-option v-for="o in orgs" :key="o.id" :label="o.orgName" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleCode" style="width: 100%">
            <el-option v-for="r in roles" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始密码"><el-input v-model="form.password" placeholder="默认 123456" /></el-form-item>
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
const orgs = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const roles = ['FARM_OPERATOR', 'PLANT_OPERATOR', 'PLANT_QC', 'LOGISTICS_DISPATCHER', 'SHOP_KEEPER', 'REGULATOR_OFFICER']
const form = ref({ username: '', realName: '', orgId: '', roleCode: 'FARM_OPERATOR', password: '123456' })

async function load() { loading.value = true; const [u, o] = await Promise.all([http.get('/users', { page: 1, size: 50 }), http.get('/orgs', { page: 1, size: 50 })]); list.value = u.records; orgs.value = o.records; loading.value = false }
function openAdd() { form.value = { username: '', realName: '', orgId: orgs.value[0]?.id || '', roleCode: 'FARM_OPERATOR', password: '123456' }; dialog.value = true }
async function submit() { await http.post('/users', form.value); ElMessage.success('用户已创建'); dialog.value = false; load() }
async function setStatus(row: any, s: number) { await http.put(`/users/${row.id}/status`, { status: s }); ElMessage.success('状态已更新'); load() }

onMounted(load)
</script>
