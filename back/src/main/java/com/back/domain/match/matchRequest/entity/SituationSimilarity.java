package com.back.domain.match.matchRequest.entity;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class SituationSimilarity {
    private SituationSimilarity() {}

    private static final Set<Situation> WORK_OVERLOAD_GROUP = EnumSet.of(
            Situation.NIGHT_WORK,
            Situation.MEETING_BOMB
    );

    private static final Set<Situation> INTERPERSONAL_GROUP = EnumSet.of(
            Situation.BOSS_BLAME,
            Situation.OFFICE_POLITICS_FATIGUE,
            Situation.OFFICE_ROMANCE_LEAK
    );

    private static final Set<Situation> CAREER_CHANGE_GROUP = EnumSet.of(
            Situation.JOB_CHANGE_URGE,
            Situation.SALARY_NEGOTIATION
    );

    private static final List<Set<Situation>> ALL_GROUPS = List.of(
            WORK_OVERLOAD_GROUP,
            INTERPERSONAL_GROUP,
            CAREER_CHANGE_GROUP
    );


    public static Set<Situation> getSimilarGroup(Situation situation) {
        return ALL_GROUPS.stream()
                .filter(group -> group.contains(situation))
                .findFirst()
                .map(EnumSet::copyOf)
                .orElseGet(() -> EnumSet.of(situation));
    }

}
