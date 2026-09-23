<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>{{ title }}</h3>
        <p class="page-subtitle">{{ subtitle }}</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openAdd">新建{{ typeLabel }}批次</el-button>
    </div>

    <el-card class="card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="batchNo" label="批次号" width="160" />
        <el-table-column prop="productName" label="产品/说明" min-width="180" />
        <el-table-column label="父批次" min-width="160">
          <template #default="{ row }">{{ row.parentNos?.length ? row.parentNos.join(' / ') : row.sourceEarTags?.join(' / ') || '—' }}</template>
        </el-table-column>
        <el-table-column label="重量" width="110"><template #default="{ row }">{{ row.weightKg }} kg</template></el-table-column>
        <el-table-column prop="produceDate" label="生产日期" width="130" />
        <el-table-column prop="holderOrgName" label="当前持有" min-width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="tagOf(batchStatusMap, row.status)[1]" size="small">{{ tagOf(batchStatusMap, row.status)[0] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goTree(row.batchNo)">批次树</el-button>
            <el-button link type="primary" @click="goReport(row.batchNo)">溯源</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" :title="`新建${typeLabel}批次`" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item :label="type === 'SLAUGHTER' ? '来源耳标' : '父批次号'">
          <el-input v-model="form.parents" :placeholder="type === 'SLAUGHTER' ? 'E1309-2025-000301,E1309-2025-000302' : 'S-20260911-001'" />
        </el-form-item>
        <el-form-item label="产品/说明"><el-input v-model="form.productName" :placeholder="type === 'SLAUGHTER' ? '待分割胴体' : '卤制驴肉(后腿)'" /></el-form-item>
        <el-form-item v-if="type !== 'SLAUGHTER'" label="重量(kg)"><el-input-number v-model="form.weightKg" :min="0" :precision="1" style="width: 100%" /></el-form-item>
        <el-form-item label="生产日期"><el-date-picker v-model="form.produceDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item v-if="type === 'PRODUCT'" label="保质期至"><el-date-picker v-model="form.expireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item :label="type === 'SLAUGHTER' ? '检疫哈希' : '检验哈希'"><el-input v-model="form.certHash" placeholder="sha256:..." /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="submit">提交（上链）</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { http } from '@/utils/request'
import { ElMessage } from 'element-plus'
import { batchStatusMap, tagOf } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const type = computed(() => (route.meta.batchType as string) || 'SLAUGHTER')
const title = computed(() => ({ SLAUGHTER: '屠宰批次管理', PROCESS: '加工批次管理', PRODUCT: '成品批次管理' }[type.value]))
const subtitle = computed(() => ({ SLAUGHTER: '核对耳标、宰前检疫、生成屠宰批次并上链', PROCESS: '选择屠宰批次分割/卤制，记录部位与重量', PRODUCT: '出厂检验合格后生成成品批次（含保质期）' }[type.value]))
const typeLabel = computed(() => ({ SLAUGHTER: '屠宰', PROCESS: '加工', PRODUCT: '成品' }[type.value]))

const list = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const form = ref({ parents: '', productName: '', weightKg: 0, produceDate: '2026-09-11', expireDate: '', certHash: '' })

const prefix = computed(() => ({ SLAUGHTER: 'S', PROCESS: 'P', PRODUCT: 'F' }[type.value]))
function genNo() { return `${prefix.value}-20260911-00${Math.floor(Math.random() * 90 + 10)}` }

async function load() { loading.value = true; const r = await http.get('/batches', { page: 1, size: 50, batchType: type.value }); list.value = r.records; loading.value = false }
function openAdd() { form.value = { parents: '', productName: '', weightKg: 0, produceDate: '2026-09-11', expireDate: '', certHash: '' }; dialog.value = true }
async function submit() {
  const payload: any = { batchNo: genNo(), productName: form.value.productName, produceDate: form.value.produceDate, certHash: form.value.certHash }
  if (type.value === 'SLAUGHTER') payload.sourceEarTags = form.value.parents.split(',').map(s => s.trim()).filter(Boolean)
  else { payload.parentNos = form.value.parents.split(',').map(s => s.trim()).filter(Boolean); payload.weightKg = form.value.weightKg }
  if (type.value === 'PRODUCT') payload.expireDate = form.value.expireDate
  const url = { SLAUGHTER: '/batches/slaughter', PROCESS: '/batches/process', PRODUCT: '/batches/product' }[type.value]
  await http.post(url, payload); ElMessage.success('批次已创建并上链'); dialog.value = false; load()
}
function goTree(no: string) { router.push(`/trace/tree?batchNo=${no}`) }
function goReport(no: string) { router.push(`/trace/report?batchNo=${no}`) }

onMounted(load)
// 同组件在 三个批次标签页间复用, 路由切换时按新类型重新加载
watch(type, () => { load() })
</script>
