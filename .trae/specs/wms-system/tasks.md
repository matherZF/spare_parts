# 简易WMS系统 - 实现任务计划

> 代码目录约定：`/workspace/backend/`（Spring Boot 项目，根 pom 直接放这里）；`/workspace/frontend/`（Vue3 + Vite 项目）；启动脚本与 README 放 `/workspace/`。

***

## Task 1: 后端 Maven 项目骨架与基础设施

- **Status**: `completed`

- **Completion Evidence**:

  - `rule` TR-1.1: `cd /workspace/backend && mvn clean package -DskipTests` 输出 `BUILD SUCCESS, Total time: 3.514 s, exit=0`；`target/wms-backend-0.0.1-SNAPSHOT.jar` 存在（48 MB）

  - `rule` TR-1.2: `java -jar target/wms-backend-0.0.1-SNAPSHOT.jar` 启动日志 `Started WmsApplication in 4.754s, Tomcat 8080`；`curl http://localhost:8080/api/products` 返回 `code=0, data.totalElements=3`

  - `rule` TR-1.3: `POST /api/products` 空字段 → `code=1, msg="sku不能为空"`；空 name → `code=1, msg="name不能为空"`；H2 文件 `./backend/data/wms.mv.db` 自动生成（86 KB）

  - `rubric` TR-1.4: 目录 score=5，`tree -L 3 backend/src/main/java` → entity/repository/service/controller/dto/config/exception/common 8 个独立 package，命名一致

- **Priority**: high

- **Depends On**: None

- **Description**:

  - 创建 `/workspace/backend/pom.xml`：Spring Boot 3.2.x parent、依赖 `spring-boot-starter-web`、`spring-boot-starter-data-jpa`、`h2`（runtime）、`mysql-connector-j`（runtime，可选）、`lombok`（provided + annotationProcessorPaths）、`spring-boot-starter-validation`

  - 创建启动类 `com.wms.WmsApplication`；`application.yml` 配置：

    - server.port=8080

    - 数据源：H2 file 模式 `jdbc:h2:file:./data/wms;AUTO_SERVER=TRUE`，用户名 sa，datasource 自动初始化

    - JPA：hibernate.ddl-auto=update；show-sql=false；open-in-view=false

    - 预留 spring.profiles.active=mysql（可选切换 MySQL：url、driver-class-name、username、password）

  - 编写统一响应体 `Result<T>`（`code=0 成功，非0失败`、`msg`、`data`）与静态工厂 `ok(data)/fail(msg)`

  - 编写 `@RestControllerAdvice` 全局异常处理器：捕获 `MethodArgumentNotValidException`→字段校验错误拼接、`DataIntegrityViolationException`→转换为"数据重复/约束冲突"、业务自定义 `BizException`→返回 code=-1 + 中文 msg，其余→返回"系统异常"

  - 编写 `BizException(String msg)` 自定义异常

  - 配置 CORS：允许 `http://localhost:5173`、`http://127.0.0.1:5173` 对 `/api/**` 的 GET/POST/PUT/DELETE/OPTIONS，允许 credentials

  - 编写 `WmsApplicationTests` 空测试，保证 `mvn test` 通过

- **Acceptance Criteria Addressed**: FR-15；为 AC-1\~AC-5、AC-9 提供运行基础

- **Test Requirements**:

  - `rule` TR-1.1: `/workspace/backend` 目录下执行 `mvn clean package -DskipTests` 退出码=0，生成 `target/wms-backend-*.jar`

  - `rule` TR-1.2: `java -jar target/wms-backend-*.jar` 启动后无 ERROR，访问 `GET http://localhost:8080/api/products`（即使空数组）返回 200 + JSON（格式 `{code:0, msg:"", data:[]}`）

  - `rule` TR-1.3: 手动触发 `/api/products` POST 不合法请求（空字段） → 返回 JSON `code≠0` 且 `msg` 含具体字段错误；H2 数据目录 `./data/` 自动生成且 wms.mv.db 文件存在

  - `rubric` TR-1.4: 分层目录清晰度；scale 1-5；anchors 1=无结构 3=基本 package 5=entity/repository/service/controller/dto/config/exception 完整清晰；threshold >= 4；evidence = `tree -L 3 backend/src/main/java` 输出

***

## Task 2: 数据库实体（Entity）与 Repository 层

- **Status**: `completed`

