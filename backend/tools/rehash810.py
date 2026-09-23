# -*- coding: utf-8 -*-
"""重算 chain_block 810 的哈希, 与库中 hash 对比; 二分定位 txsJson 差异"""
import hashlib, json

CHANNEL = 'donkey-channel'
def sha(s):
    return hashlib.sha256(s.encode('utf-8')).hexdigest()

row = {
    'block_no': 810,
    'prev_hash': '0x98788a19b881fcf0dbc190533ad9a63809291dce4d4c4aba4b1c36dec1560b1c',
    'hash': '0xe1a8b872ab42916947272f188686c812422c657296439956af0e8d6457e429f0',
    'time': '2026-09-18 22:13:39',
    'tx_id': '0x5fc0616427302ce16989',
    'op': 'BindTraceCode',
    'msp_id': 'OrgShopMSP',
    'org_name': '',
    'payload': '{"code":"DT4QAKE5YPBQLDKT","shopOrgName":"","batchNo":""}',
}

payload = json.loads(row['payload'], object_pairs_hook=dict)
tx = {'txId': row['tx_id'], 'op': row['op'], 'mspId': row['msp_id'], 'orgName': row['org_name'],
      'time': row['time'], 'payload': payload}
txs = json.dumps([tx], ensure_ascii=False, separators=(',', ':'))
h = '0x' + sha(f"{CHANNEL}|{row['block_no']}|{row['prev_hash']}|{row['time']}|{txs}")
print('recomputed:', h)
print('stored    :', row['hash'])
print('MATCH' if h == row['hash'] else 'MISMATCH')
print()
print('txsJson used:', txs[:300])
