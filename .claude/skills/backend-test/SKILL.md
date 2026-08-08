---
name: backend-test
description: 卡路里后端（calorie-server）单元测试约定 + 全部 9 个 service 签名速查。写/改/跑后端测试时加载，免重读源码。
---

# backend-test —— 后端单元测试约定与速查

写/改/跑 `calorie-server` 的单元测试时加载本 skill，直接按速查签名写测试，**不用重读 service 源码**。

## 运行测试

```bash
cd calorie-server && ./mvnw test          # 全部（含 @SpringBootTest contextLoads，需本地 MySQL 在运行）
./mvnw test -Dtest=MealRecordServiceTest  # 单类
./mvnw test -Dtest='*ServiceTest'         # 通配
```

- 覆盖率报告：`calorie-server/target/site/jacoco/index.html`（每次 mvn test 刷新，不进 git）
- 测试依赖已就位（Mockito 5.23 + JUnit Jupiter 6 + AssertJ），**无需改 pom**
- 若 `mvnw` 不可用，IDE（IDEA）直接跑测试类同样可以

## 测试固定约定

```java
@ExtendWith(MockitoExtension.class)
class XxxServiceTest {
    @Mock private FooRepository fooRepository;   // 每个依赖一个 @Mock
    @InjectMocks private FooService fooService;  // 被测 service
}
```

1. **strict stubbing**：未用到的 stub 报 `UnnecessaryStubbing` → 每个测试只 stub 实际用到的。
2. **save 类 stub**：`when(repo.save(any(Foo.class))).thenAnswer(inv -> inv.getArgument(0));`
3. **归属校验**（`SecurityUtil.requireOwner` 静态方法读 SecurityContext）：手动设置认证，不用 mockStatic：
   ```java
   @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }
   private static void loginAs(Long userId) {
       SecurityContextHolder.getContext().setAuthentication(
           new UsernamePasswordAuthenticationToken(String.valueOf(userId), null,
               List.of(new SimpleGrantedAuthority("ROLE_USER"))));
   }
   ```
   非 owner → `assertThrows(ForbiddenException.class, ...)`。
4. **`Object[]` 列表**（趋势/分组查询返回）：`List.of(new Object[]{...})` 会被 varargs 展开成 `List<Object>` → 必须 `List.<Object[]>of(new Object[]{date, cals})`。
5. **三元 `Double? : int`** 统一提升为 `double`：null 分支值是 `Double 0.0`，断言用 `assertEquals(0.0, ...)`。
6. `TimeUtil.today()` 是静态方法但**可真实调用**（WeightServiceTest 先例），别 mockStatic。
7. 断言用 JUnit5 `assert*`（`assertEquals/assertThrows/assertNotNull/assertTrue`）。
8. **速查表可能过时**：源码改动后签名会变。写之前 `Grep` 目标 service 确认方法/字段还在，再相信速查。

## 签名速查（9 个 service）

> 每个列「构造依赖」+「方法签名 → 返回」。repo 方法名 = service 内部实际调用名，mock 时直接照抄。

### UserService
依赖：`UserRepository userRepository`、`JwtUtil jwtUtil`（`BCryptPasswordEncoder` 是私有 new，不用 mock）
- `UserResponse register(RegisterRequest req)` —— 校验 existsByEmail / existsByUsername，BCrypt 加密，返回含 token
- `UserResponse login(LoginRequest req)` —— findByEmail 回退 findByUsername，密码错/人不存在抛 `BusinessException("邮箱/用户名或密码错误")`
- `UserResponse findByEmail(String email)`
- `UserResponse submitOnboarding(Long userId, UpdateBodyInfoRequest req)` —— 14 步提交，算 BMR/TDEE/目标/营养素；birthDate null 时按 age 反推当年 1月1日
- `UserResponse getProfile(Long userId)`
- `UserResponse recalculateTargetsFromWeight(Long userId, double currentWeightKg)` —— 临时 set weightKg 算完恢复起始体重
- repo：`existsByEmail`、`existsByUsername`、`save`、`findByEmail`、`findByUsername`、`findById`

