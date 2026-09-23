# -*- coding: utf-8 -*-
"""
生成 donkey_trace 数据库初始化脚本 database.sql（MySQL 8, utf8mb4）
- 建库建表 + 每表 50+ 条保定本地化假数据
- 业务数据保持引用一致: 屠宰批次引用真实耳标, 加工/成品批次挂父子链,
  运输单/产出记录/溯源码引用成品批次, 预警引用异常温湿度记录与临期证照
- chain_block 不预置数据: 后端首次启动时按统一规则回放业务数据上链(保证哈希一致)
用法: python gen_sql.py  ->  输出 ../database/database.sql
"""
import os, random, json, bcrypt

random.seed(20260918)

OUT = os.path.join(os.path.dirname(__file__), '..', 'database', 'database.sql')
os.makedirs(os.path.dirname(OUT), exist_ok=True)

TODAY = '2026-09-18'

# ---------------- 工具 ----------------
def dt(d, hm='09:00:00'):
    return f"{d} {hm}"

def rand_date(start_d, end_d):
    import datetime
    a = datetime.date.fromisoformat(start_d)
    b = datetime.date.fromisoformat(end_d)
    n = (b - a).days
    return (a + datetime.timedelta(days=random.randint(0, n))).isoformat()

def plus_days(d, n):
    import datetime
    return (datetime.date.fromisoformat(d) + datetime.timedelta(days=n)).isoformat()

def esc(s):
    if s is None:
        return 'NULL'
    if isinstance(s, (int, float)):
        return str(s)
    return "'" + str(s).replace("\\", "\\\\").replace("'", "\\'") + "'"

def jarr(lst):
    return json.dumps(lst, ensure_ascii=False, separators=(',', ':'))

def jobj(d):
    return json.dumps(d, ensure_ascii=False, separators=(',', ':'))

TX_SEQ = [1000]
def txid(prefix='tx'):
    TX_SEQ[0] += random.randint(1, 7)
    return f"0x{prefix}{TX_SEQ[0]:06x}"

# ---------------- 组织（50 家，保定各县区） ----------------
COUNTIES = ['唐县', '易县', '徐水区', '莲池区', '竞秀区', '涞水县', '定兴县', '顺平县',
            '望都县', '博野县', '蠡县', '高阳县', '安国市', '涿州市', '清苑区', '满城区',
            '阜平县', '涞源县']
FARM_SUFFIX = ['振海驴业养殖场', '兴牧驴业养殖场', '德顺驴业合作社', '牧原驴业养殖基地',
               '绿源肉驴养殖场', '太行黑驴养殖场', '富康驴业', '顺发驴养殖场',
               '山川牧驴场', '康达驴业合作社']
PLANT_SUFFIX = ['漕河驴肉加工厂', '驴福源肉制品加工厂', '老马号驴肉制品厂', '汇康肉类加工厂',
                '府河驴肉加工厂', '恒信肉业加工厂']
LOGI_SUFFIX = ['保运冷链物流', '速达冷链运输', '汇通冷链物流', '冀中冷链货运']
SHOP_SUFFIX = ['驴火·旗舰总店', '漕河驴火店', '老字号驴肉火烧店', '驴香居驴火店',
               '河间驴火分店', '驴火·广场店', '驴火·步行街店']
REGU_SUFFIX = ['畜禽产品质量安全监督中心', '市场监督管理局', '农业农村局执法大队']

orgs = []
def add_org(code, name, otype, msp, contact, region):
    orgs.append({
        'org_code': code, 'org_name': name, 'org_type': otype, 'msp_id': msp,
        'contact': contact, 'phone': '138' + str(random.randint(10000000, 99999999)),
        'region': region, 'status': 1 if random.random() > 0.06 else 0,
        'create_time': dt(rand_date('2026-01-02', '2026-06-30'))
    })

add_org('PLAT001', '驴链平台运营方', 'PLATFORM', 'OrgPlatformMSP', '王运营', '河北·保定')
add_org('REGU001', '保定市畜禽产品质量安全监督中心', 'REGULATOR', 'OrgRegulatorMSP', '郑监管', '河北·保定')
seq = {'FARM': 1, 'PLANT': 1, 'LOGISTICS': 1, 'SHOP': 1, 'REGULATOR': 1}
surnames = ['李', '王', '张', '刘', '陈', '杨', '赵', '孙', '周', '吴', '郑', '马', '田', '崔']
def contact_name():
    return random.choice(surnames) + random.choice(['场长', '厂长', '经理', '主管', '师傅', '店长', '专员'])
while len(orgs) < 50:
    t = random.choices(['FARM', 'PLANT', 'LOGISTICS', 'SHOP', 'REGULATOR'],
                       weights=[0.42, 0.2, 0.1, 0.2, 0.08])[0]
    county = random.choice(COUNTIES)
    if t == 'FARM':
        seq['FARM'] += 1
        add_org(f"FARM{seq['FARM']:03d}", f"{county}{random.choice(FARM_SUFFIX)}", 'FARM',
                'OrgFarmMSP', contact_name(), f"河北·{county}")
    elif t == 'PLANT':
        seq['PLANT'] += 1
        add_org(f"PLANT{seq['PLANT']:03d}", f"{county}{random.choice(PLANT_SUFFIX)}", 'PLANT',
                'OrgPlantMSP', contact_name(), f"河北·{county}")
    elif t == 'LOGISTICS':
        seq['LOGISTICS'] += 1
        add_org(f"LOGI{seq['LOGISTICS']:03d}", f"{county}{random.choice(LOGI_SUFFIX)}", 'LOGISTICS',
                'OrgLogisticsMSP', contact_name(), f"河北·{county}")
    elif t == 'SHOP':
        seq['SHOP'] += 1
        add_org(f"SHOP{seq['SHOP']:03d}", f"{county}{random.choice(SHOP_SUFFIX)}", 'SHOP',
                'OrgShopMSP', contact_name(), f"河北·{county}")
    else:
        seq['REGULATOR'] += 1
        add_org(f"REGU{seq['REGULATOR']:03d}", f"{county}{random.choice(REGU_SUFFIX)}", 'REGULATOR',
                'OrgRegulatorMSP', contact_name(), f"河北·{county}")

def orgs_by(t):
    return [o for o in orgs if o['org_type'] == t]
farms, plants, logistics, shops, regulators = orgs_by('FARM'), orgs_by('PLANT'), orgs_by('LOGISTICS'), orgs_by('SHOP'), orgs_by('REGULATOR')
org_by_code = {o['org_code']: o for o in orgs}

# ---------------- 用户（55 个，演示密码 123456） ----------------
BCRYPT_123456 = bcrypt.hashpw(b'123456', bcrypt.gensalt(rounds=10, prefix=b'2a')).decode()
ROLE_MAP = [
    ('PLATFORM_ADMIN', '平台管理员'), ('FARM_OPERATOR', '养殖员'), ('FARM_MANAGER', '养殖场长'),
    ('PLANT_OPERATOR', '加工员'), ('PLANT_QC', '质检员'), ('LOGISTICS_DISPATCHER', '调度员'),
    ('LOGISTICS_DRIVER', '司机'), ('SHOP_KEEPER', '门店店员'), ('REGULATOR_OFFICER', '监管员'),
    ('REGULATOR_ADMIN', '监管管理员'),
]
ROLE_BY_ORG = {'PLATFORM': ['PLATFORM_ADMIN'], 'FARM': ['FARM_OPERATOR', 'FARM_MANAGER'],
               'PLANT': ['PLANT_OPERATOR', 'PLANT_QC'], 'LOGISTICS': ['LOGISTICS_DISPATCHER', 'LOGISTICS_DRIVER'],
               'SHOP': ['SHOP_KEEPER'], 'REGULATOR': ['REGULATOR_OFFICER', 'REGULATOR_ADMIN']}
