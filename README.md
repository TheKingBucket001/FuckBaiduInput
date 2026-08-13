# FuckBaiduInput

`FuckBaiduInput` 是面向 OPPO 定制版百度输入法的 LSPosed/libxposed API 101 模块。它将可选的本地 Hook 控制项嵌入宿主原生设置界面，使用宿主的 COUI Preference 组件，不提供 WebView 或独立设置皮肤。

## 兼容性

当前开发版本仅支持：

- 包名：`com.baidu.input_oppo`
- 版本：`8.5.302.367`
- `versionCode`：`7244`

模块会校验宿主包名、版本与 APK 身份。任何不匹配都会 fail-closed，不安装业务 Hook。历史公开版本和 GitHub 上的旧构建不是当前开发工作树的实现基线。

## 功能

在宿主设置首页中，“语言及输入方式设置”上方会出现 `🚫 fuckinginput`。所有业务开关默认关闭，设置按以下分类组织：

- 剪贴板：容量、长文本、内容识别
- 账号与云：账号隔离、云备份同步、云优化、云输入
- 推荐：智慧推荐、场景化推荐
- 广告与推广：广告 SDK、商店推广、活动推荐
- 隐私与后台：统计上传、问题反馈、后台更新检查
- 皮肤与商店：在线皮肤、表情、字体、远程升级与设置入口
- 界面与工具箱：搜索、机械键盘、字体设置、AI 写作入口、纯净模式左上角 Emoji 图标

每个分类均提供“本组全部启用”开关；配置会通过受限 Provider 和 libxposed RemotePreferences 同步给 Hook 进程。

## 安装与使用

1. 在兼容 libxposed API 101 的 LSPosed 环境安装 APK，并仅勾选目标宿主。
2. 强行停止后重新启动 OPPO 定制版百度输入法。
3. 打开输入法设置，进入 `🚫 fuckinginput`，按需启用功能。
4. 带有“需重启输入法后完整生效”说明的开关，请在切换后重启宿主。

关闭开关后，模块会让对应路径回到宿主原有逻辑。

## 边界

- `99999` 是设置的容量上限，不代表无限容量。
- 本项目不解锁付费、VIP 或服务端权益。
- Hook 仅覆盖已验证的 Java 路径，不承诺覆盖 JNI、动态加载或远端服务端行为。
- 设置开关可验证控制链路；每个功能的业务效果仍应在目标版本、实际页面和冷启动场景中复测。

模块不声明 `INTERNET` 权限，也不增加常驻 Service、Receiver、Job 或定时任务。

## 本地构建

要求：JDK 17、Android SDK Platform 36、Build Tools 36.0.0。

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

`assembleRelease` 可用于未配置正式密钥时的构建检查，不能作为正式发行 APK。正式签名入口是：

```powershell
.\gradlew.bat :app:checkReleaseSigning :app:signRelease
```

通过环境变量或仓库外 `local.properties` 提供：

```text
RELEASE_STORE_FILE
RELEASE_STORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

不要提交 keystore、私钥或签名属性。仓库中的 `txtoi-local` 仅用于本地构建链测试，不能替代正式发行签名。

## 发布

推送到 `master` 仅执行源码、Debug 构建和 lint 校验。推送 `v*` tag 后，GitHub Actions 会临时恢复 GitHub Secrets 中的正式 keystore，构建、校验证书与 APK 元数据，并创建对应 GitHub Release：

```text
RELEASE_KEYSTORE_BASE64
RELEASE_STORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

tag 必须与 APK 版本严格一致，例如 `v0.8.1` 对应 `versionName = 0.8.1`。发行文件命名为 `FuckBaiduInput-v0.8.1.apk`。升级安装必须使用相同正式证书签名的 APK。

当前发布版本为 `0.8.1 / 13`。历史 `v0.8.0` 发行说明见 [.github/release-notes/v0.8.0.md](.github/release-notes/v0.8.0.md)。本地研究与测试记录见 [项目研究档案](../项目研究档案.md) 和 [真机 Hook 测试手册](../真机Hook测试手册.md)。

## 许可证

[GNU General Public License v3.0](LICENSE)
