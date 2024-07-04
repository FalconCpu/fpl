# Falcon Programming Language (fpl)

I'm designing my own computer system from scratch, and as part of that I want to develop a programming language and compiler for it. 

I'm planning to write the operating system in this language - so it needs to be fairly low level (capable of interfacing with hardware), but I want to keep as much of the niceties of modern languages as I can. So sort of a cross between Kotlin and C.

## Overview

I have borrowed heavily from other languages, mostly Kotlin, C and Python.  The compiler itself is written in Kotlin.

* Imperative
* Static Typed, but with type inference
* Manual memory management (no garbage collector)
* Python style significant indentation - but with optional end markers for blocks.
