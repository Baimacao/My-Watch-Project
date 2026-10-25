# LDDC-Android

⚠️ 注意：本项目全部代码由 AI 生成
本项目是基于 LDDC 的 Android 版本，所有代码（包括 README 文件）均由 AI 助手生成，不包含任何人工编写的代码。

一个支持多平台的精准歌词（逐字歌词）下载匹配工具 Android 版。

## 主要特性

- ⚡ 多线程快速匹配：所有歌词匹配功能均采用多线程技术，实现自动搜索与极速精准匹配每一个歌词。
- 📝 逐字歌词样式：绝大多数歌曲都能获取到逐字样式的歌词，精准同步到每个字。
- 💾 多种格式支持：支持保存歌词为逐字 LRC、逐行 LRC、增强型 LRC、SRT 和 ASS 等格式。
- 📥 便捷下载与保存：支持一键下载歌词文件，支持自定义本地保存目录（SAF），并支持开启“自动静默保存”。
- 🎵 多音乐平台歌词搜索：支持搜索 QQ音乐、酷狗音乐、网易云音乐中的单曲。
- 🛠️ 多样歌词组合：灵活组合原文、译文、罗马音的歌词内容。
- 🔧 加密歌词支持：支持解析 QQ音乐(QRC)、酷狗音乐(KRC)、网易云音乐(YRC) 等加密歌词格式。
- 📱 自适应UI：根据系统主题自动调整界面颜色。
- ⌚ 圆屏手表适配：针对 Wear OS 等圆屏设备进行了专门的 UI 适配（底部导航靠拢、标题居中），避免边缘裁切。

## 版本信息

| 项目            | 值                      |
|---------------|------------------------|
| 版本号           | 1.0.3 (versionCode: 4) |
| 最低 Android 版本 | Android 7.0 (API 24)   |
| 目标 Android 版本 | Android 15 (API 36)    |
| 发布日期          | 2026-10-25             |

## 技术栈

- 语言：Kotlin
- UI 框架：Jetpack Compose
- 架构：MVVM + 仓库模式 + UseCase
- 网络：OkHttp / Ktor
- 异步：Kotlin Coroutines + Flow
- 依赖注入：Hilt
- 数据库：Room
- 图片加载：Coil
- 序列化：Kotlinx Serialization
- 存储与文件：DataStore Preferences / SAF (DocumentFile)

## 项目结构
app/src/main/java/com/example/lddc/
├── LDDCApplication.kt          # 应用入口
├── MainActivity.kt              # 主Activity（包含圆屏底栏适配）
├── common/                      # 通用模块
│   ├── models/                  # 数据模型
│   │   ├── enums/               # 枚举类
│   │   │   ├── Language.kt      # 语言
│   │   │   ├── LyricsFormat.kt  # 歌词格式
│   │   │   ├── LyricsType.kt    # 歌词类型
│   │   │   ├── SearchType.kt    # 搜索类型
│   │   │   └── Source.kt        # 音乐平台
│   │   ├── info/                # 信息模型
│   │   │   ├── AlbumInfo.kt     # 专辑信息
│   │   │   ├── Artist.kt        # 艺术家信息
│   │   │   ├── LyricInfo.kt     # 歌词信息
│   │   │   ├── PlaylistInfo.kt  # 播放列表信息
│   │   │   └── SongInfo.kt      # 歌曲信息
│   │   └── lyrics/              # 歌词模型
│   │       ├── Lyrics.kt        # 歌词数据
│   │       ├── LyricsLine.kt    # 歌词行
│   │       └── LyricsWord.kt    # 歌词字
│   └── utils/                   # 工具类
│       ├── AlbumArtLoader.kt    # 专辑封面加载
│       ├── PermissionUtils.kt   # 权限工具
│       └── TextUtils.kt         # 文本工具
├── core/                        # 核心功能
│   ├── api/                     # API实现
│   │   ├── base/                # 基础API
│   │   ├── impl/                # 具体实现
│   │   │   ├── KugouApi.kt      # 酷狗音乐API
│   │   │   ├── NetEaseApi.kt    # 网易云音乐API
│   │   │   └── QQMusicApi.kt    # QQ音乐API
│   │   ├── LyricsApiManager.kt  # API管理器
│   │   └── NetEaseDeviceIds.kt  # 网易云设备ID
│   ├── decryptor/               # 解密器
│   │   ├── EapiDecryptor.kt     # EAPI解密
│   │   ├── KrcDecryptor.kt      # KRC解密
│   │   └── QrcDecryptor.kt      # QRC解密
│   ├── parser/                  # 解析器
│   │   └── ...
│   └── utils/                   # 核心工具类（新增）
│       ├── DownloadLocation.kt  # SAF 目录权限与路径管理
│       └── LyricsSaver.kt       # 歌词文件保存引擎（支持默认/SAF）
├── data/                        # 数据层
│   ├── local/                   # 本地数据
│   │   ├── database/            # 数据库
│   │   └── datastore/           # 数据存储
│   │       └── SettingsDataStore.kt # 设置数据存储（已适配自定义下载路径）
│   └── repository/              # 仓库
│       └── ...
├── di/                          # 依赖注入
│   └── AppModule.kt             # 应用模块
├── domain/                      # 领域层
│   └── ...
├── presentation/                # 表现层
│   ├── components/              # 组件
│   │   └── designsystem/        # 设计系统
│   └── screens/                 # 页面
│       ├── search/              # 搜索页面
│       │   ├── LyricsDetailScreen.kt    # 歌词详情（支持下载与复制提示）
│       │   ├── LyricsDetailViewModel.kt # 歌词详情VM（支持自动保存）
│       │   ├── SearchScreen.kt          # 搜索页面（支持圆屏标题居中）
│       │   └── SearchViewModel.kt       # 搜索VM
│       └── settings/            # 设置页面
│           ├── SettingsScreen.kt        # 设置（新增下载目录选择）
│           └── SettingsViewModel.kt     # 设置VM
