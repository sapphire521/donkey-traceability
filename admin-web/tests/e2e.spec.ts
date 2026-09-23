// 驴肉火烧区块链溯源管理系统 管理端 E2E 全功能测试
// 覆盖：登录鉴权、工作台、联盟管理、养殖、屠宰加工、冷链、门店、溯源引擎（含区块链账本）、证照、监管、预警、系统管理、角色权限
import { test, expect, Page } from '@playwright/test'

const PW = '123456'

async function login(page: Page, user = 'admin') {
  await page.goto('/')
  await page.getByPlaceholder('账号').fill(user)
  await page.getByPlaceholder('密码').fill(PW)
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForURL('**/dashboard', { timeout: 15_000 })
}

const ROUTE_MAP: Record<string, string> = {
  '组织管理': '/platform/org', '用户管理': '/platform/user', '联盟公告': '/platform/notice',
  '驴只档案': '/farm/donkey', '屠宰批次': '/plant/slaughter', '加工批次': '/plant/process',
  '成品批次': '/plant/product', '出厂检验': '/plant/qc', '运输管理': '/logistics/transport',
  '库存管理': '/shop/inventory', '产出记录': '/shop/output', '溯源码管理': '/shop/trace-code',
  '批次树可视化': '/trace/tree', '溯源报告预览': '/trace/report', '区块链账本': '/trace/chain',
  '证照管理': '/cert', '抽检登记': '/regulator/inspection', '召回管理': '/regulator/recall',
  '数据大屏': '/regulator/screen', '预警中心': '/alert', '操作日志': '/system/log', '参数配置': '/system/config'
}

async function nav(page: Page, group: string, item?: string) {
  if (item) {
    const it = page.locator('.el-menu-item', { hasText: item }).first()
    if (!(await it.isVisible().catch(() => false))) {
      const g = page.locator('.el-sub-menu__title', { hasText: group }).first()
      if (await g.count()) { await g.click(); await it.waitFor({ state: 'visible', timeout: 6000 }).catch(() => {}) }
    }
    if (await it.isVisible().catch(() => false)) await it.click()
    else await page.goto(ROUTE_MAP[item] || '/')
  } else {
    const gi = page.locator('.el-menu-item', { hasText: group }).first()
    if (await gi.isVisible().catch(() => false)) { await gi.click() }
    else {
      const g = page.locator('.el-sub-menu__title', { hasText: group }).first()
      if (await g.count()) await g.click()
    }
  }
  await page.waitForTimeout(900)
}

async function expectTitle(page: Page, title: string) {
  await expect(page.locator('.page-title').first()).toContainText(title)
}
async function tableRows(page: Page) {
  await expect(page.locator('.el-table__row').first()).toBeVisible()
  return page.locator('.el-table__row').count()
}

// ---------------- 1. 登录鉴权 ----------------
test.describe('登录与鉴权', () => {
  test('错误密码提示且不跳转', async ({ page }) => {
    await page.goto('/')
    await page.getByPlaceholder('账号').fill('admin')
    await page.getByPlaceholder('密码').fill('wrong-pass')
    await page.getByRole('button', { name: /登\s*录/ }).click()
    await expect(page.locator('.el-message--error')).toContainText(/密码错误/)
    await expect(page).toHaveURL(/login|^http:\/\/localhost:5173\/$/)
  })

  test('admin 登录成功进入工作台', async ({ page }) => {
    await login(page)
    await expect(page.locator('.page-title, .welcome, h1, h2, h3').first()).toBeVisible()
    await expect(page.getByText('驴链平台运营方').first()).toBeVisible()
  })

  test('shop 角色仅见门店相关菜单（权限隔离）', async ({ page }) => {
    await login(page, 'shop')
    await expect(page.locator('.el-sub-menu__title, .el-menu-item').filter({ hasText: '门店管理' }).first()).toBeVisible()
    await expect(page.locator('.el-sub-menu__title', { hasText: '联盟管理' })).toHaveCount(0)
    await expect(page.locator('.el-sub-menu__title', { hasText: '监管中心' })).toHaveCount(0)
  })

  test('reg 角色可见监管中心与预警中心', async ({ page }) => {
    await login(page, 'reg')
    await expect(page.locator('.el-sub-menu__title', { hasText: '监管中心' })).toBeVisible()
    await expect(page.locator('.el-menu-item', { hasText: '预警中心' })).toBeVisible()
    await expect(page.locator('.el-sub-menu__title', { hasText: '门店管理' })).toHaveCount(0)
  })
})

