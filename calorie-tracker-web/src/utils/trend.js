// 趋势数据补零工具：后端 trend 接口只返回有记录的日期（稀疏 Map），
// 渲染图表前需补全为连续日期数组，缺失日期补 0。
export function fillTrend(trendObj, endDate, days) {
  const arr = []
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(endDate)
    d.setDate(d.getDate() - i)
    const k = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    arr.push({ date: k, value: trendObj[k] || 0 })
  }
  return arr
}
