# 贡献指南

代码改动使用 JDK 17、Android SDK Platform 36 和 Build Tools 36.0.0，提交前执行：

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug --no-daemon
git diff --check
```

PR 请说明修改原因、验证结果和未覆盖的情况。修改 Hook 时附真实类名、方法签名，并验证开关开启、关闭及恢复；安装日志不能代替功能验收。

不要提交密钥、凭据、本地配置、未脱敏日志或构建产物。
