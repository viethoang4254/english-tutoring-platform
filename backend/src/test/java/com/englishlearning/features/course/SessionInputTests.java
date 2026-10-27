package com.englishlearning.features.course;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.course.dto.SessionDto;
import com.englishlearning.features.course.service.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionInputTests {
    static SessionDto row() {return new SessionDto(new ApiId(2),new ApiId(1),1,"Title",Instant.EPOCH,Instant.EPOCH.plusSeconds(60),"SCHEDULED","Text",null,Instant.EPOCH,Instant.EPOCH);}
    @Test void omissionNullAndAsciiTrimming() {
        assertEquals(new SessionInput("New","Text"),SessionInput.merge(Map.of("title","  New  "),row()));
        assertEquals(new SessionInput("Title",null),SessionInput.merge(Collections.singletonMap("content",null),row()));
        var fields=new HashMap<String,Object>();fields.put("title",null);fields.put("content",null);
        assertEquals(new SessionInput(null,null),SessionInput.merge(fields,row()));
    }
    @Test void unicodeLimitsCountCodePoints() {
        String emoji="\uD83D\uDE00";
        assertEquals(emoji.repeat(200),SessionInput.merge(Map.of("title",emoji.repeat(200)),row()).title());
        assertThrows(CourseFailure.class,()->SessionInput.merge(Map.of("title",emoji.repeat(201)),row()));
        assertEquals(emoji.repeat(10000),SessionInput.merge(Map.of("content",emoji.repeat(10000)),row()).content());
        assertThrows(CourseFailure.class,()->SessionInput.merge(Map.of("content",emoji.repeat(10001)),row()));
    }
    @Test void rejectsMalformedBlankControlsAndTypes() {
        for(String key:List.of("title","content"))for(Object value:List.of("","   ","\t\r\n","\uD800","\uDC00","a\u0000b","a\u007fb",42,true,List.of("a")))
            assertThrows(CourseFailure.class,()->SessionInput.merge(Map.of(key,value),row()));
        assertThrows(CourseFailure.class,()->SessionInput.merge(Map.of("title","a\nb"),row()));
        assertThrows(CourseFailure.class,()->SessionInput.merge(Map.of(),row()));
        assertThrows(CourseFailure.class,()->SessionInput.merge(Map.of("status","CANCELLED"),row()));
    }
    @Test void contentIsUninterpretedMultilinePlainText() {
        String text="<b>text</b>\n\tline\r\n";
        assertEquals(text,SessionInput.merge(Map.of("content",text),row()).content());
    }
}
