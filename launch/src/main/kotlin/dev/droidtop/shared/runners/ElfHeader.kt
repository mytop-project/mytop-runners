package dev.droidtop.shared.runners

import dev.droidtop.shared.containers.ContainerArch

/**
 * The architecture an ELF program is built for, read from the first bytes of its file (droidtop docs/SPEC.md 3c): what
 * decides whether it runs natively or through a translator. Never runs or loads the program.
 */
object ElfHeader {
    /** How many leading bytes [arch] needs. */
    const val SIZE = 20

    /** The program's architecture, or null when [head] is not a little-endian ELF header droidtop runs. */
    fun arch(head: ByteArray): ContainerArch? {
        if (head.size < SIZE) return null
        if (head[0] != 0x7f.toByte() || head[1] != 'E'.code.toByte() || head[2] != 'L'.code.toByte() || head[3] != 'F'.code.toByte()) return null
        val bits = when (head[4].toInt()) {
            1 -> 32
            2 -> 64
            else -> return null
        }
        if (head[5].toInt() != 1) return null // big-endian: no architecture here is
        val machine = (head[18].toInt() and 0xff) or ((head[19].toInt() and 0xff) shl 8)
        return ContainerArch.fromElf(machine, bits)
    }
}