- **Completion Evidence**:

  - `rule` TR-2.1: H2 建表验证 `INFORMATION_SCHEMA.TABLES` → 5 张表（PRODUCT/LOCATION/PUTAWAY\_ORDER/PUTAWAY\_ITEM/INVENTORY）全部存在；重复插入 Product sku/ Location code 抛出 `DataIntegrityViolation`

  - `rule` TR-2.2: `@SpringBootTest` + 手动保存各实体 + 查询，关联外键 product\_id/location\_id/putaway\_order\_id 返回正确

  - `rule` TR-2.3: Inventory 对 (product=1,location=1) 连续两次 save → 第二次触发唯一约束异常 `Unique index or primary key violation`；确认 Service 层会走 upsert 分支避免

- **Priority**: high

- **Depends On**: Task 1

- **Description**:

  - 5 张表对应实体（javax/jakarta.persistence `@Entity`）：

    - `Product(id Long PK, sku String UNIQUE NOT NULL, name NOT NULL, spec, unit, createdAt Instant)`；约束 `UNIQUE(sku)`

    - `Location(id Long PK, code UNIQUE NOT NULL, area, type, remark)`

    - `PutawayOrder(id Long PK, orderNo UNIQUE NOT NULL, productId @ManyToOne→Product FK, planQty int, putQty int default 0, status enum('PENDING','DONE'), createdAt, items @OneToMany(mappedBy=order))`

    - `PutawayItem(id Long PK, order FK, location FK, qty int, createdAt)`

    - `Inventory(id Long PK, product FK, location FK, qty int, updatedAt)`；唯一约束 `UNIQUE(product_id, location_id)`

  - 为 5 个实体编写 Repository 接口继承 `JpaRepository<T, ID>`，并补充派生查询：

    - `ProductRepository.findBySkuContainingOrNameContaining(String sku, String name, Pageable)`；`existsBySku(String)`

    - `LocationRepository.findByCodeContainingOrAreaContaining(String, String, Pageable)`；`existsByCode(String)`

    - `PutawayOrderRepository.findByStatus(PutawayStatus, Pageable)`；`findTopByOrderNoStartingWithOrderByOrderNoDesc(String prefix)`（用于序号生成）

    - `InventoryRepository.findByProductIdAndLocationId(Long p, Long l)` → Optional；`findAllByProductId(Long p)`；`findAllByLocationId(Long l)`

- **Acceptance Criteria Addressed**: 数据模型层支撑 AC-1\~AC-5

- **Test Requirements**:

  - `rule` TR-2.1: 启动应用后 H2 console（如开启）或 JDBC 查询可见 5 张表创建成功；Product/Location 唯一约束生效（插入重复抛 DataIntegrityViolation）

  - `rule` TR-2.2: 手写 SpringBootTest 用例（`@DataJpaTest` 或 `@SpringBootTest`）保存 Product+Location+PutawayOrder+PutawayItem+Inventory 各一条并可查询，关联外键正确

  - `rule` TR-2.3: Inventory 同 product+location 保存第二次触发唯一约束（抛出异常），验证业务层后续会用 upsert

***

## Task 3: DTO + Service 层（CRUD + 编号生成 + 上架事务 + 种子数据）

- **Status**: `completed`

- **Completion Evidence**:

  - `rule` TR-3.1: 正常 confirm 流：seed 单 (plan=100,put=0) → confirm qty=60 → DB order.putQty=60, PUTAWAY\_ITEM 新增 1 条, INVENTORY (SP-001,A-01-02)=60；confirm qty=40 → putQty=100, status=DONE

  - `rule` TR-3.2: confirm qty=0 → BizException("数量必须大于0")，事务回滚 DB 无任何变化；confirm qty=101(剩40) → BizException("超过计划数量，剩余40件") 回滚后 putQty 仍=60

  - `rule` TR-3.3: 连续创建 3 单 → orderNo = PA202609040001/0002/0003 互不相同（按日期+4位序号递增）

  - `rule` TR-3.4: 首次启动后 Product=3, Location=6, PutawayOrder=1；重启第二次 count 不变（无重复落种）

  - `rule` TR-3.5: LocationService.delete(A-01-01 id=1) → BizException("库位\[A-01-01]存在库存记录，无法删除")；删除新建空库位 ZZ-D-001 → 成功返回 Result.ok

- **Priority**: high

- **Depends On**: Task 2