users = []
def add_user(org, username, role, real_name):
    users.append({
        'org_id': None, 'org_code': org['org_code'], 'username': username, 'password': BCRYPT_123456,
        'real_name': real_name, 'phone': '139' + str(random.randint(10000000, 99999999)),
        'role_code': role, 'status': 1 if random.random() > 0.04 else 0,
        'last_login_time': dt(rand_date('2026-09-10', '2026-09-18'),
                              f"{random.randint(7,21):02d}:{random.randint(0,59):02d}:00")
    })
add_user(org_by_code['PLAT001'], 'admin', 'PLATFORM_ADMIN', '平台管理员')
add_user(org_by_code['REGU001'], 'reg', 'REGULATOR_OFFICER', '监管员小郑')
i = 1
for o in orgs:
    if o['org_type'] == 'PLATFORM':
        continue
    roles = ROLE_BY_ORG[o['org_type']]
    for r in roles:
        i += 1
        uname = o['org_code'].lower().replace('farm', 'f').replace('plant', 'p') \
                 .replace('logi', 'l').replace('shop', 's').replace('regu', 'r') + f"{i:02d}"
        add_user(o, uname, r, random.choice(surnames) + ROLE_MAP[[x[0] for x in ROLE_MAP].index(r)][1])
while len(users) < 55:
    i += 1
    o = random.choice(orgs[1:])
    r = random.choice(ROLE_BY_ORG[o['org_type']])
    add_user(o, f"user{i:03d}", r, random.choice(surnames) + '专员')

# 演示固定账号（与前端/测试约定: farmer/plant/qc/logi/shop/reg/admin）
DEMO_ACCOUNTS = [
    (farms[0], 'farmer', 'FARM_OPERATOR', '养殖员小李'),
    (plants[0], 'plant', 'PLANT_OPERATOR', '加工员小孙'),
    (plants[0], 'qc', 'PLANT_QC', '质检员小吴'),
    (logistics[0], 'logi', 'LOGISTICS_DISPATCHER', '调度员小周'),
    (shops[0], 'shop', 'SHOP_KEEPER', '店员小钱'),
]
existing_names = {u['username'] for u in users}
for org, uname, role, real in DEMO_ACCOUNTS:
    if uname not in existing_names:
        add_user(org, uname, role, real)
        existing_names.add(uname)
# 保证演示账号状态可用
for u in users:
    if u['username'] in ('admin', 'farmer', 'plant', 'qc', 'logi', 'shop', 'reg'):
        u['status'] = 1

# ---------------- 通知（50） ----------------
NOTICE_TITLES = [
    ('关于启用驴肉火烧溯源新码规则的通知', '自 2026-09-01 起，溯源码统一采用 DT+14 位格式。'),
    ('冷链温控阈值调整说明', '鲜驴肉/卤制品运输温度阈值维持 0~4℃，请各物流商严格执行。'),
    ('关于开展第三季度跨组织联合抽检的通知', '监管中心将于本月组织养殖、加工、门店三环节联合抽检。'),
    ('联盟链节点升级维护公告', 'orderer 节点将于周日凌晨 2:00-4:00 进行版本升级，期间短暂只读。'),
    ('新增两家养殖组织入盟公示', '经联盟治理委员会审议，新组织 MSP 注册完成，正式入网。'),
    ('成品批次保质期管理规范（试行）', '卤制驴肉成品默认保质期 7 天，超期自动冻结流转。'),
    ('关于规范耳标佩戴与建档的通知', '所有入栏驴只须 48 小时内完成电子耳标建档并上链。'),
]
notices = []
for i in range(50):
    t, c = random.choice(NOTICE_TITLES)
    if i < 2:
        t, c = NOTICE_TITLES[i]
    notices.append({'title': t, 'content': c, 'publisher': '平台运营方',
                    'time': dt(rand_date('2026-06-01', '2026-09-18'),
                               f"{random.randint(8,18):02d}:{random.randint(0,59):02d}:00")})

# ---------------- 参数（50 个） ----------------
CONFIG_DEFS = [
    ('temp.threshold.fresh', '0~4', '鲜驴肉温度阈值(℃)'), ('temp.threshold.frozen', '<=-18', '冷冻品温度阈值(℃)'),
    ('batch.expire.default.days', '7', '成品批次默认保质期(天)'), ('login.lock.attempts', '5', '登录失败锁定次数'),
    ('login.lock.minutes', '10', '登录锁定分钟数'), ('trace.code.length', '14', '溯源码随机位长度'),
    ('scan.limit.ip.per.min', '10', '单IP每分钟扫码上限'), ('humidity.threshold', '85', '运输湿度阈值(%)'),
    ('chain.block.tx.max', '1', '每区块最大交易数(演示)'), ('chain.channel', 'donkey-channel', '联盟链通道名'),
    ('report.qr.expire.minutes', '30', '溯源报告二维码有效期(分钟)'), ('slaughter.quarantine.required', 'true', '屠宰前是否强制检疫'),
    ('transport.upload.interval.min', '60', '温湿度上报间隔(分钟)'), ('alert.temp.auto.close.hours', '24', '温度预警自动关闭时限'),
    ('cert.expire.warn.days', '90', '证照临期提醒天数'), ('code.fake.scan.threshold', '50', '溯源码可疑扫描次数阈值'),
    ('output.oven.max.count', '400', '单炉最大出品数量'), ('file.storage.type', 'minio', '附件存储类型'),
    ('sms.notify.enabled', 'true', '短信通知开关'), ('recall.scope.auto.expand', 'true', '召回范围自动扩散开关'),
    ('batch.tree.max.depth', '5', '批次树最大展示深度'), ('org.audit.required', 'true', '新组织入盟是否需审核'),
    ('donkey.weight.warn.kg', '120', '出栏均重预警下限(kg)'), ('feed.record.daily.min', '2', '每日最少饲喂记录数'),
    ('immunize.interval.days', '180', '免疫接种间隔(天)'), ('quarantine.cert.valid.days', '3', '检疫证有效期(天)'),
    ('shop.receive.photo.required', 'true', '门店签收是否需拍照'), ('user.password.min.length', '6', '用户密码最小长度'),
    ('export.max.rows', '10000', '单次导出最大行数'), ('page.default.size', '20', '分页默认每页条数'),
    ('dashboard.refresh.seconds', '30', '监管大屏刷新间隔(秒)'), ('api.rate.limit.qps', '200', 'API 全局限流(QPS)'),
    ('log.keep.months', '12', '操作日志保留月数'), ('backup.cron', '0 2 * * *', '数据库备份计划'),
    ('minio.bucket', 'donkey-trace', '对象存储桶名'), ('fabric.network.profile', 'baoding-donkey.yaml', 'Fabric 网络配置文件'),
    ('fabric.chaincode', 'donkey-trace-cc', '链码名称'), ('fabric.chaincode.version', '1.2', '链码版本'),
    ('trace.report.share.enabled', 'true', '溯源报告分享开关'), ('complaint.auto.escalate.hours', '48', '投诉自动升级时限'),
    ('inspection.agency.default', '国家肉类食品质量监督检验中心', '默认抽检机构'), ('weight.unit', 'kg', '重量单位'),
    ('oven.temp.default', '220', '火烧炉默认炉温(℃)'), ('bake.time.default.seconds', '180', '火烧 default 烤制时长(秒)'),
    ('shop.close.inventory.lock', 'true', '打烊后是否锁定库存'), ('notice.publisher.default', '平台运营方', '公告默认发布方'),
    ('code.print.default.count', '1', '溯源码默认打印次数'), ('alert.level.escalate', 'WARN,ERROR', '预警升级级别链'),
    ('member.msp.prefix', 'Org', '联盟组织 MSP 前缀'), ('gateway.domain', 'trace.donkeychain.cn', '对外网关域名'),
]
configs = [{'config_key': k, 'config_value': v, 'remark': r} for k, v, r in CONFIG_DEFS[:50]]