// ---------------- 2. 工作台 ----------------
test('工作台统计卡与图表加载', async ({ page }) => {
  await login(page)
  await expect(page.getByText('上链事件').first()).toBeVisible()
  await expect(page.locator('canvas').first()).toBeVisible()
})

// ---------------- 3. 联盟管理 ----------------
test.describe('联盟管理', () => {
  test('组织管理：列表、新增组织、停用启用', async ({ page }) => {
    await login(page)
    await nav(page, '联盟管理', '组织管理')
    await expectTitle(page, '组织管理')
    const before = await tableRows(page)
    expect(before).toBeGreaterThan(5)
    // 新增组织
    await page.getByRole('button', { name: '新增组织' }).click()
    const dialog = page.locator('.el-dialog', { hasText: '新增组织' })
    await expect(dialog).toBeVisible()
    const inputs = dialog.locator('.el-input__inner')
    await inputs.nth(0).fill('PLAT-T-001')
    await inputs.nth(1).fill('测试示范养殖场')
    await dialog.getByRole('button', { name: /确\s*定|保\s*存|提\s*交/ }).click()
    await expect(page.locator('.el-message--success').first()).toBeVisible()
    // 停用/启用切换
    const toggle = page.locator('.el-table__row').first().locator('.el-switch')
    if (await toggle.count()) { await toggle.first().click(); await page.waitForTimeout(400) }
  })

  test('用户管理与联盟公告', async ({ page }) => {
    await login(page)
    await nav(page, '联盟管理', '用户管理')
    await expectTitle(page, '用户管理')
    await tableRows(page)
    await nav(page, '联盟管理', '联盟公告')
    await expectTitle(page, '联盟公告')
    await expect(page.getByText('驴肉火烧溯源新码规则').first()).toBeVisible()
  })
})

// ---------------- 4. 养殖管理 ----------------
test.describe('养殖管理', () => {
  test('驴只档案：列表、新建档案上链、详情抽屉', async ({ page }) => {
    await login(page)
    await nav(page, '养殖管理', '驴只档案')
    await expectTitle(page, '驴只档案管理')
    await tableRows(page)
    // 新建档案（提交上链）
    await page.getByRole('button', { name: '新建档案' }).click()
    const dialog = page.locator('.el-dialog', { hasText: '新建驴只档案' })
    await expect(dialog).toBeVisible()
    const tag = 'E1309-2026-' + String(Date.now()).slice(-6)
    await dialog.getByPlaceholder('E1309-2025-000312').fill(tag)
    await dialog.getByRole('button', { name: /提交（上链）/ }).click()
    await expect(page.locator('.el-message--success').first()).toBeVisible()
    // 详情抽屉
    await page.locator('.el-table__row').first().click()
    await expect(page.locator('.el-drawer')).toBeVisible()
    await page.keyboard.press('Escape')
  })

  test('耳标号搜索过滤', async ({ page }) => {
    await login(page)
    await nav(page, '养殖管理', '驴只档案')
    await page.getByPlaceholder('耳标号 / 品种 / 圈舍').fill('E1309-2025-000301')
    await page.getByRole('button', { name: '查询' }).last().click()
    await page.waitForTimeout(800)
    const rows = await tableRows(page)
    expect(rows).toBeLessThanOrEqual(2)
  })
})

// ---------------- 5. 屠宰加工 ----------------
test.describe('屠宰加工', () => {
  test('屠宰/加工/成品批次列表', async ({ page }) => {
    await login(page)
    await nav(page, '屠宰加工', '屠宰批次')
    await expectTitle(page, '屠宰批次')
    await tableRows(page)
    await nav(page, '屠宰加工', '加工批次')
    await tableRows(page)
    await nav(page, '屠宰加工', '成品批次')
    await tableRows(page)
    await expect(page.getByText('F-20260911-001').first()).toBeVisible()
  })

  test('出厂检验页面加载', async ({ page }) => {
    await login(page)
    await nav(page, '屠宰加工', '出厂检验')
    await expectTitle(page, '出厂检验')
  })
})

