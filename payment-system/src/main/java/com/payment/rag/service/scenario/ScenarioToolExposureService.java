package com.payment.rag.service.scenario;

import com.payment.rag.model.dto.AiScenario;
import com.payment.rag.service.AuthContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScenarioToolExposureService {

    private final AuthContextService authContextService;
    private final ScenarioToolRegistry scenarioToolRegistry;
    private final CommonScenarioTools commonScenarioTools;
    private final UserScenarioTools userScenarioTools;
    private final MerchantScenarioTools merchantScenarioTools;
    private final AdminScenarioTools adminScenarioTools;

    public Object[] exposedTools(AiScenario scenario, Object... baseTools) {
        String role = authContextService.getCurrentRole();
        List<String> permissions = authContextService.getCurrentPermissions();
        List<ScenarioToolDescriptor> descriptors = scenarioToolRegistry.resolve(scenario, role, permissions);

        List<Object> tools = new ArrayList<>();
        if (baseTools != null) {
            tools.addAll(List.of(baseTools));
        }
        tools.add(commonScenarioTools);

        if (containsAny(descriptors, "user_wallet", "user_orders")) {
            tools.add(userScenarioTools);
        }
        if (containsAny(descriptors, "merchant_orders", "merchant_marketing", "merchant_finance")) {
            tools.add(merchantScenarioTools);
        }
        if (containsAny(descriptors, "admin_governance", "admin_risk")) {
            tools.add(adminScenarioTools);
        }
        return tools.toArray();
    }

    private boolean containsAny(List<ScenarioToolDescriptor> descriptors, String... names) {
        return descriptors.stream()
                .map(ScenarioToolDescriptor::name)
                .anyMatch(name -> List.of(names).contains(name));
    }
}
