import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router'
import Layout from '@/layout/BasicLayout.vue'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'Login', component: () => import('@/views/login/Login.vue'), meta: { public: true, title: '登录' } },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      { path: '/dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/Index.vue'), meta: { title: '工作台', icon: 'Odometer' } },
      { path: '/platform/org', name: 'OrgList', component: () => import('@/views/platform/OrgList.vue'), meta: { title: '组织管理', permission: 'org:manage' } },
      { path: '/platform/user', name: 'UserList', component: () => import('@/views/platform/UserList.vue'), meta: { title: '用户管理', permission: 'org:manage' } },
      { path: '/platform/notice', name: 'Notice', component: () => import('@/views/platform/Notice.vue'), meta: { title: '联盟公告', permission: 'org:manage' } },
      { path: '/farm/donkey', name: 'DonkeyList', component: () => import('@/views/farm/DonkeyList.vue'), meta: { title: '驴只档案', permission: 'donkey:write' } },
      { path: '/plant/slaughter', name: 'SlaughterBatch', component: () => import('@/views/plant/BatchManage.vue'), meta: { title: '屠宰批次', permission: 'batch:slaughter', batchType: 'SLAUGHTER' } },
      { path: '/plant/process', name: 'ProcessBatch', component: () => import('@/views/plant/BatchManage.vue'), meta: { title: '加工批次', permission: 'batch:process', batchType: 'PROCESS' } },
      { path: '/plant/product', name: 'ProductBatch', component: () => import('@/views/plant/BatchManage.vue'), meta: { title: '成品批次', permission: 'batch:process', batchType: 'PRODUCT' } },
      { path: '/plant/qc', name: 'QcReport', component: () => import('@/views/plant/QcReport.vue'), meta: { title: '出厂检验', permission: 'qc:report' } },
      { path: '/logistics/transport', name: 'TransportOrder', component: () => import('@/views/logistics/TransportOrder.vue'), meta: { title: '运输管理', permission: 'transport:manage' } },
      { path: '/shop/inventory', name: 'Inventory', component: () => import('@/views/shop/Inventory.vue'), meta: { title: '库存管理', permission: 'shop:stock' } },
      { path: '/shop/output', name: 'OutputRecord', component: () => import('@/views/shop/OutputRecord.vue'), meta: { title: '产出记录', permission: 'shop:stock' } },
      { path: '/shop/trace-code', name: 'TraceCode', component: () => import('@/views/shop/TraceCode.vue'), meta: { title: '溯源码管理', permission: 'code:bind' } },
      { path: '/trace/tree', name: 'BatchTree', component: () => import('@/views/trace/BatchTree.vue'), meta: { title: '批次树可视化', permission: 'trace:view' } },
      { path: '/trace/report', name: 'ReportPreview', component: () => import('@/views/trace/ReportPreview.vue'), meta: { title: '溯源报告预览', permission: 'trace:view' } },
      { path: '/trace/chain', name: 'ChainLedger', component: () => import('@/views/trace/ChainLedger.vue'), meta: { title: '区块链账本', permission: 'trace:view' } },
      { path: '/cert', name: 'Cert', component: () => import('@/views/regulator/Cert.vue'), meta: { title: '证照管理', permission: 'cert:manage' } },
      { path: '/regulator/inspection', name: 'Inspection', component: () => import('@/views/regulator/Inspection.vue'), meta: { title: '抽检登记', permission: 'regulator:audit' } },
      { path: '/regulator/recall', name: 'Recall', component: () => import('@/views/regulator/Recall.vue'), meta: { title: '召回管理', permission: 'regulator:recall' } },
      { path: '/regulator/screen', name: 'BigScreen', component: () => import('@/views/regulator/BigScreen.vue'), meta: { title: '数据大屏', permission: 'stats:screen', fullscreen: true } },
      { path: '/alert', name: 'Alert', component: () => import('@/views/regulator/Alert.vue'), meta: { title: '预警中心', permission: 'alert:view' } },
      { path: '/system/log', name: 'OperationLog', component: () => import('@/views/system/OperationLog.vue'), meta: { title: '操作日志', permission: 'sys:manage' } },
      { path: '/system/config', name: 'Config', component: () => import('@/views/system/Config.vue'), meta: { title: '参数配置', permission: 'sys:manage' } }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/error/NotFound.vue'), meta: { title: '页面不存在' } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  const token = getToken()
  if (!token) return { path: '/login' }

  // 监管大屏暗色主题
  const app = useAppStore()
  app.setCockpit(to.path.startsWith('/regulator/screen'))

  const user = useUserStore()
  // 登录后首跳或无权限时回工作台
  if (to.path === '/' ) return { path: '/dashboard' }
  if (!user.accessible.has(to.path) && !user.accessible.has(to.path.replace(/\/$/, ''))) {
    return { path: '/dashboard' }
  }
  return true
})

export default router
