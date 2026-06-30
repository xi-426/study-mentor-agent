package com.studymentor.cli;

import com.studymentor.agent.PromptAgent;
import com.studymentor.llm.LlmClient;
import com.studymentor.llm.LlmConfig;
import com.studymentor.llm.MockLlmClient;
import com.studymentor.llm.OpenAiCompatibleLlmClient;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class StudyMentorCli {

    public static void main(String[] args) {
        List<String> arguments;
        if (args.length > 0) {
            arguments = Arrays.asList(args);
        } else {
            System.out.println("StudyMentor Agent");
            System.out.println("请输入你的学习目标，例如：6周学会 Java Agent 开发");
            System.out.println("默认使用 MockLlmClient；如果要调用真实模型，请添加 --real 并配置环境变量。");
            System.out.print("> ");

            arguments = splitInput(readLine());
        }

        boolean useRealLlm = arguments.contains("--real");
        List<String> goalArguments = arguments.stream()
                .filter(argument -> !"--real".equals(argument))
                .toList();
        String goal = String.join(" ", goalArguments);

        if (goal.isBlank()) {
            System.out.println("请输入你的学习目标，例如：6周学会 Java Agent 开发");
            System.out.print("> ");
            goal = readLine().trim();
        }

        if (goal.isBlank()) {
            throw new IllegalArgumentException("学习目标不能为空");
        }
        if ("demo".equalsIgnoreCase(goal)) {
            goal = "4周学会 Java Agent 开发";
        }

        LlmClient llmClient = useRealLlm
                ? new OpenAiCompatibleLlmClient(LlmConfig.fromEnv())
                : new MockLlmClient();
        PromptAgent agent = new PromptAgent(llmClient);

        String answer = agent.createLearningPlan(goal);
        System.out.println();
        System.out.println(answer);
    }

    private static String readLine() {
        if (System.console() != null) {
            return System.console().readLine();
        }
        return new Scanner(System.in).nextLine();
    }

    private static List<String> splitInput(String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(trimmed.split("\\s+"));
    }
}
