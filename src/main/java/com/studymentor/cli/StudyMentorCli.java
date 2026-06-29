package com.studymentor.cli;

import com.studymentor.agent.PromptAgent;
import com.studymentor.llm.MockLlmClient;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class StudyMentorCli {

    public static void main(String[] args) {
        PromptAgent agent = new PromptAgent(new MockLlmClient());

        String goal;
        if (args.length > 0) {
            goal = String.join(" ", args);
        } else {
            System.out.println("StudyMentor Agent");
            System.out.println("请输入你的学习目标，例如：6周学会 Java Agent 开发");
            System.out.print("> ");

            Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
            goal = scanner.nextLine();
        }

        String answer = agent.createLearningPlan(goal);
        System.out.println();
        System.out.println(answer);
    }
}
