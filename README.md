# Atomic Plugin for JetBrains Rider

[![Version](https://img.shields.io/badge/version-0.1.3-blue.svg)](https://github.com/Prylor/atomic-rider-plugin)
[![Rider](https://img.shields.io/badge/Rider-2025.1-orange.svg)](https://www.jetbrains.com/rider/)
[![Atomic Framework](https://img.shields.io/badge/Atomic%20Framework-Compatible-brightgreen.svg)](https://github.com/StarKRE22/Atomic)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

Generate entity API extension methods for the [**Atomic Framework**](https://github.com/StarKRE22/Atomic) - a reactive procedural game framework for C# and Unity that uses Entity-State-Behaviour pattern to reduce complexity in game development.

## 🎮 About Atomic Framework

The [Atomic Framework](https://github.com/StarKRE22/Atomic) is a game development framework that:
- Uses **Entity-State-Behaviour pattern** to separate data from logic
- Promotes **reactive programming** for real-time state management
- Employs **procedural paradigm** with static methods for better performance
- Provides **modular composition** of entities and behaviors

This plugin generates extension methods that seamlessly integrate with Atomic Framework entities.

## 🚀 Features

- **Framework Integration**: Built specifically for Atomic Framework entities
- **Custom DSL**: Full language support for `.atomic` files with syntax highlighting
- **Smart Completion**: IntelliSense for C# types and namespaces
- **Auto-Generation**: Automatic code regeneration on file changes
- **Find Usages**: Track generated method usage across your project
- **Refactoring**: Rename support with automatic updates
- **Validation**: Real-time error detection and quick fixes
- **Unity Support**: Optimized for Unity projects using Atomic Framework

## 📦 Installation

### From JetBrains Marketplace
1. Open JetBrains Rider
2. Go to `File` → `Settings` → `Plugins`
3. Search for "Atomic"
4. Click `Install`
5. Restart Rider

### Manual Installation
1. Download the latest release from [Releases](https://github.com/Prylor/atomic-rider-plugin/releases)
2. Go to `File` → `Settings` → `Plugins`
3. Click ⚙️ → `Install Plugin from Disk...`
4. Select the downloaded `.zip` file
5. Restart Rider

## 🎯 Quick Start

### Creating an Atomic File

1. Right-click on your project folder
2. Select `New` → `Atomic File`
3. Configure your entity API:

```atomic
header: "Generated Entity API"
entityType: "Entity"
namespace: "MyGame.Components"
className: "EntityExtensions"
directory: "Generated"
aggressiveInlining: true
unsafe: false

imports:
    System
    UnityEngine

tags:
    Player
    Enemy
    Projectile

values:
    Health: int
    Position: Vector3
    Damage: float
```

### Generating Code

- **Manual**: Press `Ctrl+Shift+G` while in an `.atomic` file
- **Automatic**: Code regenerates automatically when you save changes (if enabled)

### Generated Methods

For **Tags**:
- `HasPlayerTag()` - Check if entity has tag
- `AddPlayerTag()` - Add tag to entity
- `DelPlayerTag()` - Remove tag from entity

For **Values**:
- `GetHealth()` - Get value
- `SetHealth(int value)` - Set value
- `AddHealth()` - Add component
- `HasHealth()` - Check if component exists
- `DelHealth()` - Remove component
- `TryGetHealth(out int value)` - Try get value
- `RefHealth()` - Get reference (if unsafe enabled)

## ⚙️ Configuration

### Plugin Settings

Access settings via `File` → `Settings` → `Tools` → `Atomic Plugin`

- **Auto-generate**: Enable/disable automatic regeneration
- **Show notifications**: Toggle generation notifications
- **Debounce delay**: Set delay before auto-generation (ms)
- **Delete old files**: Remove old generated files when directory changes

### Atomic File Properties

| Property | Description | Required |
|----------|-------------|----------|
| `header` | Custom header text for generated file | No |
| `entityType` | Base entity type (e.g., Entity, GameObject) | Yes |
| `namespace` | C# namespace for generated code | Yes |
| `className` | Name of the generated static class | Yes |
| `directory` | Output directory (relative to project) | No |
| `aggressiveInlining` | Enable aggressive inlining optimization | No |
| `unsafe` | Enable unsafe code for ref returns | No |

## 🔧 Development

### Building from Source

```bash
# Clone the repository
git clone https://github.com/Prylor/atomic-rider-plugin.git
cd atomic-rider-plugin

# Build the plugin
./gradlew buildPlugin

# Run tests
./gradlew test

# Run Rider with plugin
./gradlew runIde
```

### Project Structure

```
atomic-rider-plugin/
├── src/
│   ├── rider/          # Kotlin frontend (IntelliJ Platform)
│   │   └── main/
│   │       ├── kotlin/ # Language support, actions, services
│   │       └── resources/
│   └── dotnet/         # C# backend (ReSharper)
│       └── ReSharperPlugin.AtomicPlugin/
│           └── Services/ # Code generation, validation
├── protocol/           # RD Protocol definitions
└── build.gradle.kts    # Build configuration
```

## 🤝 Contributing

Contributions are welcome! Please read our [Contributing Guidelines](CONTRIBUTING.md) before submitting PRs.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- JetBrains for the excellent Rider IDE and plugin SDK
- The Atomic community for inspiration and feedback

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/Prylor/atomic-rider-plugin/issues)
- **Documentation**: [Wiki](https://github.com/Prylor/atomic-rider-plugin/wiki)

---

**Author**: Iaroslav Sarchuk

**Version**: 0.1.3

**Compatibility**: JetBrains Rider 2025.1+