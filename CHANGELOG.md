# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [0.1.4] - 2025-08-30

### Changed
- **BREAKING**: Removed `header` keyword from atomic file syntax
- Updated code generation to use runtime initialization with `NameToId(nameof())` pattern
- Replaced compile-time hash constants with runtime field initialization
- Added Unity Editor support with conditional compilation directives
- Changed parameter name from 'obj' to 'entity' in generated extension methods
- Added #region blocks for better code organization in generated files

### Added
- Separation between manual generation (Ctrl+Shift+G) and auto-regeneration
- Manual generation now required for first-time file creation
- Auto-regeneration only updates existing files, won't create new ones
- Always include `Atomic.Entities` namespace in generated code
- Static constructor for field initialization in generated classes
- `[InitializeOnLoad]` attribute for Unity Editor compatibility

### Fixed
- Prevent auto-generation from creating new files
- Fixed compilation errors after header removal

### Removed
- `header` property from atomic file syntax (no longer supported)
- Hash-based ID generation in favor of framework's `NameToId` method

### Documentation
- Clarified manual vs automatic generation workflow
- Updated examples to remove header references
- Added clear instructions for first-time generation requirement

## [0.1.3] - 2025-08-30

### Fixed
- Fixed critical 30-second timeout when deleting .atomic files
- Replaced deprecated threading APIs (ExecuteOrQueueEx, ExecuteOrQueueReadLockEx, ExecuteOrQueue)
- Fixed RPC timeout issues during file deletion operations

### Changed
- Made file deletion completely asynchronous using coroutines
- Updated threading API calls to use Tasks.StartNew with Scheduling.MainGuard
- File deletion now uses Java File API first, falling back to VirtualFile API if needed

### Removed
- Removed DirectFileWriter.cs as it's no longer needed with improved async handling

### Technical
- Resolved synchronous VirtualFile.delete() blocking the EDT
- Improved file deletion performance using two-step strategy
- Enhanced compatibility with Rider 2025.2 threading model

## [0.1.2] - 2025-08-30

### Fixed
- Fixed keyword autocompletion in atomic files that was broken in 0.1.1
- Fixed deprecated `addBrowseFolderListener` API usage for Rider 2025.2 compatibility
- Fixed file generation to use `AddNewItemHelper` for proper project integration and IntelliSense support
- Fixed duplicate keyword suggestions in autocompletion (keywords already used in document are now filtered out)
- Fixed async Consumer-based API compatibility for CreateAtomicFileAction in Rider 2025.2
- Removed obsolete `getCommandName()` method override
- Deleted unused AtomicTypedHandlerTest class

### Added
- Added FileSystemManagerAsync for async file operations to prevent RPC timeouts
- Added DirectFileWriter for direct file writing without blocking operations
- Added AtomicHighlighterFilter to prevent duplicate highlighter registrations
- Added AtomicDocumentListener for proper document change handling
- Added AtomicTypedHandler improvements for better auto-popup behavior
- Added comprehensive unit tests for AtomicTypedHandler

### Improved
- Enhanced autocompletion validation for atomic files only
- Improved async operation handling throughout the plugin
- Better thread safety and EDT violation prevention
- Smart filtering of already-used keywords in autocompletion

### Technical
- Resolved RPC timeout issues during file generation
- Fixed duplicate highlighter registration prevention
- Improved compatibility with Rider 2025.2
- Enhanced file generation with proper MSBuild project integration

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