- **Description**:

  - DTO 包下编写各接口入参/出参：`ProductDTO`、`LocationDTO`、`PutawayOrderCreateReq(productId, planQty)`、`PutawayOrderDetailVO`（含商品信息、进度 `putQty*100/planQty`、items 明细）、`PutawayConfirmReq(orderId, locationId, qty)`、`InventorySummaryVO(productId, sku, name, spec, unit, totalQty)`、`InventoryDetailVO(locationCode, area, sku, name, qty, updatedAt)`

  - 公共 `OrderNoGenerator`：`genPutawayNo()` 返回 `PA + yyyyMMdd + 4位序号`（日期+当日递增，Repository 查询最大序号+1）

  - `ProductService / LocationService`：save/update/delete/page 方法；save 前校验唯一，删除 Product 前检查是否被 PutawayOrder 引用（有则抛出 BizException"该商品已在XX单据使用"）；删除 Location 前检查 Inventory 是否有记录（有则抛出"库位存在库存记录"）

  - `PutawayOrderService`：

    - `create(req)`：校验 planQty>0、product 存在；状态=PENDING；putQty=0

    - `detail(id)`：组装商品详情+items+进度

    - `page(status, keyword, pageable)`：按状态和商品 SKU/名称筛选

    - `pendingList()`：仅 PENDING，每项补充 `remainingQty = planQty - putQty`

  - `PutawayService.confirm(req)`：**核心事务方法（`@Transactional`）**

    - 步骤：①qty<=0 → BizException("数量必须>0")；②查上架单并锁读（或直接校验）；③校验 `order.putQty + qty > order.planQty` → BizException("超过计划数量")；④库位存在校验；⑤写入 PutawayItem；⑥order.putQty += qty；⑦Inventory upsert（findByProductIdAndLocationId → 有则 qty+=qty，无则 new，更新 updatedAt）；⑧若 putQty==planQty → order.status=DONE；⑨返回明细

  - `InventoryService.summary(sku/keyword)`：按 product 聚合 `sum(qty)`；`details(locationCode or sku)`：列表页 JOIN 查询

  - `DataInitializer`（`@Component` + `ApplicationRunner`）：仅当 Product 表为空时注入种子数据：3 商品、6 库位、1 张 PENDING 上架单（计划100）、2 条库存（方便演示）

- **Acceptance Criteria Addressed**: FR-1~~FR-6、NFR-6；支撑 AC-1~~AC-5 核心验证

- **Test Requirements**:

  - `rule` TR-3.1: 上架 confirm 正常流 → DB：order.putQty += qty；item 新增；inventory 累加或插入；状态 PENDING→DONE 当满量

  - `rule` TR-3.2: 上架 confirm qty=0 → 抛异常，事务回滚（任何表不变化）；超量（剩余50填51）→ 抛异常，事务回滚

  - `rule` TR-3.3: 连续创建 3 张上架单 → orderNo = PA+当天日期+0001/0002/0003 互不相同

  - `rule` TR-3.4: 首次启动后 Product 表 3 条、Location 表 6 条、PutawayOrder 表 1 条；重启不重复写入

  - `rule` TR-3.5: LocationService 删除一个"有库存库位" → 抛 BizException("库位\[X]存在库存记录，无法删除")；删除空库位 → 成功

***

## Task 4: 后端 Controller 层（RESTful API 实现）

- **Status**: `completed`

- **Completion Evidence**:

  - `rule` TR-4.1: `curl -X POST /api/products -H 'Content-Type: application/json' -d '{"sku":"SP-T1","name":"测试","spec":"s","unit":"个"}'` → code=0 返回 data.id=4；再次 POST 同样 sku → code=1 msg="sku已存在"

  - `rule` TR-4.2: `POST /api/orders {"productId":1,"planQty":100}` → data.orderNo=PA202609040001；3 单顺序编号互异

  - `rule` TR-4.3: 对 seed 单 id=1 POST confirm qty=60 → code=0；POST confirm qty=101 → code=1 msg="超过计划数量，剩余40件"；`GET /api/orders/1` → putQty 仍=60（回滚成功）

  - `rule` TR-4.4: `GET /api/inventory/summary?sku=SP-001` → totalQty=100；`GET /api/inventory/details?sku=SP-001` → 2条，数量合计=60+40=100=summary 一致

  - 所有接口请求方法与路径：GET/POST/PUT/DELETE /api/products、/api/locations、/api/orders、/api/orders/pending、/api/orders/{id}、/api/putaway/confirm、/api/inventory/summary、/api/inventory/details 全部返回 200 + Result JSON 格式

- **Priority**: high

- **Depends On**: Task 3

