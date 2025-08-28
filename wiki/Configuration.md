# Configuration Guide

Complete guide to configuring the Atomic plugin and .atomic files.

## Plugin Settings

Access via: `File` → `Settings` → `Tools` → `Atomic Plugin`

### General Settings

| Setting | Default | Description |
|---------|---------|-------------|
| **Auto-generate enabled** | `true` | Automatically regenerate on file save |
| **Show notifications** | `true` | Display success/error notifications |
| **Delete old files on directory change** | `true` | Remove old generated files when output directory changes |
| **Debounce delay (ms)** | `500` | Delay before auto-generation triggers |

### Settings Explained

#### Auto-generate Enabled
- **When enabled**: Code regenerates automatically on save
- **When disabled**: Use Ctrl+Shift+G for manual generation
- **Recommendation**: Enable for active development, disable for large projects

#### Show Notifications
- **When enabled**: See popup notifications for generation results
- **When disabled**: Check Event Log for results
- **Recommendation**: Enable to catch errors quickly

#### Delete Old Files
- **When enabled**: Old generated files are deleted when you change `directory` or `className`
- **When disabled**: Manual cleanup required
- **Recommendation**: Enable to avoid orphaned files

#### Debounce Delay
- **Range**: 100-5000ms
- **Lower values**: Faster feedback but more CPU usage
- **Higher values**: Less CPU usage but slower feedback
- **Recommendation**: 500ms for most projects, 1000ms+ for large files

## File Configuration

### Required Properties

These must be present in every .atomic file:

```atomic
entityType: Entity        # The base type for entities
namespace: Game.ECS      # Target namespace
className: Components    # Generated class name
```

### Optional Properties

```atomic
# File header
header: "Custom header text"

# Output location
directory: "Generated"
solution: "MyGame.sln"

# Performance options
aggressiveInlining: true
unsafe: false
```

## Project Structure

### Standard Structure

```
/YourProject
  /Components
    - Player.atomic
    - Enemy.atomic
  /Generated          # Generated files go here
    - Player.cs
    - Enemy.cs
```

### Unity Structure

```
/Assets
  /Scripts
    /Components
      - Player.atomic
    /Generated
      - Player.cs
  /Plugins
    # Third-party code
```

### Multi-Project Structure

```
/Solution
  /Shared
    /Components      # Shared .atomic files
  /GameClient
    /Generated      # Client-specific generation
  /GameServer  
    /Generated      # Server-specific generation
```

## Performance Configuration

### For Development

Optimize for iteration speed:

```atomic
aggressiveInlining: false  # Faster compilation
unsafe: false              # Safer debugging
```

Plugin settings:
- Auto-generate: **Enabled**
- Debounce: **300-500ms**

### For Production

Optimize for runtime performance:

```atomic
aggressiveInlining: true   # Maximum performance
unsafe: true               # If needed for hot paths
```

Plugin settings:
- Auto-generate: **Disabled** (manual control)
- Debounce: **Not relevant**

## Type Resolution

### Import Configuration

Control type resolution with imports:

```atomic
imports:
    System                      # Basic types
    System.Collections.Generic  # Collections
    UnityEngine                # Unity types
    MyGame.Core               # Custom types
```

### Resolution Order

Types are resolved in this order:
1. Fully qualified names in values
2. Imported namespaces (top to bottom)
3. System namespace (implicit)
4. Project global usings

## Output Configuration

### Directory Options

```atomic
# Same as source file (default)
# directory: not specified

# Relative to project root
directory: "Generated"
directory: "Scripts/Generated"

# Parent directory
directory: "../Shared/Generated"

# Absolute path (not recommended)
directory: "C:/Projects/Game/Generated"
```

### File Naming

Generated file name = `{className}.cs`

```atomic
className: PlayerComponents
# Generates: PlayerComponents.cs

className: EntityExt
# Generates: EntityExt.cs
```

## Environment-Specific Configuration

### Debug Configuration

Create `Debug.atomic`:

```atomic
entityType: Entity
namespace: Game.Debug
className: DebugComponents
directory: "Generated/Debug"

values:
    DebugInfo: string
    ProfileData: float[]
```

### Release Configuration

Create `Release.atomic`:

```atomic
entityType: Entity
namespace: Game
className: Components
directory: "Generated/Release"
aggressiveInlining: true
unsafe: true

# Same components without debug data
```

## Template Configurations

### Minimal Configuration

```atomic
entityType: Entity
namespace: Game
className: Components

values:
    Health: int
```

### Standard Game Configuration

```atomic
header: "Auto-generated - Do not edit"
entityType: GameObject
namespace: MyGame.Components
className: GameComponents
directory: "Generated"
aggressiveInlining: true

imports:
    System
    UnityEngine
    System.Collections.Generic

tags:
    Player
    Enemy
    Interactable

values:
    Health: int
    Position: Vector3
    Name: string
```

### High-Performance Configuration

```atomic
entityType: Entity
namespace: Game.ECS
className: FastComponents
aggressiveInlining: true
unsafe: true

values:
    Position: Vector3
    Velocity: Vector3
    # Use ref returns for hot paths
```

## Validation Configuration

### Strict Validation

Enable all checks:
- Type validation
- Duplicate detection  
- Import validation
- Namespace verification

### Relaxed Validation

For prototyping:
- Disable auto-generation
- Use manual generation
- Fix errors as needed

## IDE Configuration

### Color Scheme

Customize colors: `Settings` → `Editor` → `Color Scheme` → `Atomic`

### File Templates

Create template: `Settings` → `Editor` → `File and Code Templates`

```atomic
#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")namespace ${PACKAGE_NAME}#end
entityType: ${EntityType}
namespace: ${Namespace}
className: ${ClassName}

tags:
    ${Tag1}

values:
    ${Value1}: ${Type1}
```

### Live Templates

Create snippets for common patterns:

```atomic
# Abbreviation: "val"
# Template:
$NAME$: $TYPE$
```

## Build Configuration

### MSBuild Integration

Exclude generated files from analysis:

```xml
<ItemGroup>
  <Compile Remove="**\*.generated.cs" />
  <None Include="**\*.generated.cs" />
</ItemGroup>
```

### CI/CD Configuration

Generate during build:

```bash
# Pre-build step
rider generate-atomic **/*.atomic

# Build
dotnet build
```

## Tips

1. **Start Simple**: Begin with minimal configuration
2. **Profile First**: Measure before enabling unsafe/aggressive
3. **Organize by Feature**: Group related components
4. **Document Choices**: Comment why certain options are enabled
5. **Version Control**: Track .atomic files, not generated code

## Troubleshooting Configuration

If configuration isn't working:

1. Check syntax errors
2. Verify required fields
3. Test with minimal config
4. Check plugin settings
5. Review logs for details

See [Troubleshooting](Troubleshooting) for more help.