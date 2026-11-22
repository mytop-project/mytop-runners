package dev.droidtop.shared.runners

import dev.droidtop.shared.containers.Translator

/**
 * The command that runs a foreign program through a [Translator] inside a container (droidtop docs/SPEC.md 3c),
 * without root: the translator's own launcher in front of the program, since registering it with the kernel
 * (binfmt_misc) needs root.
 */
object TranslatedCommand {
    /**
     * The translator's launcher, by the names it has been installed under, newest first: FEX's interpreter is `FEX`
     * from FEX 2509 on and was `FEXInterpreter` before. The caller runs the first one present in the container.
     */
    fun launchers(translator: Translator): List<String> = when (translator) {
        Translator.FEX -> listOf("FEX", "FEXInterpreter")
        Translator.BOX64 -> listOf("box64")
    }

    /** [program] with its [args], run through [launcher] (one of [launchers]); the program alone without a translator. */
    fun of(launcher: String?, program: String, args: List<String> = emptyList()): List<String> =
        listOfNotNull(launcher) + program + args
}
