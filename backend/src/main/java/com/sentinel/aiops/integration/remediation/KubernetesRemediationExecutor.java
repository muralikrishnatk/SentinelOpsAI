package com.sentinel.aiops.integration.remediation;

import com.sentinel.aiops.domain.enums.RunbookActionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

/**
 * <b>Adapter</b> over the Kubernetes API. RESTART_SERVICE triggers a rollout restart
 * (patch a pod-template annotation); SCALE_OUT patches the deployment's replica count.
 * Live when an API URL + token are configured.
 */
@Component
@Slf4j
public class KubernetesRemediationExecutor implements RemediationExecutor {

    private final String apiUrl;
    private final String namespace;
    private final String token;

    public KubernetesRemediationExecutor(
            @Value("${aiops.remediation.kubernetes.api-url:}") String apiUrl,
            @Value("${aiops.remediation.kubernetes.namespace:default}") String namespace,
            @Value("${aiops.remediation.kubernetes.token:}") String token) {
        this.apiUrl = apiUrl;
        this.namespace = namespace;
        this.token = token;
    }

    @Override public String mode() { return "kubernetes"; }
    @Override public boolean live() { return apiUrl != null && !apiUrl.isBlank() && token != null && !token.isBlank(); }
    @Override public String detail() { return live() ? apiUrl + " ns=" + namespace : "not configured (aiops.remediation.kubernetes.*)"; }

    private RestClient client() {
        return RestClient.builder().baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + token).build();
    }

    @Override
    public ExecResult execute(RunbookActionType action, String service, String params) {
        String deploymentPath = "/apis/apps/v1/namespaces/" + namespace + "/deployments/" + service;
        switch (action) {
            case RESTART_SERVICE -> {
                // Rollout restart: patch a restartedAt annotation on the pod template.
                client().patch().uri(deploymentPath)
                        .header("Content-Type", "application/strategic-merge-patch+json")
                        .body(Map.of("spec", Map.of("template", Map.of("metadata", Map.of(
                                "annotations", Map.of("kubectl.kubernetes.io/restartedAt", Instant.now().toString()))))))
                        .retrieve().toBodilessEntity();
                log.info("[remediation/k8s] rollout restart {}", service);
                return new ExecResult("[k8s] rollout restart " + service, true);
            }
            case SCALE_OUT -> {
                int replicas = parseReplicas(params, 4);
                client().patch().uri(deploymentPath + "/scale")
                        .header("Content-Type", "application/strategic-merge-patch+json")
                        .body(Map.of("spec", Map.of("replicas", replicas)))
                        .retrieve().toBodilessEntity();
                log.info("[remediation/k8s] scaled {} to {}", service, replicas);
                return new ExecResult("[k8s] scaled " + service + " to " + replicas + " replicas", true);
            }
            default -> {
                // Non-mutating / unsupported in-cluster actions are recorded only.
                return new ExecResult("[k8s] noted " + action + " for " + service + " (manual)", false);
            }
        }
    }

    private int parseReplicas(String params, int fallback) {
        if (params == null) return fallback;
        try {
            String digits = params.replaceAll("\\D+", "");
            return digits.isEmpty() ? fallback : Integer.parseInt(digits);
        } catch (NumberFormatException e) { return fallback; }
    }
}