# ---------------- 驴只（60 头） ----------------
BREEDS = ['德州驴', '关中驴', '新疆驴', '晋南驴', '杂交']
donkeys = []
for i in range(60):
    farm = random.choice(farms)
    d = rand_date('2025-01-05', '2025-12-20')
    status = random.choices(['RAISED', 'QUARANTINED', 'SLAUGHTERED'], weights=[0.55, 0.2, 0.25])[0]
    donkeys.append({
        'ear_tag_id': f"E1309-2025-000{300 + i}",
        'breed': random.choice(BREEDS), 'gender': 'M' if i % 2 == 0 else 'F',
        'birth_date': d, 'org_code': farm['org_code'], 'org_name': farm['org_name'],
        'barn_no': f"{random.choice('ABCD')}-{random.randint(1, 8)}",
        'status': status, 'create_txid': txid('dk'),
        'photos': jarr(['minio://donkey/d%d.jpg' % (i * 2 + 1), 'minio://donkey/d%d.jpg' % (i * 2 + 2)]),
        'create_time': dt(d, '09:00:00')
    })
donkey_by_tag = {d['ear_tag_id']: d for d in donkeys}

# ---------------- 驴只事件（150+） ----------------
FEEDS = ['饲喂：牧草+豆粕混合料 2.5kg', '饲喂：秸秆青贮+精料补充 3.0kg', '饲喂：苜蓿草料 2.8kg', '饮水补盐： electrolyte 添加']
IMMS = ['免疫：破伤风类毒素 批号TT2026-03', '免疫：口蹄疫灭活疫苗 批号FMD2026-11', '驱虫：伊维菌素皮下注射']
donkey_events = []
for d in donkeys:
    for _ in range(2):
        donkey_events.append({'donkey_tag': d['ear_tag_id'], 'event_type': 'FEED',
                              'event_time': dt(rand_date('2026-03-01', '2026-08-31')),
                              'content': random.choice(FEEDS), 'detail_hash': None,
                              'tx_id': txid('fd'), 'block_no': None})
    donkey_events.append({'donkey_tag': d['ear_tag_id'], 'event_type': 'IMMUNIZE',
                          'event_time': dt(rand_date('2026-04-01', '2026-07-31')),
                          'content': random.choice(IMMS), 'detail_hash': None,
                          'tx_id': txid('im'), 'block_no': None})
    if d['status'] in ('QUARANTINED', 'SLAUGHTERED'):
        donkey_events.append({'donkey_tag': d['ear_tag_id'], 'event_type': 'QUARANTINE',
                              'event_time': dt(rand_date('2026-08-20', '2026-09-16'), '14:00:00'),
                              'content': f"出栏检疫合格 动物A证 No.130926{random.randint(1000, 9999)}",
                              'detail_hash': 'sha256:' + '%032x' % random.getrandbits(128),
                              'tx_id': txid('qt'), 'block_no': None})

# ---------------- 批次（60 个：屠宰 12 / 加工 20 / 成品 28） ----------------
batches = []
PRODUCT_NAMES = ['卤制驴肉(后腿)', '卤制驴肉(肋条)', '酱驴肉', '五香驴肉', '手撕驴肉']
slaughter_list = []
for i in range(12):
    d = rand_date('2026-08-22', '2026-09-17')
    plant = random.choice(plants)
    tags = random.sample([x['ear_tag_id'] for x in donkeys if x['status'] in ('QUARANTINED', 'SLAUGHTERED')], 3)
    b = {'batch_no': f"S-2026{d.replace('-', '')[4:]}-{i+1:03d}", 'batch_type': 'SLAUGHTER',
         'parent_nos': [], 'source_ear_tags': tags, 'org_code': plant['org_code'], 'org_name': plant['org_name'],
         'holder_code': plant['org_code'], 'holder_name': plant['org_name'],
         'product_name': '待分割胴体', 'weight_kg': round(random.uniform(220, 330), 1),
         'produce_date': d, 'expire_date': None, 'cert_hash': 'sha256:' + '%x' % random.getrandbits(32),
         'status': 'CONSUMED', 'create_txid': txid('sl'), 'qc_report_uri': None,
         'create_time': dt(d, '09:00:00')}
    batches.append(b); slaughter_list.append(b)
    for t in tags:
        donkey_by_tag[t]['status'] = 'SLAUGHTERED'
process_list = []
for i in range(20):
    parent = random.choice(slaughter_list)
    d = plus_days(parent['produce_date'], random.randint(0, 1))
    b = {'batch_no': f"P-2026{d.replace('-', '')[4:]}-{i+1:03d}", 'batch_type': 'PROCESS',
         'parent_nos': [parent['batch_no']], 'source_ear_tags': [],
         'org_code': parent['org_code'], 'org_name': parent['org_name'],
         'holder_code': parent['org_code'], 'holder_name': parent['org_name'],
         'product_name': random.choice(PRODUCT_NAMES), 'weight_kg': round(parent['weight_kg'] * random.uniform(0.35, 0.5), 1),
         'produce_date': d, 'expire_date': None, 'cert_hash': 'sha256:' + '%x' % random.getrandbits(32),
         'status': 'CONSUMED', 'create_txid': txid('pr'), 'qc_report_uri': None,
         'create_time': dt(d, '10:00:00')}
    batches.append(b); process_list.append(b)
product_list = []
for i in range(28):
    parent = random.choice(process_list)
    d = plus_days(parent['produce_date'], random.randint(0, 1))
    shop = random.choice(shops)
    exp = plus_days(d, 7)
    st = 'IN_STORE' if exp >= '2026-09-18' else random.choice(['IN_STORE', 'EXPIRED'])
    b = {'batch_no': f"F-2026{d.replace('-', '')[4:]}-{i+1:03d}", 'batch_type': 'PRODUCT',
         'parent_nos': [parent['batch_no']], 'source_ear_tags': [],
         'org_code': parent['org_code'], 'org_name': parent['org_name'],
         'holder_code': shop['org_code'], 'holder_name': shop['org_name'],
         'product_name': f"驴肉火烧·{parent['product_name']}", 'weight_kg': round(parent['weight_kg'] * random.uniform(0.6, 0.9), 1),
         'produce_date': d, 'expire_date': exp, 'cert_hash': 'sha256:' + '%x' % random.getrandbits(32),
         'status': st, 'create_txid': txid('pr2'), 'qc_report_uri': f"minio://qc/f{i+1}.pdf",
         'create_time': dt(d, '11:00:00')}
    batches.append(b); product_list.append(b)

