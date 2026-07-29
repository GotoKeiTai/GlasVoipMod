# stubs/

Hand-written compile-only stubs — not decompiled or vendored game code. Each file provides
only the exact method/field signatures this project's own code calls (see the header comment
in each file for which real class it stands in for). Used as a `compileOnly` fallback in
`build.gradle.kts` when no real Project Zomboid install is available (e.g. CI). Never bundled
into the shipped jar, never executed.
