package com.englishlearning.features.paymentaccount.service;
import java.util.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
@Component
public class BankDirectory {
    public record Bank(String bin,String code,String name) {}
    private final Map<String,Bank> banks;
    public BankDirectory(JsonMapper json) {
        var result=new HashMap<String,Bank>(); var codes=new HashSet<String>(); var bins=new HashSet<String>();
        try(var in=BankDirectory.class.getResourceAsStream("/bank-directory/vietqr-v1.json")) {
            var root=json.readTree(in);
            if(root.path("version").asInt()!=1 || !root.path("banks").isArray()) throw new IllegalArgumentException();
            for(var row:root.path("banks")) {
                String bin=row.path("bin").asText(),code=row.path("code").asText(),name=row.path("name").asText();
                if(!row.path("bin").isTextual() || !bin.matches("[0-9]{6}") || code.isBlank() || name.isBlank()
                    || !bins.add(bin) || !codes.add(code.toUpperCase(Locale.ROOT)) || !row.path("transferSupported").isBoolean()) throw new IllegalArgumentException();
                if(row.path("transferSupported").asBoolean()) result.put(bin,new Bank(bin,code,name));
            }
            if(result.isEmpty()) throw new IllegalArgumentException();
        } catch(Exception e) { throw new IllegalStateException("Invalid bundled bank directory."); }
        banks=Map.copyOf(result);
    }
    public Bank require(String bin) {
        Bank bank=bin==null?null:banks.get(bin); if(bank==null) throw new ReceivingFailure(ReceivingFailure.Reason.INVALID); return bank;
    }
}