# 演示约定: 首个成品批次固定为 F-20260911-001 (测试/演示码 DT9F3K2A8Q7M5X1 引用)
product_list[0]['batch_no'] = 'F-20260911-001'

# ---------------- 批次事件（200+） ----------------
batch_events = []
def add_be(batch, etype, operator, hm, summary):
    batch_events.append({'batch_no': batch['batch_no'], 'event_type': etype,
                         'org_name': batch['org_name'], 'operator': operator,
                         'event_time': dt(batch['produce_date'], hm), 'summary': summary,
                         'tx_id': txid('be'), 'block_no': None})
for b in batches:
    add_be(b, 'BATCH_CREATE', random.choice(['小孙', '孙厂长', '加工员小王']), '09:05:00',
           f"创建{b['batch_type']}批次 {b['batch_no']}")
for b in product_list:
    add_be(b, 'QC_PASS', random.choice(['小吴', '质检员小刘']), '11:20:00', f"出厂检验合格 {b['product_name']}")
    add_be(b, 'TRANSFER', random.choice(['小周', '调度员小赵']), '13:00:00', f"发往 {b['holder_name']}")
    add_be(b, 'RECEIVE', '店员', '17:30:00', f"{b['holder_name']} 签收入库")

# ---------------- 运输单（60） + 温湿度记录（240+） ----------------
transport_orders, transport_records = [], []
PLATES = ['冀F·6X8K2', '冀F·9L2M5', '冀F·3T7Q1', '冀F·5R8N3', '冀F·7K4P9']
for i, b in enumerate(product_list):
    d = b['produce_date']
    lg = random.choice(logistics)
    st = 'RECEIVED'
    if random.random() < 0.12:
        st = 'IN_TRANSIT' if b['expire_date'] >= '2026-09-18' else 'RECEIVED'
    o = {'transport_no': f"T-2026{d.replace('-', '')[4:]}-{i+1:03d}", 'batch_no': b['batch_no'],
         'from_code': b['org_code'], 'from_name': b['org_name'],
         'to_code': b['holder_code'], 'to_name': b['holder_name'],
         'vehicle_no': random.choice(PLATES), 'driver_name': random.choice(['吴师傅', '郑师傅', '马师傅', '崔师傅']),
         'driver_phone': '137' + str(random.randint(10000000, 99999999)),
         'depart_time': dt(d, '13:00:00'), 'expect_arrive_time': dt(d, '17:00:00'),
         'actual_arrive_time': dt(d, f"{random.randint(16, 18)}:{random.randint(0,59):02d}:00") if st == 'RECEIVED' else None,
         'status': st, 'file_uri': f"minio://transport/t{i+1}.csv" if st == 'RECEIVED' else None,
         'file_hash': 'sha256:' + '%x' % random.getrandbits(32) if st == 'RECEIVED' else None,
         'tx_id': txid('tr'), 'create_time': dt(d, '13:00:00')}
    transport_orders.append(o)
    n_rec = random.randint(4, 6)
    has_abnormal = random.random() < 0.2
    ab_idx = random.randrange(n_rec) if has_abnormal else -1
    for h in range(n_rec):
        temp = round(random.uniform(0.8, 3.6), 1)
        if h == ab_idx:
            temp = round(random.uniform(4.4, 6.2), 1)
        transport_records.append({'transport_no': o['transport_no'],
                                  'record_time': dt(d, f"{13 + h}:{(10 + h * 7) % 60:02d}:00"),
                                  'temperature': temp, 'humidity': random.randint(70, 88),
                                  'abnormal': 1 if h == ab_idx else 0})

# 补充历史运输单至 55 条（旧批次多次运输/补录）
ext = 0
while len(transport_orders) < 55:
    ext += 1
    b = random.choice(product_list)
    d = rand_date('2026-08-22', '2026-09-15')
    lg = random.choice(logistics)
    o = {'transport_no': f"T-H2026{d.replace('-', '')[4:]}-{ext:03d}", 'batch_no': b['batch_no'],
         'from_code': b['org_code'], 'from_name': b['org_name'],
         'to_code': b['holder_code'], 'to_name': b['holder_name'],
         'vehicle_no': random.choice(PLATES), 'driver_name': random.choice(['吴师傅', '郑师傅', '马师傅', '崔师傅']),
         'driver_phone': '137' + str(random.randint(10000000, 99999999)),
         'depart_time': dt(d, '13:00:00'), 'expect_arrive_time': dt(d, '17:00:00'),
         'actual_arrive_time': dt(d, f"{random.randint(16, 18)}:{random.randint(0, 59):02d}:00"),
         'status': 'RECEIVED', 'file_uri': f"minio://transport/h{ext}.csv",
         'file_hash': 'sha256:' + '%x' % random.getrandbits(32),
         'tx_id': txid('tr'), 'create_time': dt(d, '13:00:00')}
    transport_orders.append(o)
    n_rec = random.randint(4, 6)
    for h in range(n_rec):
        transport_records.append({'transport_no': o['transport_no'],
                                  'record_time': dt(d, f"{13 + h}:{(10 + h * 7) % 60:02d}:00"),
                                  'temperature': round(random.uniform(0.8, 3.8), 1),
                                  'humidity': random.randint(70, 86), 'abnormal': 0})

# ---------------- 产出记录（60） ----------------
outputs = []
for i in range(60):
    b = random.choice([x for x in product_list if x['status'] == 'IN_STORE'] or product_list)
    outputs.append({'batch_no': b['batch_no'], 'shop_code': b['holder_code'], 'shop_name': b['holder_name'],
                    'output_time': dt(b['produce_date'], f"{random.randint(17, 19)}:{random.randint(0, 59):02d}:00"),
                    'oven_no': f"{random.randint(1, 5)}号炉", 'chef': random.choice(['马师傅', '马师傅', '杨师傅', '崔师傅']),
                    'qty': random.randint(80, 260), 'used_weight': round(random.uniform(10, 35), 1),
                    'create_time': dt(b['produce_date'], '18:20:00')})

# ---------------- 溯源码（120+） ----------------
CODE_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
def gen_code():
    return 'DT' + ''.join(random.choice(CODE_CHARS) for _ in range(14))
