package es.udc.paproject.backend;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;

import jakarta.persistence.Entity;

/**
 * Pruebas de integración estructurales (caja blanca) con ArchUnit.
 */
@AnalyzeClasses(packages = "es.udc.paproject.backend", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    private static final ArchCondition<JavaClass> IMPLEMENTAR_LA_INTERFAZ_SIN_SUFIJO_IMPL =
        new ArchCondition<JavaClass>("implementar la interfaz con su mismo nombre sin el sufijo 'Impl'") {
            @Override
            public void check(JavaClass clase, ConditionEvents events) {
                String nombre = clase.getName();
                String nombreInterfaz = nombre.substring(0, nombre.length() - "Impl".length());
                boolean implementa = clase.getAllRawInterfaces().stream()
                    .anyMatch(interfaz -> interfaz.getName().equals(nombreInterfaz));
                String mensaje = nombre + (implementa ? " implementa " : " NO implementa ") + nombreInterfaz;
                events.add(new SimpleConditionEvent(clase, implementa, mensaje));
            }
        };

    // -------------------------------------------------------------------------
    // 1. Dependencias entre capas y paquetes
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule el_modelo_no_depende_de_rest =
        noClasses().that().resideInAPackage("..model..")
            .should().dependOnClassesThat().resideInAPackage("..rest..");

    @ArchTest
    static final ArchRule las_entidades_no_dependen_de_servicios_ni_de_rest =
        noClasses().that().resideInAPackage("..model.entities..")
            .should().dependOnClassesThat().resideInAnyPackage("..model.services..", "..rest..");

    @ArchTest
    static final ArchRule las_excepciones_solo_dependen_de_java_y_de_su_paquete =
        classes().that().resideInAPackage("..model.exceptions..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("..model.exceptions..", "java..");

    @ArchTest
    static final ArchRule los_dtos_no_dependen_de_los_controladores =
        noClasses().that().resideInAPackage("..rest.dtos..")
            .should().dependOnClassesThat().resideInAPackage("..rest.controllers..");

    @ArchTest
    static final ArchRule solo_los_servicios_acceden_a_los_daos =
        classes().that().haveSimpleNameEndingWith("Dao")
            .should().onlyBeAccessed().byAnyPackage("..model.services..", "..model.entities..");

    @ArchTest
    static final ArchRule el_modelo_no_depende_de_tecnologia_web =
        noClasses().that().resideInAPackage("..model..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web..", "jakarta.servlet..");

    @ArchTest
    static final ArchRule jpa_solo_se_usa_en_las_entidades =
        noClasses().that().resideOutsideOfPackage("..model.entities..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule no_hay_ciclos_entre_paquetes =
        slices().matching("es.udc.paproject.backend.(*).(*)..").should().beFreeOfCycles();

    // -------------------------------------------------------------------------
    // 2. Nomenclatura y ubicación
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule los_controladores_estan_en_rest_controllers =
        classes().that().haveSimpleNameEndingWith("Controller")
            .should().resideInAPackage("..rest.controllers..");

    @ArchTest
    static final ArchRule los_daos_estan_en_model_entities =
        classes().that().haveSimpleNameEndingWith("Dao")
            .should().resideInAPackage("..model.entities..");

    @ArchTest
    static final ArchRule los_daos_son_interfaces =
        classes().that().haveSimpleNameEndingWith("Dao")
            .should().beInterfaces();

    @ArchTest
    static final ArchRule los_servicios_estan_en_model_services =
        classes().that().haveSimpleNameEndingWith("ServiceImpl")
            .should().resideInAPackage("..model.services..");

    @ArchTest
    static final ArchRule cada_impl_de_servicio_implementa_su_interfaz =
        classes().that().resideInAPackage("..model.services..").and().haveSimpleNameEndingWith("Impl")
            .should(IMPLEMENTAR_LA_INTERFAZ_SIN_SUFIJO_IMPL);

    @ArchTest
    static final ArchRule los_dtos_estan_en_rest_dtos_o_rest_common =
        classes().that().haveSimpleNameEndingWith("Dto")
            .should().resideInAnyPackage("..rest.dtos..", "..rest.common..");

    @ArchTest
    static final ArchRule los_conversores_estan_en_rest_dtos =
        classes().that().haveSimpleNameEndingWith("Conversor")
            .should().resideInAPackage("..rest.dtos..");

    @ArchTest
    static final ArchRule las_excepciones_estan_en_model_exceptions =
        classes().that().haveSimpleNameEndingWith("Exception")
            .should().resideInAPackage("..model.exceptions..");

    // -------------------------------------------------------------------------
    // 3. Anotaciones y herencia
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule los_controladores_llevan_rest_controller =
        classes().that().haveSimpleNameEndingWith("Controller")
            .should().beAnnotatedWith(RestController.class);

    @ArchTest
    static final ArchRule las_impl_de_servicio_llevan_service =
        classes().that().resideInAPackage("..model.services..").and().haveSimpleNameEndingWith("Impl")
            .should().beAnnotatedWith(Service.class);

    @ArchTest
    static final ArchRule las_impl_de_servicio_llevan_transactional =
        classes().that().resideInAPackage("..model.services..").and().haveSimpleNameEndingWith("Impl")
            .should().beAnnotatedWith(Transactional.class);

    @ArchTest
    static final ArchRule las_entidades_llevan_entity =
        classes().that().resideInAPackage("..model.entities..").and().areTopLevelClasses().and().areNotInterfaces()
            .should().beAnnotatedWith(Entity.class);

    @ArchTest
    static final ArchRule las_clases_entity_estan_en_model_entities =
        classes().that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..model.entities..");

    @ArchTest
    static final ArchRule los_daos_extienden_crud_repository =
        classes().that().haveSimpleNameEndingWith("Dao")
            .should().beAssignableTo(CrudRepository.class);

    @ArchTest
    static final ArchRule las_excepciones_heredan_de_exception =
        classes().that().haveSimpleNameEndingWith("Exception")
            .should().beAssignableTo(Exception.class);

    // -------------------------------------------------------------------------
    // 4. Reglas generales de código
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule no_se_usan_los_flujos_estandar =
        GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    static final ArchRule no_se_lanzan_excepciones_genericas =
        GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
}