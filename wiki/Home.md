# Atomic Plugin Wiki

Welcome to the Atomic Plugin documentation for JetBrains Rider!

## 📚 Documentation

### Getting Started
- [**Installation Guide**](Installation) - How to install the plugin
- [**Quick Start**](Getting-Started) - Create your first .atomic file
- [**Basic Tutorial**](Tutorial) - Step-by-step walkthrough

### Reference
- [**Atomic File Syntax**](Atomic-File-Syntax) - Complete syntax reference
- [**Generated Code Examples**](Generated-Code) - What code gets generated
- [**Configuration Options**](Configuration) - All configuration settings

### Advanced Topics
- [**Unity Integration**](Unity-Integration) - Working with Unity projects
- [**Performance Optimization**](Performance) - Best practices for performance
- [**Custom Types**](Custom-Types) - Working with custom types and generics

### Help & Support
- [**Troubleshooting**](Troubleshooting) - Common issues and solutions
- [**FAQ**](FAQ) - Frequently asked questions
- [**Known Issues**](Known-Issues) - Current limitations

## 🚀 Quick Links

- [Atomic Framework](https://github.com/StarKRE22/Atomic) - The game framework this plugin supports
- [Plugin Repository](https://github.com/Prylor/atomic-rider-plugin)
- [Issue Tracker](https://github.com/Prylor/atomic-rider-plugin/issues)
- [Releases](https://github.com/Prylor/atomic-rider-plugin/releases)
- [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/xxxxx-atomic)

## 💡 What is Atomic Plugin?

Atomic Plugin is a code generation tool for the [**Atomic Framework**](https://github.com/StarKRE22/Atomic) - a reactive procedural game framework for C# and Unity. The plugin generates extension methods for Atomic Framework entities from simple `.atomic` configuration files.

### About Atomic Framework

The Atomic Framework revolutionizes game development by:
- Using **Entity-State-Behaviour pattern** to separate data from logic
- Implementing **reactive properties** for real-time state changes
- Promoting **procedural programming** with static methods
- Enabling **modular composition** of game entities

### Key Benefits

- **🎯 Type Safety**: Full IntelliSense and compile-time checking
- **⚡ Performance**: Generated code with aggressive inlining options
- **🔧 Maintainability**: Single source of truth for your entity API
- **🚀 Productivity**: Automatic regeneration on file changes
- **🎨 IDE Integration**: Full language support with syntax highlighting

## 📖 Plugin Features

| Feature | Description |
|---------|-------------|
| **Custom DSL** | `.atomic` files with full IDE support |
| **Auto-completion** | Smart completion for types and namespaces |
| **Find Usages** | Track where generated methods are used |
| **Refactoring** | Rename with automatic updates |
| **Validation** | Real-time error detection |
| **Code Folding** | Collapse sections for better navigation |

## 🎯 Use Cases

Perfect for:
- Game development with ECS architectures
- Unity projects using entity systems
- High-performance C# applications
- Code generation for repetitive patterns

## 📝 Example

```atomic
entityType: "Entity"
namespace: "Game.Components"
className: "EntityExtensions"

tags:
    Player
    Enemy

values:
    Health: int
    Position: Vector3
```

Generates methods like:
```csharp
entity.SetHealth(100);
entity.AddPlayerTag();
if (entity.HasPosition()) { ... }
```

## 🤝 Contributing

We welcome contributions! See our [Contributing Guide](Contributing) for details.

## 📄 License

This project is licensed under the MIT License.

---

**Version**: 0.1.1 | **Author**: Iaroslav Sarchuk | **Compatibility**: Rider 2025.1+