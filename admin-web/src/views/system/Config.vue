<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3 class="page-title"><span class="bar"></span>参数配置</h3>
        <p class="page-subtitle">温度阈值、保质期默认值、锁屏策略、码长度等系统参数（key-value，sys_config）</p>
      </div>
    </div>

    <el-card class="card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="configKey" label="参数键" width="240" />
        <el-table-column prop="remark" label="说明" min-width="220" />
        <el-table-column label="当前值" min-width="200">
          <template #default="{ row }">
            <el-input v-if="editing === row.configKey" v-model="editValue" size="small" style="max-width: 220px" @keyup.enter="save(row)" />
            <span v-else class="mono">{{ row.configValue }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="editing !== row.configKey" link type="primary" @click="startEdit(row)">编辑</el-button>
            <template v-else>
              <el-button link type="success" @click="save(row)">保存</el-button>
              <el-button link @click="editing = ''">取消</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { http } from '@/utils/request'
import { ElMessage } from 'element-plus'

const list = ref<any[]>([])
const loading = ref(false)
const editing = ref(''); const editValue = ref('')

async function load() { loading.value = true; list.value = await http.get('/system/configs'); loading.value = false }
function startEdit(row: any) { editing.value = row.configKey; editValue.value = row.configValue }
async function save(row: any) { await http.put(`/system/configs/${row.configKey}`, { configValue: editValue.value }); ElMessage.success('参数已更新'); editing.value = ''; load() }
onMounted(load)
</script>
