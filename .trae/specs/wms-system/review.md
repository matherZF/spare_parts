# WMS 系统独立审查报告 — Review R1

- **审查角色**: Review R1（只读独立审查者）
- **审查日期**: 2026-09-04
- **审查范围**: /workspace 简易 WMS 项目（Spring Boot 3 后端 + Vue3/Element Plus 前端）
- **审查依据**: spec.md 9 条 Acceptance Criteria（AC-1 ~ AC-9），覆盖 tasks.md 每个 Task 的 rule/rubric TR（测试要求）

---

## 一、Checkpoints（CP-R1 ~ CP-R9，每条均附 Evidence）

### CP-R1 ｜ AC-1 rule 商品 CRUD —— score: **pass**

**Evidence（全部可复现 curl）**

| 请求 | 断言 | 实际结果 |
|---|---|---|
| `POST /api/products {"sku":"SP-R1","name":"ReviewGood","spec":"r","unit":"pc"}` | `code=0`，返回 id、sku、name | code=0，id=4，sku=SP-R1，name=ReviewGood ✅ |
| 重复 sku `POST /api/products {"sku":"SP-R1" ...}` | `code!=0`，`msg` 含"已存在" | code=1，msg="sku已存在" ✅ |
| `PUT /api/products/4 {"name":"NEW_REVIEW","sku":"SP-R1-HACKED"}` | name 更新、**sku 不变（锁定字段生效）** | code=0，name=NEW_REVIEW，sku 仍为 SP-R1 ✅ |
| `DELETE /api/products/4` → `GET /api/products?page=0&size=10` | 删除后 `totalElements=3`（恢复种子 3 条） | delete code=0，totalElements=3 ✅ |

**对齐 TR-4.1**：SKU 唯一性校验、PUT SKU 锁定、删除后计数还原——全部实测通过。

---

### CP-R2 ｜ AC-2 rule 库位 CRUD + 删除库存保护 —— score: **pass**

**Evidence**

| 请求 | 断言 | 实际结果 |
|---|---|---|
| 种子 6 库位记录 | 6 个 code 齐全 | A-01-01(id=1)/A-01-02(id=2)/A-02-01(id=3)/B-01-01(id=4)/B-01-02(id=5)/C-01-01(id=6) ✅ |
| `DELETE /api/locations/1`（A-01-01 含 SP-002 50 件库存） | `code!=0`，msg 含"存在库存" | code=1，msg="库位[A-01-01]存在库存记录，无法删除" ✅ |
| `POST /api/locations {"code":"ZZ-TEST-001","area":"Z","type":"其他"}` | code=0 | code=0，id=7，code=ZZ-TEST-001 ✅ |
| `DELETE /api/locations/7`（空库位） | code=0 | code=0 ✅ |

**对齐 TR-7.4（删除保护）+ TR-3.5（空库位可删）**：两种场景均通过。

---

### CP-R3 ｜ AC-3 rule 上架单创建 + 单号递增 + 详情 —— score: **pass**

**Evidence**

| 请求 | 断言 | 实际结果 |
|---|---|---|
| `POST /api/orders {"productId":1,"planQty":100}` | 返回合法 orderNo，`code=0` | code=0，orderNo=**PA202609040001**，格式 PAyyyyMMddNNNN 匹配 ✅ |
| 连续 `POST` 2 次（共 3 单） | 3 个 orderNo **互不相同**、格式都合法 | `PA202609040001 / PA202609040002 / PA202609040003`，互不相同且均匹配正则 `^PA\d{8}\d{4}$` ✅ |
| `POST planQty=0` | `code!=0`，msg 含"必须大于0" | code=1，msg="planQty必须大于0" ✅ |
| `GET /api/orders/2` 详情 | 含 sku、productName、putQty=0、progress=0 | sku=SP-001，productName=深沟球轴承，putQty=0，progress=0 ✅ |

**对齐 spec 单号规则、planQty 正整数校验、详情字段完整性**——通过。

---

