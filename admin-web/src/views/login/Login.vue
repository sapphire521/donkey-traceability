<template>
  <div class="login">
    <!-- 背景装饰：渐变 + 光斑 + 网格 -->
    <div class="bg-grid"></div>
    <div class="bg-ring r1"></div>
    <div class="bg-ring r2"></div>
    <div class="bg-glow"></div>

    <div class="login-card stagger">
      <!-- 左侧品牌区 -->
      <div class="brand-side">
        <div class="brand-top">
          <div class="logo">
            <svg viewBox="0 0 32 32" width="34" height="34" aria-hidden="true">
              <rect x="3" y="3" width="26" height="26" rx="8" fill="rgba(255,255,255,0.14)" />
              <path d="M9 12h14M9 16h14M9 20h10" stroke="#fff" stroke-width="2.2" stroke-linecap="round" />
              <circle cx="23" cy="20" r="2.4" fill="#4ade80" />
            </svg>
          </div>
          <div class="brand-title">驴链 DonkeyTrace</div>
          <div class="brand-sub">驴肉火烧区块链溯源管理系统</div>
        </div>

        <div class="brand-feature">
          <div class="feat"><el-icon><Link /></el-icon><div><b>链上可信</b><span>关键溯源事件实时上链，不可篡改可验证</span></div></div>
          <div class="feat"><el-icon><Postcard /></el-icon><div><b>一物一码</b><span>每份火烧绑定唯一溯源码，扫码见全链路</span></div></div>
          <div class="feat"><el-icon><Aim /></el-icon><div><b>监管穿透</b><span>批次级一键回溯与召回，守护食品安全</span></div></div>
        </div>

        <div class="brand-foot">Hyperledger Fabric 联盟链 · Spring Boot · Vue 3</div>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-side">
        <h2>欢迎登录</h2>
        <p class="form-tip">请输入账号密码进入管理端</p>

        <el-form :model="form" :rules="rules" ref="formRef" size="large" @keyup.enter="onSubmit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="账号" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="密码" :prefix-icon="Lock" />
          </el-form-item>
          <el-button type="primary" size="large" class="submit" :loading="loading" @click="onSubmit">登 录</el-button>
        </el-form>

        <div class="demo">
          <div class="demo-title">演示账号（密码均为 123456）</div>
          <div class="demo-list">
            <button v-for="d in demoAccounts" :key="d.username" class="demo-item" @click="fill(d)">
              <span class="demo-role">{{ d.role }}</span>
              <span class="demo-user mono">{{ d.username }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Link, Postcard, Aim } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const demoAccounts = [
  { username: 'admin', role: '平台管理员' },
  { username: 'farmer', role: '养殖员' },
  { username: 'plant', role: '加工员' },
  { username: 'qc', role: '质检员' },
  { username: 'logi', role: '调度员' },
  { username: 'shop', role: '门店店员' },
  { username: 'reg', role: '监管员' }
]

function fill(d: { username: string }) {
  form.username = d.username
  form.password = '123456'
}
async function onSubmit() {
  await formRef.value.validate(async (ok: boolean) => {
    if (!ok) return
    loading.value = true
    try {
      await userStore.login(form.username, form.password)
      ElMessage.success('登录成功')
      router.push('/dashboard')
    } catch (e) {
      // 错误已在 request 层提示
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login {
  height: 100vh;
  display: grid;
  place-items: center;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(1100px 560px at 12% -10%, rgba(22, 163, 74, 0.32), transparent 60%),
    radial-gradient(900px 520px at 105% 110%, rgba(13, 148, 136, 0.30), transparent 60%),
    linear-gradient(150deg, #0d3f27 0%, #0b2e21 48%, #06251a 100%);
}
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px);
  background-size: 34px 34px;
  mask-image: radial-gradient(75% 75% at 50% 45%, #000 30%, transparent 100%);
}
.bg-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.08);
  pointer-events: none;
}
.bg-ring.r1 { width: 520px; height: 520px; right: -140px; top: -160px; }
.bg-ring.r2 { width: 380px; height: 380px; left: -120px; bottom: -120px; border-color: rgba(74, 222, 128, 0.14); }
.bg-glow {
  position: absolute;
  width: 640px; height: 320px;
  left: 50%; top: 50%;
  transform: translate(-50%, -50%);
  background: radial-gradient(ellipse, rgba(74, 222, 128, 0.10), transparent 70%);
  pointer-events: none;
}

.login-card {
  position: relative;
  z-index: 1;
  width: min(880px, calc(100vw - 48px));
  min-height: 520px;
  display: grid;
  grid-template-columns: 46% 54%;
  border-radius: 22px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.97);
  box-shadow: 0 30px 80px rgba(2, 20, 12, 0.55), 0 0 0 1px rgba(255, 255, 255, 0.12);
}

/* 左侧品牌面 */
.brand-side {
  position: relative;
  padding: 40px 36px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: #fff;
  background: var(--grad-deep);
  overflow: hidden;
}
.brand-side::after {
  content: '';
  position: absolute;
  right: -110px;
  bottom: -110px;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.09);
}
.brand-top { position: relative; z-index: 1; }
.logo {
  width: 56px; height: 56px;
  display: grid; place-items: center;
  border-radius: 15px;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.16);
  margin-bottom: 18px;
}
.brand-title { font-size: 26px; font-weight: 700; letter-spacing: 1px; }
.brand-sub { color: var(--side-text-dim); margin-top: 8px; font-size: 13px; }
.brand-feature { position: relative; z-index: 1; display: flex; flex-direction: column; gap: 20px; }
.feat { display: flex; gap: 13px; align-items: flex-start; }
.feat .el-icon {
  font-size: 20px; color: #4ade80; margin-top: 2px;
  width: 38px; height: 38px; border-radius: 11px;
  display: grid; place-items: center;
  background: rgba(74, 222, 128, 0.12);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}
.feat b { display: block; font-size: 15px; }
.feat span { color: var(--side-text-dim); font-size: 12px; }
.brand-foot { position: relative; z-index: 1; color: var(--side-text-dim); font-size: 11px; font-family: var(--font-mono); }

/* 右侧表单面 */
.form-side { padding: 44px 42px; display: flex; flex-direction: column; justify-content: center; }
.form-side h2 { margin: 0 0 6px; font-size: 24px; color: var(--text-1); letter-spacing: 0.5px; }
.form-tip { color: var(--text-3); margin: 0 0 26px; font-size: 13px; }
.submit { width: 100%; margin-top: 6px; font-weight: 600; letter-spacing: 2px; height: 44px; }

.demo { margin-top: 26px; border-top: 1px dashed var(--border); padding-top: 16px; }
.demo-title { font-size: 12px; color: var(--text-3); margin-bottom: 10px; }
.demo-list { display: flex; flex-wrap: wrap; gap: 8px; }
.demo-item {
  display: flex; flex-direction: column; gap: 2px;
  padding: 7px 11px; border-radius: 10px;
  border: 1px solid var(--border); background: var(--bg-hover);
  cursor: pointer; transition: all 0.18s ease; text-align: left;
}
.demo-item:hover { border-color: var(--brand); background: var(--brand-soft-2); transform: translateY(-1px); box-shadow: var(--shadow-sm); }
.demo-role { font-size: 12px; color: var(--text-1); font-weight: 600; }
.demo-user { font-size: 11px; color: var(--text-3); }

@media (max-width: 900px) {
  .login-card { grid-template-columns: 1fr; min-height: 0; }
  .brand-side { display: none; }
}
</style>
