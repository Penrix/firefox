# Penrix Private × Firefox Android — 专属网站 App 模块需求与落地路线（Working）

> 记录日期：2026-11-04  
> 状态：**Firefox 方案优先试跑，尚未选定最终生产底座**。这是 Penrix 私人 Android 浏览器的产品契约，不是 Mozilla 官方需求。  
> **2026-11-04 最新用户定义：**“我有官方的Firefox ，你这个版本只做私人浏览器，深度定制我要求的几个网站。做到极致优化……就像是这几个网站的app一样……打开私人浏览器打开这几个网站就跟app里几个模块一样。”**最新方向以 [SITE-APP-SHELL.md](SITE-APP-SHELL.md) 为第一优先级。**这不是“通用 Firefox + 小说按钮”，也不是“网站快捷链接页”，而是专属站点 App 模块的容器。Chrome 扩展只是加分项。保持 K20 Android 9。  
> 对照：原项目 [Penrix/Penrix-Browser](https://github.com/Penrix/Penrix-Browser)，Kiwi 备用 [Penrix/Kiwi](https://github.com/Penrix/Kiwi)；本试验以 [Penrix/firefox](https://github.com/Penrix/firefox) 的 Firefox Android / Fenix 为主。

## 1. 我们真正要做的产品

**给作者本人使用的专属网站超级应用**：从私人模块主页进入作者指定的几个网站，每个站都拥有类似独立 App 的入口、内容导航、连续任务、站点动作、返回与状态恢复。Firefox Fenix/GeckoView 是运行平台，不作为用户主要感知的通用浏览器；未被专门适配的普通网页仅作为外链和异常兜底。具体场景和实现规范以 [SITE-APP-SHELL.md](SITE-APP-SHELL.md) 为准。

不要把“私人浏览器”误读成单一的隐私加固产品；更不能把它缩成“会打开某几个网址的 WebView”。**主要产品是几款网站专属 App 融合于一个入口，底层继承成熟 Firefox 浏览器基础。**

### 优先级与验收原则

| 级别 | 需求 | 应达到的行为 / 不能接受的结果 |
|---|---|---|
| P0 | Redmi K20 Pro / Android 9 | 真机安装、浏览、后台恢复；**不要求升级 Android** |
| P0 | **私人模块入口 + 各站专属 App 化** | 启动后展示专属模块而非通用 Firefox 首页；重点网站进入后具备自己的导航、页面状态、连续操作和返回恢复；普通网页模式是兜底，而非主界面 |
| P0 | 用户数据与旧 APK 保护 | 原 Penrix WebView 浏览器继续可用；新 APK 独立安装、独立数据；不覆盖旧本地未提交工作区、不清浏览器或站点登录数据 |
| P1 | 广告与页面净化 | 完整版 uBlock Origin 优先；网络请求及页面元素过滤有效、可临时暂停、不过度误伤正文与播放 |
| P1 | 小说正文接管 | 实际网页 → 识别作品和章节 → 提取 → 可审计清洗 → 原生 Reader → 下一章 → 回到网页 |
| P1 | 书架与阅读进度 | 独立于 Firefox 普通书签；作品、章节、进度、缓存持久化，杀进程/重启后恢复 |
| P1 | 搜书 / 目录 / 来源 | 先支持原项目已有来源；同一作品可以由支持的来源搜索、进入目录和章节；失败显示具体原因且可回网页 |
| P1 | Epic × AutoJs6 | 仍由手机 AutoJs6 管定时和交互、使用**新浏览器自己的已登录会话**；没有实机验收不得替换旧稳定工作流 |
| P1 | 真实网站浏览体验 | 继承 Firefox 的地址栏/标签/历史/下载/上传/账号和权限基础；小说、论坛、漫画、视频等重点站定制后不能破坏键盘、滚动、登录和播放；外链保留完整网页兜底 |
| P2 | 自有 WebExtensions | 适配 lightrans-direct、Prompt-Shelf、chatgpt-continuity 等需要的功能；逐项判断手机适用性，不预装全部 |
| P2 | 备份与迁移 | 先实现旧库导出→新库校验→导入，后续才做 Google Drive 备份与冲突处理 |
| 加分 | 桌面 Chrome 扩展 | 不以“任意 CRX 直接安装”为首发标准；可寻找 Firefox 对应插件或移植自研扩展 |

不要求首版支持任何未知网站的自动提取，也不要求首版创建账号/服务器或在 Firefox 中重新实现所有 Chrome 扩展 API。

## 2. 继承与新增：不把成熟浏览器再造一遍

### 继承 Firefox 官方代码

- Fenix：浏览器 UI、标签页、历史、书签、下载、站点权限、浏览器设置。
- Android Components：状态管理、导航、浏览器功能组件。
- GeckoView：页面引擎和 WebExtensions 能力。
- Firefox 扩展体系：先使用现成的 **完整版 uBlock Origin**，不用为了“去广告”又引入第二套常驻过滤引擎。

### 新增 Penrix 模块（实现方向，并非宣称已有）

1. **PenrixContentBridge**：内置 WebExtension 从当前 GeckoView 页面提取 DOM/章节信息，双向消息传给 Android 原生逻辑。采用 GeckoView 支持的 built-in WebExtension / Native Messaging，保留请求来源、当前标签和 URL，不依赖 Chrome CRX。
2. **PenrixSource**：迁移原仓库的 SourceContract、SourceManifest、来源识别、搜索、元数据、目录、取章、失败分类与离线证据回放。先分离不依赖 WebView 的 Kotlin 逻辑，再重写 WebView/CookieManager 专属适配层；不要整包复制旧 Activity 或假定旧 Cookie 可用。
3. **PenrixReader**：独立于 Firefox 通用 Reader Mode 的小说阅读器；保留可控正文净化、上一页/下一页/下一章、书架、进度恢复、正文清洗审计。
4. **PenrixLibrary**：保留原项目的 Favorites / Works / Progress / Chapters 区分与 stable IDs、时间戳、删除墓碑；**Firefox 书签 != Penrix 书架**。Firefox 负责普通浏览历史/书签，Penrix 只维护业务数据，避免双重数据真相。
5. **PenrixSiteActions**：对特定站点增加“阅读本章”“加入书架”“查找其他来源”等上下文动作，普通网页不接管、不干扰。
6. **PenrixAutomationEntry**：为 AutoJs6 提供稳定、可识别的入口与可观测页面状态；执行任务仍在 AutoJs6，不能把未验证的后台自动领取误报成功。

### 最重要的运行关系

```text
Firefox 普通浏览器
    |
    +-- 普通网页 ------> 保持 Firefox 原样浏览
    |
    +-- 支持的作品/章节
           |
           v
     PenrixContentBridge（当前网页与会话）
           |
           v
     PenrixSource（识别/目录/正文/净化）
           |
       +---+----------------------+
       |                          |
       v                          v
    成功提取                  页面保护/失败
       |                          |
       v                          v
  PenrixReader             回到 Firefox 真实网页
       |                          |
       +---- 书架/进度/缓存 <------+
       |
       +---- 下一章 / 换来源
                 |
                 +---- 继续提取
                 +---- 无法提取 → 回网页
```

**核心取舍**：网页登录、Cookie、浏览器标签由 Firefox/GeckoView 管；正文与书架由 Penrix 管。需要登录或过验证的内容优先沿当前浏览器会话执行，不假定另开的 HTTP 客户端自动共享 Cookie。

## 3. 现有资产如何处理（基于真实代码）

原仓库已存在：
- `source/SourceContract.kt`：SEARCH / WORK_METADATA / CHAPTER_LIST / CHAPTER_CONTENT / WEB_FALLBACK / WEB_CLEANUP 等能力与 Source API。
- `source/{twkan,threea,novel,runtime,android,...}`：已有来源与 Source 执行逻辑；区分可迁移纯逻辑和依赖 WebView 的部分。
- `data/{Models,PenrixDatabase,PenrixRepository,StableIds}.kt`：收藏、作品、章节、进度及本地 SQLite。
- `ReaderActivity.kt`：当前独立 Reader，左右点击翻页、继续下一章、返回网页、保存进度。
- `adblock-rust` JNI 与过滤规则资源：已在原 K20 APK 验证部分网页效果。Firefox 首版优先 uBlock Origin，不重复维护常驻 Rust 过滤器，只有证明 Firefox 扩展能力确实缺了某项才重新评估。
- `integrations/epic/` 与 `tools/epic/`（原项目相应 PR 分支）：由 AutoJs6 控制 K20 上的私人浏览器，严格校验 0 元、订单及实际已入库状态。

原 K20 记录：WebView 版 2026-10-28 有原生过滤、书架缓存、章节 3→4、登录会话复用及已拥有游戏检查的真机证据；**这些不自动等于 Firefox 版已验证**。

### 数据与设备保护（必须）

- 新 Firefox 试验 APK 使用**独立应用标识、名称和图标**，与现有 `app.penrix.browser` 并存。
- 不通过覆盖安装尝试读取另一应用沙盒；旧数据迁移需由旧版显式导出、校验字段/版本后新版本导入，并对照作品、章节及进度计数。
- Cookie/登录状态通常不能跨浏览器自动迁移；Epic 等网站需要在新浏览器单独验证登录。绝不导出账户密码、Cookie 或会话到 GitHub。
- 原项目 Windows 工作区存在尚未提交的本地变动；在确认真实 diff、备份之前不得 reset/覆盖。
- 试验阶段不改变手机屏幕物理设置，不改写原 AutoJs6 定时任务，不卸载原浏览器。

## 4. 验收节奏（按依赖顺序，而不是并行堆功能）

### F0 — 证明官方源码能够独立生成自己的 Firefox APK

- 校验 `Penrix/firefox` 分支与 Mozilla Central 的 Fenix 路径。
- 优先使用官方 **Artifact Mode**，在不修改 Gecko C++ 的阶段复用已编译内核；应用层改动在 `mobile/android/fenix/` 下迭代。
- 从仓库根目录按官方文档用 `./mach` 引导与构建；不把旧 Fenix 迁移前的 `./gradlew` 说明当成当前主入口。
- 修改应用标识、可见名称和图标，签出自己的 debug APK。构建环境可选 Linux CI/独立构建机；**不能把当前本地配置假定为能直接编译大型 Firefox 工程**。
- **验收**：APK 文件、构建命令、源码 commit、签名及版本可追溯；K20 Android 9 安装、开启多标签、登录、输入、刷新与后台恢复，旧版 Penrix 不受影响。

### F0.5 — 私人网站模块主界面（在 F0 与 F1 之间）

- 打开新版 App 不显示普通 Firefox 主页，而是作者专属网站模块总入口。
- 至少一个重点网站做到：点击模块→恢复专属页面/上次位置→站内分类、详情与内容动作→切换其他模块→返回时保留上下文；网页失败可以返回完整原网页。
- 旧 `SiteRegistry` 的网址格子只视作候选目录，不能拿 `setOnClickListener { openUrl(site.startUrl) }` 当作站点 App 化交付。
- **验收**：像 App 中的一个模块，而不是普通浏览器加几个链接。详细标准见 [SITE-APP-SHELL.md](SITE-APP-SHELL.md)。

### F1 — 第一个站点的真实 App 化小说闭环

- 只选一个旧项目已有真实证据的来源（优先 TWKAN 或 69书吧），固定章节样本；从该站的专属入口、作品/目录导航开始，不是从 Firefox 地址栏开始。
- 当前 Firefox 标签 → built-in WebExtension 读取章节 → Native Messaging → 原生 Reader → 下一章 → 返回原网页；创建并读取书架/进度。
- 失败时显示来源/阶段/错误并回到真实网页，保留继续阅读机会。
- **验收**：K20 真机跨章节、重启后恢复进度、正文净化不吞正常内容。不能以 Mock 或截图替代闭环。

### F2 — 扩展 Source 能力、跨来源搜索与阅读体验

- 复用旧 Source 合同，接入作品信息、目录、源搜索、缓存、验证恢复和可审计的正文清洗。
- 对真实站点比较原网页和清洗后的内容；支持手动切换来源和浏览/Reader 来回转换。
- 完成适合单手手机使用的常用浏览器界面与站点操作入口；**不要重造 Firefox 的标签/历史系统**。

### F3 — 广告净化与网站兼容

- 装上完整版 uBlock Origin，真机验证网络过滤、元素净化与误杀恢复；需要时补充 Penrix 的站点特定清理规则。
- 验收论坛、漫画、视频、小说等常用站点的登录、滚动、键盘、视频和全屏。
- 不为“看起来更安全”添加与需求无关的第二个过滤器、网络代理或多套请求状态。

### F4 — Epic / AutoJs6 重新接入

- Firefox 自有会话登录 Epic；保持 AutoJs6 为唯一任务执行者，读取真实页面和可访问性节点。
- 不因为 Firefox UI 不同就沿用旧 WebView 固定坐标；重新验收定时唤醒、入口、已拥有判断、非零订单拒绝、0 元结算、后台/断线等边界。
- 在端到端未通过之前，**继续保留原工作流**，不得将“已经拥有”误记为新游戏领取成功。

### F5 — 非关键加分项

- 评估与移植作者自研扩展（现有 `lightrans-direct` 已有 Firefox manifest；其他按 API 兼容性适配）。
- 备份/迁移增强；Google Drive 可选且不能取代本地数据源。
- 桌面 Chrome 扩展不作为第一阶段硬要求。

## 5. 验收横表：完成标准而不是编译次数

| 阶段 | 实际交付 | 判定 |
|---|---|---|
| F0 | 独立 APK + K20 浏览/登录/标签可用 | 证明底层可用，**但还不是私人浏览器的产品体验** |
| F0.5 | 私人首页与站点独立模块、状态恢复、原网页兜底 | **从这里开始有网站 App 化体验** |
| F1 | 从具体站点模块搜书/进入作品/目录→Reader→下一章→书架进度→回站点 | **第一个完整网站 App 模块** |
| F2 | 至少已有来源的搜索/作品/目录/章内容贯通、失败回网页 | 私人内容产品成形 |
| F3 | uBlock 真机有效、目标站点浏览功能正常 | 日常使用可行 |
| F4 | AutoJs6 真机任务在新浏览器中完成对应验收 | 才允许替代旧自动化入口 |
| F5 | 自研插件/备份按需工作 | 加分项 |

每轮实际验收需记录：设备/系统版本、浏览器 APK hash、源码 commit、测试站点与章节、所用账号**已登录但不透露凭据**、实际成功路径、失败/未验证项。不能凭模拟测试、编译通过或用户已经拥有的 Epic 游戏判断整个任务完成。

## 6. 和 Kiwi/Brave 的关系，以及停止条件

Firefox 是**当前试验主线，不是提前宣布最终胜者**。Kiwi 仓库与原 Penrix-Browser 保留，不再做大规模平行移植。

- Firefox 在 F0–F1（含 F0.5 专属模块外壳）真机成立且没有不可修复的关键站点问题 → 继续扩展 F2–F4。
- 发现具体的 GeckoView 站点功能障碍 → 定位并修复，回到该真实用例重新测；不能因一个错误立刻换框架。
- 经过真实排障仍无法满足某项主功能 → 与 Kiwi 在**同一用例**对照，再决定是否切换。
- 浏览器必须先满足真实功能闭环；代码架构漂亮、GitHub Stars 多、最新版号高，都不算交付。

## 7. 证据链接

- Firefox 官方源码： https://github.com/mozilla-firefox/firefox
- 官方 Android/Fenix 构建： https://firefox-source-docs.mozilla.org/mobile/android/fenix.html
- GeckoView 内置扩展与 Native Messaging： https://firefox-source-docs.mozilla.org/mobile/android/geckoview/consumer/web-extensions.html
- Firefox Android 当前系统支持： https://support.mozilla.org/en-US/kb/will-firefox-work-my-mobile-device
- 旧项目产品： https://github.com/Penrix/Penrix-Browser
- 旧项目 K20 验收： https://github.com/Penrix/Penrix-Browser/blob/codex/k20-browser-live-20261028/docs/2026-10-28-k20-acceptance.md
- 旧项目 Epic 约束： https://github.com/Penrix/Penrix-Browser/blob/codex/k20-browser-live-20261028/integrations/epic/README.md
- K20 本地代码保护与实机限制： https://github.com/Penrix/android-automation-node/issues/3

> 文档只锁定用户要求和可验证目标；具体实现可以在真实源码和真机测试中调整。若测试事实与设计冲突，以实际内容/网站/设备为准修订 Working。
