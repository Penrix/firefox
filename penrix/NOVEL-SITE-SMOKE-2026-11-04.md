# 小说站公开网页链路检查 × 去广告接入记录（2026-11-04）

> 这是一次**通过公开网页检索工具进行的在线 HTTP/HTML 内容检查**，并非在 Penrix Firefox APK 内跑的 GeckoView 浏览器测试。Firefox 定制 APK 还没有编译，因此**不存在**“在 K20 上去广告成功”的实测结果。
> 任务对象是用户要求的「内置去广告了吗？然后测试下几个小说网站」。与本报告配套的代码已写在本 PR：`PenrixAdblockBootstrap.kt`、`PenrixNovelRoutes.kt` 和相应测试源码。

## 1. 去广告：实现/未实现界线

### 已写代码

1. 在 `HomeActivity.onCreate` 初始化 `PenrixAdblockBootstrap`。
2. 启动时通过 Fenix 现有 `addonsProvider.getAddonByID("uBlock0@raymondhill.net")` 读取官方 Mozilla Add-ons 的完整版 uBlock Origin 元数据，**只接受官方 Mozilla HTTPS 下载域名**。
3. 如果这份 Firefox 的应用数据中还没有该扩展，使用 **Firefox 现有 `AddonManager.installAddon(url, InstallationMethod.MANAGER)`** 自动发起首次安装；若 Firefox 展示权限确认则由用户确认。
4. 首次安装成功记录完成状态；之后用户主动停用/卸载不会被强行恢复。第一次网络失败时下次冷启动可重试。
5. 私人模块主页显示首次安装状态；扩展的正式启停、过滤规则更新、手动卸载继续由 Firefox 扩展管理器负责。
6. 沿用 Firefox 现有跟踪保护。**没有**另外嵌入 adblock-rust，也没有把不可靠的 CSS 隐藏当作网络过滤。

### 还没有做到

- 目前采用**首次联网安装已签名的官方 XPI**，并非把 XPI 二进制包直接打入 APK，因此完全离线首次启动仍无法获得 uBO。用户要求长期离线内置可再加随 APK 分发已签名 XPI 的构建链。
- 尚未编译、运行或确认扩展权限提示和安装回调；首装是否最终启用仍须以 K20 真机扩展管理器和实际拦截请求为准。
- 尚未检测过滤器是否误删目录、正文、图片、章节导航；目前不宣称拦截效果。
- **最新版 Firefox 的 Android 9 运行与 APK 独立安装仍待 F0 验证。**

官方技术依据：
- uBO Android 官方版本：https://addons.mozilla.org/en-US/android/addon/ublock-origin/
- Fenix / Android 扩展管理 API：`mobile/android/android-components/components/feature/addons/src/main/java/mozilla/components/feature/addons/AddonManager.kt`
- GeckoView 内置扩展接口：https://firefox-source-docs.mozilla.org/mobile/android/geckoview/consumer/web-extensions.html

## 2. 实际站点公开网页检查

| 网站 | 首页 | 作品 / 目录 | 正文页 | 下一章链接 | 现场发现 | 结论 |
|---|---|---|---|---|---|---|
| **69书吧** | 可读 | 作品页、独立目录可读 | `/txt/90442/41051913` 可读 | 页面有导航 | 目录出现同名章重复条目；已实测最新章节的“下一章”回到作品页，不能把非章节链接当正文或用标题作唯一 ID | **公开链路成立**；需验证页面 JS、UI 与扩展过滤 |
| **TWKAN** | 可读 | 作品页、`index.html` 目录可读 | `/txt/82888/49191968` 第1章可读 | 点击能进入第2章 `49191969` | 作品/章节中有 Facebook / Google Tag Manager iframe；正文夹杂多种 Unicode 域名水印 | **公开链路成立**；uBO 不一定去掉正文内嵌水印，需单独正文清洗 |
| **UU看书** | 可读 | `/book/25312/` 同时包含作品与超长目录 | `/book/25312/16302351.html` 第1章可读 | 第1→第2→第3章均能取到 | 大目录一次展示超过千章；页面存在 Facebook iframe | **公开链路成立**；应做目录分段/缓存，不直接全量 Compose 渲染 |
| **八叉书库** | 可读公开首页 | 没有检查到可公开访问的作品正文 | 未验证 | 未验证 | 首页明确提示“请登录账号访问本站”，并提示 AI 访问行为可能触发封禁 | **认证门槛**；必须经用户本人真实登录后再测，不能冒称正文读取成功 |

### 实际查看的 URL

**69书吧**
- 首页 https://www.69shuba.com/
- 作品 https://www.69shuba.com/book/90442.htm
- 目录 https://www.69shuba.com/book/90442/
- 章节 https://www.69shuba.com/txt/90442/41051913

**TWKAN**
- 首页 https://twkan.com/
- 作品 https://twkan.com/book/82888.html
- 目录 https://twkan.com/book/82888/index.html
- 第1章 https://twkan.com/txt/82888/49191968
- 第2章 https://twkan.com/txt/82888/49191969

**UU看书**
- 首页 https://uukanshu.cc/
- 作品兼目录 https://uukanshu.cc/book/25312/
- 第1章 https://uukanshu.cc/book/25312/16302351.html
- 第2章 https://uukanshu.cc/book/25312/16302352.html
- 第3章 https://uukanshu.cc/book/25312/16302353.html

**八叉书库** https://bachashuku.org/

## 3. 已变成可调用代码的发现

新增 `PenrixNovelRoutes.kt` 与 `PenrixNovelRoutesTest.kt`，根据上述**实际访问路径**按站点分别识别：
- 69书吧：`/book/{id}.htm`（作品）、`/book/{id}/`（目录）、`/txt/{id}/{chapter}`（章节）
- TWKAN：`/book/{id}.html`（作品）、`/book/{id}/index.html`（目录）、`/txt/{id}/{chapter}`（章节）
- UU看书：`/book/{id}/`（作品与目录同页）、`/book/{id}/{chapter}.html`（章节）

并新增 `isNextChapter(fromUrl, candidateUrl)`：必须同一站同一本书、两个 URL 都是不同章节，才允许作为连续阅读候选。特别拦住 69书吧最新章“下一章”回到作品页的实际边界。**仅为 URL 级分类，不表示 DOM 正文抽取已写好。** 实际正文水印、页面元素/搜索和章节下一步操作仍需要从原 Penrix-Browser Source adapters 迁移，并用 GeckoView 真机验证。

## 4. 下一轮必须上设备验证的项目

| 测试动作 | 真实通过标准 |
|---|---|
| 安装 Penrix Private debug APK | 与官方 Firefox 并存，在 K20 Android 9 打开 |
| 扩展启动 | uBlock Origin 已安装、启用，并能查看过滤规则与过滤计数 |
| 69书吧 | 首页→作品→目录→正文→下一章；真实可见按钮，拦截不误删内容 |
| TWKAN | 首页→作品→目录→第1→第2章；GTM/广告资源减少且页面无白屏；正文水印单独清洗 |
| UU看书 | 首页→作品/目录→第1→第2→第3章；长目录顺畅，不损失跳章能力 |
| 八叉书库 | 用户本人登录成功后，实测正文页面、站内会话复用与回退；不绕过验证 |
| 移动体验 | 登录、保存书架、返回位置、字体、滚动、暗色模式与弹窗、离线启动 |
| 性能 | 确认安装后首屏时延、滑动帧、占用内存和过滤规则启用时的后台恢复 |

**目前仅完成在线页面链路检查与源码接入，不能替代 Firefox APK 的网络与视觉实测。**
