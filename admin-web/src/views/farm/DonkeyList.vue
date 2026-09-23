<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>驴只档案管理</h3>
        <p class="page-subtitle">耳标建档 · 饲喂/免疫记录上链 · 出栏检疫，已屠宰档案只读</p>
      </div>
      <div>
        <el-button :icon="Upload" @click="onImport">Excel 批量建档</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新建档案</el-button>
      </div>
    </div>

    <el-card class="card">
      <div class="filter">
        <el-input v-model="kw" placeholder="耳标号 / 品种 / 圈舍" clearable style="width: 240px" :prefix-icon="Search" @keyup.enter="load" />
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 140px" @change="load">
          <el-option label="饲养中" value="RAISED" /><el-option label="已检疫" value="QUARANTINED" /><el-option label="已屠宰" value="SLAUGHTERED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe @row-click="openDetail" class="clickable">
        <el-table-column prop="earTagId" label="耳标号" width="160" />
        <el-table-column prop="breed" label="品种" width="110" />
        <el-table-column label="性别" width="80"><template #default="{ row }">{{ row.gender === 'M' ? '公' : '母' }}</template></el-table-column>
        <el-table-column prop="birthDate" label="出生日期" width="130" />
        <el-table-column prop="barnNo" label="圈舍" width="100" />
        <el-table-column prop="orgName" label="所属养殖场" min-width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="tagOf(donkeyStatusMap, row.status)[1]" size="small">{{ tagOf(donkeyStatusMap, row.status)[0] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="上链" width="90"><template #default="{ row }"><span class="dot green"></span>已上链</template></el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" title="新建驴只档案" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="耳标号"><el-input v-model="form.earTagId" placeholder="E1309-2025-000312" /></el-form-item>
        <el-form-item label="品种"><el-select v-model="form.breed" style="width: 100%"><el-option v-for="b in breeds" :key="b" :label="b" :value="b" /></el-select></el-form-item>
        <el-form-item label="性别"><el-radio-group v-model="form.gender"><el-radio value="M">公</el-radio><el-radio value="F">母</el-radio></el-radio-group></el-form-item>
        <el-form-item label="出生日期"><el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="圈舍编号"><el-input v-model="form.barnNo" placeholder="A-B1" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="submit">提交（上链）</el-button></template>
    </el-dialog>

    <el-drawer v-model="drawer" :title="current?.earTagId || '档案详情'" size="520px">
      <template v-if="current">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="品种">{{ current.breed }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="tagOf(donkeyStatusMap, current.status)[1]" size="small">{{ tagOf(donkeyStatusMap, current.status)[0] }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="出生">{{ current.birthDate }}</el-descriptions-item>
          <el-descriptions-item label="圈舍">{{ current.barnNo }}</el-descriptions-item>
          <el-descriptions-item label="建档交易" :span="2"><span class="mono">{{ current.createTxid }}</span></el-descriptions-item>
        </el-descriptions>

        <div class="timeline-head">
          <span>事件时间轴</span>
          <el-button size="small" type="primary" plain :icon="Plus" @click="openEvent">追加记录</el-button>
        </div>
        <el-timeline>
          <el-timeline-item v-for="e in events" :key="e.id" :timestamp="e.eventTime" placement="top" :type="e.eventType === 'QUARANTINE' ? 'success' : 'primary'">
            <div class="ev"><b>{{ eventLabel(e.eventType) }}</b> · {{ e.content }}</div>
            <div class="ev-tx mono" v-if="e.txId">txId: {{ e.txId }} · 区块 #{{ e.blockNo }}</div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>

    <el-dialog v-model="eventDialog" title="追加饲喂/免疫记录" width="440px">
      <el-form :model="eventForm" label-width="80px">
        <el-form-item label="类型"><el-select v-model="eventForm.eventType" style="width: 100%"><el-option label="饲喂" value="FEED" /><el-option label="免疫" value="IMMUNIZE" /><el-option label="健康" value="HEALTH" /></el-select></el-form-item>
        <el-form-item label="内容"><el-input v-model="eventForm.content" type="textarea" :rows="3" placeholder="如：饲喂牧草+豆粕 2.5kg" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="eventDialog = false">取消</el-button><el-button type="primary" @click="submitEvent">提交上链</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Search, Upload } from '@element-plus/icons-vue'
import { http } from '@/utils/request'
import { ElMessage } from 'element-plus'
import { donkeyStatusMap, tagOf } from '@/utils/format'

const list = ref<any[]>([])
const events = ref<any[]>([])
const loading = ref(false)
const kw = ref(''); const statusFilter = ref('')
const dialog = ref(false); const eventDialog = ref(false)
const drawer = ref(false); const current = ref<any>(null)
const breeds = ['德州驴', '关中驴', '新疆驴', '杂交']
const form = ref({ earTagId: '', breed: '德州驴', gender: 'M', birthDate: '', barnNo: '' })
const eventForm = ref({ eventType: 'FEED', content: '' })

async function load() { loading.value = true; const r = await http.get('/donkeys', { page: 1, size: 50, keyword: kw.value || undefined, status: statusFilter.value || undefined }); list.value = r.records; loading.value = false }
function openAdd() { form.value = { earTagId: '', breed: '德州驴', gender: 'M', birthDate: '', barnNo: '' }; dialog.value = true }
async function submit() { await http.post('/donkeys', form.value); ElMessage.success('建档成功，已上链'); dialog.value = false; load() }
function onImport() { ElMessage.info('演示：将调用 Excel 模板导入，导入结果含校验报告') }
async function openDetail(row: any) { current.value = row; drawer.value = true; const r = await http.get(`/donkeys/${row.earTagId}`); events.value = r.events || [] }
function eventLabel(t: string) { return { FEED: '饲喂', IMMUNIZE: '免疫', HEALTH: '健康', QUARANTINE: '出栏检疫' }[t] || t }
function openEvent() { eventForm.value = { eventType: 'FEED', content: '' }; eventDialog.value = true }
async function submitEvent() { await http.post(`/donkeys/${current.value.earTagId}/events`, eventForm.value); ElMessage.success('记录已上链'); eventDialog.value = false; openDetail(current.value) }

onMounted(load)
</script>

<style scoped>
.filter { display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; }
.clickable :deep(.el-table__row) { cursor: pointer; }
.timeline-head { display: flex; justify-content: space-between; align-items: center; margin: 18px 0 10px; font-weight: 600; color: var(--text-1); }
.ev { font-size: 13px; color: var(--text-1); }
.ev-tx { font-size: 12px; color: var(--text-3); margin-top: 3px; }
</style>
