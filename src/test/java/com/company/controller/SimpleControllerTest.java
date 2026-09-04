package com.company.controller;

import com.company.service.HelloService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SimpleControllerTest {
    @Test
    void healthControllerReportsUp() {
        assertThat(new HealthController().health()).isEqualTo("UP");
    }

    @Test
    void helloControllerDelegatesToService() {
        HelloService service = mock(HelloService.class);
        when(service.getHelloMessage()).thenReturn("hello");
        assertThat(new HelloController(service).sayHello()).isEqualTo("hello");
        verify(service).getHelloMessage();
    }
}
