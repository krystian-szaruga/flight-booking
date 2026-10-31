package dev.maxaure.flightbooking.bootstrap

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import kotlin.test.Test

internal class ArchitectureRulesTest {

    private val imported = ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("dev.maxaure.flightbooking")

    @Test
    fun `when core modules are checked then they depend only on the platform and on each other`() {
        classes()
            .that().resideInAPackage("dev.maxaure.flightbooking.domain..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "java..",
                "kotlin..",
                "org.jetbrains.annotations..",
                "dev.maxaure.flightbooking.domain..",
            )
            .check(imported)

        classes()
            .that().resideInAPackage("dev.maxaure.flightbooking.application..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "java..",
                "kotlin..",
                "org.jetbrains.annotations..",
                "dev.maxaure.flightbooking.application..",
                "dev.maxaure.flightbooking.domain..",
            )
            .check(imported)
    }

    @Test
    fun `when adapters are checked then they depend only on application and domain`() {
        val forbidden = adapters + "bootstrap"
        adapters.forEach { adapter ->
            noClasses()
                .that().resideInAPackage("dev.maxaure.flightbooking.$adapter..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                    *(forbidden - adapter)
                        .map { "dev.maxaure.flightbooking.$it.." }
                        .toTypedArray(),
                )
                .check(imported)
        }
    }

    @Test
    fun `when web adapter is checked then it does not reach into persistence`() {
        noClasses()
            .that().resideInAPackage("dev.maxaure.flightbooking.web..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "dev.maxaure.flightbooking.persistence..",
                "jakarta.persistence..",
            )
            .check(imported)
    }

    private companion object {
        val adapters = listOf(
            "web",
            "persistence",
            "security",
            "provider",
            "scheduler",
            "mail",
            "payment",
            "currency",
        )
    }
}
