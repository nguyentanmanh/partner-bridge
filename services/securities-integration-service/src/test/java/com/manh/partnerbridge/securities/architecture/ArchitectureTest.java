package com.manh.partnerbridge.securities.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import com.manh.partnerbridge.securities.application.port.out.ItemQueryPort;
import com.manh.partnerbridge.securities.application.port.in.GetItemUseCase;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.manh.partnerbridge.securities")
class ArchitectureTest {
    @ArchTest static final ArchRule domain_is_framework_free = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "..application..", "..adapter..", "..infrastructure..");
    @ArchTest static final ArchRule application_is_inside_adapters = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..", "..infrastructure..");
    @ArchTest static final ArchRule domain_exceptions_are_http_free = noClasses().that().resideInAPackage("..domain.exception..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.http..", "jakarta.servlet..");
    @ArchTest static final ArchRule controllers_do_not_access_outbound_ports = noClasses().that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().areAssignableTo(ItemQueryPort.class);
    @ArchTest static final ArchRule controllers_access_inbound_port = classes().that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().areAssignableTo(GetItemUseCase.class);
    @ArchTest static final ArchRule outbound_adapters_implement_ports = classes().that().resideInAPackage("..adapter.out..")
            .and().haveSimpleNameEndingWith("Adapter").should().implement(ItemQueryPort.class);
}