trace_codes, scan_logs = [], []
for i, o in enumerate(outputs):
    for k in range(2):
        c = {'code': gen_code(), 'batch_no': o['batch_no'], 'shop_name': o['shop_name'],
             'output_id': i + 1, 'status': 'BOUND', 'bind_txid': txid('bc'),
             'print_count': 1, 'scan_times': random.randint(0, 9),
             'create_time': dt(random.choice([b for b in product_list if b['batch_no'] == o['batch_no']])['produce_date'], '18:25:00')}
        trace_codes.append(c)
        for s in range(random.randint(0, 2)):
            scan_logs.append({'code': c['code'], 'ip': f"223.104.{random.randint(1, 250)}.{random.randint(1, 250)}",
                              'region': random.choices(['河北·保定', '北京', '天津', '河北·石家庄'], weights=[0.7, 0.12, 0.1, 0.08])[0],
                              'ua': random.choice(['WeChat/8.0', 'WeChat/8.0', 'Alipay/10.5', 'Browser/Chrome']),
                              'scan_time': dt(rand_date('2026-09-05', '2026-09-18'), f"{random.randint(8, 22):02d}:{random.randint(0, 59):02d}:00"),
                              'risk_flag': 0})
# 固定演示码（与前端/测试约定）: 绑定演示批次 F-20260911-001
demo_code = 'DT9F3K2A8Q7M5X1'
trace_codes.append({'code': demo_code, 'batch_no': product_list[0]['batch_no'],
                    'shop_name': product_list[0]['holder_name'], 'output_id': 1, 'status': 'BOUND',
                    'bind_txid': txid('bc'), 'print_count': 1, 'scan_times': 3,
                    'create_time': dt('2026-09-11', '18:25:00')})
for _ in range(3):
    scan_logs.append({'code': demo_code, 'ip': '223.104.3.12', 'region': '河北·保定', 'ua': 'WeChat/8.0',
                      'scan_time': dt('2026-09-17', '19:30:00'), 'risk_flag': 0})

# ---------------- 证照（55） ----------------
CERT_TYPES = ['动物防疫条件合格证', '食品生产许可证', '食品经营许可证', '畜禽养殖代码证', '道路运输经营许可证']
CERT_PREFIX = {'动物防疫条件合格证': '动防证', '食品生产许可证': 'SC', '食品经营许可证': 'JY',
               '畜禽养殖代码证': 'YZ', '道路运输经营许可证': 'YS'}
certs = []
for i in range(55):
    o = random.choice(orgs[1:])
    ct = random.choice(list(CERT_PREFIX.keys()))
    issue = rand_date('2024-01-10', '2026-06-30')
    expire = plus_days(issue, random.choice([365 * 2, 365 * 3, 400]))
    certs.append({'org_code': o['org_code'], 'org_name': o['org_name'], 'cert_type': ct,
                  'cert_no': f"{CERT_PREFIX[ct]}({issue[:4]})第{i+1:03d}号",
                  'issue_date': issue, 'expire_date': expire,
                  'file_uri': f"minio://cert/c{i+1}.pdf", 'file_hash': 'sha256:' + '%x' % random.getrandbits(32),
                  'audit_status': random.choices(['PASSED', 'PENDING', 'NONE'], weights=[0.85, 0.1, 0.05])[0],
                  'status': 1})

# ---------------- 抽检（55） ----------------
AGENCIES = ['国家肉类食品质量监督检验中心', '河北省产品质量监督检验研究院', '保定市食品药品检验所']
INSPECT_ITEMS = ['瘦肉精/水分/兽残快检/掺假物种鉴定', '菌落总数/大肠菌群/致病菌', '兽药残留/重金属/亚硝酸盐']
inspections = []
for i in range(55):
    b = random.choice(product_list)
    result = random.choices(['PASS', 'FAIL'], weights=[0.93, 0.07])[0]
    inspections.append({'batch_no': b['batch_no'], 'org_name': b['holder_name'],
                        'agency': random.choice(AGENCIES), 'items': random.choice(INSPECT_ITEMS),
                        'result': result, 'report_uri': f"minio://insp/i{i+1}.pdf",
                        'report_hash': 'sha256:' + '%x' % random.getrandbits(32),
                        'tx_id': txid('in'), 'create_time': dt(rand_date('2026-08-25', '2026-09-18'), '10:00:00')})

# ---------------- 召回（50） ----------------
REASONS = ['出厂检验报告造假嫌疑', '抽检不合格：检出违禁兽药残留', '冷链运输断链超时', '消费者投诉掺假核实', '邻近批次交叉污染风险']
recalls = []
for i in range(50):
    b = random.choice(product_list)
    recalls.append({'batch_no': b['batch_no'], 'reason': random.choice(REASONS),
                    'scope_json': jobj({'downstreamBatches': [], 'shops': [b['holder_name']],
                                        'codes': random.randint(5, 40), 'scanned': random.randint(3, 30)}),
                    'status': random.choices(['RECALLING', 'COMPLETED', 'CANCELLED'], weights=[0.25, 0.65, 0.1])[0],
                    'initiator': random.choice(['监管员小郑', '监管员小李', '监管管理员']),
                    'create_time': dt(rand_date('2026-08-20', '2026-09-18'), '15:00:00')})

# ---------------- 投诉（50） ----------------
CATEGORIES = ['怀疑非驴肉', '口感异常', '包装破损', '过期产品', '扫码失败', '疑似变质']
complaints = []
for i in range(50):
    c = random.choice(trace_codes)
    complaints.append({'code': c['code'], 'category': random.choice(CATEGORIES),
                       'content': random.choice(['口感不对，怀疑不是真驴肉', '扫码打不开溯源页面', '肉质发酸疑似变质',
                                                 '包装漏气，日期模糊', '味道与之前差异较大']),
                       'phone': '136' + str(random.randint(10000000, 99999999)),
                       'status': random.choices(['PENDING', 'PROCESSING', 'RESOLVED'], weights=[0.3, 0.3, 0.4])[0],
                       'create_time': dt(rand_date('2026-08-25', '2026-09-18'), f"{random.randint(9, 21):02d}:00:00")})

# ---------------- 预警（55） ----------------
alerts = []
abnormal_recs = [r for r in transport_records if r['abnormal'] == 1][:12]
for r in abnormal_recs:
    o = next(x for x in transport_orders if x['transport_no'] == r['transport_no'])
    alerts.append({'alert_type': 'TEMP_EXCEED', 'level': 'WARN', 'target_type': 'TRANSPORT',
                   'target_id': o['transport_no'],
                   'content': f"运输单 {o['transport_no']} 温度记录 {r['temperature']}℃ 超阈值(0~4℃)",
                   'status': random.choices(['OPEN', 'RESOLVED'], weights=[0.35, 0.65])[0],
                   'handler': None if random.random() < 0.35 else random.choice(['小郑', '小周']),
                   'handle_note': None, 'handle_time': None,
                   'create_time': r['record_time']})
for c in certs:
    if c['expire_date'] <= plus_days(TODAY, 90) and len([a for a in alerts if a['alert_type'] == 'CERT_EXPIRED']) < 12:
        alerts.append({'alert_type': 'CERT_EXPIRED', 'level': 'INFO', 'target_type': 'CERT',
                       'target_id': c['cert_no'],
                       'content': f"{c['cert_type']} 将于 {c['expire_date']} 到期",
                       'status': random.choices(['OPEN', 'RESOLVED'], weights=[0.4, 0.6])[0],
                       'handler': random.choice(['小郑', '王运营']), 'handle_note': '已提醒企业续期',
                       'handle_time': dt(rand_date('2026-09-01', '2026-09-17')), 'create_time': dt(rand_date('2026-08-28', '2026-09-15'), '09:00:00')})
