// WITH_RUNTIME

abstract class A {
    protected abstract fun optional(): String?
    protected abstract suspend fun slow(): String

    suspend fun optimized(): String {
        val value = optional()
        if(value != null){
            return value
        }
        else {
            return slow()
        }
    }

    suspend fun notOptimized(): String {
        return optional() ?: slow()
    }
}

abstract class C {
    protected abstract fun ready(): Boolean
    protected abstract suspend fun slow(): Boolean

    suspend fun optimized(): Boolean {
        if(ready()){
            return true
        }
        else {
            return slow()
        }
    }

    suspend fun notOptimized(): Boolean {
        return if(ready()){
            true
        }
        else {
            slow()
        }
    }
}
