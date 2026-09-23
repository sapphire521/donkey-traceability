# 驴链 DonkeyTrace · 驴肉火烧区块链溯源管理系统（管理端 / admin-web）

基于 **Vue 3 + TypeScript + Vite + Element Plus** 的三端合一管理后台（运营 / 企业 / 监管按角色动态路由）。
设计遵循文档 `docs/产品开发文档.md`，视觉方向由 **ui-ux-pro-max** 技能确定：暗色石板侧栏 + 亮色内容区的专业管理壳、信任绿 `#16A34A` 品牌色、Fira 数据字体、Stagger 入场动效，监管驾驶舱 / 大屏独立暗色主题。

> 当前为**前端独立可运行演示版**：通过内置 Mock 服务端（内存数据 + 模拟延迟）完整呈现所有页面与交互，无需启动 Fabric / Spring Boot 后端即可体验。

## 快速开始

```bash
cd admin-web
npm install
npm run dev        # http://localhost:5173
# 生产构建
npm run build      # 产物在 dist/
npm run preview    # 预览构建产物
```

## 演示账号（密码均为 123456）

| 账号 | 角色 | 可见模块 |
|------|------|---------|
| `admin` | 平台管理员 | 全部（含组织/用户/系统/大屏） |
| `farmer` | 养殖员 | 驴只档案、证照、预警 |
| `plant` | 加工员 | 屠宰/加工批次、溯源 |
| `qc` | 质检员 | 出厂检验、溯源 |
| `logi` | 调度员 | 冷链运输、溯源 |
| `shop` | 门店店员 | 库存、产出、溯源码、溯源 |
| `reg` | 监管员 | 抽检、召回、大屏、预警 |

登录页提供一键填充演示账号。

## 目录结构

```
admin-web/
├── src/
│   ├── main.ts / App.vue
│   ├── styles/index.css        # 设计系统：主题变量 / 动效 / Element Plus 覆盖
│   ├── types.ts                # 领域类型（对齐文档 §8/§9/§10）
│   ├── utils/                  # request(请求) / auth(鉴权) / format(状态枚举)
│   ├── mock/                   # db(演示数据) + server(Mini REST 路由 + 聚合)
│   ├── stores/                 # pinia: user / app
│   ├── router/                 # 路由 + 角色可见性守卫
│   ├── layout/                 # BasicLayout + Sidebar + Navbar
│   ├── components/             # StatCard / EChart
│   └── views/                  # 各业务模块页面
│       ├── login/ dashboard/ platform/ farm/ plant/ logistics/
│       ├── shop/ trace/ regulator/ system/ error/
```

## 接入真实后端

`src/utils/request.ts` 是唯一请求出口。将 `mockServer(...)` 替换为 `axios`/`fetch` 调用即可对接文档 §10 的 REST 接口（字段与错误码前缀 `ERR_*` 已对齐）：
- 登录：`POST /auth/login` 返回 accessToken / 用户信息 / `permissions` / `menus`
- 其余接口与 §10.2 的 42 个端点一一对应，响应体为 `{ code, message, data, traceId }`
- 公开接口：`/auth/login`、`/trace/codes/{code}`

## 已覆盖模块

组织/用户、驴只档案（含上链事件时间轴）、屠宰/加工/成品批次、出厂检验、冷链运输（温湿度记录/签收）、门店库存/产出/溯源码、批次树可视化、溯源报告预览、证照、抽检、召回、预警中心、监管数据大屏、操作日志、参数配置。