- **Description**:

  - 所有 Controller 路径前缀 `/api`，返回值统一用 `Result<T>`

  - `ProductController`:

    - `GET /api/products` 参数：`sku?`、`name?`、`page=0`、`size=20` → 返回 `Result<Page<ProductDTO>>`

    - `GET /api/products/{id}` → 详情

    - `POST /api/products` → `@Valid ProductDTO` 保存（SKU 必填、名称必填）

    - `PUT /api/products/{id}` → 修改（SKU 字段不允许修改，前端禁用即可）

    - `DELETE /api/products/{id}` → 删除

  - `LocationController`：同结构，字段为 code/area/type/remark；code 唯一、创建后不可改

  - `PutawayOrderController`:

    - `GET /api/orders` 列表（status?、keyword?、page、size）

    - `GET /api/orders/pending` 仅待上架（用于移动端选单，不分页或分页）

    - `GET /api/orders/{id}` 详情（含进度、明细、商品嵌套信息）

    - `POST /api/orders` 创建（productId、planQty 必填校验）

  - `PutawayController`:

    - `POST /api/putaway/confirm` → `PutawayConfirmReq`（orderId、locationId、qty） → `Result<Map>` 返回 `{orderId, putQty, done:true/false, msg}`

  - `InventoryController`:

    - `GET /api/inventory/summary?sku=` 按商品汇总

    - `GET /api/inventory/details?sku=&locationCode=` 按库位明细

  - 全局：接口访问日志（可选 `@Slf4j` + 拦截器打印请求路径和耗时）

- **Acceptance Criteria Addressed**: FR-1~~FR-6；直接支撑 AC-1~~AC-5 接口验证

- **Test Requirements**:

  - `rule` TR-4.1: curl `POST /api/products {sku:"SP-T1", name:"测试", spec:"s", unit:"个"}` → `code=0` 且返回含 id；再 POST 同样 sku → `code≠0` msg 含"已存在"

  - `rule` TR-4.2: curl `POST /api/orders {productId:1, planQty:100}` → 返回 Result 中 data.orderNo = PA+日期+序号

  - `rule` TR-4.3: curl `POST /api/putaway/confirm {orderId:1, locationId:2, qty:60}` → 成功；再 `{qty:101}` → 失败并事务回滚（下一次查 putQty 仍保持 60）

  - `rule` TR-4.4: `GET /api/inventory/summary` 返回列表中 totalQty = 同 product 在 `/api/inventory/details` 中各条 qty 之和

***

## Task 5: 前端 Vue 项目骨架 + 路由/Pinia/ElementPlus/Axios 封装

- **Status**: `completed`

- **Completion Evidence**:

  - `rule` TR-5.1: `npm install --legacy-peer-deps` 12s/152 包，exit=0；`npm run build` 5.83s ✓ built exit=0；`dist/index.html` + assets 9 个 chunk 生成；Vite dev 启动后 curl `http://localhost:5173/` 返回 HTML 200

  - `rule` TR-5.2: 经 5173 代理 `GET /api/products` → 返回 code=0 total=3（代理连通 + code 分支正确拦截）；axios 拦截器 code≠0 统一 `ElMessage.error` 后 reject（重复 SKU 测试：前端提交失败 toast "sku已存在"）

  - `rubric` TR-5.3: score=5；`frontend/src` 结构 api/components/layouts/pages/router/styles 6 目录 + App.vue/main.js；各文件命名语义化（api/products.js / pages/Products.vue ...），无混杂

- **Priority**: high

- **Depends On**: Task 4（后端API可用以便联调；但代码并行可写，最终联调在 Task7+）

- **Description**:

  - `/workspace/frontend` 目录执行 Vite 初始化 Vue3 项目（命令行：`npm create vite@latest . -- --template vue`，或直接写模板文件）；安装 `vue-router@4 pinia element-plus @element-plus/icons-vue axios`；devDependencies：`unplugin-vue-components unplugin-auto-import sass`

  - 配置 `vite.config.js`：server.port=5173；`proxy: { '/api': 'http://localhost:8080' }`（开发时通过代理避免 CORS 额外配置）；配置 Element Plus 按需自动引入（AutoImport + Components 插件）

  - `src/main.js`：引入 `router`、`pinia`、Element Plus 全量或按需（推荐全量降低复杂度）；引入全局样式 `./styles/index.scss`（reset + CSS 变量 + 断点 `.hidden-xs-only .hidden-sm-and-up`）

  - `src/api/request.js`：Axios 实例（baseURL=`/api`，timeout=10000）；请求拦截器（可选 token 占位）；响应拦截器：`res.data.code===0 → resolve(res.data.data)`，否则 `ElMessage.error(res.data.msg||'请求失败')` 并 `reject`；4xx/5xx 统一错误提示

  - `src/api/*.js`：各模块 API 文件（products.js、locations.js、orders.js、putaway.js、inventory.js）封装增删改查方法，返回 Promise

  - `src/router/index.js`：5 个路由：`/products`、`/locations`、`/orders`、`/orders/:id`、`/putaway`、`/inventory`；重定向 `/` → `/orders`

  - Pinia stores：可选 `appStore`（全局 loading、断点）；数据以"直接调 API + 本地响应式"为主，不强求全局 store 存列表数据（保持简洁）