### CP-R4 ｜ AC-4 rule 上架事务 + 回滚 —— score: **pass**

**Evidence**（使用 PENDING 单 id=2：SP-001 plan=100 put=0）

| 请求 | 断言 | 实际结果 |
|---|---|---|
| `POST /api/putaway/confirm {orderId:2, locationId:2(A-01-02), qty:60}` | code=0，putQty=60，remaining=40，done=false | putQty=60 / remaining=40 / done=false ✅ |
| `POST confirm {orderId:2, locationId:4(B-01-01), qty:101}` | 失败，msg 含"超过计划数量"；**立即 GET 详情 putQty 仍=60（回滚）** | code=1 msg="超过计划数量，剩余40件"；GET 详情 putQty=60（回滚生效）✅ |
| `POST confirm {orderId:2, locationId:4(B-01-01), qty:40}` | code=0，done=true，remaining=0；status=DONE | putQty=100 / remaining=0 / done=true；status=DONE ✅ |
| DONE 单再 `POST confirm qty=1` | 失败，msg 含"已完成"；items 条数仍=2 | code=1 msg="该上架单已完成，无需继续上架"；items count=2 ✅ |

**关键：超量失败后 putQty 未被篡改——证明 @Transactional(rollbackFor=Exception.class) + BizException 抛异常触发回滚——与 PutawayService.java:29 代码一致。**

---

### CP-R5 ｜ AC-5 rule 库存汇总 = 明细 —— score: **pass**

**Evidence**（C4 之后 SP-001 的库存 = A-01-02 60 件 + B-01-01 40 件）

| 请求 | 断言 | 实际结果 |
|---|---|---|
| `GET /api/inventory/summary?sku=SP-001` | SP-001 totalQty=100 | totalQty=100 ✅ |
| `GET /api/inventory/details?sku=SP-001` | 2 条明细，60+40=100 == 汇总 | count=2，qtys=[60, 40]，sum=100 ✅ |

**对齐 TR-4.4 一致性要求**：汇总与明细完全吻合——通过。

---

### CP-R6 ｜ AC-6 rubric 双端响应式 —— score: **9 / 10**（阈值 7，通过）

评分锚点（满分 10）：桌面/移动双 layout(3) + 断点一致(2) + 移动端大按钮(2) + 路由覆盖(3)。

| 检查项 | Evidence | 得分 |
|---|---|---|
| DesktopLayout / MobileLayout 组件存在 | `frontend/src/layouts/DesktopLayout.vue` + `MobileLayout.vue` 均存在并分别实现侧边栏 5 菜单 / 底部 5 Tab + 顶部蓝色 Header | 3/3 |
| 断点 window.innerWidth<768 与 `$md:768px` 一致 | `App.vue:21` `isMobile = window.innerWidth < 768`；`styles/variables.scss:2` `$md: 768px`；`styles/index.scss:57` `@media (max-width: $md)` 移动端适配 | 2/2 |
| `.primary-mobile-btn { min-height:44px }` 存在 + 被 Putaway Step2/Step3 主按钮引用 | `styles/index.scss:37-41` 声明；`Putaway.vue:98` Step2 下一步按钮、`Putaway.vue:176` 确认上架按钮均使用该 class | 2/2 |
| 路由数量 ≥ 5，覆盖 6 大模块 | `router/index.js:4-41` 共 **7 条路由**：/products /locations /orders /orders/:id /putaway /inventory + /→重定向，完整覆盖功能 | 2/3（-1：无明确 pinia stores 目录；当前应用规模无跨页共享状态需求不影响核心功能，记为 advisory） |

**评分理由**：4 项核心锚点 + 路由齐全均满足；仅缺 stores/ 目录（但未用 Pinia 且无跨页状态需求），扣 1 分 advisory，不影响阈值。

---

### CP-R7 ｜ AC-7 rubric 导航 + 反馈 —— score: **9.5 / 10**（阈值 7，通过）

