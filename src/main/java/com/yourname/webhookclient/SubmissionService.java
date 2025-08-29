package com.yourname.webhookclient;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class SubmissionService implements CommandLineRunner {

    private final WebClient webClient;

    public SubmissionService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://webhook-tester.azurewebsites.net/api").build();
    }

    private static final String NAME = "John Doe";
    private static final String REG_NO = "22bec7153"; // Your Reg No
    private static final String EMAIL = "john@example.com";

    @Override
    public void run(String... args) throws Exception {
        try {
            // Step 1: Generate Webhook
            SubmissionRequest submissionRequest = new SubmissionRequest(NAME, REG_NO, EMAIL);
            WebhookResponse webhookResponse = webClient.post()
                    .uri("/generateWebhook/JAVA")
                    .body(Mono.just(submissionRequest), SubmissionRequest.class)
                    .retrieve()
                    .bodyToMono(WebhookResponse.class)
                    .block();

            if (webhookResponse != null) {
                String accessToken = webhookResponse.getAccessToken();
                System.out.println("Webhook generated successfully. Access Token: " + accessToken);

            // Step 2: Prepare SQL Query as per info.mlx
            String sqlQuery = "SELECT P.AMOUNT AS SALARY, " +
                "CONCAT(E.FIRST_NAME, ' ', E.LAST_NAME) AS NAME, " +
                "FLOOR(DATEDIFF(CURDATE(), E.DOB) / 365.25) AS AGE, " +
                "D.DEPARTMENT_NAME " +
                "FROM PAYMENTS P " +
                "JOIN EMPLOYEE E ON P.EMP_ID = E.EMP_ID " +
                "JOIN DEPARTMENT D ON E.DEPARTMENT = D.DEPARTMENT_ID " +
                "WHERE DAY(P.PAYMENT_TIME) <> 1 " +
                "AND P.AMOUNT = (SELECT MAX(AMOUNT) FROM PAYMENTS WHERE DAY(PAYMENT_TIME) <> 1)";

                // Step 3: Submit the Solution
                SolutionRequest solutionRequest = new SolutionRequest(sqlQuery);
                String submissionResponse = webClient.post()
                        .uri("/testWebhook/JAVA")
                        .header("Authorization", accessToken)
                        .body(Mono.just(solutionRequest), SolutionRequest.class)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                System.out.println("Solution submitted successfully: " + submissionResponse);
            }
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
    }
}
