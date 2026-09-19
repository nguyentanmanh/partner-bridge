package com.manh.partnerbridge.payment.architecture;

import com.manh.partnerbridge.payment.adapter.out.persistence.InMemoryItemAdapter;
import com.manh.partnerbridge.payment.application.port.in.GetItemUseCase;
import com.manh.partnerbridge.payment.application.usecase.GetItemService;
import com.manh.partnerbridge.payment.architecture.fixtures.adapter.out.AdapterFixtures;
import com.manh.partnerbridge.payment.architecture.fixtures.application.port.in.NotAnInterface;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.net.http.HttpClient;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArchitectureRulesTest {
    static class BadAdapterController {
        InMemoryItemAdapter adapter;
        GetItemUseCase useCase;
    }
    static class BadHttpController {
        HttpClient client;
        GetItemUseCase useCase;
    }
    static class BadUseCaseController { GetItemService service; }
    static class GoodController { GetItemUseCase useCase; }
    static class InboundClassController { NotAnInterface value; }
    @org.springframework.web.bind.annotation.RestController
    static class BadEndpoint { HttpClient client; GetItemUseCase useCase; }

    @Test void concreteOutboundDependencyIsRejectedEvenWithValidInboundPort() {
        var classes = new ClassFileImporter().importClasses(BadAdapterController.class);
        assertTrue(ArchitectureTest.controllers_do_not_access_outbound_ports.evaluate(classes).hasViolation());
        assertTrue(ArchitectureTest.controllers_do_not_access_adapters_or_http_clients.evaluate(classes).hasViolation());
    }

    @Test void directHttpClientIsRejectedEvenWithValidInboundPort() {
        assertTrue(ArchitectureTest.controllers_do_not_access_adapters_or_http_clients
            .evaluate(new ClassFileImporter().importClasses(BadHttpController.class)).hasViolation());
    }

    @Test void concreteUseCaseIsNotAnInboundInterface() {
        var classes = new ClassFileImporter().importClasses(BadUseCaseController.class);
        assertTrue(ArchitectureTest.controllers_access_inbound_port.evaluate(classes).hasViolation());
        assertTrue(ArchitectureTest.controllers_do_not_access_concrete_use_cases.evaluate(classes).hasViolation());
    }

    @Test void interfaceUseCaseIsAccepted() {
        var classes = new ClassFileImporter().importClasses(GoodController.class);
        assertFalse(ArchitectureTest.controllers_access_inbound_port.evaluate(classes).hasViolation());
        assertFalse(ArchitectureTest.controllers_do_not_access_concrete_use_cases.evaluate(classes).hasViolation());
    }

    @Test void inboundPackageAloneDoesNotQualifyAsUseCase() {
        var classes = new ClassFileImporter().importClasses(InboundClassController.class);
        assertTrue(ArchitectureTest.controllers_access_inbound_port.evaluate(classes).hasViolation());
        assertTrue(ArchitectureTest.controllers_do_not_access_concrete_use_cases.evaluate(classes).hasViolation());
    }

    @Test void abstractAdaptersMustImplementPortsToo() {
        assertTrue(ArchitectureTest.outbound_adapters_implement_ports
            .evaluate(new ClassFileImporter().importClasses(AdapterFixtures.MissingPortAdapter.class)).hasViolation());
        assertFalse(ArchitectureTest.outbound_adapters_implement_ports
            .evaluate(new ClassFileImporter().importClasses(AdapterFixtures.ValidAbstractAdapter.class)).hasViolation());
    }

    @Test void controllerAnnotationCannotBypassRulesByChangingTheClassName() {
        assertTrue(ArchitectureTest.controllers_do_not_access_adapters_or_http_clients
            .evaluate(new ClassFileImporter().importClasses(BadEndpoint.class)).hasViolation());
    }
}