| 检查项 | Evidence | 得分 |
|---|---|---|
| axios 拦截器：`code!=0` → `ElMessage.error(msg)` + `Promise.reject(body)`；`code=0` → `resolve body.data` | `api/request.js:10-36` 完整实现，三段分支全覆盖 ✅ | 3/3 |
| 后端错误中文文案质量（友好且语义化） | "sku已存在"、"库位[A-01-01]存在库存记录，无法删除"、"超过计划数量，剩余40件"、"planQty必须大于0"、"该上架单已完成，无需继续上架"——5 类均实测通过，中文准确、可定位问题 ✅ | 2.5/2.5 |
| 前端删除二次确认 `ElMessageBox.confirm`（Products/Locations 两页） | `Products.vue:139-145` 删除商品含 SKU 与后果提示；`Locations.vue:133-139` 删除库位含存在库存提醒；两处均有 type="warning" + cancelButtonText ✅ | 2/2 |
| Putaway 成功提示具体到件数："上架成功 X 件" | `Putaway.vue:309` `ElMessage.success(\`上架成功 ${qtyDone} 件\`)` + 后续 ElMessageBox 分别区分"全部上架完成"与"本次上架 X 件，剩余 Y 件"场景 ✅ | 2/2（-0.5 细微：Putaway 确认前的前端剩余量校验是 ElMessage.warning 而非 ElMessageBox，与 C4 后端校验文案略不统一，但无功能影响——advisory） |

---

### CP-R8 ｜ AC-8 rubric 代码分层 —— score: **9.5 / 10**（阈值 7，通过）

#### 后端 package 齐全性（抽查 3 层 tree）
`backend/src/main/java/com/wms` 下 **8 个包齐全**：
- entity（Product/Location/PutawayOrder/PutawayItem/Inventory + PutawayStatus 枚举）
- repository（5 个 JpaRepository）
- service（Product/Location/PutawayOrder/Putaway/Inventory + DataInitializer）
- dto（9 个 record DTO/VOS）
- controller（5 个 RestController）
- config（WebConfig / CorsConfig）
- exception（GlobalExceptionHandler）
- common（Result / BizException）

**抽查 3 个 public 方法**（命名一致性 + 职责清晰）：
1. `PutawayService.confirm(PutawayConfirmReq req)`（`service/PutawayService.java:29-80`）：`@Transactional(rollbackFor=Exception.class)` 事务上架——参数校验→状态校验→扣减超量→加明细→加 putQty→库存累加→返回 DONE/MSG。**命名一致、职责单一、事务边界清晰**。
2. `GlobalExceptionHandler.handleBiz(BizException e)`（`exception/GlobalExceptionHandler.java:20-24`）：统一 `Result.fail(e.getMessage())` + HTTP 200（业务错误不抛 HTTP 非 2xx）。**全局异常策略一致**。
3. `ProductService.create(ProductDTO dto)`（`service/ProductService.java:41-49`）：`@Transactional` + sku/name 必填 + `existsBySku` 唯一性校验 + 保存返回 DTO。**命名一致、校验完整**。

#### 前端目录齐全性
`frontend/src` 下齐全：`pages / components / layouts / api / router / styles`；**缺 `stores/` 目录**（原因：当前版本无跨页共享状态需求，未引入 Pinia 实际使用，App.vue 用 ref 做 viewport 检测即可运行）。

#### 3 段核心代码审查（AC-8 评分依据）
- **PutawayService.confirm**（`PutawayService.java:29`）：`@Transactional(rollbackFor=Exception.class)` 正确；在 order.save 与 inventory.save 之间任何异常都会整批回滚（含超量抛 BizException）；order 与 inventory 在同一事务内一致性有保障。
- **GlobalExceptionHandler**（`GlobalExceptionHandler.java`）：BizException/Valid/Constraint/DataIntegrity/Exception 5 分支全覆盖，都包装成 HTTP 200 + `Result.fail(msg)` → 与前端 axios 拦截器 `code!=0` 分支形成闭环。
- **request.js**（`api/request.js`）：`baseURL="/api"` + 10s 超时；code=0 解包 data，code!=0 自动 `ElMessage.error` 并 reject 让调用侧进入 catch → 与 AC-7 反馈完全对齐。