### WeightService
依赖：`WeightRecordRepository`、`UserService userService`（mock 依赖，验证联动用）
- `WeightRecord addWeight(Long userId, double weightKg, LocalDate date, Double bodyFatPct, Double waistCm, Double hipCm)` —— 同日有记录→更新最新条+删多余重复；体成分只在非 null 时更新；save 后 `userService.recalculateTargetsFromWeight(userId, 最新体重)`
- `Map<String,Object> getLatestWeight(Long userId)` —— keys: `weightKg`、`diff`（空→均 null；单条→diff null）
- `List<WeightHistoryPoint> getWeightHistory(Long userId, int days)` —— record: `date()/weightKg()/bodyFatPct()/waistCm()/hipCm()`
- `List<WeightRecord> getRecords(Long userId)`
- `WeightRecord updateWeight(Long recordId, double weightKg, LocalDate date, Double, Double, Double)` —— 404 BusinessException；owner 校验；save 后重算
- `void deleteWeight(Long recordId)` —— 404/owner/delete + 重算
- repo：`findByUserIdAndRecordedDateOrderByIdDesc`、`findTop2ByUserIdOrderByRecordedDateDescIdDesc`、`findById`、`findByUserIdAndRecordedDateBetweenOrderByRecordedDateAscIdAsc`、`findByUserIdOrderByRecordedDateDescIdDesc`、`save`、`delete`、`deleteAll`

### MealRecordService
依赖：`MealRecordRepository`、`FoodRepository`、`UserRepository`、`ExerciseRecordRepository`
- `List<MealRecordResponse> batchAddRecords(Long userId, List<MealRecordRequest> reqs)` —— 一次 `findAllById` 批量加载食物；缺食物抛 `BusinessException("食物不存在: id")`；热量 = `grams/100 × caloriesPer100g`，`Math.round(x*10)/10` 保留 1 位
- `MealRecordResponse addRecord(Long userId, MealRecordRequest req)` —— 食物不存在抛 BusinessException
- `List<MealRecordResponse> getRecordsByDate(Long userId, LocalDate date)`
- `List<MealRecordResponse> getRecordsByDateAndMeal(Long userId, LocalDate date, MealRecord.MealType type)`
- `List<MealRecordResponse> getYesterdayMeal(Long userId, String mealType)` —— 内部取 `TimeUtil.today().minusDays(1)`，`type.toUpperCase()`
- `MealRecordResponse updateRecord(Long recordId, Double newGrams)` —— 404/owner/重算热量
- `void deleteRecord(Long recordId)` —— 404/owner/delete
- `void deleteRecordsByDate(Long userId, LocalDate date)`
- `Double getTotalCalories(Long userId, LocalDate date)` —— sumCalories 空 → `0.0`
- `Map<LocalDate,Double> getWeeklyTrend(Long userId, LocalDate endDate, int days)`
- `DailyNutritionResponse getDailyNutrition(Long userId, LocalDate date)` —— 营养素累加（null 按 0）；目标 = `user.dailyCalorieTarget + exerciseCalories × 0.9`，最大 = `user.tdee + exerciseCalories × 0.9`；macro 换算走 `CalorieCalculator.macroGrams(calories, ratio, 4|9)`（真实调用）；user 为 null → 各 target 为 null
- `DailyMealResponse getDailySummary(Long userId, LocalDate date)` —— 按 mealType 分组，四餐次 key（BREAKFAST/LUNCH/DINNER/SNACK）都保证存在，totalCalories 汇总
- repo：meal `findAllById`、`saveAll`、`findById`、`save`、`delete`、`deleteByUserIdAndMealDate`、`sumCaloriesByUserIdAndMealDate`、`sumCaloriesGroupByDate`、`findByUserIdAndMealDate`、`findByUserIdAndMealDateAndMealType`；food `findById`；user `findById`；exercise `sumCaloriesByUserIdAndDate`

