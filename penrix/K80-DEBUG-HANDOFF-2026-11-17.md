# Penrix Private（Firefox 底座）K80 崩溃排查 — 进度交接

日期：2026-11-17
目标：让 `org.mozilla.penrix.private`（分支 `penrix/browser-requirements-20261104` 的 CI 产物）在小米 Redmi K80 (24117RK2CC, Android 16 / SDK 36) 上正常启动，然后继续实机验收。

## 已完成

1. **定位用户目标**（`penrix/PRODUCT-REQUIREMENTS.md`）：私人浏览器 = 几个专属网站深度 App 化的超级容器；扩展只是加分项。
2. **确认底座选择**：Firefox 版方向正确，WebView 版（Penrix-Browser）继续作为旧主力并存；Kiwi fork 已加弃用注释（README 两个分支，commit c44c182a / 8480df3f），不删除不投入。
3. **确认崩溃现象**：在手机端后台屏 3 次复现——应用启动即失联，前台也打不开。用户已手动删除该应用。
4. **crash 缓冲区排查**：`logcat -b crash` 里没有任何 Penrix/Fenix 的 Java 崩溃记录（只有 uiautomator 自身崩溃）→ 应用死得太快，Java 层没来得及写日志。**尚未拿到崩溃堆栈。**
5. **代码审查**（分支全部 40 个文件的 diff）：
   - 已包含 e98e5c6848「Fix Xiaomi TurboSched StrictMode startup crash on K80」（只处理 DiskReadViolation 一类）
   - 启动路径新增钩子：`HomeActivity.onCreate` 里 `PenrixSiteTracker.start()` + `PenrixAdblockBootstrap.start()`；`Core.kt` 里 `PenrixReadingFeature.install()`（注册内置 WebExtension `reading@penrix.local`）
   - debug 变体移除了 LeakCanary，包名 `org.mozilla.penrix.private`
   - HomeFragment 非 private 模式渲染 `PenrixSiteHome`
6. **发现的结构性风险**：CI 用 artifact build，`MOZ_ARTIFACT_REVISION=dcafde48`（11-03 上游 mozilla-central rev，fork 自己的 rev 不在 taskcluster 上所以只能用上游的）下载 GeckoView AAR，而 Fenix 应用层代码已到 11-07 → **内核与应用层有 4 天错位**。已读 `python/mozbuild/mozbuild/artifacts.py` 确认 `install_from_revset` 接受任意 40 位 sha（fork sha 无法解析到远端 artifact）。
7. **测试 APK 已就位**：手机 `/data/local/tmp/p.apk`（177MB，run 42353628405 / commit 1d09bf160bbb 的产物），源 APK 备份在手机 `/sdcard/Download/Penrix-Private-latest.apk`。

## 假说排序

1. **Android 16 (SDK 36) 兼容性**——分支按 Android 9 (API 28) 定制，TurboSched 修复只覆盖 StrictMode DiskReadViolation；K80 是 Android 16，debug 包 minSdk 28 targetSdk 低版本在新系统上的行为差异。最可能。
2. **Gecko 内核与应用层 4 天错位**——次可能（新增 API 都是新增文件，旧 API 未动）。
3. 启动钩子（AdblockBootstrap 用 AMO 元数据装 uBO）在无网/受限网时的异常路径——低。

## 下一步（PC 端执行清单）

1. **抓崩溃堆栈**（关键，一切修复的前提）：
   - `adb install -r /sdcard/Download/Penrix-Private-latest.apk`（PC adb 直接装，绕开 Shizuku 工具限制）
   - `adb logcat -c` 清缓冲
   - 启动应用（点图标或 `adb shell am start -n org.mozilla.penrix.private/org.mozilla.fenix.HomeActivity`；若 activity 名不对先 `pm dump org.mozilla.penrix.private | grep -A2 android.intent.action.MAIN` 找 launcher activity）
   - `adb logcat -d > crash.log`，重点看 `FATAL`、`AndroidRuntime`、`SIGSEGV`/`SIGABRT`（native 崩溃会带 `libxul.so` 或 Gecko 符号）、`tombstone`
2. **按堆栈修复**：
   - 若 StrictMode 类：在 `ThreadPenaltyDeathWithIgnoresListener.kt` 按堆栈类名追加 ignore 分支（现有模式照抄，如 `isXiaomiTurboSchedViolation`）
   - 若 native/Gecko 层：考虑把 `MOZ_ARTIFACT_REVISION` 升到与分支头最近的上游 rev（需查 fork head 1d09bf16 对应的 mozilla-central 上游 commit），重新构建
   - 修复推到分支 `penrix/browser-requirements-20261104`，CI 自动构建新 APK
3. **实机验收**（修复启动后按顺序）：安装并存检查 → Penrix 模块首页 → 首启 uBO 自动安装 → TWKAN 作品→目录→正文→下一章 → 正文简繁/水印清洗 → 书架/进度杀进程恢复。标准见 `penrix/SITE-APP-SHELL.md` 与 PR #1 描述。

## 环境与凭据

- GitHub 令牌在设备 `/root/.dsh/github/token`（600），helper `/root/.dsh/github/gh.js`；PC 端 `gh auth login --with-token` 可复用同一令牌（scopes 含 repo/workflow，无 delete_repo）
- 手机 adb 已有：PC 上直接 adb 即可；手机端 Shizuku 通道对大 APK 的 pm install 有工具限制（invalidArguments），PC adb 不受影响

## 其他进行中事项

- Penrix-Browser（WebView）PR #1 的 CI 已清理 63 个旧 artifact（664MB）并重跑，等结果合并
- dsh-go 手机引擎被 HyperOS 后台杀（reason=10）导致手机端工具经常整轮失效——PC 端继续不受影响