**评分理由**：8 包齐全、命名一致、核心三段代码结构清晰。**-0.5 分 advisory**：DataInitializer 初始化种子 `PASEED0001` 单号格式与业务 `PAyyyyMMddNNNN` 略有偏差（无日期）。不影响功能（仅影响种子单号可辨识度）。

---

### CP-R9 ｜ AC-9 rule 可构建启动 —— score: **pass**

| 检查项 | Evidence |
|---|---|
| 后端构建 `mvn clean package -DskipTests` | EXIT_CODE=0；最后 5 行：`[INFO] BUILD SUCCESS` / `Total time: 3.452 s`；`target/wms-backend-0.0.1-SNAPSHOT.jar` 生成并 repackage ✅ |
| 清理 `data/` 后启动 jar 20s，`GET /api/products` 返回 `code=0` 且 `totalElements=3`（种子 3 商品） | code=0，totalElements=3，skus=[SP-003, SP-002, SP-001] ✅ |
| 前端 `npm install --legacy-peer-deps`（本环境 node_modules 已存在跳过安装）+ `npm run build` | ✓ built in 5.76s；EXIT=0；`dist/index.html` 存在；`dist/assets/` 含 20 个 chunk（8 个 CSS + 12 个 JS），主 JS 1.2MB ✅ |

**对齐 spec 两种数据库启动方式**：README.md 提供 H2 默认（已实测）+ application-mysql.yml 切换方式（文档齐全，未实测 MySQL 属沙箱环境无 MySQL 服务，不影响 pass）。

---

## 二、Review History

| 轮次 | 角色 | 日期 | 结论 | 关键变化 |
|---|---|---|---|---|
| R1 | 独立只读审查（本报告） | 2026-09-04 | **Result: pass**（9/9 CP 通过，0 actionable findings，rubric 分均超阈值） | 首版审查报告产出 |

---

## 三、Review R1 详细报告

### 3.1 Checks Performed（执行过的实际检查清单，非臆测）

1. **代码结构静态检查（只读）**
   - tree `/workspace/backend/src/main/java`：8 包齐全，35 个 Java 文件，命名一致
   - tree `/workspace/frontend/src`：pages(6)/components(4)/layouts(2)/api(6+request.js)/router(index.js)/styles(2 scss) 齐全，stores/ 不存在
   - 读取 PutawayService.confirm @Transactional 方法源码
   - 读取 GlobalExceptionHandler 5 分支源码
   - 读取 request.js axios 拦截器 code/msg/data 解包逻辑
   - 读取 README.md（9 章节齐全：启动、MySQL 切换、FAQ 6 条、接口表 6 模块 13 接口）
   - 读取 start.sh（build/dev/stop 三分支完整，支持清除 data/）
   - 抽查 ProductService.create / LocationService.delete（AC-1/AC-2 实现一致性）

2. **构建 + 启动 + API 实际 curl 测试（非 mock）**
   - 后端 `mvn clean package -DskipTests` exit=0，jar 生成
   - 清 data/ → 启动 jar 非阻塞 → sleep 12s 健康检查通过
   - 执行 C1~C5 五大类共 **21 次** curl 请求（详见上方 Evidence 表），每条断言均通过
   - 前端 `npm run build` exit=0，dist 产物齐全

3. **响应式与体验 rubric 静态评分**
   - Desktop/Mobile Layout 源代码 + App.vue 断点判断代码审查
   - SCSS `$md:768px` 与 JS `window.innerWidth<768` 数值一致性核对
   - `.primary-mobile-btn` 在 index.scss 中 `min-height:44px` 声明 + Putaway.vue 两处引用核对
   - router/index.js 路由数量与路径核对
   - axios 拦截器成功/失败分支审查
   - Products/Locations 两页 `ElMessageBox.confirm` 调用审查
   - Putaway.vue 成功 toast `上架成功 ${qtyDone} 件` 与结果弹窗文案审查
   - 后端 5 类中文错误 msg（实测而非只看代码）

