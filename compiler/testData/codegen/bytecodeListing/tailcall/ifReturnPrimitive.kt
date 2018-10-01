// WITH_RUNTIME

suspend fun f1(i: Int): Long {
    return if (i > 0) 1L else f_2()
}

suspend fun f2(i: Int): Long {
    if (i > 0) return 1L
    return f_2()
}

suspend fun f3(i: Int): Long {
    if (i > 0) return 1L
    else return f_2()
}

private suspend fun f_2(): Long = TODO()