for b in product_list:
    if b['status'] == 'EXPIRED' and len([a for a in alerts if a['alert_type'] == 'OVERDUE']) < 12:
        alerts.append({'alert_type': 'OVERDUE', 'level': 'WARN', 'target_type': 'BATCH',
                       'target_id': b['batch_no'], 'content': f"成品批次 {b['batch_no']} 已超过保质期，禁止流转",
                       'status': random.choices(['OPEN', 'RESOLVED'], weights=[0.5, 0.5])[0],
                       'handler': random.choice([None, '小郑']), 'handle_note': None,
                       'handle_time': None, 'create_time': dt(b['expire_date'], '09:00:00')})
    if b['expire_date'] >= '2026-09-18' and (b['expire_date'] <= plus_days(TODAY, 2)) \
            and len([a for a in alerts if a['alert_type'] == 'OVERDUE']) < 14:
        alerts.append({'alert_type': 'OVERDUE', 'level': 'WARN', 'target_type': 'BATCH',
                       'target_id': b['batch_no'], 'content': f"成品批次 {b['batch_no']} 剩余保质期不足30%",
                       'status': 'OPEN', 'handler': None, 'handle_note': None, 'handle_time': None,
                       'create_time': dt(TODAY, '09:00:00')})
while len(alerts) < 55:
    c = random.choice(trace_codes)
    alerts.append({'alert_type': 'FAKE_SCAN', 'level': 'INFO', 'target_type': 'CODE',
                   'target_id': c['code'],
                   'content': f"溯源码 {c['code']} 24h 内扫描 >50 次，标记为可疑",
                   'status': random.choices(['OPEN', 'RESOLVED'], weights=[0.3, 0.7])[0],
                   'handler': random.choice([None, '小郑']), 'handle_note': None,
                   'handle_time': None, 'create_time': dt(rand_date('2026-08-30', '2026-09-17'), '12:00:00')})

# ---------------- 操作日志（80） ----------------
mods = ['认证', '组织', '用户', '驴只', '批次', '运输', '门店', '溯源', '预警', '系统']
ops = ['登录', '查询', '新增', '导出', '审核', '修改', '删除']
operation_logs = []
for i in range(80):
    operation_logs.append({'user_id': (i % len(users)) + 1, 'user_name': random.choice(['平台管理员', '养殖员小李', '加工员小孙', '店员小钱', '监管员小郑']),
                           'module': mods[i % len(mods)], 'operation': ops[i % len(ops)],
                           'method': ['GET', 'POST', 'PUT'][i % 3], 'params': jobj({'id': i + 1}),
                           'result_code': 0, 'ip': f"192.168.1.{i % 50 + 2}", 'cost_ms': 20 + (i * 13) % 80,
                           'create_time': dt(rand_date('2026-09-06', '2026-09-18'), f"{random.randint(7, 22):02d}:{random.randint(0, 59):02d}:{random.randint(0, 59):02d}")})