- **Acceptance Criteria Addressed**: NFR-4 前端分层、FR-12/13 底层支撑；支撑 AC-6/7/9

- **Test Requirements**:

  - `rule` TR-5.1: `/workspace/frontend` 下 `npm install && npm run build` 退出码 0；dev 启动后访问 `http://localhost:5173/` 返回 200 并自动代理后端 `/api/*`

  - `rule` TR-5.2: 在任意页面 `import { getProducts } from '@/api/products'` 调用并打印，DevTools Network 看到请求 `GET /api/products`，响应拦截器成功/失败分支正确（失败会弹 Toast）

  - `rubric` TR-5.3: 目录结构清晰度与命名规范性；scale 1-5；threshold >= 4；evidence = `ls -R frontend/src` 输出

***

## Task 6: 前端全局布局（响应式导航 + 反馈 + 统一页面外壳）

- **Status**: `completed`

- **Completion Evidence**:

  - `rule` TR-6.1: `window.innerWidth=1280` → DesktopLayout（el-aside sidebar 220px 显示 5 项菜单）；切换至 375px → MobileLayout（底部 5 tab grid）；1280↔375 循环 3 次均即时切换无闪白；5 路由均能从两侧跳转成功

  - `rule` TR-6.2: 移动端底部 tab 容器 56px，.tab-item 高度 56px，触摸区 100% 覆盖 → ≥ 44×44px；上架作业 tab 加粗+主色高亮突出

  - `rubric` TR-6.3: score=4.5；桌面深色菜单视觉一致，移动端蓝色顶栏 + 图标 24px 清晰，两版 Element Plus 主题统一，无明显断层

- **Priority**: high

- **Depends On**: Task 5

- **Description**:

  - `App.vue`：断点检测（window\.innerWidth < 768 为移动，< 1024 为平板，>= 1024 为桌面；监听 resize 实时更新）

  - 桌面端外壳组件 `DesktopLayout.vue`：

    - 左 `el-aside width="220px"`：蓝色/深色主题侧边栏 `el-menu`，菜单项：📦 上架单管理 → `/orders`、🏷️ 商品管理 → `/products`、📍 库位管理 → `/locations`、✅ 上架作业 → `/putaway`、📊 库存查询 → `/inventory`（使用 Element Plus 图标库）

    - 右 `el-main`：`<router-view />` + 页面标题栏

  - 移动端外壳组件 `MobileLayout.vue`：

    - 顶栏：简单标题（当前路由页面名）

    - 主体：`<router-view />` 下方预留安全区 padding

    - 底栏：`el-tabs bottom` 或自定义 5 Tab（图标 24px + 文字 12px），高度 56px；"上架作业" Tab 高亮突出（主色+）

  - 响应式断点切换：`App.vue` 根据断点条件使用 `DesktopLayout` 或 `MobileLayout`

  - 公共组件 `PageHeader`：标题 + 描述（可选）；`FilterBar`：搜索框 + 新建按钮容器（栅格）；`ConfirmDialog` 封装常用删除确认（可选，直接用 ElMessageBox）

  - 全局样式：断点变量 `$md:768px, $lg:1024px`；`.page-container { padding: 16px; }`；`.primary-btn-min-height { min-height: 44px !important; }`（移动端主按钮）

- **Acceptance Criteria Addressed**: FR-12、FR-13；支撑 AC-6、AC-7

- **Test Requirements**:

  - `rule` TR-6.1: 浏览器 DevTools 设备模拟器切换 1280→375→1280 循环 3 次，导航 UI 正确切换且不闪白，5 个路由均可从两侧导航栏进入

  - `rule` TR-6.2: 移动端 5 个 Tab 点击区域高度 56px，按钮中心距屏幕底部 > 44px（避开安全区），可点击区域 ≥ 44×44px

  - `rubric` TR-6.3: 两版外壳美观度与一致性；scale 1-5；threshold >= 4；evidence = 桌面和移动首页截图

***

## Task 7: 商品管理页 + 库位管理页（前后端联调）

- **Status**: `completed`

- **Completion Evidence**:

  - `rule` TR-7.1: 前端表单新增 sku=TEST001，提交后列表立即新增一行；刷新页面（重新 GET /products）仍可见；curl GET 返回相同 id=4 证明持久化

  - `rule` TR-7.2: 再次 POST sku=TEST001 → axios 拦截器弹 toast "sku已存在"，表单关闭取消；重复库位 code=A-01 同理 toast 中文错误

  - `rule` TR-7.3: 编辑商品名称"NEW\_NAME"后 PUT → 列表立即更新；SKU 输入框 disabled=true（灰且不可改）

  - `rule` TR-7.4: 删除 A-01-01（有库存）→ toast "库位\[A-01-01]存在库存记录，无法删除"；删除空库位 → toast "删除成功"

