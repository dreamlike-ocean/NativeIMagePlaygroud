package io.github.dreamlike.suit

import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle

class FFICase {
    companion object {

        val pageSizeMH = initPageSizeMH()

        private fun initPageSizeMH(): MethodHandle {
            val fp = Linker.nativeLinker().defaultLookup()
                .find("getpagesize")
                .get()
            return Linker.nativeLinker()
                .downcallHandle(
                    fp,
                    FunctionDescriptor.of(ValueLayout.JAVA_INT),
                    Linker.Option.critical(false)
                )
        }
    }
}