<template>
  <div class="stat" :style="{ '--accent': color }">
    <div class="deco"></div>
    <div class="stat-icon"><el-icon><component :is="icon" /></el-icon></div>
    <div class="stat-body">
      <div class="stat-label">{{ label }}</div>
      <div class="stat-value">{{ value }}<span class="stat-unit">{{ unit }}</span></div>
      <div v-if="hint" class="stat-hint">{{ hint }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  value: number | string
  unit?: string
  icon: string
  color?: string
  hint?: string
}>()
</script>

<style scoped>
.stat {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}
.stat:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
  border-color: color-mix(in srgb, var(--accent, var(--brand)) 35%, var(--border));
}
/* 右上角同色系氛围光斑（参考图指标卡的柔和色块感） */
.deco {
  position: absolute;
  top: -34px;
  right: -34px;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: radial-gradient(circle, color-mix(in srgb, var(--accent, var(--brand)) 14%, transparent), transparent 70%);
  pointer-events: none;
}
.stat-icon {
  width: 46px; height: 46px; border-radius: 13px; flex-shrink: 0;
  display: grid; place-items: center; font-size: 22px;
  color: #fff;
  background: linear-gradient(135deg, var(--accent, var(--brand)) 0%, color-mix(in srgb, var(--accent, var(--brand)) 62%, #0d9488) 100%);
  box-shadow: 0 6px 14px color-mix(in srgb, var(--accent, var(--brand)) 34%, transparent), inset 0 1px 0 rgba(255, 255, 255, 0.3);
}
.stat-label { color: var(--text-2); font-size: 13px; }
.stat-value { font-size: 26px; font-weight: 700; color: var(--text-1); font-family: var(--font-mono); line-height: 1.2; }
.stat-unit { font-size: 13px; font-weight: 500; color: var(--text-3); margin-left: 4px; font-family: var(--font-sans); }
.stat-hint { font-size: 12px; color: var(--text-3); margin-top: 2px; }
.theme-dark .stat { background: var(--bg-card); border-color: var(--border); }
</style>