### FoodService
依赖：`FoodRepository`、`FoodCategoryRepository`、`UserFavoriteRepository`、`MealRecordRepository`
- 查询类：`getAllCategories()` / `getCommonFoods()` / `getFoodsByCategory(Long)` / `getCommonFoodsByCategory(Long)` / `getUserFoods(Long)` —— 直接透传 repo
- `List<Food> searchFoods(String keyword)` —— null/空/全空白 → `List.of()` **不查库**；否则 trim 后查
- `FoodPageResponse getFoodLibraryPage(int page, int size)` / `searchFoodsPage(String, int, int)` —— Page mock：`when(page.getContent()/getTotalElements()/hasNext())`，repo 用 `any(Pageable.class)` 匹配
- `void addFavorite(Long userId, Long foodId)` —— 已收藏抛 `BusinessException("已收藏该食物")`；食物不存在抛 `"食物不存在: id"`；成功冗余存 `foodName`
- `void removeFavorite(Long userId, Long foodId)` / `boolean isFavorited(Long,Long)`
- `List<Food> getFavoriteFoods(Long)` / `List<Long> getFavoriteFoodIds(Long)`
- `Food createCustomFood(Long userId, CreateFoodRequest req)` —— 重名抛 `"已存在同名食物: "`；分类不存在抛 `"分类不存在，请重新选择"`；protein/fat/carbs 为 null → 0；`isPublic=false, isCommon=false, creator=userId`
- `void deleteCustomFood(Long foodId, Long userId)` —— 404；`isPublic=true` 抛 `"公共食物不可删除"`；creator 为 null 或非本人抛 `"无权删除该食物"`；被 meal_records 引用抛 `"该食物已被饮食记录使用，无法删除。请先删除引用它的饮食记录"`；成功先 `deleteByFoodId` 再 `delete`
- repo：`findByName`、`findByCreatorId`、`findById`、`findByIsPublicTrueAndIsCommonTrue`、`findByCategoryIdAndIsPublicTrueOrderByCaloriesPer100gAsc`、`findByCategoryIdAndIsPublicTrueAndIsCommonTrueOrderByCaloriesPer100gAsc`、`findByNameContainingAndIsPublicTrueOrderByCaloriesPer100gAsc`、`findByIsPublicTrueOrderByCaloriesPer100gAsc`、`save`、`delete`；category `findAll`、`existsById`；favorite `findByUserIdAndFoodId`、`deleteByUserIdAndFoodId`、`findByUserId`、`deleteByFoodId`、`save`；meal `existsByFoodId`

### ExerciseService
依赖：`ExerciseRecordRepository`
- `double calculateCalories(double metValue, double weightKg, int durationMin)` —— `MET×kg×时×1.05`，保留 1 位
- `ExerciseRecord addExercise(Long userId, ExerciseRequest req, double weightKg)` —— recordDate 空→今天
- `List<ExerciseRecord> getByDate(Long userId, String dateStr)`
- `Map<String,Object> getSummary(Long userId, LocalDate date)` —— date null→今天；keys: `records/totalCalories/count/date`
- `ExerciseRecord updateExercise(Long recordId, ExerciseRequest, double weightKg)` —— 404/owner/重算
- `void deleteExercise(Long recordId)` —— 404/owner
- `Map<LocalDate,Double> getExerciseTrend(Long userId, LocalDate endDate, int days)`
- `List<String> getRecentExerciseTypes(Long userId)` —— 最近 50 条去重取前 10
- repo：`findByUserIdAndRecordDateOrderByIdDesc`、`sumCaloriesByUserIdAndDate`、`sumCaloriesGroupByDate`、`findById`、`findTop50ByUserIdOrderByRecordDateDescIdDesc`、`save`、`delete`

### WaterService
依赖：`WaterRecordRepository`
- `WaterRecord addWater(Long userId, LocalDate date, int amountMl)`
- `List<WaterRecord> getByDate(Long userId, LocalDate date)`
- `Map<String,Object> getSummary(Long userId, LocalDate date)` —— keys: `records/totalMl/count/date`
- `Map<LocalDate,Integer> getTrend(Long userId, LocalDate endDate, int days)` —— `((Number) row[1]).intValue()`
- `WaterRecord updateWater(Long recordId, int amountMl)` / `void deleteWater(Long recordId)` —— 404/owner
- repo：`findByUserIdAndDateOrderByIdDesc`、`sumMlByUserIdAndDate`、`sumMlGroupByDate`、`findById`、`save`、`delete`

### SleepService
依赖：`SleepRecordRepository`
- `SleepRecord saveSleep(Long userId, LocalDate date, int durationMin)` —— 同日已有→更新覆盖
- `SleepRecord getByDate(Long userId, LocalDate date)` —— 无记录返回 null
- `Map<String,Object> getSummary(Long userId, LocalDate date)` —— keys: `record/durationMin/totalMin/date`
- `Map<LocalDate,Integer> getTrend(Long userId, LocalDate endDate, int days)`
- `SleepRecord updateSleep(Long recordId, int durationMin)` / `void deleteSleep(Long recordId)` —— 404/owner
- repo：`findByUserIdAndDate`、`sumMinByUserIdAndDate`、`sumMinGroupByDate`、`findById`、`save`、`delete`

