# Kotlin Multiplatform Project Guidelines

## Architecture

• Frontend: Kotlin Multiplatform (Android, iOS, Desktop, Web)
• Backend: FastAPI with LangGraph
• Database: PostgreSQL

## Current Implementation Status

• Authentication: ✅ Login/Registration with JWT
• Password Validation: ✅ Real-time validation with requirements display
• Error Handling: ✅ Improved backend error parsing and user-friendly messages
• Current Feature: Chat implementation (DEV-003 branch)

## Tech Stack

• UI: Compose Multiplatform
• Network: Ktor Client
• Serialization: kotlinx.serialization
• Logging: Custom Logger utility
• State Management: ViewModels with StateFlow/SharedFlow

## Backend Integration

• Base URL: http://localhost:8000/api/v1
• Auth endpoints: /auth/register, /auth/login
• Error format: 422 for validation, 400 for business logic
• Password requirements: 8+ chars, upper/lower case, number, special char


## Common Tasks

• Run tests: `./gradlew test`
• Build all targets: `./gradlew build`
• Run web app: `./gradlew wasmJsBrowserRun`
• Run desktop app: `./gradlew desktopRun`

## Development Notes

• JWT tokens set to 30 days (development) - TODO: reduce for production
• Password validation: 8+ chars, uppercase, lowercase, number, special char
• Current branch: DEV-003-chat-implementation

## Project Architecture

• This is a Kotlin Multiplatform project following Clean Architecture principles
• Each feature is organized in its own module
• Follow the standard Clean Architecture layers:
• Domain (entities, use cases, repository interfaces)
• Data (repository implementations, data sources, mappers)
• Presentation (viewmodels, UI states, events)
• All modules should have clear dependencies flowing inward (domain ← data ← presentation)

## Module Structure

• Create a separate module for each feature
• Common modules:
• `:core` - Core utilities, extensions, and base classes
• `:core:ui` - Common UI components and Compose resources
• `:core:testing` - Test utilities and mocks
• `:core:network` - Network-related code
• `:core:database` - Database-related code
• `:core:design-system` - Shared design components for Compose Multiplatform
• Feature modules should follow the pattern `:feature:[feature-name]`
• Each feature module should contain its own implementation of clean architecture layers
• UI components should be in a separate `:ui` submodule within each feature

## Coding Standards

• Use named parameters for function calls with multiple parameters
• No "magic strings" - all strings must be defined as constants or variables
• Extract repeated string literals to constants
• Keep functions small and focused on a single responsibility
• Follow Kotlin coding conventions for naming:
• Classes and interfaces: PascalCase
• Functions and properties: camelCase
• Constants: SCREAMING_SNAKE_CASE
• Use existing error handling patterns
• Follow established validation approaches
• Maintain consistent logging

## Dependency Injection

• Use Koin for dependency injection
• Each module should define its own Koin module
• Organize Koin modules by feature and layer
• Example structure:
```
val dataModule = module {
single { MyRepository(get(), get()) }
}

val domainModule = module {
factory { MyUseCase(get()) }
}

val presentationModule = module {
viewModel { MyViewModel(get()) }
}
```

val featureModule = listOf(dataModule, domainModule, presentationModule)

## Resource Management

• All strings should be localized in `strings.xml` or similar resource files
• Use dimensions resources for consistent spacing and sizing
• Colors should be defined in a theme or colors resource file
• No hardcoded dimensions or colors in the UI code
• For Compose Multiplatform, define common theme elements in the shared module
• Use Material 3 theming system across all platforms for consistency

## Testing Requirements

• Unit tests for all use cases and repositories
• UI tests for critical user flows
• Prefer using TestCoroutineDispatcher for testing coroutines
• Use fakes/mocks for dependencies in tests
• Aim for at least 70% code coverage in domain and data layers

## Multiplatform Guidelines

• Use Kotlin Multiplatform Compose for UI across Android, iOS, and web applications
• Follow Compose Multiplatform best practices for shared UI components
• Maximize code sharing in the common module, especially UI logic and components
• Platform-specific code should be minimal and contained to expect/actual implementations
• Use multiplatform libraries where possible (Ktor, SQLDelight, etc.)
• For platform-specific features, create appropriate interfaces in common code
• Maintain a consistent UI/UX across all platforms while respecting platform-specific design guidelines

## Error Handling

• Use `Result<T>` or similar patterns for error handling in the domain and data layers
• Handle errors at the appropriate level, typically in the ViewModel
• Log all errors with appropriate context
• Define error types as sealed classes for clear error handling

## Directory Structure Guidelines

• Keep files organized by feature and layer
• Follow a consistent naming pattern:
• Repositories: `[Name]Repository`
• Use cases: `[Action][Entity]UseCase`
• ViewModels: `[Feature]ViewModel`
• Screens/Composables: `[Feature]Screen`

## Git Workflow

• Use feature branches for development
• Branch naming convention: `feature/[feature-name]` or `bugfix/[issue-number]`
• Commits should be atomic and have clear messages
• PRs should include tests and documentation updates

## Performance Considerations

• Be mindful of UI performance, particularly with composable functions
• Use appropriate coroutine dispatchers for IO and CPU-intensive operations
• Consider pagination for large data sets
• Optimize images and resources for mobile platforms

## Troubleshooting

• If Compose preview doesn't work, try invalidating caches and restarting
• For network errors, check if the backend server is running on port 8000
• WASM build issues may require clearing the browser cache
• For iOS build problems, ensure XCode is updated to the latest version

## Code Review Checklist

• All strings are properly defined in constants/resources (no magic strings)
• Error handling follows the established patterns
• ViewModels use StateFlow/SharedFlow appropriately
• Composables are properly recomposed and optimized
• Authentication is properly handled in protected routes
• UI matches the design system
• Tests are included for all new functionality
• Documentation is updated with any new components

## Library Versions

• Kotlin: 1.9.23
• Compose Multiplatform: 1.6.0
• Ktor: 2.3.8
• Koin: 3.5.3
• Coroutines: 1.7.3