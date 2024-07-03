
class Label (val name:String) {
    var index = 0
    var use = mutableListOf<Instr>()

    override fun toString() = name
}