### StatsService
依赖：`MealRecordRepository`、`WeightRecordRepository`、`ExerciseRecordRepository`、`UserRepository`
- `Map<String,Object> getStreak(Long userId)` —— 打卡 = 当天有饮食 ∪ 体重记录；keys: `currentStreak/longestStreak/lastCheckInDate/today`；今天未打卡→锚点退昨天；`Object[]` 用 `List.<Object[]>of`
- `Map<String,Object> getReport(Long userId, LocalDate endDate, int days)` —— keys: `startDate/endDate/days/dailyCalorieTarget/totalCalories/averageCalories/exerciseCalories/startWeight/endWeight/weightChange/checkInDates/goalDays/intakeTrend`；weightChange = 首末差值（≥2 条）；goalDays = 摄入在 target ±10% 内天数（target null → 0）
- repo：meal `findDistinctDatesByUserId`、`sumCaloriesGroupByDate`；weight `findDistinctDatesByUserId`、`findByUserIdAndRecordedDateBetweenOrderByRecordedDateAscIdAsc`；exercise `sumCaloriesGroupByDate`；user `findById`

### ExportService
依赖：8 个 repo（`UserRepository/WeightRecordRepository/MealRecordRepository/ExerciseRecordRepository/WaterRecordRepository/SleepRecordRepository/UserFavoriteRepository/FoodRepository`）
- `Map<String,Object> exportUserData(Long userId)` —— 用户不存在抛 BusinessException；keys: `user/weightRecords/mealRecords/exerciseRecords/waterRecords/sleepRecords/favorites/customFoods`；mealRecords 已转 `MealRecordResponse`
- repo 方法：各 `findByUserIdAnd*Between`（日期 2000-01-01 ~ 2100-12-31）、`findByUserIdOrderByRecordedDateDescIdDesc`、`findByUserId`、`findByCreatorId`、`findById`

## 关键 DTO / 实体（构造 & 访问）

- `MealRecordRequest`：`foodId(Long)/grams(Double)/mealType(MealRecord.MealType)/mealDate(LocalDate)/note(String)`，setter 注入
- `MealRecordResponse`：`@Builder`，`fromEntity(MealRecord)` 静态；getter：`getFoodId()/getFoodName()/getTotalCalories()/getMealType()`
- `DailyNutritionResponse`：record，访问器 `protein()/fat()/carbs()/totalCalories()/proteinTarget()/.../calorieTarget()/calorieMax()`
- `FoodPageResponse`：`getItems()/getTotal()/isHasMore()`
- `CreateFoodRequest`：`name/categoryId/caloriesPer100g/proteinPer100g/fatPer100g/carbsPer100g`，setter
- `UserResponse`：getter `getToken()/getUsername()/getRole()/getBmr()/getTdee()/getDailyCalorieTarget()/...`（`@Builder(toBuilder=true)`）
- 实体 builder：`Food.builder().id().name().category().caloriesPer100g().proteinPer100g().fatPer100g().carbsPer100g().isPublic().isCommon().creator().build()`
  `MealRecord.builder().id().user().food().grams().totalCalories().mealType().mealDate().note().build()`
  `User.builder().id().gender().heightCm().weightKg().birthDate().targetWeightKg().targetDays().dailyCalorieTarget().tdee().proteinRatio().fatRatio().carbsRatio().role().build()`
  `WeightRecord.builder().id().user().weightKg().recordedDate().bodyFatPct().waistCm().hipCm().build()`
  `UserFavorite.builder().id().user().food().foodName().build()`
- 异常：`BusinessException(String)`（业务 400）、`ForbiddenException`（归属 403，包 `security`）

## 已知坑

- `@SpringBootTest contextLoads` 会连真实 MySQL → 测试期间 MySQL 必须启动
- Page 返回的 mock：`@Mock Page<Food> page;` + `when(page.getContent()).thenReturn(List.of(food))` 等
- mock `PageRequest.of(...)` 调用用 `any(Pageable.class)`
- 热量保留 1 位小数的断言：`assertEquals(计算式, resp.getTotalCalories(), 0.001)`（double 比较用 delta）
