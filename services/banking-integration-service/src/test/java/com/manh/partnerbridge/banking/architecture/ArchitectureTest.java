package com.manh.partnerbridge.banking.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import com.manh.partnerbridge.banking.application.port.out.ItemQueryPort;
import com.manh.partnerbridge.banking.application.port.in.GetItemUseCase;
import com.manh.partnerbridge.banking.application.port.in.BankingUseCase;
import com.manh.partnerbridge.banking.application.port.out.BankProviderPort;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.manh.partnerbridge.banking")
class ArchitectureTest {
    @ArchTest static final ArchRule domain_is_framework_free = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "..application..", "..adapter..", "..infrastructure..");
    @ArchTest static final ArchRule application_is_inside_adapters = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..", "..infrastructure..");
    @ArchTest static final ArchRule domain_exceptions_are_http_free = noClasses().that().resideInAPackage("..domain.exception..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.http..", "jakarta.servlet..");
    @ArchTest static final ArchRule controllers_do_not_access_outbound_ports = noClasses().that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().areAssignableTo(ItemQueryPort.class);
    @ArchTest static final ArchRule item_controller_accesses_inbound_port = classes().that().haveSimpleName("ItemController")
            .should().dependOnClassesThat().areAssignableTo(GetItemUseCase.class);
    @ArchTest static final ArchRule banking_controller_accesses_inbound_port = classes().that().haveSimpleName("BankingController")
            .should().dependOnClassesThat().areAssignableTo(BankingUseCase.class);
    @ArchTest static final ArchRule item_adapter_implements_port = classes().that().haveSimpleName("InMemoryItemAdapter")
            .should().implement(ItemQueryPort.class);
    @ArchTest static final ArchRule bank_adapter_implements_port = classes().that().haveSimpleName("BankProviderAdapter")
            .should().implement(BankProviderPort.class);
}