# ================= 输出 SQL =================
TABLES = [
    ('sys_org', '''CREATE TABLE sys_org (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  org_code VARCHAR(32) NOT NULL UNIQUE,
  org_name VARCHAR(64) NOT NULL,
  org_type VARCHAR(16) NOT NULL COMMENT 'PLATFORM/FARM/PLANT/LOGISTICS/SHOP/REGULATOR',
  msp_id VARCHAR(32) NOT NULL,
  contact VARCHAR(32),
  phone VARCHAR(16),
  region VARCHAR(32),
  status TINYINT DEFAULT 1,
  create_time DATETIME
)''', [[o['org_code'], o['org_name'], o['org_type'], o['msp_id'], o['contact'], o['phone'], o['region'], o['status'], o['create_time']]
       for o in orgs],
     '(org_code, org_name, org_type, msp_id, contact, phone, region, status, create_time)'),

    ('sys_user', '''CREATE TABLE sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  org_id BIGINT,
  username VARCHAR(32) NOT NULL UNIQUE,
  password VARCHAR(80) NOT NULL,
  real_name VARCHAR(32),
  phone VARCHAR(16),
  role_code VARCHAR(32) NOT NULL,
  status TINYINT DEFAULT 1,
  last_login_time DATETIME
)''', None, '(org_id, username, password, real_name, phone, role_code, status, last_login_time)'),  # org_id 后补

    ('sys_notice', '''CREATE TABLE sys_notice (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(128) NOT NULL,
  content VARCHAR(512),
  publisher VARCHAR(32),
  time DATETIME
)''', [[n['title'], n['content'], n['publisher'], n['time']] for n in notices], '(title, content, publisher, time)'),

    ('sys_config', '''CREATE TABLE sys_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  config_key VARCHAR(64) NOT NULL UNIQUE,
  config_value VARCHAR(128),
  remark VARCHAR(128)
)''', [[c['config_key'], c['config_value'], c['remark']] for c in configs], '(config_key, config_value, remark)'),

    ('biz_donkey', '''CREATE TABLE biz_donkey (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  ear_tag_id VARCHAR(32) NOT NULL UNIQUE,
  breed VARCHAR(16),
  gender CHAR(1),
  birth_date DATE,
  org_id BIGINT,
  org_name VARCHAR(64),
  barn_no VARCHAR(16),
  status VARCHAR(16) DEFAULT 'RAISED',
  create_txid VARCHAR(40),
  photos JSON,
  create_time DATETIME
)''', None, '(ear_tag_id, breed, gender, birth_date, org_id, org_name, barn_no, status, create_txid, photos, create_time)'),

    ('biz_donkey_event', '''CREATE TABLE biz_donkey_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  donkey_id BIGINT,
  ear_tag_id VARCHAR(32),
  event_type VARCHAR(16) NOT NULL,
  event_time DATETIME,
  content VARCHAR(255),
  detail_hash VARCHAR(80),
  tx_id VARCHAR(40),
  block_no INT
)''', None, '(donkey_id, ear_tag_id, event_type, event_time, content, detail_hash, tx_id, block_no)'),

    ('biz_batch', '''CREATE TABLE biz_batch (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_no VARCHAR(32) NOT NULL UNIQUE,
  batch_type VARCHAR(16) NOT NULL COMMENT 'SLAUGHTER/PROCESS/PRODUCT',
  parent_nos JSON,
  source_ear_tags JSON,
  org_id BIGINT,
  org_name VARCHAR(64),
  holder_org_id BIGINT,
  holder_org_name VARCHAR(64),
  product_name VARCHAR(64),
  weight_kg DECIMAL(8,1),
  produce_date DATE,
  expire_date DATE,
  cert_hash VARCHAR(80),
  status VARCHAR(16),
  create_txid VARCHAR(40),
  qc_report_uri VARCHAR(128),
  create_time DATETIME,
  KEY idx_type (batch_type),
  KEY idx_holder (holder_org_name)
)''', None, '(batch_no, batch_type, parent_nos, source_ear_tags, org_id, org_name, holder_org_id, holder_org_name, product_name, weight_kg, produce_date, expire_date, cert_hash, status, create_txid, qc_report_uri, create_time)'),

    ('biz_batch_event', '''CREATE TABLE biz_batch_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_no VARCHAR(32) NOT NULL,
  event_type VARCHAR(16) NOT NULL,
  org_name VARCHAR(64),
  operator VARCHAR(32),
  event_time DATETIME,
  summary VARCHAR(255),
  tx_id VARCHAR(40),
  block_no INT,
  KEY idx_batch (batch_no)
)''', None, '(batch_no, event_type, org_name, operator, event_time, summary, tx_id, block_no)'),

    ('biz_transport_order', '''CREATE TABLE biz_transport_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  transport_no VARCHAR(32) NOT NULL UNIQUE,
  batch_no VARCHAR(32),
  from_org_id BIGINT, from_org_name VARCHAR(64),
  to_org_id BIGINT, to_org_name VARCHAR(64),
  vehicle_no VARCHAR(16),
  driver_name VARCHAR(16),
  driver_phone VARCHAR(16),
  depart_time DATETIME,
  expect_arrive_time DATETIME,
  actual_arrive_time DATETIME,
  status VARCHAR(16),
  file_uri VARCHAR(128),
  file_hash VARCHAR(80),
  tx_id VARCHAR(40),
  create_time DATETIME,
  KEY idx_batch (batch_no)
)''', None, '(transport_no, batch_no, from_org_id, from_org_name, to_org_id, to_org_name, vehicle_no, driver_name, driver_phone, depart_time, expect_arrive_time, actual_arrive_time, status, file_uri, file_hash, tx_id, create_time)'),

    ('biz_transport_record', '''CREATE TABLE biz_transport_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT,
  record_time DATETIME,
  temperature DECIMAL(4,1),
  humidity INT,
  abnormal TINYINT DEFAULT 0,
  KEY idx_order (order_id)
)''', None, '(order_id, record_time, temperature, humidity, abnormal)'),

    ('biz_output_record', '''CREATE TABLE biz_output_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_no VARCHAR(32),
  shop_org_id BIGINT,
  shop_org_name VARCHAR(64),
  output_time DATETIME,
  oven_no VARCHAR(16),
  chef VARCHAR(16),
  qty INT,
  used_weight DECIMAL(8,1),
  create_time DATETIME
)''', None, '(batch_no, shop_org_id, shop_org_name, output_time, oven_no, chef, qty, used_weight, create_time)'),

    ('biz_trace_code', '''CREATE TABLE biz_trace_code (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(20) NOT NULL UNIQUE,
  batch_no VARCHAR(32),
  shop_org_name VARCHAR(64),
  output_id BIGINT,
  status VARCHAR(16) DEFAULT 'BOUND',
  bind_txid VARCHAR(40),
  print_count INT DEFAULT 0,
  scan_times INT DEFAULT 0,
  create_time DATETIME,
  KEY idx_batch (batch_no)
)''', None, '(code, batch_no, shop_org_name, output_id, status, bind_txid, print_count, scan_times, create_time)'),

    ('biz_scan_log', '''CREATE TABLE biz_scan_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(20),
  ip VARCHAR(32),
  region VARCHAR(32),
  ua VARCHAR(32),
  scan_time DATETIME,
  risk_flag TINYINT DEFAULT 0,
  KEY idx_code (code)
)''', [[s['code'], s['ip'], s['region'], s['ua'], s['scan_time'], s['risk_flag']] for s in scan_logs], '(code, ip, region, ua, scan_time, risk_flag)'),

    ('biz_alert', '''CREATE TABLE biz_alert (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  alert_type VARCHAR(20),
  level VARCHAR(8),
  target_type VARCHAR(16),
  target_id VARCHAR(32),
  content VARCHAR(255),
  status VARCHAR(12) DEFAULT 'OPEN',
  handler VARCHAR(32),
  handle_note VARCHAR(128),
  handle_time DATETIME,
  create_time DATETIME
)''', None, '(alert_type, level, target_type, target_id, content, status, handler, handle_note, handle_time, create_time)'),

    ('biz_cert', '''CREATE TABLE biz_cert (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  org_id BIGINT,
  org_name VARCHAR(64),
  cert_type VARCHAR(32),
  cert_no VARCHAR(48),
  issue_date DATE,
  expire_date DATE,
  file_uri VARCHAR(128),
  file_hash VARCHAR(80),
  audit_status VARCHAR(12) DEFAULT 'NONE',
  status TINYINT DEFAULT 1
)''', None, '(org_id, org_name, cert_type, cert_no, issue_date, expire_date, file_uri, file_hash, audit_status, status)'),

    ('biz_inspection', '''CREATE TABLE biz_inspection (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_no VARCHAR(32),
  org_name VARCHAR(64),
  agency VARCHAR(64),
  items VARCHAR(128),
  result VARCHAR(8),
  report_uri VARCHAR(128),
  report_hash VARCHAR(80),
  tx_id VARCHAR(40),
  create_time DATETIME
)''', [[i['batch_no'], i['org_name'], i['agency'], i['items'], i['result'], i['report_uri'], i['report_hash'], i['tx_id'], i['create_time']] for i in inspections], '(batch_no, org_name, agency, items, result, report_uri, report_hash, tx_id, create_time)'),

    ('biz_recall', '''CREATE TABLE biz_recall (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_no VARCHAR(32),
  reason VARCHAR(255),
  scope_json JSON,
  status VARCHAR(16),
  initiator VARCHAR(32),
  create_time DATETIME
)''', [[r['batch_no'], r['reason'], r['scope_json'], r['status'], r['initiator'], r['create_time']] for r in recalls], '(batch_no, reason, scope_json, status, initiator, create_time)'),

    ('biz_complaint', '''CREATE TABLE biz_complaint (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(20),
  category VARCHAR(16),
  content VARCHAR(255),
  phone VARCHAR(16),
  status VARCHAR(12) DEFAULT 'PENDING',
  create_time DATETIME
)''', [[c['code'], c['category'], c['content'], c['phone'], c['status'], c['create_time']] for c in complaints], '(code, category, content, phone, status, create_time)'),

    ('chain_block', '''CREATE TABLE chain_block (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  block_no INT NOT NULL UNIQUE,
  prev_hash VARCHAR(80) NOT NULL,
  hash VARCHAR(80) NOT NULL,
  time DATETIME,
  tx_count INT DEFAULT 1,
  channel VARCHAR(32),
  tx_id VARCHAR(40) NOT NULL UNIQUE,
  op VARCHAR(48) NOT NULL,
  msp_id VARCHAR(32),
  org_name VARCHAR(64),
  payload TEXT COMMENT '原始交易载荷JSON文本(保持键序以便哈希重算)',
  KEY idx_op (op)
)''', [], None),

    ('sys_operation_log', '''CREATE TABLE sys_operation_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  user_name VARCHAR(32),
  module VARCHAR(16),
  operation VARCHAR(16),
  method VARCHAR(8),
  params VARCHAR(255),
  result_code INT,
  ip VARCHAR(32),
  cost_ms INT,
  create_time DATETIME
)''', [[l['user_id'], l['user_name'], l['module'], l['operation'], l['method'], l['params'], l['result_code'], l['ip'], l['cost_ms'], l['create_time']] for l in operation_logs], '(user_id, user_name, module, operation, method, params, result_code, ip, cost_ms, create_time)'),
]

# org_id / donkey.org_id / batch.org_id / cert.org_id 等在 SQL 里直接用子查询没必要;
# 我们在生成时直接用序号: 约定 org id = 插入顺序(1..N), 同理 donkey/batch/transport/output/trace_code。
org_idx = {o['org_code']: i + 1 for i, o in enumerate(orgs)}

