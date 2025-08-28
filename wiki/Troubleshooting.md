# Troubleshooting Guide

Common issues and their solutions when using the Atomic plugin.

## Installation Issues

### Plugin Not Showing in Rider

**Problem**: After installation, the plugin doesn't appear in Rider.

**Solutions**:
1. Ensure you have Rider 2025.1 or later
2. Restart Rider after installation
3. Check `File` → `Settings` → `Plugins` → `Installed` tab
4. Try manual installation from ZIP file
5. Check Rider logs: `Help` → `Show Log in Explorer`

### Incompatible Version Error

**Problem**: "Plugin is incompatible with current Rider version"

**Solution**:
- Update Rider to version 2025.1 or later
- Download the correct plugin version for your Rider

## File Recognition Issues

### .atomic Files Not Recognized

**Problem**: Files with `.atomic` extension show as plain text.

**Solutions**:
1. Restart Rider
2. Check file associations: `Settings` → `Editor` → `File Types`
3. Manually associate `.atomic` with Atomic file type
4. Reinstall the plugin

### No Syntax Highlighting

**Problem**: Atomic files open but have no colors.

**Solutions**:
1. Ensure the file has `.atomic` extension (not `.atomic.txt`)
2. Check color scheme: `Settings` → `Editor` → `Color Scheme` → `Atomic`
3. Try switching color schemes and back
4. File → Invalidate Caches and Restart

## Code Generation Issues

### "Generate Entity API" Action Not Available

**Problem**: Ctrl+Shift+G doesn't work or action is missing.

**Solutions**:
1. Ensure cursor is in an `.atomic` file
2. Check the file has required fields (`entityType`, `namespace`, `className`)
3. Look for syntax errors (red underlines)
4. Try right-click → Generate Entity API

### Generation Fails Silently

**Problem**: No code is generated and no error shown.

**Solutions**:
1. Check Rider Event Log: `View` → `Tool Windows` → `Event Log`
2. Verify all required fields are present
3. Check write permissions on output directory
4. Look for validation errors in the file
5. Try manual generation with Test Auto Generation action

### Generated File Not Found

**Problem**: Generation succeeds but can't find the output file.

**Solutions**:
1. Check the `directory` property in your .atomic file
2. Refresh the project: `File` → `Reload Project`
3. Look in the same directory as the .atomic file (default)
4. Check if the path is relative to project root
5. Verify the project structure matches expectations

## Auto-Completion Issues

### No IntelliSense for Types

**Problem**: Ctrl+Space doesn't show type suggestions.

**Solutions**:
1. Ensure you're in a `values:` section
2. Type at least one character before Ctrl+Space
3. Check that your project has compiled successfully
4. Wait for indexing to complete (see bottom status bar)
5. Try File → Invalidate Caches and Restart

### Namespaces Not Suggested

**Problem**: No namespace completion in imports section.

**Solutions**:
1. Ensure project references are loaded
2. Build the project first
3. Check that assemblies are referenced correctly
4. Wait for project indexing to complete

## Validation Errors

### "Type 'X' cannot be resolved"

**Problem**: Type validation fails for known types.

**Solutions**:
1. Add the namespace to `imports:` section
2. Use fully qualified type name
3. Ensure the type exists in referenced assemblies
4. Build the project to update type information
5. Check for typos in type name

### "Ambiguous type reference"

**Problem**: Multiple types with same name found.

**Solutions**:
1. Use fully qualified name: `System.Collections.Generic.List<T>`
2. Remove conflicting imports
3. Use Quick Fix (Alt+Enter) to resolve

### "Required field 'X' is missing"

**Problem**: Validation error for missing required field.

**Solutions**:
1. Add the missing field (entityType, namespace, or className)
2. Use Quick Fix (Alt+Enter) to add automatically
3. Check spelling of field names

## Performance Issues

### Slow Auto-Generation

**Problem**: File changes trigger slow regeneration.

**Solutions**:
1. Increase debounce delay in settings
2. Disable auto-generation and use manual (Ctrl+Shift+G)
3. Split large files into smaller ones
4. Check for circular dependencies

### IDE Freezes During Generation

**Problem**: Rider becomes unresponsive.

**Solutions**:
1. Disable auto-generation in settings
2. Report issue with thread dump
3. Check for infinite loops in configuration
4. Reduce file complexity

## Refactoring Issues

### Rename Doesn't Update Generated Code

**Problem**: Renaming in .atomic file doesn't update usages.

**Solutions**:
1. Use Shift+F6 for rename refactoring (not manual edit)
2. Ensure auto-generation is enabled
3. Manually trigger regeneration after rename
4. Check that generated files aren't read-only

### Find Usages Shows Nothing

**Problem**: Find Usages returns empty results.

**Solutions**:
1. Ensure code has been generated
2. Build the project after generation
3. Wait for indexing to complete
4. Check that generated files are included in project

## Unity-Specific Issues

### Generated Files Not Included in Unity

**Problem**: Unity doesn't recognize generated C# files.

**Solutions**:
1. Place generated files inside Assets folder
2. Ensure .meta files are created
3. Refresh Unity project
4. Check assembly definition settings
5. Verify file encoding is UTF-8

### Assembly Definition Conflicts

**Problem**: Generated code can't access types from other assemblies.

**Solutions**:
1. Add assembly references to .asmdef file
2. Use fully qualified type names
3. Generate into the same assembly
4. Check assembly dependency order

## Common Error Messages

### "Backend service not ready"

**Meaning**: The C# backend hasn't initialized yet.

**Solution**: Wait a few seconds and try again. If persists, restart Rider.

### "Cannot find project for file"

**Meaning**: The .atomic file isn't associated with a C# project.

**Solution**: Ensure the file is inside a project folder, not solution folder.

### "Invalid identifier"

**Meaning**: Tag or value name isn't a valid C# identifier.

**Solutions**:
- Remove special characters
- Don't start with numbers
- Avoid C# keywords
- Use only letters, numbers, underscores

## Debugging Steps

### Enable Verbose Logging

1. Help → Diagnostic Tools → Debug Log Settings
2. Add: `#com.jetbrains.rider.plugins.atomic:all`
3. Reproduce the issue
4. Check logs: Help → Show Log in Explorer

### Test Components Individually

1. Use Test Auto Generation action
2. Use Diagnose Auto Generation action
3. Check each service separately
4. Verify file system permissions

### Report an Issue

Include:
1. Rider version
2. Plugin version
3. .atomic file content
4. Error messages
5. Log files
6. Steps to reproduce

## Quick Fixes

| Problem | Quick Solution |
|---------|----------------|
| No generation | Check required fields |
| No completion | Wait for indexing |
| Type not found | Add to imports |
| Slow performance | Disable auto-gen |
| File not recognized | Check extension |
| No syntax colors | Restart Rider |

## Need More Help?

- Check [FAQ](FAQ) for common questions
- Report issues on [GitHub](https://github.com/Prylor/atomic-rider-plugin/issues)
- Include diagnostic information from Diagnose Auto Generation action