### 3.2 Evidence 汇总表

| CP | Type | Result | 关键实测证据摘要 |
|---|---|---|---|
| CP-R1 | rule | pass | POST sku=SP-R1 id=4；重 sku msg 含已存在；PUT sku 不变；DELETE 后 total=3 |
| CP-R2 | rule | pass | DELETE A-01-01 msg 含"存在库存"；POST 再 DELETE 空库位成功 |
| CP-R3 | rule | pass | orderNo PA202609040001/2/3 互不相同；planQty=0 msg"必须大于0"；详情 sku/putQty=0/progress=0 |
| CP-R4 | rule | pass | qty 60 OK；qty 101 失败且 putQty 仍=60（回滚）；qty 40 → DONE；DONE 后拒绝且 items=2 |
| CP-R5 | rule | pass | summary SP-001 totalQty=100；details 60+40=100 == summary |
| CP-R6 | rubric(7) | 9/10 pass | 双 layout + 断点一致 + 44px 大按钮 + 7 条路由（缺 stores/） |
| CP-R7 | rubric(7) | 9.5/10 pass | 拦截器 + 中文文案 + 删除二次确认 + Putaway 成功具体件数 |
| CP-R8 | rubric(7) | 9.5/10 pass | 后端 8 包齐全；前端 6/7 目录齐全；核心 3 段代码审查通过；种子单号 PASEED0001 格式轻微不一致 |
| CP-R9 | rule | pass | 后端构建 BUILD SUCCESS；启动 totalElements=3；前端 build 5.76s dist 齐全 |

### 3.3 Checkpoint Results（最终）

| # | Checkpoint | Verdict | Evidence anchor |
|---|---|---|---|
| 1 | CP-R1 商品 CRUD | ✅ pass | 上文 C1 4 条 curl 记录 |
| 2 | CP-R2 库位 CRUD + 删除保护 | ✅ pass | 上文 C2 3 条 curl 记录 |
| 3 | CP-R3 上架单创建 + 单号递增 + 详情 | ✅ pass | 上文 C3 4 条 curl 记录 |
| 4 | CP-R4 上架事务 + 回滚 | ✅ pass | 上文 C4 4 条 curl 记录（含失败后 putQty 未变） |
| 5 | CP-R5 库存汇总 = 明细 | ✅ pass | 上文 C5 2 条 curl 记录 |
| 6 | CP-R6 双端响应式 | ✅ pass（9/10 ≥ 7） | App.vue 断点 + SCSS $md + router 7 条 + primary-mobile-btn |
| 7 | CP-R7 导航 + 反馈 | ✅ pass（9.5/10 ≥ 7） | request.js 拦截器 + 中文实测文案 + ElMessageBox + Putaway 具体 toast |
| 8 | CP-R8 代码分层 | ✅ pass（9.5/10 ≥ 7） | 后端 8 包 + 前端 6/7 目录 + PutawayService/GEXH/request.js 三段核心代码 |
| 9 | CP-R9 可构建启动 | ✅ pass | BUILD SUCCESS + totalElements=3 + npm run build OK + dist 齐全 |

### 3.4 Findings 分级

**Actionable Findings（致 Result=fail 的 bug，数量：0）**

> 无。所有 rule CP 通过，无构建失败、无数据一致性破坏、无无法启动问题。

**Advisory Findings（可改进但不影响功能，数量：3）**

| # | 级别 | 简短描述 | 影响 AC/CP |
|---|---|---|---|
| A-1 | advisory | 种子上架单 orderNo=`PASEED0001`，与业务规则 `PAyyyyMMddNNNN` 格式轻微不一致 | AC-3 / CP-R3（不影响逻辑，仅可辨识度） |
| A-2 | advisory | 前端 `stores/` 目录不存在，README 架构图与「Vite Pinia」文字提及 Pinia，但实际未使用 | AC-6 / CP-R6（不影响功能；若后续扩展跨页状态建议加 Pinia） |
| A-3 | advisory | Putaway.vue 前端剩余数量校验使用 `ElMessage.warning` 轻提示；与后端"超过计划数量，剩余N件" ElMessage.error 视觉等级略有差异 | AC-7 / CP-R7（不影响功能；前端拦截器走 warning 分支与后端 error 分支视觉略有不统一） |