def insert_rows(table, cols, rows):
    out = []
    for i in range(0, len(rows), 40):
        chunk = rows[i:i + 40]
        vals = ',\n  '.join('(' + ', '.join(esc(v) for v in r) + ')' for r in chunk)
        out.append(f"INSERT INTO {table} {cols} VALUES\n  {vals};")
    return '\n'.join(out)

sql = []
sql.append("""-- ============================================================
-- 驴肉火烧区块链溯源管理系统 数据库初始化脚本
-- MySQL 8.0+ / utf8mb4   生成时间: 2026-09-18
-- 用法: mysql -uroot -p < database.sql
-- 说明: chain_block 由后端首次启动时回放业务数据自动上链, 故此处不预置
-- ============================================================
CREATE DATABASE IF NOT EXISTS donkey_trace DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE donkey_trace;
""")

for name, ddl, rows, cols in TABLES:
    sql.append(f"-- ---------- {name} ----------\nDROP TABLE IF EXISTS {name};\n{ddl};\n")
    if rows:
        sql.append(insert_rows(name, cols, rows) + '\n')

# --- 依赖序号的数据: 用户/驴只/批次/运输/产出/溯源码/证照/预警 ---
user_rows = [[org_idx[u['org_code']], u['username'], u['password'], u['real_name'], u['phone'],
              u['role_code'], u['status'], u['last_login_time']] for u in users]
sql.append(insert_rows('sys_user', TABLES[1][3], user_rows) + '\n')

donkey_rows = [[d['ear_tag_id'], d['breed'], d['gender'], d['birth_date'], org_idx[d['org_code']],
                d['org_name'], d['barn_no'], d['status'], d['create_txid'], d['photos'], d['create_time']]
               for d in donkeys]
sql.append(insert_rows('biz_donkey', TABLES[4][3], donkey_rows) + '\n')
donkey_idx = {d['ear_tag_id']: i + 1 for i, d in enumerate(donkeys)}

de_rows = [[donkey_idx[e['donkey_tag']], e['donkey_tag'], e['event_type'], e['event_time'],
            e['content'], e['detail_hash'], e['tx_id'], e['block_no']] for e in donkey_events]
sql.append(insert_rows('biz_donkey_event', TABLES[5][3], de_rows) + '\n')

batch_rows = [[b['batch_no'], b['batch_type'], jarr(b['parent_nos']), jarr(b['source_ear_tags']),
               org_idx[b['org_code']], b['org_name'], org_idx[b['holder_code']], b['holder_name'],
               b['product_name'], b['weight_kg'], b['produce_date'], b['expire_date'],
               b['cert_hash'], b['status'], b['create_txid'], b['qc_report_uri'], b['create_time']]
              for b in batches]
sql.append(insert_rows('biz_batch', TABLES[6][3], batch_rows) + '\n')
batch_idx = {b['batch_no']: i + 1 for i, b in enumerate(batches)}

be_rows = [[e['batch_no'], e['event_type'], e['org_name'], e['operator'], e['event_time'],
            e['summary'], e['tx_id'], e['block_no']] for e in batch_events]
sql.append(insert_rows('biz_batch_event', TABLES[7][3], be_rows) + '\n')

to_rows = [[o['transport_no'], o['batch_no'], org_idx[o['from_code']], o['from_name'],
            org_idx[o['to_code']], o['to_name'], o['vehicle_no'], o['driver_name'], o['driver_phone'],
            o['depart_time'], o['expect_arrive_time'], o['actual_arrive_time'], o['status'],
            o['file_uri'], o['file_hash'], o['tx_id'], o['create_time']] for o in transport_orders]
sql.append(insert_rows('biz_transport_order', TABLES[8][3], to_rows) + '\n')
transport_idx = {o['transport_no']: i + 1 for i, o in enumerate(transport_orders)}

tr_rows = [[transport_idx[r['transport_no']], r['record_time'], r['temperature'], r['humidity'], r['abnormal']]
           for r in transport_records]
sql.append(insert_rows('biz_transport_record', TABLES[9][3], tr_rows) + '\n')

op_rows = [[o['batch_no'], org_idx[o['shop_code']], o['shop_name'], o['output_time'], o['oven_no'],
            o['chef'], o['qty'], o['used_weight'], o['create_time']] for o in outputs]
sql.append(insert_rows('biz_output_record', TABLES[10][3], op_rows) + '\n')
output_idx_by_key = {}
for i, o in enumerate(outputs):
    output_idx_by_key[(o['batch_no'], o['output_time'])] = i + 1

# 溯源码的 output_id 需要对应产出记录序号: 生成时按 outputs 顺序两码一组
tc_rows = []
k = 0
for i, o in enumerate(outputs):
    for _ in range(2):
        c = trace_codes[k]; k += 1
        tc_rows.append([c['code'], c['batch_no'], c['shop_name'], i + 1, c['status'],
                        c['bind_txid'], c['print_count'], c['scan_times'], c['create_time']])
tc_rows.append([demo_code, trace_codes[-1]['batch_no'], trace_codes[-1]['shop_name'],
                1, trace_codes[-1]['status'], trace_codes[-1]['bind_txid'],
                trace_codes[-1]['print_count'], trace_codes[-1]['scan_times'], trace_codes[-1]['create_time']])
sql.append(insert_rows('biz_trace_code', TABLES[11][3], tc_rows) + '\n')

cert_rows = [[org_idx[c['org_code']], c['org_name'], c['cert_type'], c['cert_no'], c['issue_date'],
              c['expire_date'], c['file_uri'], c['file_hash'], c['audit_status'], c['status']] for c in certs]
sql.append(insert_rows('biz_cert', TABLES[14][3], cert_rows) + '\n')

alert_rows = [[a['alert_type'], a['level'], a['target_type'], a['target_id'], a['content'],
               a['status'], a['handler'], a['handle_note'], a['handle_time'], a['create_time']] for a in alerts]
sql.append(insert_rows('biz_alert', TABLES[13][3], alert_rows) + '\n')

counts = {'sys_org': len(orgs), 'sys_user': len(users), 'sys_notice': len(notices), 'sys_config': len(configs),
          'biz_donkey': len(donkeys), 'biz_donkey_event': len(donkey_events), 'biz_batch': len(batches),
          'biz_batch_event': len(batch_events), 'biz_transport_order': len(transport_orders),
          'biz_transport_record': len(transport_records), 'biz_output_record': len(outputs),
          'biz_trace_code': len(tc_rows), 'biz_scan_log': len(scan_logs), 'biz_alert': len(alerts),
          'biz_cert': len(certs), 'biz_inspection': len(inspections), 'biz_recall': len(recalls),
          'biz_complaint': len(complaints), 'sys_operation_log': len(operation_logs)}

sql.append('-- ---------- 完成校验 ----------\n')
for t, n in counts.items():
    sql.append(f"-- {t}: {n} 行")

with open(OUT, 'w', encoding='utf-8') as f:
    f.write('\n'.join(sql))

print('OUT:', os.path.abspath(OUT))
print('ROWS:', json.dumps(counts, ensure_ascii=False))
