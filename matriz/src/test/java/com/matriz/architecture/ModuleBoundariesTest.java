package com.matriz.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Rules 2 and 3 of docs/architecture.md section 5. */
@AnalyzeClasses(packages = "com.matriz", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundariesTest {

    private static final String BASE = "com.matriz.";

    /** Business modules do not depend on each other: coordination lives in the use cases. */
    @ArchTest
    static final ArchRule accessIsIndependent = independent("access", "projects", "tasks", "recycle", "views");

    @ArchTest
    static final ArchRule projectsIsIndependent = independent("projects", "access", "tasks", "recycle", "views");

    @ArchTest
    static final ArchRule tasksIsIndependent = independent("tasks", "access", "projects", "recycle", "views");

    @ArchTest
    static final ArchRule recycleIsIndependent = independent("recycle", "access", "projects", "tasks", "views");

    /** Views read from projects and tasks only. */
    @ArchTest
    static final ArchRule viewsOnlyReadProjectsAndTasks = noClasses().that().resideInAPackage(BASE + "views..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    BASE + "recycle..", BASE + "usecases..", BASE + "api..")
            .allowEmptyShould(true);

    /** The shared core depends on no module. */
    @ArchTest
    static final ArchRule sharedDependsOnNoModule = noClasses().that().resideInAPackage(BASE + "shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    BASE + "access..", BASE + "projects..", BASE + "tasks..", BASE + "views..",
                    BASE + "recycle..", BASE + "usecases..", BASE + "api..")
            .allowEmptyShould(true);

    /** Dependencies go one way: api -> use cases -> modules. */
    @ArchTest
    static final ArchRule useCasesDoNotDependOnApi = noClasses().that().resideInAPackage(BASE + "usecases..")
            .should().dependOnClassesThat().resideInAPackage(BASE + "api..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule modulesDoNotDependOnUseCasesOrApi = noClasses().that().resideInAnyPackage(
                    BASE + "access..", BASE + "projects..", BASE + "tasks..", BASE + "views..", BASE + "recycle..")
            .should().dependOnClassesThat().resideInAnyPackage(BASE + "usecases..", BASE + "api..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule noCycles = slices().matching(BASE + "(*)..").should().beFreeOfCycles()
            .allowEmptyShould(true);

    private static ArchRule independent(String module, String... others) {
        String[] packages = new String[others.length];
        for (int i = 0; i < others.length; i++) {
            packages[i] = BASE + others[i] + "..";
        }
        return noClasses().that().resideInAPackage(BASE + module + "..")
                .should().dependOnClassesThat().resideInAnyPackage(packages)
                .allowEmptyShould(true);
    }
}
