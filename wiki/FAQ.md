# Frequently Asked Questions

## General Questions

### What is Atomic Plugin?

Atomic Plugin is a code generation tool for JetBrains Rider that creates extension methods for the [Atomic Framework](https://github.com/StarKRE22/Atomic) - a reactive procedural game framework for C# and Unity.

### What is the Atomic Framework?

The [Atomic Framework](https://github.com/StarKRE22/Atomic) is a game development framework that uses Entity-State-Behaviour pattern to reduce complexity in game interactions. It separates data from logic, uses reactive properties for state management, and promotes procedural programming for better performance.

### Why use Atomic Plugin instead of writing code manually?

- **Framework Integration**: Seamlessly works with Atomic Framework patterns
- **Consistency**: All entity operations follow Atomic Framework conventions
- **Performance**: Generated code uses aggressive inlining when needed
- **Maintenance**: Single source of truth for your entity API
- **Type Safety**: Full IntelliSense and compile-time checking
- **Productivity**: Automatic regeneration on changes

### How does this relate to ECS (Entity Component Systems)?

While the Atomic Framework shares some concepts with ECS (entities, components), it's a distinct approach that emphasizes:
- Reactive properties instead of systems
- Static extension methods instead of system updates
- Procedural paradigm for interactions
- Behaviour activation/deactivation patterns

### Is the plugin free?

Yes, the Atomic Plugin is open-source and free to use under the MIT license.

## Installation & Compatibility

### What versions of Rider are supported?

Atomic requires JetBrains Rider 2025.1 or later.

### Can I use Atomic with ReSharper?

No, Atomic is specifically designed for Rider and uses Rider-specific features.

### Does Atomic work with Unity?

Yes! Atomic has special support for Unity projects:
- Recognizes Unity project structure
- Handles .asmdef files
- Supports Unity types (Vector3, GameObject, etc.)

### How do I update the plugin?

Rider will notify you of updates. To update:
1. Go to `Settings` → `Plugins`
2. Find Atomic in the Installed tab
3. Click Update
4. Restart Rider

## Usage Questions

### What's the difference between tags and values?

- **Tags**: Boolean flags (present or not). Example: `Player`, `Dead`, `Flying`
- **Values**: Typed data attached to entities. Example: `Health: int`, `Position: Vector3`

### Can I use custom types in values?

Yes! Any type available in your project can be used:
```atomic
values:
    CustomData: MyCustomClass
    Config: GameConfiguration
```

### How do I use generic types?

Specify generic parameters with angle brackets:
```atomic
values:
    Items: List<Item>
    Cache: Dictionary<string, object>
    Grid: Cell[,]
```

### Can I generate multiple classes from one file?

No, each .atomic file generates one static class. Create multiple files for better organization:
- `Physics.atomic` → `PhysicsComponents.cs`
- `Combat.atomic` → `CombatComponents.cs`

### Where are generated files placed?

By default, in the same directory as the .atomic file. Use the `directory` property to customize:
```atomic
directory: "Generated"      # Relative to project
directory: "../Shared"      # Parent directory
directory: "Assets/Scripts" # Specific path
```

## Performance Questions

### What is aggressive inlining?

When `aggressiveInlining: true`, the compiler is instructed to inline methods at call sites, eliminating method call overhead. Use for performance-critical code.

### When should I use unsafe code?

Enable `unsafe: true` when you need ref returns for direct memory access:
```csharp
ref int health = ref entity.RefHealth();
health += 10; // Direct modification
```

Use with caution - only for performance-critical paths.

### Does generation impact build time?

No, generation happens in the IDE before build. Generated files are normal C# files that compile quickly.

### How large can .atomic files be?

There's no hard limit, but for maintainability:
- Keep under 100 tags/values per file
- Split by feature or system
- Use multiple files for better organization

## Troubleshooting Questions

### Why isn't my code generating?

Check:
1. Required fields present (`entityType`, `namespace`, `className`)
2. No syntax errors (red underlines)
3. File has `.atomic` extension
4. Plugin is installed and enabled

### Why can't I find generated files?

1. Check the `directory` property
2. Refresh project view
3. Look in same folder as .atomic file (default)
4. Check Event Log for errors

### Why is auto-completion not working?

1. Wait for project indexing to complete
2. Build your project first
3. Ensure types are referenced
4. Try File → Invalidate Caches

### Can I version control generated files?

It's recommended to:
- **DO** version control .atomic files
- **DON'T** version control generated .cs files
- Add generated files to .gitignore:
```gitignore
**/Generated/*.cs
**/*.generated.cs
```

## Advanced Questions

### Can I customize the generation template?

Currently, the generation template is fixed. Future versions may support custom templates.

### How are hash codes generated?

Hash codes are deterministic based on tag/value names. The same name always produces the same hash across machines and sessions.

### Can I use Atomic with source generators?

Yes, Atomic-generated files work with C# source generators. Generation order:
1. Atomic generates .cs files
2. Build picks up generated files
3. Source generators process all code

### Does Atomic support partial classes?

Generated classes are static and cannot be partial. However, you can create extension methods in other files that work alongside Atomic-generated methods.

### Can I debug generated code?

Yes! Generated code is normal C# that you can:
- Set breakpoints
- Step through
- View in debugger
- Profile

## Best Practices Questions

### How should I organize .atomic files?

Recommended structure:
```
/Components
  /Core
    - Entity.atomic
    - Transform.atomic
  /Combat  
    - Health.atomic
    - Weapons.atomic
  /Physics
    - Movement.atomic
    - Collision.atomic
```

### Should I use one large file or many small ones?

Many small files are better:
- Easier to maintain
- Better version control
- Clearer organization
- Faster regeneration

### How do I handle breaking changes?

1. Update .atomic file
2. Regenerate code
3. Fix compilation errors
4. Use Find Usages to update all references

### What naming convention should I use?

- Files: `Feature.atomic` (e.g., `Player.atomic`)
- Classes: `FeatureComponents` or `FeatureExt`
- Tags/Values: PascalCase (e.g., `PlayerHealth`)

## Plugin Development

### Is Atomic open source?

Yes! Find the source at: https://github.com/Prylor/atomic-rider-plugin

### Can I contribute?

Absolutely! We welcome:
- Bug reports
- Feature requests
- Pull requests
- Documentation improvements

### How do I build from source?

```bash
git clone https://github.com/Prylor/atomic-rider-plugin.git
cd atomic-rider-plugin
./gradlew buildPlugin
```

### Where can I report bugs?

Report issues at: https://github.com/Prylor/atomic-rider-plugin/issues

## Still Have Questions?

- Check [Troubleshooting](Troubleshooting) for technical issues
- Read [Documentation](Home) for detailed guides
- Ask on [GitHub Discussions](https://github.com/Prylor/atomic-rider-plugin/discussions)
- Report bugs as [GitHub Issues](https://github.com/Prylor/atomic-rider-plugin/issues)