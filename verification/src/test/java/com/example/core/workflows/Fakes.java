package com.example.core.workflows;

import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.HistoryItem;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.model.WorkflowNode;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Minimal dynamic-proxy fakes for the Granite Workflow API. Unused methods throw. */
final class Fakes {

    private Fakes() {
    }

    @SuppressWarnings("unchecked")
    static <T> T fake(Class<T> type, Map<String, Object> answers) {
        return (T) Proxy.newProxyInstance(Fakes.class.getClassLoader(), new Class<?>[] {type},
            (proxy, method, args) -> {
                if (answers.containsKey(method.getName())) {
                    Object answer = answers.get(method.getName());
                    return answer instanceof Function<?, ?> f
                        ? ((Function<Object[], Object>) f).apply(args) : answer;
                }
                if (method.getName().equals("toString")) {
                    return type.getSimpleName() + "Fake";
                }
                throw new UnsupportedOperationException(type.getSimpleName() + "." + method.getName());
            });
    }

    /** A MetaDataMap backed by a HashMap, with Granite's typed get semantics. */
    static MetaDataMap metaDataMap(Map<String, Object> backing) {
        return (MetaDataMap) Proxy.newProxyInstance(Fakes.class.getClassLoader(),
            new Class<?>[] {MetaDataMap.class}, (proxy, method, args) -> {
                if (method.getName().equals("get") && args.length == 2) {
                    Object value = backing.get((String) args[0]);
                    if (args[1] instanceof Class<?> type) {
                        return value == null ? null : type.cast(value);
                    }
                    return value == null ? args[1] : value;
                }
                return Map.class.getMethod(method.getName(), method.getParameterTypes())
                    .invoke(backing, args);
            });
    }

    /** A ModifiableValueMap backed by a HashMap, with Sling's typed get semantics. */
    static org.apache.sling.api.resource.ModifiableValueMap modifiableValueMap(Map<String, Object> backing) {
        return (org.apache.sling.api.resource.ModifiableValueMap) Proxy.newProxyInstance(
            Fakes.class.getClassLoader(),
            new Class<?>[] {org.apache.sling.api.resource.ModifiableValueMap.class}, (proxy, method, args) -> {
                if (method.getName().equals("get") && args.length == 2) {
                    Object value = backing.get((String) args[0]);
                    if (args[1] instanceof Class<?> type) {
                        return value == null ? null : type.cast(value);
                    }
                    return value == null ? args[1] : value;
                }
                return Map.class.getMethod(method.getName(), method.getParameterTypes())
                    .invoke(backing, args);
            });
    }

    static MetaDataMap processArgs(String value) {
        Map<String, Object> map = new HashMap<>();
        if (value != null) {
            map.put("PROCESS_ARGS", value);
        }
        return metaDataMap(map);
    }

    /** A work item in a running workflow, with its workflow metadata exposed for assertions. */
    record Step(WorkItem item, WorkflowSession session, Map<String, Object> workflowMetadata) {

        static Step of(String workItemId, List<String> historyUsers, Object adaptToResult) {
            // The persisted metadata lives on WorkflowData; Workflow's own map is not saved by
            // the engine, so the fake makes it throw to catch any template still using it.
            Map<String, Object> wfMeta = new HashMap<>();
            Workflow workflow = fake(Workflow.class, Map.of(
                "getId", "/var/workflow/instances/server0/2026-11-17/test_1",
                "getInitiator", "author1"));
            WorkflowNode node = fake(WorkflowNode.class, Map.of("getTitle", "Approve", "getId", "node2"));
            WorkflowData data = fake(WorkflowData.class, Map.of(
                "getPayload", "/content/site/en/page", "getMetaDataMap", metaDataMap(wfMeta)));
            WorkItem item = fake(WorkItem.class, Map.of(
                "getWorkflow", workflow, "getId", workItemId, "getNode", node,
                "getWorkflowData", data, "getCurrentAssignee", "workflow-process-service"));

            // Each entry is a completed participant step: "user" or "user|comment"
            List<HistoryItem> history = new ArrayList<>();
            for (String entry : historyUsers) {
                String[] parts = entry.split("\\|", 2);
                WorkflowNode reviewNode = fake(WorkflowNode.class,
                    Map.of("getType", WorkflowNode.TYPE_PARTICIPANT, "getTitle", "Review"));
                WorkItem reviewItem = fake(WorkItem.class, Map.of("getNode", reviewNode));
                history.add(fake(HistoryItem.class, Map.of(
                    "getUserId", parts[0],
                    "getComment", parts.length > 1 ? parts[1] : "",
                    "getWorkItem", reviewItem)));
            }
            Map<String, Object> sessionAnswers = new HashMap<>();
            sessionAnswers.put("getHistory", history);
            if (adaptToResult != null) {
                sessionAnswers.put("adaptTo", adaptToResult);
            }
            return new Step(item, fake(WorkflowSession.class, sessionAnswers), wfMeta);
        }
    }
}
