package burp.ui

const val TRANSFORMER_EDITOR_HINT = """// Transformer function JavaScript code

// value: string - the value to transform
// values: object - contains key-value pairs of all values
// Lib: object - the library object (contains lodash, etc.)

// returns: string - the transformed value (last line is returned implicitly)

// Example 1:
// value.toUpperCase()

// Example 2:
// Lib._.camelCase(values.refreshToken)
"""
