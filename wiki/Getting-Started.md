# Getting Started with Atomic Plugin

This guide will help you create your first `.atomic` file and generate extension methods for the [Atomic Framework](https://github.com/StarKRE22/Atomic).

## Prerequisites

- JetBrains Rider 2025.1 or later
- Atomic Plugin installed
- [Atomic Framework](https://github.com/StarKRE22/Atomic) added to your project
- C# project open in Rider

## Step 1: Create an Atomic File

### Method 1: Using the New File Menu
1. Right-click on your project or folder in the Solution Explorer
2. Select **New** → **Atomic File**
3. Enter a name (e.g., `PlayerComponents.atomic`)
4. Click **OK**

### Method 2: Manual Creation
1. Create a new file with `.atomic` extension
2. The plugin will automatically recognize it

## Step 2: Configure Your Entity API

Here's a minimal example to get started:

```atomic
entityType: "Entity"
namespace: "MyGame.Components"
className: "EntityExtensions"

tags:
    Player

values:
    Health: int
```

### Required Fields

| Field | Description | Example |
|-------|-------------|---------|
| `entityType` | The base type for your entities | `Entity`, `GameObject` |
| `namespace` | C# namespace for generated code | `MyGame.Components` |
| `className` | Name of the generated static class | `EntityExtensions` |

## Step 3: Generate the Code

### Manual Generation
1. Open your `.atomic` file
2. Press **Ctrl+Shift+G**
3. Check the notification for success/errors

### Automatic Generation
If enabled in settings, code regenerates automatically when you save the file.

## Step 4: Use the Generated Code

The plugin generates extension methods you can use immediately:

```csharp
using MyGame.Components;

public class GameLogic
{
    public void InitializePlayer(Entity entity)
    {
        // Tag methods
        entity.AddPlayerTag();
        
        // Value methods
        entity.SetHealth(100);
        
        if (entity.HasPlayerTag())
        {
            var health = entity.GetHealth();
            // ...
        }
    }
}
```

## Complete Example

### Input: `Components.atomic`

```atomic
header: "Auto-generated Entity Components"
entityType: "Entity"
namespace: "Game.ECS"
className: "EntityComponents"
directory: "Generated"
aggressiveInlining: true

imports:
    System
    UnityEngine
    System.Collections.Generic

tags:
    Player
    Enemy
    Projectile
    Pickup

values:
    Health: int
    Position: Vector3
    Velocity: Vector3
    Damage: float
    Name: string
    Inventory: List<Item>
```

### Generated Methods

For each **tag**, you get:
- `bool HasPlayerTag()`
- `void AddPlayerTag()`
- `void DelPlayerTag()`

For each **value**, you get:
- `int GetHealth()`
- `void SetHealth(int value)`
- `void AddHealth()`
- `bool HasHealth()`
- `void DelHealth()`
- `bool TryGetHealth(out int value)`

## Next Steps

- Learn about [Atomic File Syntax](Atomic-File-Syntax)
- Explore [Advanced Features](Advanced-Features)
- See [Generated Code Examples](Generated-Code)
- Configure [Plugin Settings](Configuration)

## Tips

1. **Use IntelliSense**: Type `Ctrl+Space` in .atomic files for auto-completion
2. **Organize by Feature**: Create multiple .atomic files for different systems
3. **Version Control**: Commit .atomic files, exclude generated .cs files
4. **Performance**: Enable `aggressiveInlining` for hot paths

## Common Patterns

### Component Groups

```atomic
// Physics.atomic
values:
    Position: Vector3
    Rotation: Quaternion
    Velocity: Vector3
    AngularVelocity: Vector3

// Combat.atomic  
values:
    Health: int
    Armor: int
    Damage: int
    AttackSpeed: float
```

### System-Specific APIs

```atomic
// AISystem.atomic
tags:
    AIControlled
    Patrolling
    Attacking
    
values:
    AIState: AIStateEnum
    Target: Entity
    PatrolPath: Vector3[]
```

## Need Help?

- Check the [Troubleshooting Guide](Troubleshooting)
- Read the [FAQ](FAQ)
- Report issues on [GitHub](https://github.com/Prylor/atomic-rider-plugin/issues)