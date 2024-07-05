class AstConstructor(
    location: Location,
    private val obj: Ast,
    private val args: List<Ast>,
    private val local : Boolean
) : Ast(location) {
    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append("  ".repeat(indent) + "CONSTRUCTOR $local\n")
        obj.dump(sb, indent + 1)
        args.forEach { it.dump(sb, indent + 1) }
    }


    private fun heapAlloc(cb: CodeBlock, size:Int, type:Type) : Symbol {
        val malloc = allCodeBlocks.find{it.name=="mallocObject"}
        check(malloc!=null)
        cb.addMov(cb.getReg(1), makeSymbolIntLit(size))
        cb.addCall(malloc)
        return cb.addCopy(cb.getReg(8), type)
    }

    private fun stackAlloc(cb: CodeBlock, size:Int, type:Type) : Symbol {
        cb.hasLocalStackVars = true
        val sizeRounded = ((size-1) or 3)+1  // Round up to multiple of 4
        val ret = cb.addAluOp(AluOp.SUB_I, cb.getReg(31), makeSymbolIntLit(sizeRounded), type)
        cb.addMov(cb.getReg(31), ret)
        return ret
    }

    private fun codeGenAllocArray(cb: CodeBlock, context: AstBlock, arrayType: TypeArray): Symbol {
        if (args.size != 1)
            return makeSymbolError(location, "Array constructor takes exactly one argument")
        val numElements = args[0].codeGenExpression(cb, context)
        if (numElements !is SymbolIntLit)
            return makeSymbolError(location, "Array constructor takes an integer argument")

        val arraySize = arrayType.base.getSize() * numElements.value
        return if (local) stackAlloc(cb, arraySize, arrayType) else heapAlloc(cb, arraySize, arrayType)
    }

    private fun codeGenAllocClass(cb: CodeBlock, context: AstBlock, cls: TypeClass): Symbol {
        val ret = if (local) stackAlloc(cb, cls.size, cls) else heapAlloc(cb, cls.size, cls)

        val argSym = args.map { it.codeGenExpression(cb, context) }
        val params = cls.astClass.constructorParams
        if (argSym.size != params.size)
            return makeSymbolError(location, "Constructor for $cls expects ${params.size} arguments, but got ${argSym.size}")

        cb.addMov(cb.getReg(1), ret)
        for (index in argSym.indices) {
            val arg = argSym[index]
            val param = params[index]
            if (!param.type.isTypeCompatible(arg))
                return makeSymbolError(
                    args[index].location,
                    "Parameter $param is of type ${param.type} but got ${arg.type}"
                )
            cb.addMov(cb.getReg(index + 2), arg)
        }
        cb.addCall(cls.astClass.codeBlock)
        return ret
    }

    override fun codeGenExpression(cb: CodeBlock, context: AstBlock): Symbol {
        val objType = obj.resolveType(context)
        if (objType is TypeError) return SymbolError()

        return when(objType) {
            is TypeArray -> codeGenAllocArray(cb, context, objType)
            is TypeClass -> codeGenAllocClass(cb, context, objType)
            else -> makeSymbolError(location, "Cannot allocate non-class type")
        }
    }
}