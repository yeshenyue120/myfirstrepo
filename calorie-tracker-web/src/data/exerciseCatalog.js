// 运动项目数据（MET 值 + 60 分钟参考消耗）—— Home.vue 与 ExerciseRecord.vue 共享
// kcal60 = 70kg 参考体重下运动 60 分钟的消耗（千卡），按 MET × 70 × 1.05 四舍五入取整
// 实际记录时后端按「MET × 用户体重 × 时长 × 1.05」实时计算，kcal60 仅作列表参考展示
export const exerciseGroups = [
  {
    label: '🚶 步行',
    items: [
      { name: '散步 (3km/h)', met: 2.8, kcal60: 206 },
      { name: '慢走 (4km/h)', met: 3.0, kcal60: 221 },
      { name: '快走 (5km/h)', met: 4.3, kcal60: 316 },
      { name: '健走 (6km/h)', met: 5.0, kcal60: 368 },
      { name: '竞走 (7km/h)', met: 6.5, kcal60: 478 },
      { name: '爬楼梯（缓慢）', met: 4.0, kcal60: 294 },
      { name: '爬楼梯（快速）', met: 8.0, kcal60: 588 },
      { name: '登山/徒步（轻松）', met: 5.5, kcal60: 404 },
      { name: '登山/徒步（陡坡负重）', met: 8.0, kcal60: 588 },
      { name: '负重背包远足', met: 7.0, kcal60: 515 }
    ]
  },
  {
    label: '🏃 跑步',
    items: [
      { name: '慢跑 (6km/h)', met: 6.0, kcal60: 441 },
      { name: '慢跑 (8km/h)', met: 8.0, kcal60: 588 },
      { name: '跑步 (10km/h)', met: 10.0, kcal60: 735 },
      { name: '快跑 (12km/h)', met: 12.0, kcal60: 882 },
      { name: '冲刺跑 (14km/h+)', met: 14.0, kcal60: 1029 },
      { name: '上坡跑', met: 10.0, kcal60: 735 },
      { name: '越野跑', met: 9.0, kcal60: 662 },
      { name: '间歇跑', met: 11.0, kcal60: 809 }
    ]
  },
  {
    label: '🚴 骑行',
    items: [
      { name: '休闲骑行 (12km/h)', met: 4.0, kcal60: 294 },
      { name: '骑行通勤', met: 4.5, kcal60: 331 },
      { name: '轻松骑行 (16km/h)', met: 6.0, kcal60: 441 },
      { name: '中等骑行 (19km/h)', met: 8.0, kcal60: 588 },
      { name: '快速骑行 (22km/h)', met: 10.0, kcal60: 735 },
      { name: '竞速骑行 (25km/h+)', met: 12.0, kcal60: 882 },
      { name: '爬坡骑行', met: 10.5, kcal60: 772 },
      { name: '山地越野骑行', met: 8.5, kcal60: 625 }
    ]
  },
  {
    label: '⚽ 球类',
    items: [
      { name: '台球/斯诺克', met: 2.5, kcal60: 184 },
      { name: '保龄球', met: 3.0, kcal60: 221 },
      { name: '高尔夫（开车）', met: 3.0, kcal60: 221 },
      { name: '飞盘（休闲）', met: 3.0, kcal60: 221 },
      { name: '踢毽子', met: 5.0, kcal60: 368 },
      { name: '高尔夫（步行背包）', met: 4.5, kcal60: 331 },
      { name: '乒乓球（休闲）', met: 4.0, kcal60: 294 },
      { name: '乒乓球（比赛）', met: 5.5, kcal60: 404 },
      { name: '羽毛球（双打）', met: 5.0, kcal60: 368 },
      { name: '排球（娱乐）', met: 3.5, kcal60: 257 },
      { name: '棒球/垒球', met: 5.0, kcal60: 368 },
      { name: '网球（双打）', met: 6.0, kcal60: 441 },
      { name: '排球（比赛）', met: 6.0, kcal60: 441 },
      { name: '羽毛球（单打）', met: 7.0, kcal60: 515 },
      { name: '足球（娱乐）', met: 7.0, kcal60: 515 },
      { name: '网球（单打）', met: 8.0, kcal60: 588 },
      { name: '篮球（投篮练习）', met: 6.5, kcal60: 478 },
      { name: '篮球（全场对抗）', met: 8.0, kcal60: 588 },
      { name: '手球', met: 8.0, kcal60: 588 },
      { name: '橄榄球', met: 8.0, kcal60: 588 },
      { name: '沙滩排球', met: 8.0, kcal60: 588 },
      { name: '极限飞盘', met: 8.0, kcal60: 588 },
      { name: '足球（比赛）', met: 10.0, kcal60: 735 },
      { name: '壁球', met: 12.0, kcal60: 882 }
    ]
  },
  {
    label: '🏊 水上运动',
    items: [
      { name: '放松戏水', met: 3.5, kcal60: 257 },
      { name: '水中漫步', met: 4.0, kcal60: 294 },
      { name: '浮潜', met: 4.0, kcal60: 294 },
      { name: '帆板/帆船', met: 3.5, kcal60: 257 },
      { name: '划船（休闲）', met: 4.0, kcal60: 294 },
      { name: '冲浪', met: 5.0, kcal60: 368 },
      { name: '皮划艇/独木舟', met: 5.0, kcal60: 368 },
      { name: '漂流', met: 5.0, kcal60: 368 },
      { name: '游泳（蛙泳）', met: 5.3, kcal60: 390 },
      { name: '潜水（休闲）', met: 7.0, kcal60: 515 },
      { name: '游泳（仰泳）', met: 8.0, kcal60: 588 },
      { name: '游泳（自由泳，慢）', met: 8.0, kcal60: 588 },
      { name: '水中健身操', met: 8.0, kcal60: 588 },
      { name: '水中慢跑', met: 8.0, kcal60: 588 },
      { name: '花样游泳', met: 9.0, kcal60: 662 },
      { name: '游泳（自由泳，快）', met: 10.0, kcal60: 735 },
      { name: '游泳（蝶泳）', met: 11.0, kcal60: 809 },
      { name: '划船（激烈/竞赛）', met: 12.0, kcal60: 882 }
    ]
  },
  {
    label: '🎿 其他运动',
    items: [
      { name: '飞镖', met: 2.5, kcal60: 184 },
      { name: '射击', met: 2.5, kcal60: 184 },
      { name: '钓鱼', met: 2.5, kcal60: 184 },
      { name: '射箭', met: 3.5, kcal60: 257 },
      { name: '骑马（漫步）', met: 3.0, kcal60: 221 },
      { name: '骑马（快步/奔跑）', met: 6.0, kcal60: 441 },
      { name: '滑板', met: 5.0, kcal60: 368 },
      { name: '攀岩（轻松）', met: 5.0, kcal60: 368 },
      { name: '轮滑/旱冰', met: 7.0, kcal60: 515 },
      { name: '滑冰', met: 7.0, kcal60: 515 },
      { name: '单板滑雪', met: 7.0, kcal60: 515 },
      { name: '高山滑雪（娱乐）', met: 7.0, kcal60: 515 },
      { name: '攀岩（激烈）', met: 8.0, kcal60: 588 },
      { name: '越野滑雪', met: 9.0, kcal60: 662 },
      { name: '跳绳（慢速）', met: 8.0, kcal60: 588 },
      { name: '跳绳（中速）', met: 11.0, kcal60: 809 },
      { name: '跳绳（快速）', met: 12.0, kcal60: 882 }
    ]
  },
  {
    label: '🏋️ 健身综合',
    items: [
      { name: '弹力带训练', met: 3.5, kcal60: 257 },
      { name: '力量训练（轻重量）', met: 4.0, kcal60: 294 },
      { name: '俯卧撑/仰卧起坐', met: 4.0, kcal60: 294 },
      { name: '平板支撑/核心训练', met: 4.0, kcal60: 294 },
      { name: '力量训练（中重量）', met: 5.0, kcal60: 368 },
      { name: '引体向上', met: 5.0, kcal60: 368 },
      { name: '力量训练（大重量）', met: 6.0, kcal60: 441 },
      { name: '举重/奥林匹克举', met: 6.0, kcal60: 441 },
      { name: '深蹲/硬拉', met: 6.0, kcal60: 441 },
      { name: 'TRX 悬挂训练', met: 5.5, kcal60: 404 },
      { name: '壶铃训练', met: 8.0, kcal60: 588 },
      { name: '战绳训练', met: 8.0, kcal60: 588 },
      { name: '高强度循环训练/CrossFit', met: 8.0, kcal60: 588 },
      { name: '波比跳', met: 10.0, kcal60: 735 }
    ]
  },
  {
    label: '🏃♂️ 有氧器械',
    items: [
      { name: '跑步机（快走）', met: 4.0, kcal60: 294 },
      { name: '跑步机（上坡走）', met: 5.5, kcal60: 404 },
      { name: '跑步机（慢跑）', met: 7.0, kcal60: 515 },
      { name: '跑步机（快跑）', met: 9.8, kcal60: 720 },
      { name: '椭圆机（轻松）', met: 4.0, kcal60: 294 },
      { name: '椭圆机（中等）', met: 5.5, kcal60: 404 },
      { name: '椭圆机（高强度）', met: 8.0, kcal60: 588 },
      { name: '动感单车（低强度）', met: 5.0, kcal60: 368 },
      { name: '动感单车（中强度）', met: 7.0, kcal60: 515 },
      { name: '动感单车（高强度）', met: 8.5, kcal60: 625 },
      { name: '划船机（轻松）', met: 5.0, kcal60: 368 },
      { name: '划船机（中等）', met: 7.0, kcal60: 515 },
      { name: '划船机（高强度）', met: 9.0, kcal60: 662 },
      { name: '登山机/阶梯机', met: 7.0, kcal60: 515 },
      { name: '登山机/阶梯机（快）', met: 9.5, kcal60: 698 },
      { name: '踏步机', met: 5.5, kcal60: 404 },
      { name: '卧式单车', met: 4.0, kcal60: 294 }
    ]
  },
  {
    label: '💃 舞蹈跳操',
    items: [
      { name: '交谊舞（慢速）', met: 3.0, kcal60: 221 },
      { name: '广场舞', met: 4.0, kcal60: 294 },
      { name: '肚皮舞', met: 4.0, kcal60: 294 },
      { name: '交谊舞（快速）', met: 4.5, kcal60: 331 },
      { name: '民族舞', met: 4.5, kcal60: 331 },
      { name: '拉丁舞', met: 5.0, kcal60: 368 },
      { name: '爵士舞', met: 5.0, kcal60: 368 },
      { name: '芭蕾', met: 5.0, kcal60: 368 },
      { name: '有氧操（低冲击）', met: 5.0, kcal60: 368 },
      { name: '街舞/嘻哈', met: 6.0, kcal60: 441 },
      { name: '钢管舞', met: 6.0, kcal60: 441 },
      { name: '健美操', met: 6.5, kcal60: 478 },
      { name: '尊巴/有氧舞蹈', met: 7.0, kcal60: 515 },
      { name: '踏板操', met: 7.0, kcal60: 515 },
      { name: '有氧操（高冲击）', met: 8.0, kcal60: 588 }
    ]
  },
  {
    label: '🥋 武术传统',
    items: [
      { name: '八段锦', met: 3.0, kcal60: 221 },
      { name: '太极拳（缓慢）', met: 3.0, kcal60: 221 },
      { name: '太极拳（中等）', met: 4.0, kcal60: 294 },
      { name: '五禽戏', met: 3.5, kcal60: 257 },
      { name: '武术套路（舒缓）', met: 4.5, kcal60: 331 },
      { name: '太极拳（快/竞赛）', met: 5.5, kcal60: 404 },
      { name: '剑道', met: 6.0, kcal60: 441 },
      { name: '击剑', met: 6.0, kcal60: 441 },
      { name: '拳击', met: 7.0, kcal60: 515 },
      { name: '武术套路（激烈）', met: 8.0, kcal60: 588 },
      { name: '散打/自由搏击', met: 8.0, kcal60: 588 },
      { name: '空手道', met: 8.0, kcal60: 588 },
      { name: '跆拳道', met: 8.0, kcal60: 588 },
      { name: '柔道', met: 8.0, kcal60: 588 },
      { name: '综合格斗训练', met: 10.0, kcal60: 735 }
    ]
  },
  {
    label: '🧘 柔韧拉伸',
    items: [
      { name: '冥想/呼吸练习', met: 1.5, kcal60: 110 },
      { name: '静态拉伸', met: 2.0, kcal60: 147 },
      { name: '瑜伽（阴瑜伽/恢复）', met: 2.0, kcal60: 147 },
      { name: '瑜伽（哈他/基础）', met: 2.5, kcal60: 184 },
      { name: '泡沫轴放松', met: 2.5, kcal60: 184 },
      { name: '普拉提', met: 3.0, kcal60: 221 },
      { name: '芭蕾形体/柔韧训练', met: 3.0, kcal60: 221 },
      { name: '瑜伽（流/力量）', met: 4.0, kcal60: 294 },
      { name: '空中瑜伽', met: 4.0, kcal60: 294 },
      { name: '瑜伽（热瑜伽）', met: 4.5, kcal60: 331 }
    ]
  }
]

// 热门运动（ExerciseRecord 页面「热门」分组展示，名称需与 exerciseGroups.items 完全一致）
export const HOT_EXERCISES = [
  '快走 (5km/h)',
  '慢跑 (8km/h)',
  '散步 (3km/h)',
  '跑步 (10km/h)',
  '游泳（蛙泳）',
  '瑜伽（哈他/基础）',
  '跳绳（中速）',
  '动感单车（中强度）',
  '篮球（全场对抗）',
  '羽毛球（单打）'
]

// 计算某 MET 值在 70kg 参考体重下运动 60 分钟的消耗（千卡）
export function calKcal60(met) {
  return Math.round(met * 70 * 1.05)
}

// 构建 name -> MET 快速查找
export const metMap = {}
exerciseGroups.forEach(g => g.items.forEach(ex => { metMap[ex.name] = ex.met }))

// 按运动所属分类返回 emoji 图标（找不到时兜底 💪）
export function getExerciseIcon(name) {
  const group = exerciseGroups.find(g => g.items.some(i => i.name === name))
  return group ? group.label.replace(/\s.*$/, '') : '💪'
}
