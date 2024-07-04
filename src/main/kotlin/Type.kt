sealed class Type (val name:String) {
    override fun toString() = name
}

object TypeNull   : Type("null")
object TypeBool   : Type("Bool")
object TypeChar   : Type("Char")
object TypeShort  : Type("Short")
object TypeInt    : Type("Int")
object TypeReal   : Type("Real")
object TypeString : Type("String")
object TypeError  : Type("<Error>")
object TypeUnit   : Type("Unit")
object TypeAddress : Type("<Address>")

class TypeArray(val base:Type)
    : Type("$base[]")

class TypeNullable(val base:Type)
    : Type("$base?")

class TypeFunction(val params:List<Type>, val retType:Type)
    : Type(params.joinToString(prefix = "(", postfix = ")") + "->$retType")

class TypeClass(name:String, val astClass: AstClass)
    : Type(name) {
        val members = mutableListOf<Symbol>()
    }

val allTypeArray = mutableListOf<TypeArray>()
fun makeTypeArray(base:Type): TypeArray {
    return allTypeArray.find{it.base==base} ?: run {
        val new = TypeArray(base)
        allTypeArray += new
        new
    }
}

val allTypeNullable = mutableListOf<TypeNullable>()
fun makeTypeNullable(location: Location, base:Type): Type {
    if (base is TypeNullable || base is TypeError)
        return base
    if (base !is TypeClass)
        return makeTypeError(location,"Only struct types can be nullable")

    return allTypeNullable.find{it.base==base} ?: run {
        val new = TypeNullable(base)
        allTypeNullable += new
        new
    }
}

val allTypeFunction = mutableListOf<TypeFunction>()
fun makeTypeFunction(params: List<Type>, retType: Type): TypeFunction {
    return allTypeFunction.find{it.params==params && it.retType==retType} ?: run {
        val new = TypeFunction(params, retType)
        allTypeFunction += new
        new
    }
}

val allTypeClass = mutableListOf<TypeClass>()
fun makeTypeClass(name: String, astClass: AstClass): TypeClass {
    val ret = TypeClass(name, astClass)
    allTypeClass += ret
    return ret
}

fun calculateMemberOffsets() {
    for(cls in allTypeClass) {
        var offset = 0
        for(member in cls.members) {
            if (member is SymbolMember) {
                member.offset = offset
                offset += member.type.getSize()
            }
        }
    }
}

fun makeTypeError(location: Location, message:String) : TypeError {
    Log.error(location, message)
    return TypeError
}


fun Type.isTypeCompatible(rhs:Symbol): Boolean {
    if (this== rhs.type || this is TypeError || rhs.type is TypeError)
        return true

    if (this is TypeNullable)
        return rhs.type is TypeNull || base.isTypeCompatible(rhs)

    return false
}

// ================================================================
//                     predefined block
// ================================================================
// Create a Block with symbols for the predefined types

val lengthSymbol = SymbolMember("length", TypeInt, false)
val predefinedSymbols = createPredefinedBlock()

private fun createPredefinedBlock() : Map<String,Symbol> {
    val ret = mutableMapOf<String,Symbol>()
    for(type in listOf( TypeUnit, TypeBool, TypeChar, TypeShort, TypeInt, TypeReal, TypeString))
        ret[type.toString()] = SymbolTypeName(type.toString(), type)
    ret["null"] = SymbolIntLit("null", TypeNull, 0)
    ret["true"] = SymbolIntLit("true", TypeBool, 1)
    ret["false"] = SymbolIntLit("false", TypeBool, 0)
    lengthSymbol.offset = -4
    return ret
}

// ================================================================
//                     Get Size
// ================================================================
// Get the size of a type
fun Type.getSize(): Int = when (this) {
    TypeNull -> 4
    TypeUnit -> 0
    TypeBool -> 4
    TypeChar -> 1
    TypeShort -> 2
    TypeError -> 0
    TypeInt -> 4
    TypeReal -> 8
    TypeString -> 4
    TypeAddress -> 4
    is TypeArray -> 4
    is TypeFunction -> 4
    is TypeNullable -> 4
    is TypeClass -> 4
}

