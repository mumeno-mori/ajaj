package kotowski.mm.backend.ajaj.chat;

import kotowski.mm.backend.ajaj.chat.rag.RagService;
import kotowski.mm.backend.ajaj.infrastructure.config.ProjectsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Scanner;

@Service
@RequiredArgsConstructor
public class ChatRunnerService {

    private final RagService ragService;
    private final ProjectsProperties projectsProperties;

    @Bean
    CommandLineRunner chatRunner(ChatClient chatClient) {
        ragService.buildDb();
        return args -> {
            System.out.println("💬 Spring AI CLI chat (type 'exit' to quit)\n");
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Ty: ");
                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("exit")) break;
                askAi(chatClient, input);
                System.out.println();
            }
            System.out.println("👋 Zakończono rozmowę.");
        };
    }

    private void askAi(ChatClient chatClient, String input) {
        var spinner = new Spinner();
        var enrichedInput = """
                    <--BEGIN QUESTION-->
                    %s
                    <--END QUESTION-->
                
                    <--PROJECT CONTEXT-->
                    %s
                    <--END PROJECT CONTEXT-->
                
                    <--RAG (Retrieval Augmented Generation) CONTEXT-->
                    %s
                    <--END RAG CONTEXT-->
                """.formatted(
                input,
                projectsProperties.getContext(),
                ragService.getInitialContextForQuestion(input)
        );
        chatClient.prompt()
                .system("""
                    You are an expert assistant. Use provided context to answer accurately.
                    Use getAnyContextData tool function to get more context if needed.
                    """)
                .user(enrichedInput)
                .stream()
                .chatResponse()
                .doOnNext(response -> {
                    var output = response.getResult().getOutput();
                    var text = output.getText();
                    if (StringUtils.hasText(text)) {
                        spinner.stop();
                        System.out.print(text);
                    }
                })
                .blockLast();
    }

}
