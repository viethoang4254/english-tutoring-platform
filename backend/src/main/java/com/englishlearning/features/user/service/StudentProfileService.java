package com.englishlearning.features.user.service;
import com.englishlearning.features.user.dto.StudentProfile;
import com.englishlearning.features.user.repository.StudentProfileRepository;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.auth.security.StudentRegistrationInput;
import java.time.Clock;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class StudentProfileService {
    private final StudentProfileRepository profiles;
    private final CurrentAccountService accounts;
    private final UserCredentialService users;
    private final Clock clock;
    public StudentProfileService(StudentProfileRepository profiles,CurrentAccountService accounts,UserCredentialService users,@Qualifier("refreshSessionClock") Clock clock){this.profiles=profiles;this.accounts=accounts;this.users=users;this.clock=clock;}
    @Transactional(readOnly=true)
    public StudentProfile read(Long id){check(accounts.findById(id).orElse(null));return profiles.read(id);}
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public StudentProfile update(Long id,Map<String,Object> fields){
        check(users.lockAccountById(id).orElse(null));
        if(fields==null || fields.isEmpty() || !java.util.Set.of("fullName","phone").containsAll(fields.keySet()))throw new ProfileFailure(ProfileFailure.Reason.INVALID);
        for(Object value:fields.values())if(!(value instanceof String text) || text.isBlank() || text.indexOf(0)>=0)throw new ProfileFailure(ProfileFailure.Reason.INVALID);
        String phone=null;
        if(fields.containsKey("phone"))try{phone=StudentRegistrationInput.phone((String)fields.get("phone"));}catch(IllegalArgumentException invalid){throw new ProfileFailure(ProfileFailure.Reason.INVALID);}
        profiles.update(id,(String)fields.get("fullName"),phone,clock.instant());return profiles.read(id);
    }
    private void check(AccountSnapshot account){
        if(account==null || !account.loginEligible())throw new ProfileFailure(ProfileFailure.Reason.UNAUTHENTICATED);
        if(account.role()!=Role.STUDENT)throw new ProfileFailure(ProfileFailure.Reason.DENIED);
    }
}
