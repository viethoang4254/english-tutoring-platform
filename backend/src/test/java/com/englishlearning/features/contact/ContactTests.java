package com.englishlearning.features.contact;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpServletRequest;

class ContactTests {
    @SuppressWarnings("unchecked") @Test void disabledAndBoundedDeliveryNeverFabricateSuccess(){
        var provider=(ObjectProvider<JavaMailSender>)mock(ObjectProvider.class);
        var sender=mock(JavaMailSender.class);when(provider.getIfAvailable()).thenReturn(sender);
        var input=new ContactController.Input("Demo","demo@example.invalid",null,"Question","Message");
        assertEquals(503,new ContactController(provider,false,"","").submit(input,new MockHttpServletRequest()).getStatusCode().value());
        verifyNoInteractions(sender);
        var controller=new ContactController(provider,true,"sender@example.invalid","support@example.invalid");
        for(int i=0;i<20;i++)assertEquals(200,controller.submit(input,new MockHttpServletRequest()).getStatusCode().value());
        assertEquals(429,controller.submit(input,new MockHttpServletRequest()).getStatusCode().value());
        verify(sender,times(20)).send(any(org.springframework.mail.SimpleMailMessage.class));
        assertThrows(IllegalArgumentException.class,()->new ContactController(provider,true,"", ""));
    }
}