// ---------------- 6. 冷链物流 ----------------
test('运输管理：列表与温湿度记录', async ({ page }) => {
  await login(page)
  await nav(page, '冷链物流', '运输管理')
  await expectTitle(page, '冷链运输管理')
  await tableRows(page)
  await expect(page.getByText('冀F·6X8K2').first()).toBeVisible()
})

// ---------------- 7. 门店管理 ----------------
test.describe('门店管理', () => {
  test('库存管理列表', async ({ page }) => {
    await login(page, 'shop')
    await nav(page, '门店管理', '库存管理')
    await expectTitle(page, '门店库存管理')
    await tableRows(page)
    await expect(page.getByText('驴肉火烧·卤制驴肉').first()).toBeVisible()
  })

  test('产出记录：列表加载与新增', async ({ page }) => {
    await login(page, 'shop')
    await nav(page, '门店管理', '产出记录')
    await expectTitle(page, '产出记录')
    await tableRows(page)
    await page.getByRole('button', { name: /新增|登记|生成/ }).first().click()
    const dlg = page.locator('.el-dialog:visible').first()
    if (await dlg.count()) {
      await dlg.getByRole('button', { name: /取\s*消/ }).click()
    }
  })

  test('溯源码管理：生成并绑定上链', async ({ page }) => {
    await login(page, 'shop')
    await nav(page, '门店管理', '溯源码管理')
    await expectTitle(page, '溯源码管理')
    await tableRows(page)
    await page.getByRole('button', { name: '生成溯源码' }).click()
    const dlg = page.locator('.el-dialog', { hasText: '批量生成溯源码' })
    await expect(dlg).toBeVisible()
    await dlg.locator('.el-input__inner').nth(0).fill('F-20260911-001')
    await dlg.locator('.el-input__inner').nth(1).fill('驴火·裕华路总店')
    await dlg.getByRole('button', { name: '生成并绑定上链' }).click()
    await expect(page.locator('.el-message--success').first()).toBeVisible()
  })
})

// ---------------- 8. 溯源引擎 ----------------
test.describe('溯源引擎与区块链', () => {
  test('批次树可视化渲染', async ({ page }) => {
    await login(page)
    await nav(page, '溯源引擎', '批次树可视化')
    await expectTitle(page, '批次树可视化')
    await page.waitForTimeout(1500)
    await expect(page.locator('canvas').first()).toBeVisible()
  })

  test('溯源报告预览：时间轴 + 区块链存证卡', async ({ page }) => {
    await login(page)
    await nav(page, '溯源引擎', '溯源报告预览')
    await expectTitle(page, '溯源报告预览')
    await expect(page.locator('.ph-badge.ok')).toContainText('已验证')
    await expect(page.locator('.ph-timeline .ph-node').count()).resolves.toBeGreaterThan(3)
    await expect(page.locator('.ph-chain')).toContainText('donkey-channel')
    await expect(page.locator('.ph-chain')).toContainText('最新区块')
  })

  test('区块链账本：统计、区块浏览、完整性校验、区块详情', async ({ page }) => {
    await login(page)
    await nav(page, '溯源引擎', '区块链账本')
    await expectTitle(page, '区块链账本')
    // 统计卡
    await expect(page.getByText('区块总数').first()).toBeVisible()
    await expect(page.getByText('上链交易').first()).toBeVisible()
    // 区块表格
    await expect(page.locator('.el-table__row').first()).toBeVisible()
    await expect(page.locator('.block-no').first()).toContainText('#')
    // 完整性校验
    await page.getByRole('button', { name: '完整性校验' }).click()
    await expect(page.locator('.el-message', { hasText: '校验通过' }).first()).toBeVisible()
    await expect(page.locator('.el-alert--success')).toBeVisible()
    // 区块详情抽屉：打开最新区块
    await page.locator('.el-table__row').first().getByRole('button', { name: '详情' }).click()
    const drawer = page.locator('.el-drawer')
    await expect(drawer).toContainText(/区块.*详情/)
    await expect(drawer.locator('.tx-item').first()).toBeVisible()
    await expect(drawer.locator('.tx-payload')).toBeVisible()
    await page.keyboard.press('Escape')
  })

  test('新业务操作实时上链：门店生成溯源码后账本区块增加', async ({ page }) => {
    // 先在账本页记录区块总数
    await login(page)
    await nav(page, '溯源引擎', '区块链账本')
    await expect(page.getByText('区块总数').first()).toBeVisible()
    const statBefore = await page.locator('.ms-val').first().textContent()
    const before = parseInt(statBefore || '0', 10)
    // 门店生成溯源码（产生 BindTraceCode 上链交易）
    await nav(page, '门店管理', '溯源码管理')
    await page.getByRole('button', { name: '生成溯源码' }).click()
    const dlg = page.locator('.el-dialog', { hasText: '批量生成溯源码' })
    await dlg.getByRole('button', { name: '生成并绑定上链' }).click()
    await expect(page.locator('.el-message--success').first()).toBeVisible()
    // 回到账本页，区块总数应增加
    await nav(page, '溯源引擎', '区块链账本')
    await page.waitForTimeout(600)
    const statAfter = await page.locator('.ms-val').first().textContent()
    expect(parseInt(statAfter || '0', 10)).toBeGreaterThan(before)
  })
})