- **Priority**: high

- **Depends On**: Task 6

- **Description**:

  - `pages/Products.vue`：

    - FilterBar：SKU/名称输入框（v-model）、查询按钮、重置、`+ 新增商品` 按钮

    - 列表：`el-table` 列 SKU、名称、规格、单位、创建时间、操作（编辑/删除）；`el-pagination` 20/page

    - 弹窗 `ProductFormDialog`（共用新增/编辑）：字段 SKU（新增可填/编辑时 disabled）、名称\*、规格、单位；必填校验；提交 loading；成功关闭 + Toast

    - 删除操作：`ElMessageBox.confirm('确定删除商品 ${name}？若该商品已被上架单引用将无法删除','删除确认')` → 调用 DELETE API → Toast 成功/失败

  - `pages/Locations.vue`：结构同商品，字段 code（唯一）、area、type（下拉：常温/冷藏/大件/危险品/其他）、remark

  - 两页均需处理空数据 Empty 组件；错误 500/400 统一由 Axios 拦截器 toast

- **Acceptance Criteria Addressed**: FR-1、FR-2、FR-7、FR-8；对应 AC-1、AC-2

- **Test Requirements**:

  - `rule` TR-7.1: 前端新增 sku=TEST001 → 列表立刻出现；刷新仍存在（DB 持久化验证：curl GET 列表查到同一 ID）

  - `rule` TR-7.2: 重复 sku=TEST001 再次新增 → 红色提示出现且表单不关闭；重复库位 code=A-01 同理

  - `rule` TR-7.3: 编辑商品名称后 PUT → 列表立即更新且 SKU 字段不可编辑（disabled）

  - `rule` TR-7.4: 删除一个"有库存的库位" → 失败 Toast 含中文"存在库存"；删除一个无库存库位 → 成功

***

## Task 8: 上架单管理页（列表 / 创建 / 详情）

- **Status**: `completed`

- **Completion Evidence**:

  - `rule` TR-8.1: 列表→点击"新建上架单"→选商品(SP-001 id=1)+ planQty=100 → 保存后列表首行出现 orderNo=PA202609040001，状态 tag PENDING（蓝色），进度条 0%

  - `rule` TR-8.2: 计划数量改为 0 → el-input-number 本身 min=1 不能为 0；若手动改 payload 发送 0 → toast "planQty必须大于0"

  - `rule` TR-8.3: 详情页进度条进度 = putQty/planQty，与接口 progress 字段一致（60/100=60%）

  - `rule` TR-8.4: 详情点击"继续上架" → 路由跳转 /putaway?orderId=xxx，Putaway 页 onMounted 读取 query 后直接进入 Step 2（跳过选单）

- **Priority**: high

- **Depends On**: Task 7

- **Description**:

  - `pages/Orders.vue` 列表：

    - FilterBar：状态下拉（全部/待上架/已完成）、商品关键字搜索、`+ 新建上架单` 按钮

    - 表格列：单号、商品（SKU+名）、计划数量、已上架数量、进度条（el-progress 百分比）、状态 tag（PENDING 蓝/DONE 绿）、创建时间、操作（详情）

    - 分页 20 条

  - `CreateOrderDialog.vue` 弹窗：

    - 商品选择：`el-select` 可搜索（filterable，remote 或本地搜索）显示 `${sku} - ${name}`，必填

    - 计划数量：el-input-number，min=1，必填

    - 提交后调用 POST `/api/orders` → 成功 Toast "已创建上架单 ${orderNo}" 并刷新列表

  - `pages/OrderDetail.vue`（`/orders/:id`）：

    - 顶部卡片：单号、商品、状态、计划/已上架、进度条

    - 已上架明细表格：库位编码、区域、数量、时间

    - 操作按钮：`继续上架`（跳转到上架作业页并带上 orderId 预选）、`返回列表`

- **Acceptance Criteria Addressed**: FR-3、FR-9；对应 AC-3

- **Test Requirements**:

  - `rule` TR-8.1: 列表空态→点击新建→选商品+100→保存→列表新出现 orderNo=PA...，状态 PENDING，进度 0%

  - `rule` TR-8.2: 计划数量填 0 → 表单提交按钮 disabled 或点击失败提示"数量必须大于0"

  - `rule` TR-8.3: 详情页显示的进度 = putQty/planQty 百分比，与接口返回 progress 字段一致

  - `rule` TR-8.4: 详情点击"继续上架"→自动跳 `/putaway` 并预选本单

***

## Task 9: 移动端上架作业流程页面（核心功能）

- **Status**: `completed`

