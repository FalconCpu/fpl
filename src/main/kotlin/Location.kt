class Location (
    private val fileName : String,
    private val firstLine : Int,
    private val firstColumn: Int,
    private val lastLine : Int,
    private val lastColumn : Int
) {
    override fun toString() = "$fileName:$firstLine.$firstColumn-$lastLine.$lastColumn"

    constructor(start: Location, end: Location) :
            this(start.fileName, start.firstLine, start.firstColumn, end.lastLine, end.lastColumn)

}

val nullLocation= Location("",0,0,0,0)
