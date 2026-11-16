# MyLedger · 我的账本

使用 Kotlin、Jetpack Compose 和 Room 构建的 Android 本地记账应用。

## 功能

- 手动录入、文字描述记账、系统语音记账。
- 一句话拆分多笔账单，例如“吃饭20，打车30”识别为餐饮20元、出行30元。
- 日期筛选、流水日历、收支分类构成、分类排行和趋势。
- 物品管理、使用成本统计。
- CSV 导入、导出和用户指定目录的自动备份。
- 中文日期选择器、浅色/深色主题。

账本保存在应用私有的 Room 数据库中。语音使用设备的系统识别服务，可能需要联网；应用不保存录音。语音最终识别结果完整时自动记账，文字输入可先查看识别结果再保存。

## 构建

项目当前配置为 Android SDK 37、Gradle 9.6.0、Android Gradle Plugin 9.4.1；Gradle Daemon 的 JDK 版本由 `gradle/gradle-daemon-jvm.properties` 指定为25。请使用能够支持项目配置的 Android Studio 或对应命令行工具。

1. 克隆仓库，在本机安装所需 Android SDK。
2. 通过 Android Studio 生成 `local.properties`，或复制 `local.properties.example` 并修改 `sdk.dir`。
3. 运行：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

macOS/Linux 使用 `./gradlew`。调试包输出于 `app/build/outputs/apk/debug/`。

设备测试需要启动模拟器或连接测试手机：

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

测试会安装调试应用，请使用专用测试设备。真实账单、私有验收备份和本机截图不属于仓库内容。

发布签名由未跟踪的 `local.properties` 配置，字段见示例文件。仓库不包含签名密钥或密码；签名配置中的文件路径指向本机私有位置。本仓库上传源码不代表发布新的安装包。

## 代码结构

```text
app/src/main/java/com/myledger/app/
├── data/       # Room 数据库、事务、仓库及备份存储
├── domain/     # 金额与多笔解析、统计、导入导出、物品成本算法
└── ui/         # Compose 页面、组件、输入交互与 ViewModel
app/src/test/         # 纯逻辑单元测试
app/src/androidTest/  # 数据库和设备界面回归测试
app/schemas/          # Room 结构历史，用于数据库迁移
```

多笔记账的规则和事务边界见 [架构说明](docs/architecture.md)。

## 数据与版本状态

源码包含持续迭代中的功能。当前验证覆盖解析、数据库事务回滚、文字/语音提交和滚动边界；真机性能及实际语音准确率仍需设备验证。

`.gitignore` 排除了本地缓存、构建产物、签名材料、私人账单、数据库文件和本机验收报告。Gradle Wrapper 与 Room 结构 JSON 保留，方便复现构建和检查迁移。
