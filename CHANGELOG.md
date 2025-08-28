# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [0.1.1] - 2025-08-29

### Fixed
- Fixed EDT (Event Dispatch Thread) violations in all action classes
- Added proper `ActionUpdateThread.BGT` declarations for thread safety in:
  - AtomicGenerateApiAction
  - AtomicFindUsagesAction
  - CreateAtomicFileAction
  - DiagnoseAutoGenerationAction
  - TestAutoGenerationAction
- Fixed lifetime management in AtomicGenerationService using protocol scheduler
- Fixed solution lifetime usage in ProjectManager (using UntilSolutionCloseLifetime)
- Added plugin verification support for multiple Rider versions (2025.1.6, 2025.2.0.1)

### Technical
- Resolved "virtualFile is requested on EDT" exceptions
- Improved thread safety across all action components
- Enhanced compatibility with newer IntelliJ Platform requirements

## [0.1.0] - 2024-01-28

### Added
- Initial release of Atomic Plugin for JetBrains Rider 2025.1
- Full support for [Atomic Framework](https://github.com/StarKRE22/Atomic) entity API generation
- Complete language support for .atomic configuration files
- C# extension method generation for Atomic Framework entities
- Smart code completion for types and namespaces with IntelliSense
- Find usages functionality for generated extension methods
- Rename refactoring with automatic updates across the project
- Automatic code regeneration on file changes
- Real-time validation with error detection and quick fixes
- Syntax highlighting and code folding for .atomic files
- Custom file templates for creating new .atomic files
- Support for tags (HasTag, AddTag, DelTag methods)
- Support for values (Get, Set, Add, Has, Del, TryGet, Ref methods)
- Aggressive inlining and unsafe access options for performance
- Unity project support optimized for Atomic Framework usage

### Framework Integration
- Designed specifically for the Atomic Framework's Entity-State-Behaviour pattern
- Supports reactive properties and procedural paradigm
- Generates static extension methods following Atomic Framework conventions

### Technical Details
- Kotlin frontend with C# backend architecture
- RD Protocol for cross-language communication
- PSI-based language implementation
- Multi-layered file monitoring system
- Comprehensive validation engine
