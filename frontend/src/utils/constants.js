export const REPORT_STATUS = {
  0: { label: '待一审', type: 'info' },
  1: { label: '待终审', type: 'warning' },
  2: { label: '已通过', type: 'success' },
  3: { label: '已驳回', type: 'danger' },
  4: { label: '已撤销', type: '' }
}

export const PERFORM_TYPES = ['歌唱', '乐器', '舞蹈', '杂技', '魔术', '相声', '其他']

export const BASE_CREDIT_EVENT_TYPE = {
  1: { label: '按时演出',     delta: +2,  color: '#67c23a', type: 'success' },
  2: { label: '迟到15分钟内', delta: -5,  color: '#e6a23c', type: 'warning' },
  3: { label: '迟到超15分钟', delta: -10, color: '#f56c6c', type: 'danger'  },
  4: { label: '超时演出',     delta: -8,  color: '#e6a23c', type: 'warning' },
  5: { label: '扰民投诉',     delta: -15, color: '#f56c6c', type: 'danger'  },
  6: { label: '爽约',         delta: -20, color: '#f56c6c', type: 'danger'  },
  7: { label: '系统奖励',     delta: +10, color: '#409eff', type: 'primary' }
}

// 向后兼容旧引用，使用内置类型
export const CREDIT_EVENT_TYPE = BASE_CREDIT_EVENT_TYPE

// 获取合并了自定义类型的完整列表（在 CreditManage 中使用）
export function getMergedEventTypes() {
  const merged = { ...BASE_CREDIT_EVENT_TYPE }
  try {
    const custom = JSON.parse(localStorage.getItem('pdk_custom_event_types') || '[]')
    custom.forEach(item => { merged[item.id] = item })
  } catch {}
  return merged
}

export function saveCustomEventTypes(list) {
  localStorage.setItem('pdk_custom_event_types', JSON.stringify(list))
}

export function loadCustomEventTypes() {
  try { return JSON.parse(localStorage.getItem('pdk_custom_event_types') || '[]') } catch { return [] }
}

export const USER_ROLE = {
  1: '艺人',
  2: '管理员',
  3: '超级管理员'
}
