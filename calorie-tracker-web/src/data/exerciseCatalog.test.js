import { describe, it, expect } from 'vitest'
import { exerciseGroups, HOT_EXERCISES, calKcal60, getExerciseIcon, metMap } from './exerciseCatalog'

describe('exerciseCatalog', () => {
  it('每个运动项都有合法的 name 和 met', () => {
    for (const group of exerciseGroups) {
      for (const item of group.items) {
        expect(item.name).toBeTruthy()
        expect(item.met).toBeGreaterThan(0)
      }
    }
  })

  it('运动名称全局唯一（metMap 无覆盖）', () => {
    const allItems = exerciseGroups.flatMap(g => g.items)
    const names = allItems.map(i => i.name)
    expect(new Set(names).size).toBe(names.length)  // 无重复
  })

  it('calKcal60 按 MET×70×1.05 四舍五入', () => {
    expect(calKcal60(3.0)).toBe(Math.round(3.0 * 70 * 1.05))  // 221
    expect(calKcal60(8.0)).toBe(Math.round(8.0 * 70 * 1.05))  // 588
    expect(calKcal60(10.0)).toBe(Math.round(10.0 * 70 * 1.05)) // 735
  })

  it('每个运动项的 kcal60 与 calKcal60(met) 一致', () => {
    for (const group of exerciseGroups) {
      for (const item of group.items) {
        expect(item.kcal60).toBe(calKcal60(item.met))
      }
    }
  })

  it('HOT_EXERCISES 里的名字都能在 exerciseGroups 找到', () => {
    const allNames = new Set(exerciseGroups.flatMap(g => g.items.map(i => i.name)))
    for (const name of HOT_EXERCISES) {
      expect(allNames.has(name)).toBe(true)
    }
  })

  it('getExerciseIcon 返回分组 emoji 或兜底', () => {
    expect(getExerciseIcon('快走 (5km/h)')).toBe('🚶')
    expect(getExerciseIcon('慢跑 (8km/h)')).toBe('🏃')
    expect(getExerciseIcon('不存在的运动')).toBe('💪')  // 兜底
  })

  it('metMap 能查到每个运动名的 MET', () => {
    for (const group of exerciseGroups) {
      for (const item of group.items) {
        expect(metMap[item.name]).toBe(item.met)
      }
    }
  })
})