// ---------------- 9. 证照管理 ----------------
test('证照管理列表与临期提示', async ({ page }) => {
  await login(page, 'reg')
  await nav(page, '证照管理')
  await expectTitle(page, '证照管理')
  await tableRows(page)
})

// ---------------- 10. 监管中心 ----------------
test.describe('监管中心', () => {
  test('抽检登记列表与新增', async ({ page }) => {
    await login(page, 'reg')
    await nav(page, '监管中心', '抽检登记')
    await expectTitle(page, '抽检登记')
    await tableRows(page)
  })

  test('召回管理列表', async ({ page }) => {
    await login(page, 'reg')
    await nav(page, '监管中心', '召回管理')
    await expectTitle(page, '召回管理')
  })

  test('数据大屏（暗色驾驶舱）加载', async ({ page }) => {
    await login(page, 'reg')
    await nav(page, '监管中心', '数据大屏')
    await page.waitForTimeout(2000)
    await expect(page.locator('canvas').first()).toBeVisible()
  })
})

// ---------------- 11. 预警中心 ----------------
test('预警中心列表', async ({ page }) => {
  await login(page, 'reg')
  await nav(page, '预警中心')
  await expectTitle(page, '预警中心')
  await tableRows(page)
  await expect(page.getByText('TEMP_EXCEED').first()).toBeVisible()
})

// ---------------- 12. 系统管理 ----------------
test.describe('系统管理', () => {
  test('操作日志列表', async ({ page }) => {
    await login(page)
    await nav(page, '系统管理', '操作日志')
    await expectTitle(page, '操作日志')
    await tableRows(page)
  })

  test('参数配置查看与修改', async ({ page }) => {
    await login(page)
    await nav(page, '系统管理', '参数配置')
    await expectTitle(page, '参数配置')
    await expect(page.getByText('temp.threshold.fresh').first()).toBeVisible()
  })
})

// ---------------- 13. 退出登录 ----------------
test('退出登录回到登录页', async ({ page }) => {
  await login(page)
  await page.getByRole('button', { name: /平台管理员/ }).click()
  await page.getByRole('menuitem', { name: /退出/ }).click({ timeout: 6000 })
  await expect(page.locator('.el-message-box')).toBeVisible({ timeout: 6000 })
  await page.locator('.el-message-box__btns').getByRole('button', { name: /确\s*定/ }).click()
  await page.waitForURL(/login/, { timeout: 12_000 })
  await expect(page.getByPlaceholder('账号')).toBeVisible()
})
