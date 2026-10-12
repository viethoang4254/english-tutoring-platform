package com.englishlearning.features.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.englishlearning.features.user.repository.UserCredentialRepository;
import com.englishlearning.features.user.service.UserCredentialService;
import org.junit.jupiter.api.Test;

class UserCredentialServiceTests {
    @Test void canonicalizesWithoutProviderSpecificNormalization() {
        var repository = mock(UserCredentialRepository.class);
        var service = new UserCredentialService(repository);
        service.findByEmail("  Some.One+Tag@EXAMPLE.COM  ");
        verify(repository).findByEmail("some.one+tag@example.com");
        assertTrue(service.findByEmail(null).isEmpty());
        verifyNoMoreInteractions(repository);
    }
}
