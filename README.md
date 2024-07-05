# Falcon Programming Language (fpl)

FPL is a relatively simple programming language, intended to be the language for writing the
operating system of my falcon computer system, based on my custom F32 processor. 

This compiler compiles fpl code into F32 assembly.

## Overview

I have borrowed heavily from other languages, mostly Kotlin, C and Python. 
The compiler itself is written in Kotlin.

* Imperative
* Static Typed, but with type inference
* Null safe - 
* Manual memory management (no garbage collector)(although I want to explore ways to make it memory safe later)
* Python style significant indentation - but with optional end markers for blocks.

## Examples

```
# functions begin with the 'fun' keyword
# parameter types have the form name:Type, with the function return type shown afrer an arrow

fun factorial(n:Int)->Int
    if n <= 1
        return 1
    else
        return n * factorial(n-1)

# For loops are written as 'for' followed by a variable name, an 'in' keyword, and a range expression
# The upper limit can be inclusive 1..10 or exclusive 1..<10

fun main()
    for i in 1..10
        println(factorial(i))
```


```
# Blocks are marked by indentation, but can optionally be terminated by an 'end' statement
# this helps to mitigate the whitespace cliff seen in Python. The recommendation is short
# blocks are marked using indentation only, longer blocks with explicit 'end'

# variables can be mutable (var) or immutable (val)
# Arrays are written as Array<Type>, and are indexed using []

fun bubblesort(arr:Array<Int>)
    repeat
        var madeChange = false
        for index in 0..<arr.size-1
            if arr[index]>arr[index+1]
                val temp = arr[index]
                arr[index] = arr[index+1]
                arr[index+1] = temp
                madeChange = true
        end for
    until !madeChange
end fun

fun main()
    val myArray = local Array<Int> {3,8,12,4,16,5,9}
    bubblesort(myArray)    
    
```

```
# Classes are defined with the 'class' keyword. 
# Fields can be defined eiter as parameters to the constructor
# Nullable types are indicated with a ?
# The compiler must be able to statically prove access through possibly null references are 
# safe based on control flow constructs

class LinkedList (val value:Int, val next:LinkedList?)

fun total(list:LinkedList?)->Int
    var ret = 0
    var current = list
    while current!=null
        ret += current.value
        current = current.next
    return ret

fun main()
    val list = new LinkedList(1, new LinkedList(2, new LinkedList(3, null)))
    println(total(list))
```

## More documentation to come later...
