# Xiaomi 8 IR Camera

[中文](#中文) | [English](#english)

---

## 中文

# 小米8红外摄像头

一个专为小米 8 手机 IR 红外功能优化的 Android 应用程序，提供红外摄影、手机夜视仪和红外摄像头功能。

### 功能特性 ✨

- 📸 **红外摄影** - 利用小米 8 手机的红外传感器进行红外摄影
- 🌙 **手机夜视仪** - 在低光环境下提供夜视功能
- 🎥 **红外视频录制** - 支持红外视频拍摄和录制
- 🖼️ **图像处理** - 基础的图像增强和处理功能

### 系统要求 📱

- **最低 SDK**: Android 5.0 (API 21)
- **目标 SDK**: Android 9 (API 28)
- **编译 SDK**: Android 11 (API 30)
- **设备**: Xiaomi 8 或更高版本（需要红外摄像头硬件支持）

### 技术栈 🛠️

- **语言**: Java
- **构建工具**: Gradle 7.4.2
- **开发框架**: Android Jetpack
  - AndroidX AppCompat
  - ConstraintLayout

### 快速开始 🚀

#### 前置条件
- JDK 8+
- Android Studio
- Gradle

#### 构建

1. 克隆仓库：
```bash
git clone https://github.com/hikwin/Xiaomi8IRCamera.git
cd Xiaomi8IRCamera
```

2. 使用 Android Studio 打开项目

3. 构建应用：
```bash
./gradlew build
```

4. 安装调试版本：
```bash
./gradlew installDebug
```

### 项目结构 📁

```
.
├── app/                          # 应用主模块
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/            # Java 源代码
│   │   │   ├── res/             # 资源文件（布局、字符串、图片等）
│   │   │   └── AndroidManifest.xml
│   │   ├── debug/               # 调试配置
│   │   └── release/             # 发布配置
│   └── build.gradle
├── build.gradle                  # 顶级构建配置
├── settings.gradle               # Gradle 项目设置
└── gradle/                       # Gradle 包装器
```

### 使用说明 📖

1. 启动应用程序
2. 授予必要的权限（摄像头、存储等）
3. 选择拍照或视频模式
4. 使用红外模式进行摄影/摄像
5. 图片和视频将保存到设备的相册中

### 构建配置 ⚙️

#### 应用信息
- Package: `com.xiaomi.ircamera`
- 版本代码: 2
- 版本名称: 1.1

#### 编译选项
- 源/目标兼容性: Java 8
- 发布模式: 启用代码混淆 (ProGuard)
- 调试模式: 禁用代码混淆

### 常见问题 ❓

**Q: 这个应用支持其他小米手机吗？**

A: 由于红外硬件支持因设备而异，目前主要针对小米 8 优化。其他设备可能需要调整。

**Q: 为什么没有看到红外效果？**

A: 请确保：
1. 你的设备是小米 8 或支持红外功能的更高版本
2. 已正确授予摄像头权限
3. 在足够暗的环境中测试

### 贡献指南 🤝

欢迎提交 Pull Request 或报告 Issue！

### 许可证 📄

本项目采用 [Apache License 2.0](LICENSE) 许可证。

---

## English

# Xiaomi 8 IR Camera

An Android application optimized for the IR infrared capabilities of Xiaomi 8 phones, providing infrared photography, mobile night vision, and infrared camera functionality.

### Features ✨

- 📸 **Infrared Photography** - Utilize the infrared sensor in Xiaomi 8 phones for IR photography
- 🌙 **Mobile Night Vision** - Provides night vision functionality in low-light environments
- 🎥 **Infrared Video Recording** - Support for infrared video capture and recording
- 🖼️ **Image Processing** - Basic image enhancement and processing capabilities

### System Requirements 📱

- **Minimum SDK**: Android 5.0 (API 21)
- **Target SDK**: Android 9 (API 28)
- **Compile SDK**: Android 11 (API 30)
- **Device**: Xiaomi 8 or higher (requires infrared camera hardware support)

### Technology Stack 🛠️

- **Language**: Java
- **Build Tool**: Gradle 7.4.2
- **Development Framework**: Android Jetpack
  - AndroidX AppCompat
  - ConstraintLayout

### Quick Start 🚀

#### Prerequisites
- JDK 8+
- Android Studio
- Gradle

#### Build

1. Clone the repository:
```bash
git clone https://github.com/hikwin/Xiaomi8IRCamera.git
cd Xiaomi8IRCamera
```

2. Open the project in Android Studio

3. Build the application:
```bash
./gradlew build
```

4. Install debug build:
```bash
./gradlew installDebug
```

### Project Structure 📁

```
.
├── app/                          # Main application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/            # Java source code
│   │   │   ├── res/             # Resource files (layouts, strings, images, etc.)
│   │   │   └── AndroidManifest.xml
│   │   ├── debug/               # Debug configuration
│   │   └── release/             # Release configuration
│   └── build.gradle
├── build.gradle                  # Top-level build configuration
├── settings.gradle               # Gradle project settings
└── gradle/                       # Gradle wrapper
```

### Usage Guide 📖

1. Launch the application
2. Grant necessary permissions (camera, storage, etc.)
3. Select photo or video mode
4. Use infrared mode for photography/videography
5. Images and videos will be saved to your device's photo gallery

### Build Configuration ⚙️

#### App Information
- Package: `com.xiaomi.ircamera`
- Version Code: 2
- Version Name: 1.1

#### Compilation Options
- Source/Target Compatibility: Java 8
- Release Mode: Code obfuscation enabled (ProGuard)
- Debug Mode: Code obfuscation disabled

### FAQ ❓

**Q: Does this app support other Xiaomi phones?**

A: Since infrared hardware support varies by device, it is currently optimized for Xiaomi 8. Other devices may require adjustments.

**Q: Why don't I see infrared effects?**

A: Please ensure:
1. Your device is Xiaomi 8 or a higher version with infrared support
2. Camera permissions have been granted correctly
3. Test in a sufficiently dark environment

### Contributing Guide 🤝

Pull requests and issue reports are welcome!

### License 📄

This project is licensed under the [Apache License 2.0](LICENSE).

---

If you have any questions or suggestions, feel free to [open an issue](https://github.com/hikwin/Xiaomi8IRCamera/issues) or [submit a pull request](https://github.com/hikwin/Xiaomi8IRCamera/pulls)!
