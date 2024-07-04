sealed class Symbol (val name:String, val type:Type) {
    var index = 0
    val def = mutableListOf<InstrData>()
    val use = mutableListOf<Instr>()

    override fun toString() = name
}

class SymbolLocalVar(name: String, type: Type, val mutable:Boolean)
    : Symbol(name, type)

class SymbolGlobalVar(name: String, type: Type, val mutable:Boolean)
    : Symbol(name, type)  {
    var offset = 0
}

class SymbolMember(name: String, type: Type, val mutable:Boolean)
    : Symbol(name, type) {
        var offset = 0
    }

class SymbolIntLit(name:String, type: Type, val value:Int)
    : Symbol(name, type) {
        override fun toString() = value.toString()
    }

class SymbolStringLit(name:String, type: Type, val value:String)
    : Symbol(name, type)

class SymbolFunction(name:String, type: Type, val function: AstFunction)
    : Symbol(name, type)

class SymbolTypeName(name:String, type: Type)
    : Symbol(name, type)

class SymbolTemp(name: String, type: Type)
    : Symbol(name, type)

class SymbolError
    : Symbol("<ERROR>", TypeError)

class SymbolReg(name: String) : Symbol(name, TypeInt)

val allSymbolIntLit = mutableMapOf<Pair<Int,Type>,SymbolIntLit>()
fun makeSymbolIntLit(value: Int, type: Type = TypeInt): SymbolIntLit {
    return allSymbolIntLit.getOrPut(value to type) {SymbolIntLit(value.toString(), type, value) }
}

val allStringLit = mutableMapOf<String,SymbolStringLit>()
val symbolZero = makeSymbolIntLit(0)
fun makeSymbolStringLit(value: String): SymbolStringLit {
    return allStringLit.getOrPut(value) { SymbolStringLit(value, TypeString, value) }
}

fun makeSymbolError(location: Location, message:String) : SymbolError {
    Log.error(location, message)
    return SymbolError()
}

val allSymbolReg = listOf(
    SymbolReg("0"),
    SymbolReg("%1"),
    SymbolReg("%2"),
    SymbolReg("%3"),
    SymbolReg("%4"),
    SymbolReg("%5"),
    SymbolReg("%6"),
    SymbolReg("%7"),
    SymbolReg("%8"),
    SymbolReg("%9"),
    SymbolReg("%10"),
    SymbolReg("%11"),
    SymbolReg("%12"),
    SymbolReg("%13"),
    SymbolReg("%14"),
    SymbolReg("%15"),
    SymbolReg("%16"),
    SymbolReg("%17"),
    SymbolReg("%18"),
    SymbolReg("%19"),
    SymbolReg("%20"),
    SymbolReg("%21"),
    SymbolReg("%22"),
    SymbolReg("%23"),
    SymbolReg("%24"),
    SymbolReg("%25"),
    SymbolReg("%26"),
    SymbolReg("%27"),
    SymbolReg("%28"),
    SymbolReg("%29"),
    SymbolReg("%30"),
    SymbolReg("%sp"),
)