# Material3 TIM · SilverCirno Fork

这是 [linx3141/Material3TIM](https://github.com/linx3141/Material3TIM) 的分支。它用 Jetpack Compose 替换 Android TIM 的界面，保留上游的 **Material 3 Expressive** 和 **Miuix** 两套风格。作为模块启用时使用 TIM 的账号、会话和消息；单独安装则可用示例数据预览界面。

## 相比原仓库加了什么？

以下是相对上游 [`v0.1.0`（`f4c8aed`）](https://github.com/linx3141/Material3TIM/tree/f4c8aed1423748fef4b9d62b327382529eed38bc) 的新增功能和修复，不重复罗列上游已经支持的功能。

| 功能 | 这个分支的变化 |
| --- | --- |
| **通知快捷回复** | 展开私聊或群聊的新消息通知，点「回复」即可直接发送文字，不用打开 TIM。发送确认后撤下对应通知，不覆盖发送期间收到的新通知；未确认时提示进入聊天查看。通知改用 TIM 的应用图标。 |
| **通知去重与清理** | 同一消息的重复回调不重复发通知；通知还在时，同会话的新消息静默更新原通知。从列表、搜索、联系人等入口打开聊天时，只撤下该会话的模块通知。 |
| **合并转发** | 昵称与头像按原消息的发送者显示，不把同名的人合在一起。文件已下载时可直接打开，否则先下载再打开，兼容嵌套记录的附件定位。 |
| **图片左右滑动** | 点开聊天或合并转发中的图片，可以左右浏览当前已加载的图片。保留原图加载、缩放和保存，放大时单指拖动用于移动图片。 |
| **聊天输入区** | 表情和附件面板在聊天底部展开，并优化它们与键盘之间的切换。输入栏支持按住说话转文字，识别结果进入输入框，确认后再发送；需要系统语音识别服务及麦克风权限。 |
| **TIM 收藏与文件助手** | 「我的收藏」和「我的文件」接入 TIM 原生收藏和文件助手；聊天中可以通过消息菜单收藏内容。 |
| **Telegram 贴纸** | 可在设置登录 Telegram，同步账号已安装的贴纸集合。在聊天表情面板底部切换 TIM 和 Telegram 贴纸；TGS、WebM 动态贴纸首次发送时转换为 GIF。[登录、构建和格式说明](docs/telegram-stickers.md) |
| **扫一扫、联系人和主题** | 扫一扫改为应用内相机实时识别，仍可选择相册图片；「我的」和联系人资料页增加 QQ 空间入口，联系人和「新的朋友」里的加好友入口接通搜索与验证；主题新增「跟随系统取色」。QQ 空间目前仍打开 TIM 原生页面，[MD3 接入分析](docs/qzone-integration.md)尚未完成。 |
| **横竖屏切换** | 从横屏 App 切回 TIM 时，导航布局按实际窗口尺寸更新，避免竖屏手机仍显示横屏侧边栏；宽屏设备仍可使用侧边栏。 |
| **NPatch 与 QAuxv** | 支持 NPatch 内嵌模式并隔离模块资源，减少与 QAuxv 的资源冲突。可在设置里打开 TIM 原生设置或切回原版界面，也可从 TIM 原生设置中的「Material3 TIM」入口重新启用。 |

## 安装和使用

- **LSPosed**：安装模块 APK，在 LSPosed 中启用「Material3 TIM」，勾选作用域 `com.tencent.tim`，然后强制停止并重新打开 TIM。宿主适配主要面向 **TIM 4.1.0.4050**；其他版本不保证兼容。
- **NPatch**：将模块 APK 内嵌到 TIM，可与 QAuxv 一起内嵌。要同步 Telegram 贴纸，还须在设备上单独安装同版本的 Material3TIM 模块 APK。
- **独立预览**：单独安装模块 APK 即可查看示例界面，不会读取 TIM 数据或收发消息。

快捷回复仅作用于启用模块后收到的**新消息通知**，需要允许 TIM 发送通知，并保持 TIM 宿主进程可用。Release 构建和单元测试已通过；新消息提醒、合并转发文件等实际交互仍需在设备上回归。

## 从源码构建

使用 **JDK 21**、Android SDK **37.2** 和 Build Tools **37.0.0**。

```bash
./gradlew :app:testDebugUnitTest :app:assembleRelease
```

Windows 使用 `gradlew.bat`。模块 APK 输出到 `app/build/outputs/apk/release/app-release.apk`；不包含 TIM，也不是 NPatch 整合包。Telegram 构建配置见[贴纸同步文档](docs/telegram-stickers.md)。

## 上游与许可证

- 上游项目和历史更新：[linx3141/Material3TIM](https://github.com/linx3141/Material3TIM)
- 分支源码：[SilverCirno/Material3TIM](https://github.com/SilverCirno/Material3TIM)
- 感谢上游作者及 [KernelSU Manager](https://github.com/tiann/KernelSU)、[Miuix](https://github.com/miuix-kotlin-multiplatform/miuix) 和 [MaterialKolor](https://github.com/jordond/materialkolor) 项目。
- 本项目按 [GPL-3.0](LICENSE) 开源。它是非官方 TIM 模块，与腾讯无关。
