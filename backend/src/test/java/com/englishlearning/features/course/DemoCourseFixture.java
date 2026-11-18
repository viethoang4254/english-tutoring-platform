package com.englishlearning.features.course;

import java.util.*;
import com.englishlearning.features.course.service.CourseService;
import com.englishlearning.features.course.dto.CourseOutline;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Test classpath only. Never packaged in the backend JAR. */
final class DemoCourseFixture {
    record Demo(String name,String amount,String level,String category,String startDate,int weekday,
                String startTime,String endTime,String description,String objectives,List<String> topics,
                String requirements,String outcomes,List<String> sessionObjectives) {}
    static List<Long> seed(ConfigurableApplicationContext context,long teacher,Map<String,Long> categories) throws Exception {
        if(!"true".equals(System.getenv("GITHUB_ACTIONS")) || !"github-hosted".equals(System.getenv("RUNNER_ENVIRONMENT"))
                || !"jdbc:postgresql://127.0.0.1:15437/task010_courses".equals(context.getEnvironment().getProperty("spring.datasource.url")))
            throw new IllegalStateException("Disposable hosted course fixture only");
        Demo[] demos;
        try(var input=DemoCourseFixture.class.getResourceAsStream("/demo-courses.json")){
            demos=JsonMapper.builder().build().readValue(input,Demo[].class);
        }
        if(demos.length!=4 || Arrays.stream(demos).anyMatch(d->d.topics().size()!=5 || d.sessionObjectives().size()!=5))throw new IllegalStateException("Expected four courses and twenty topics/objectives");
        var courses=context.getBean(CourseService.class);
        var jdbc=context.getBean(JdbcTemplate.class);
        return new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).execute(tx->{
            jdbc.execute("SELECT pg_advisory_xact_lock(20261119)");
            var ids=new ArrayList<Long>();
            for(var demo:demos){
                String description="[DEMO-CI-20261119] "+demo.description();
                var existing=jdbc.queryForList("SELECT id,description FROM courses WHERE teacher_id=? AND name=?",teacher,demo.name());
                if(!existing.isEmpty()){
                    if(existing.size()!=1 || !description.equals(existing.get(0).get("description")))throw new IllegalStateException("Demo name collision; no unrelated course changed");
                    long id=((Number)existing.get(0).get("id")).longValue();
                    var row=courses.get(teacher,id);
                    var sessions=courses.sessions(teacher,id,0,20,null).items();
                    var expected=com.englishlearning.features.course.service.SchedulePlan.generate(
                        List.of(new com.englishlearning.features.course.service.SchedulePlan.Rule(demo.weekday(),java.time.LocalTime.parse(demo.startTime()),java.time.LocalTime.parse(demo.endTime()))),
                        java.time.LocalDate.parse(demo.startDate()),"Asia/Ho_Chi_Minh",5,java.time.Instant.parse("2026-11-18T00:00:00Z"));
                    if(!row.tuition().amount().equals(demo.amount()) || row.sessionCount()!=5 || !row.status().equals("DRAFT") ||
                            !row.plannedStartDate().toString().equals(demo.startDate()) || !row.timezone().equals("Asia/Ho_Chi_Minh") ||
                            row.category().id().value()!=categories.get(demo.category()) || sessions.size()!=5 ||
                            sessions.stream().anyMatch(s->!s.status().equals("SCHEDULED") || !demo.topics().get(s.sessionNumber()-1).equals(s.title())))
                        throw new IllegalStateException("Existing demo differs; no automatic repair");
                    for(int i=0;i<5;i++)if(!sessions.get(i).scheduledStartAt().equals(expected.get(i).start()) || !sessions.get(i).scheduledEndAt().equals(expected.get(i).end()))
                        throw new IllegalStateException("Existing demo schedule differs; no automatic repair");
                    var outline=courses.outline(teacher,id);
                    if(!outline.requirements().equals(demo.requirements()) || !outline.outcomes().equals(demo.outcomes()) ||
                            !outline.objectives().equals(demo.objectives()) || outline.stages().size()!=1 || outline.stages().get(0).topics().size()!=5)
                        throw new IllegalStateException("Existing demo outline differs; no automatic repair");
                    for(int i=0;i<5;i++){
                        var topic=outline.stages().get(0).topics().get(i);
                        if(!topic.title().equals("Buổi "+(i+1)+": "+demo.topics().get(i)) || !topic.description().equals(demo.sessionObjectives().get(i)) ||
                                !demo.sessionObjectives().get(i).equals(courses.session(teacher,id,sessions.get(i).id().value()).content()))
                            throw new IllegalStateException("Existing demo objective differs; no automatic repair");
                    }
                    ids.add(id);continue;
                }
                var fields=new HashMap<String,Object>();
                fields.put("name",demo.name());fields.put("description",description);
                fields.put("categoryId",Long.toString(Objects.requireNonNull(categories.get(demo.category()))));
                fields.put("tuition",Map.of("amount",demo.amount(),"currency","VND"));
                fields.put("minStudents",1);fields.put("maxStudents",12);fields.put("sessionCount",5);
                fields.put("plannedStartDate",demo.startDate());fields.put("timezone","Asia/Ho_Chi_Minh");
                long id=courses.create(teacher,fields).id().value();
                courses.replaceOutline(teacher,id,new CourseOutline(demo.description(),demo.objectives(),demo.level(),
                    demo.requirements(),demo.outcomes(),List.of(new CourseOutline.Stage("Năm buổi thực hành",
                    java.util.stream.IntStream.range(0,5).mapToObj(i->new CourseOutline.Topic("Buổi "+(i+1)+": "+demo.topics().get(i),demo.sessionObjectives().get(i))).toList()))));
                courses.replaceSchedule(teacher,id,Map.of("rules",List.of(Map.of("dayOfWeek",demo.weekday(),"startTime",demo.startTime(),"endTime",demo.endTime()))));
                courses.generate(teacher,id);
                for(var session:courses.sessions(teacher,id,0,20,null).items())
                    courses.updateSession(teacher,id,session.id().value(),Map.of("title",demo.topics().get(session.sessionNumber()-1),"content",demo.sessionObjectives().get(session.sessionNumber()-1)));
                ids.add(id);
            }
            return List.copyOf(ids);
        });
    }
}
