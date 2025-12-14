# Function Metadata Reader

Kotlin compiler-based module for extracting function metadata from source code.

## Overview

Uses Kotlin's embedded compiler API to parse function declarations and extract:

- Function name
- Parameter names and types (with nullability information)
- Return type (with nullability information)

## Usage

```kotlin
val sourceCode = """
    fun add(a: Int, b: Int): Int {
        return a + b
    }
"""

val metadata = readFunctionMetadata(sourceCode)
// Returns: FunctionMetadata(
//   name = "add",
//   parameters = [
//     ParameterMetadata(
//       name = "a",
//       type = TypeMetadata(className = "Int", nullable = false),
//       defaultValue = null
//     ),
//     ParameterMetadata(
//       name = "b",
//       type = TypeMetadata(className = "Int", nullable = false),
//       defaultValue = null
//     )
//   ],
//   returnType = TypeMetadata(className = "Int", nullable = false)
// )
```

### Data Models

**TypeMetadata** - Represents type information:

```kotlin
data class TypeMetadata(
    val className: String,  // e.g., "String", "List<Int>", "Map<String, Int>"
    val nullable: Boolean   // true for nullable types (String?)
)
```

**ParameterMetadata** - Represents function parameter:

```kotlin
data class ParameterMetadata(
    val name: String,
    val type: TypeMetadata,
    val defaultValue: ValueMetadata?,  // null if no default value
    val isVararg: Boolean              // true for vararg parameters
)
```

**ValueMetadata** - Represents a value as string:

```kotlin
data class ValueMetadata(
    val value: String  // String representation of value (e.g., "42", "\"World\"", "true")
)
```

## Features

- Handles multiple parameters
- Supports nullable types with explicit nullable flag
- Supports generic types (List&lt;Int&gt;, Map&lt;String, Int&gt;)
- Infers Unit return type when not specified
- Works with expression body functions
- Detects vararg parameters
- Extracts default parameter values as strings

## Testing

```bash
./gradlew :function-metadata-reader:test
```
