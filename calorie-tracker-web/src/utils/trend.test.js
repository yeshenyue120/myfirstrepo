import { describe, it, expect } from 'vitest'
import { fillTrend } from './trend'

describe('fillTrend', () => {
  it('补全连续日期，缺失日期补 0', () => {
    // 后端只返回有记录的日期（稀疏）
    const trend = { '2026-08-01': 500, '2026-08-03': 800 }
    const result = fillTrend(trend, '2026-08-03', 3)
    expect(result).toEqual([
      { date: '2026-08-01', value: 500 },
      { date: '2026-08-02', value: 0 },   // 缺失，补 0
      { date: '2026-08-03', value: 800 },
    ])
  })

  it('返回 days 数量的日期，从 endDate 往前推', () => {
    const result = fillTrend({}, '2026-08-05', 5)
    expect(result).toHaveLength(5)
    expect(result[0].date).toBe('2026-08-01')  // 往前推 4 天
    expect(result[4].date).toBe('2026-08-05')  // 最后是 endDate
  })

  it('空对象时全部补 0', () => {
    const result = fillTrend({}, '2026-06-01', 2)
    expect(result).toEqual([
      { date: '2026-05-31', value: 0 },
      { date: '2026-06-01', value: 0 },
    ])
  })

  it('处理跨月边界正确', () => {
    // endDate 是 3 月 1 日，往前推跨到 2 月 28
    const result = fillTrend({}, '2026-03-01', 2)
    expect(result[0].date).toBe('2026-02-28')
    expect(result[1].date).toBe('2026-03-01')
  })

  it('处理跨年边界正确', () => {
    const result = fillTrend({}, '2026-01-01', 2)
    expect(result[0].date).toBe('2025-12-31')
    expect(result[1].date).toBe('2026-01-01')
  })
})