### 3.5 Recommended Issues（均为 advisory 级别，无紧急优先）

| # | 标题 | 优先级 | 影响 AC/CP | 复现步骤 / 观察 | 修复建议 | 修复后的回归条件 |
|---|---|---|---|---|---|---|
| RI-1 | DataInitializer 种子 orderNo 格式未按 PAyyyyMMddNNNN | P4 Low | AC-3 / CP-R3 | GET /api/orders/pending 可见 `PASEED0001` 单号无日期前缀 | 在 DataInitializer.java 中构造 seed orderNo 时使用与 PutawayOrderService.create 相同的 `yyyMMdd` 日期 + NNNN 序号生成器 | 启动后 `/api/orders/pending` 首个 orderNo 格式可由 `^PA\d{8}\d{4}$` 匹配；仍为 PENDING SP-001 planQty=100 |
| RI-2 | 前端未实际使用 Pinia（stores/ 目录缺失）与 README 描述不一致 | P4 Low | AC-6 / CP-R6 与 AC-8 / CP-R8 | ls frontend/src/stores → NOT FOUND；README 架构图写 Vue3+Pinia | 二选一：(a) 从 README 架构图移除 Pinia 字样，改为 "组合式 API (Composition API) + ref/reactive 管理状态"；或 (b) 新增 stores/wms.js 管理 viewport 与 pending 列表（若后续扩展跨页状态） | README 与代码状态管理方式一致；前端构建 npm run build 仍 exit=0 |
| RI-3 | Putaway 页前端剩余量超限视觉等级与后端不一致 | P5 Info | AC-7 / CP-R7 | Putaway Step3 前端填 `qty > remaining` 触发 ElMessage.warning（黄），而后端同一场景经 axios 拦截器触发 ElMessage.error（红） | 统一为 ElMessage.error 或统一 warning；建议与后端一致用.error 以强化风险提示 | 前端剩余量超限与后端超量失败均触发相同颜色等级 toast；其他功能无 regress |

---

## 四、最终 Result 判定

| 判定维度 | 结果 |
|---|---|
| 所有 rule CP（R1~R5、R9）通过？ | ✅ 是（6/6 全部 pass） |
| 每个 rubric CP 分 **≥ 阈值**？ | ✅ 是（R6=9/10≥7，R7=9.5/10≥7，R8=9.5/10≥7） |
| Actionable Findings？ | ❌ 无 |
| Blocking Issue（环境/权限/依赖）？ | ❌ 无 |

**🔵 最终 Result: `pass`**

9 个 Acceptance Criteria 全部满足（rule 全通过 + rubric 分均超阈值），无可复现的 actionable bug，构建与启动链路完整，库存一致性事务回滚实测到位，双端响应式代码与体验锚点齐全。

附：关键构建/测试命令最后几行输出

（1）后端 mvn package 最后 5 行
```
[INFO] Replacing main artifact wms-backend-0.0.1-SNAPSHOT.jar with repackaged archive
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  3.452 s
```

（2）健康检查种子
```
code= 0
totalElements= 3
skus= ['SP-003', 'SP-002', 'SP-001']
```

（3）前端 npm run build 最后 3 行
```
dist/assets/index-XktWE_qQ.js  1,207.73 kB │ gzip: 387.55 kB
(!) Some chunks are larger than 500 kB — 仅 chunk 警告（可后续 code-split，未阻断构建）
✓ built in 5.76s  EXIT=0
```

（4）C4 回滚验证关键输出
```
101件失败后 msg=| 超过计划数量，剩余40件 | → 立即 GET order putQty= 60（回滚生效，未被污染）
```
