# FuckBaiduInput

这是一个面向 `com.baidu.input_oppo` 的 LSPosed/libxposed API 101 模块，用于解除 OPPO 定制版百度输入法的剪贴板限制。

已知测试目标：OPPO 定制版百度输入法 `v8.5.302.328`。模块代码中不会强制检查目标应用版本。

## 功能

- 剪贴板条目数量限制：`300` -> `99999`
- 剪贴板计数文本：`/300)` -> `/99999)`
- 绕过粘贴截断和长记录过滤

## 使用

在兼容 libxposed API 101 的 LSPosed 环境中安装 APK，启用模块，然后强行停止并重启目标输入法。

## 构建

```powershell
.\gradlew.bat :app:assembleDebug
```

Release 签名配置在仓库外部维护。不要提交 keystore、私钥或本地签名属性文件。

## 许可证

MIT License
