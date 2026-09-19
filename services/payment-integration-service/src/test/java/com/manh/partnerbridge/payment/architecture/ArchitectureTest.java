package com.manh.partnerbridge.payment.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

@AnalyzeClasses(packages = "com.manh.partnerbridge.payment", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    private static boolean inPackage(JavaClass type, String suffix) {
        return type.getPackageName().endsWith(suffix) || type.getPackageName().contains(suffix + ".");
    }
    private static final DescribedPredicate<JavaClass> CONTROLLERS = new DescribedPredicate<>("controllers by annotation or name") {
        public boolean test(JavaClass type) {
            return type.getSimpleName().endsWith("Controller")
                || type.isAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                || type.isAnnotatedWith("org.springframework.stereotype.Controller");
        }
    };
    private static final DescribedPredicate<JavaClass> INBOUND_INTERFACE = new DescribedPredicate<>("an inbound port interface") {
        public boolean test(JavaClass type) {
            return type.isInterface() && inPackage(type, ".application.port.in");
        }
    };
    private static final DescribedPredicate<JavaClass> OUTBOUND_INTERFACE = new DescribedPredicate<>("an outbound port interface") {
        public boolean test(JavaClass type) {
            return type.isInterface() && inPackage(type, ".application.port.out");
        }
    };
    private static final DescribedPredicate<JavaClass> OUTBOUND_TYPE = new DescribedPredicate<>("an outbound port or implementation") {
        public boolean test(JavaClass type) {
            return OUTBOUND_INTERFACE.test(type) || type.getAllRawInterfaces().stream().anyMatch(OUTBOUND_INTERFACE::test);
        }
    };
    private static final DescribedPredicate<JavaClass> INVALID_USE_CASE_TYPE = new DescribedPredicate<>("a concrete use case or non-interface inbound type") {
        public boolean test(JavaClass type) {
            return inPackage(type, ".application.usecase")
                || (inPackage(type, ".application.port.in") && !type.isInterface())
                || (!type.isInterface() && type.getAllRawInterfaces().stream().anyMatch(INBOUND_INTERFACE::test));
        }
    };

    @ArchTest
    static final ArchRule domain_is_framework_free = noClasses().that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..", "com.fasterxml.jackson..",
            "java.net..", "org.apache.hc..", "okhttp3..", "org.slf4j..", "..application..", "..adapter..", "..infrastructure..");

    @ArchTest
    static final ArchRule application_is_inside_adapters = noClasses().that().resideInAPackage("..application..")
        .should().dependOnClassesThat().resideInAnyPackage("..adapter..", "..infrastructure..",
            "org.springframework..", "jakarta..", "com.fasterxml.jackson..", "java.net..",
            "org.apache.hc..", "okhttp3..", "retrofit2..", "feign..", "org.slf4j..");

    @ArchTest
    static final ArchRule domain_exceptions_are_http_free = noClasses().that().resideInAPackage("..domain.exception..")
        .should().dependOnClassesThat().resideInAnyPackage("org.springframework.http..", "jakarta.servlet..", "java.net..");

    @ArchTest
    static final ArchRule controllers_do_not_access_outbound_ports = noClasses().that(CONTROLLERS)
        .should().dependOnClassesThat(OUTBOUND_TYPE);

    @ArchTest
    static final ArchRule controllers_do_not_access_adapters_or_http_clients = noClasses().that(CONTROLLERS)
        .should().dependOnClassesThat().resideInAnyPackage("..adapter.out..", "java.net..", "org.springframework.web.client..",
            "org.springframework.web.reactive.function.client..", "org.springframework.http.client..",
            "org.apache.http..", "org.apache.hc..", "okhttp3..", "retrofit2..", "feign..");

    @ArchTest
    static final ArchRule controllers_access_inbound_port = classes().that(CONTROLLERS)
        .should().dependOnClassesThat(INBOUND_INTERFACE);

    @ArchTest
    static final ArchRule controllers_do_not_access_concrete_use_cases = noClasses().that(CONTROLLERS)
        .should().dependOnClassesThat(INVALID_USE_CASE_TYPE);

    @ArchTest
    static final ArchRule outbound_adapters_implement_ports = classes().that().resideInAPackage("..adapter.out..")
        .and().haveSimpleNameEndingWith("Adapter").should(new ArchCondition<JavaClass>("implement an outbound port, including abstract adapters") {
            public void check(JavaClass type, ConditionEvents events) {
                events.add(new SimpleConditionEvent(type, OUTBOUND_TYPE.test(type),
                    type.getName() + " must implement an outbound port interface"));
            }
        });
}