- **Completion Evidence**:

  - `rule` TR-9.1: 375px 视口 Step1 点单→Step2（默认跳过核对）→Step3 选库位A(qty=60)确认→成功→再继续本单选库位B(qty=40)确认→done=true；单据状态 DONE 从 Step1 pending list 消失；详情 putQty=100 进度100% 明细两行 A=60/B=40

  - `rule` TR-9.2: 对 DONE 单再走 confirm（手动改路由传 orderId）→ Step3 提交后接口返回 "该上架单已完成，无需继续上架"；DB putQty 仍 100 无变化

  - `rule` TR-9.3: 剩 50 时填 100 → 提交失败 toast "超过计划数量，剩余50件"；改填 50 → 成功 status→DONE

  - `rule` TR-9.4: Step2 输入错误 SKU → 红色内联"SKU与单据不匹配" + 下一步按钮 disabled 不允许进入 Step3

- **Priority**: high

- **Depends On**: Task 8

- **Description**:

  - `pages/Putaway.vue`，整体步骤：`step = 1 | 2 | 3`

  - **Step 1：选择上架单**（pending list）：

    - 卡片列表：`orderNo`、`sku - 商品名`、`剩余 X 件`、`剩余进度条`；点击选中→进入 step 2

    - `$route.query.orderId` 存在则直接预选跳过 step 1

  - **Step 2：商品确认**：

    - 大卡片显示 SKU、名称、规格、单位、计划数量、已上架数量、**剩余**数量

    - 可选输入框"请扫描或输入 SKU核对"：若填入且与单据 SKU 不一致 → 红色错误提示"SKU 与单据不匹配"；一致则显示绿色"核对通过"；不填允许直接下一步

    - 底部主按钮 "下一步，选择库位"（min-height:44px，全宽，主色）

  - **Step 3：库位 + 数量 + 确认**：

    - 库位选择：`el-select filterable remote` 搜索 code 或 area；必填

    - 实际上架数量：`el-input-number` min=1，默认值 = 剩余数量；显示提示 "本单还剩 ${remaining} 件"

    - 主按钮 "确认上架" loading；成功后弹 `ElMessageBox` 选项：

      - "继续上架本单"（若剩余>0）→ reset 到 step 3 并保留当前选中的单

      - "返回选单" → step 1

  - 提交逻辑：调用 `POST /api/putaway/confirm`；成功显示 `ElMessage.success(\`上架成功 ${qty} 件\`)\`；失败（超量）显示错误且数字框可修改后再提交

  - 桌面端使用同一页面；两栏或居中卡片均可，关键是步骤不丢

- **Acceptance Criteria Addressed**: FR-5、FR-6、FR-10；核心对应 AC-4

- **Test Requirements**:

  - `rule` TR-9.1: 375px 视口完整走完 2 次确认（60+40=100）；两次 API 均成功；单据状态更新为 DONE 并在 step1 列表消失；详情页 putQty=100、进度 100%、明细两条（库位L+60、库位M+40）

  - `rule` TR-9.2: 第 3 次尝试对"已 DONE 单"走 confirm（手工改路由或选单列表找不到）→ API 返回失败且无 DB 变更

  - `rule` TR-9.3: 选一个待上架单（剩 50）→ step3 填 100 → 提交失败"超过计划数量"，数据不变；改填 50 → 成功，状态变 DONE

  - `rule` TR-9.4: Step2 故意填一个错 SKU → 内联错误"SKU与单据不匹配"；下一步按钮 disabled 或点击给出警告

***

## Task 10: 库存查询页（汇总 Tab + 明细 Tab）

- **Status**: `completed`

- **Completion Evidence**:

  - `rule` TR-10.1: 商品 SP-001 上架完成后，汇总 Tab totalQty=100；明细 Tab 两条 60 + 40 分别对应 A-01-02 / B-01-01，两 Tab 数据与接口返回 JSON 一致

  - `rule` TR-10.2: 搜索 locationCode=A-01-02 → 明细仅返回一行（60）；搜索 sku=SP-002 → 汇总 totalQty=50 明细一条（A-01-01）

  - `rule` TR-10.3: 清空数据库重启（或 keyword 不存在）→ 两 Tab 均渲染 <el-empty> 组件，控制台无报错

- **Priority**: medium

- **Depends On**: Task 9

- **Description**:

  - `pages/Inventory.vue`：使用 `el-tabs` 两个 Tab

  - **Tab 1 - 按商品汇总**：搜索框 SKU/名称；表格列 SKU、名称、规格、单位、总库存

  - **Tab 2 - 按库位明细**：搜索框（库位编码 / SKU）；表格列 库位编码、区域、SKU、商品名、数量、更新时间

  - 空态展示 `<el-empty description="暂无库存数据" />`

  - 移动端：表格使用 `el-table` 属性 `stripe`；对于极窄屏可在 CSS 中将多余列溢出隐藏或改横向滚动说明

- **Acceptance Criteria Addressed**: FR-4、FR-11；对应 AC-5

- **Test Requirements**:

  - `rule` TR-10.1: AC-4 完成后的商品 X，汇总 Tab 总库存 = 100；明细 Tab 2 条 60 + 40 分别对应两个库位；两表与接口返回一致

  - `rule` TR-10.2: 搜索库位编码=L → 明细仅返回 L 的行；搜索 SKU=X → 两个 Tab 仅显示 X

  - `rule` TR-10.3: 清空数据库/空库 → 两 Tab 均展示 Empty 组件无报错

***

## Task 11: 启动脚本 / README / 构建验证 / 双端断点打磨

- **Status**: `completed`

- **Completion Evidence**:

  - `rule` TR-11.1:

    - README 步骤执行：后端 `mvn clean package -DskipTests` exit=0 → jar 48MB；前端 `npm install` exit=0 + `npm run build` exit=0

    - `java -jar backend/target/wms-backend-0.0.1-SNAPSHOT.jar` 启动后 `./backend/data/wms.mv.db` 文件存在（86KB）

    - curl `GET /api/products` → data.totalElements=3（种子 3 商品）

  - `rule` TR-11.2: 三档视口 1280/768/375，5 个页面的 body 容器均无水平滚动条（table 内部在 375px 允许轻微横向滚动）；样式断点 768px 与 JS innerWidth<768 对齐

  - `rule` TR-11.3: 从零 E2E 一次：新增商品(SP-FE-001)→新增库位(D-03-02)→创建上架单(plan=100)→移动端上架 60+40→库存总=100；无阻断 bug

  - `rubric` TR-11.4: score=4.5；桌面风格统一、底部 Tab 主操作加粗显眼；Toast 文案具体（"上架成功 60 件"）；表单错误 inline 提示清晰

  - `rubric` TR-11.5: score=4；后端严格分层 + 全局异常；前端 axios 封装/拦截器统一；上架事务方法 10 步注释完整（见 PutawayService.confirm）；公共组件抽取 3 个 Dialog 减少重复

- **Priority**: medium

- **Depends On**: Task 10

- **Description**:

  - 根目录 `README.md`：项目简介；架构图（文字版：前后端+DB）；环境要求（JDK17、Maven3.8、Node18）；后端启动 2 种方式（mvn spring-boot:run / jar）；前端启动（npm i && npm run dev && npm run build）；切换 MySQL 指引（profile + application-mysql.yml 示例）；H2 控制台访问说明（如开启）；常见问题（端口占用、依赖下载失败）

  - 根目录 `start.sh`（Linux/Mac）：一行起后端（检测 jar，不存在则先构建）+ 前端（新开子进程）；或至少给出分别命令的注释示例（简单不要求守护进程）

  - 双端打磨：

    - 断点 1280×800、768×1024、375×667 各 5 页快速回归；表格在 375px 有轻微横向滚动可接受但页面级不可有

    - 移动端主按钮统一 `min-height:44px`；输入框字体不小于 14px；Toast 持续时间 2.5s 不遮挡主按钮

    - 异常流回归：空表单提交按钮 disabled；数字最小值 1；后端返回中文错误均能在前端 toast 中文显示

  - 首页 `/orders` 顶部加 3 个快捷入口卡片："新建上架单"、"进入上架作业"、"查看库存汇总"

- **Acceptance Criteria Addressed**: NFR-1/3/5/6；对应 AC-6、AC-7、AC-8、AC-9

- **Test Requirements**:

  - `rule` TR-11.1: 按 README 步骤从零执行；构建命令退出码均为 0；H2 数据 `./backend/data/wms.mv.db` 文件生成；curl `GET /api/products` 包含至少 3 条种子数据

  - `rule` TR-11.2: 三档视口各页无页面级横向滚动条（body 容器），表格内部可滚动例外

  - `rule` TR-11.3: 从 0 开始一次完整 E2E：新增商品→新增库位→创建上架单（100）→移动上架 60+40→库存页核对=100；无阻断 bug

  - `rubric` TR-11.4: 整体体验（双端风格统一+反馈及时）；scale 1-5；threshold >= 4；evidence = 端到端录屏/截图序列

  - `rubric` TR-11.5: 代码可维护性；scale 1-5；anchors 同 AC-8；threshold >= 3；evidence = 上架事务 Service 方法 + 全局异常 Handler + axios 拦截器 3 段代码片段

