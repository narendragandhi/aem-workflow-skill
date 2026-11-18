package com.example.verification;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.HistoryItem;
import com.adobe.granite.workflow.exec.Route;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.model.WorkflowModel;
import com.adobe.granite.workflow.model.WorkflowNode;
import com.adobe.granite.workflow.model.WorkflowTransition;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.example.core.workflows.AiProposeDescriptionProcess;
import com.example.core.workflows.ApplyApprovedDescriptionProcess;
import com.example.core.workflows.ApprovalDecisionRecorder;
import com.example.core.workflows.LongRunningJobStep;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Session;
import javax.servlet.Servlet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Drives the skill's templates inside real workflows on a local AEM author.
 * Only in the verification bundle, which run-on-aem.sh installs and removes again.
 *
 *   POST op=setup                    create test models and a test page
 *   POST op=start&model=..&payload=.. start a workflow, returns its id
 *   GET  op=state&id=..              workflow state, metadata, work items, routes, history
 *   GET  op=page                     the test page's jcr:description
 *   POST op=cleanup                  terminate test workflows, delete models and content
 */
@Component(service = Servlet.class, property = {
    "sling.servlet.paths=/bin/aem-workflow-skill/verify",
    "sling.servlet.methods=GET",
    "sling.servlet.methods=POST"
})
public class VerificationServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    static final String MODEL_ROOT = "/var/workflow/models/aem-workflow-skill-it";
    static final String CONTENT_ROOT = "/content/aem-workflow-skill-it";
    static final String PAGE = CONTENT_ROOT + "/page";
    static final String RESULTS_ROOT = "/var/example/workflow-job-results";
    static final String JOBS_USER = "aem-workflow-skill-it-jobs";
    static final String MAPPING_FACTORY_PID =
        "org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended";
    static final String CONFIG_NAME = "aem-workflow-skill-it";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Reference
    private transient ConfigurationAdmin configAdmin;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {
        handle(request, response);
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {
        handle(request, response);
    }

    private void handle(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {
        ResourceResolver resolver = request.getResourceResolver();
        WorkflowSession wf = resolver.adaptTo(WorkflowSession.class);
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            switch (String.valueOf(request.getParameter("op"))) {
                case "setup" -> setup(resolver, wf, request.getParameter("run"), out);
                case "start" -> out.put("id", wf.startWorkflow(
                    wf.getModel(request.getParameter("model")),
                    wf.newWorkflowData("JCR_PATH", request.getParameter("payload"))).getId());
                case "state" -> state(wf, wf.getWorkflow(request.getParameter("id")), out);
                case "page" -> {
                    Resource content = resolver.getResource(PAGE + "/jcr:content");
                    out.put("description", content == null ? null
                        : content.getValueMap().get("jcr:description", String.class));
                }
                case "configure" -> configure(resolver, out);
                case "cleanup" -> {
                    cleanup(resolver, wf, out);
                    unconfigure(resolver, out);
                }
                default -> {
                    response.setStatus(400);
                    out.put("error", "unknown op");
                }
            }
        } catch (Exception e) {
            response.setStatus(500);
            out.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JSON.writeValue(response.getWriter(), out);
    }

    // ------------------------------------------------------------------ setup

    private void setup(ResourceResolver resolver, WorkflowSession wf, String run, Map<String, Object> out)
            throws Exception {
        // The engine caches models by id and version. A model deleted and recreated under the
        // same id would be served from that cache, so each run gets its own model ids.
        String prefix = (run == null || !run.matches("[a-z0-9-]{1,40}") ? "run" : run) + "-";

        // approval: Review → OR Split → Approve | Reject (the reviewer picks the branch)
        Graph approval = new Graph(wf, prefix + "approval");
        WorkflowNode review = approval.add(participant("Review", "admin"));
        approval.branches(review, List.of(
            List.of(recorder("Approve", "approve")),
            List.of(recorder("Reject", "reject"))));
        out.put("approval", approval.deploy());

        // long-running job: external process step, polled every 5 s
        Graph job = new Graph(wf, prefix + "job");
        WorkflowNode wait = job.add(new WorkflowSpec("Run job and wait", WorkflowNode.TYPE_EXTERNAL_PROCESS,
            Map.of("EXTERNAL_PROCESS", LongRunningJobStep.class.getName(),
                "period", "5", "timeout", "300", "PROCESS_AUTO_ADVANCE", "true")));
        job.link(wait, job.end);
        out.put("job", job.deploy());

        // AI proposal: Propose → Review → OR Split → Approve (record, apply) | Reject (record)
        Graph ai = new Graph(wf, prefix + "ai");
        WorkflowNode propose = ai.add(process("AI propose", AiProposeDescriptionProcess.class.getName()));
        WorkflowNode reviewProposal = ai.add(participant("Review proposal", "admin"));
        ai.link(propose, reviewProposal);
        ai.branches(reviewProposal, List.of(
            List.of(recorder("Approve", "approve"),
                process("Apply approved", ApplyApprovedDescriptionProcess.class.getName())),
            List.of(recorder("Reject", "reject"))));
        out.put("ai", ai.deploy());

        PageManager pages = resolver.adaptTo(PageManager.class);
        if (resolver.getResource(PAGE) == null) {
            if (resolver.getResource(CONTENT_ROOT) == null) {
                pages.create("/content", "aem-workflow-skill-it", null, "Workflow skill verification", false);
            }
            Page page = pages.create(CONTENT_ROOT, "page", null, "Surf Camp", false);
            page.getContentResource().adaptTo(ModifiableValueMap.class).remove("jcr:description");
        }
        resolver.commit();
        out.put("page", PAGE);
    }

    private record WorkflowSpec(String title, String type, Map<String, String> metadata) {
    }

    private static WorkflowSpec process(String title, String className) {
        return new WorkflowSpec(title, WorkflowNode.TYPE_PROCESS,
            Map.of("PROCESS", className, "PROCESS_AUTO_ADVANCE", "true"));
    }

    private static WorkflowSpec participant(String title, String participant) {
        return new WorkflowSpec(title, WorkflowNode.TYPE_PARTICIPANT, Map.of("PARTICIPANT", participant));
    }

    private static WorkflowSpec recorder(String title, String decision) {
        return new WorkflowSpec(title, WorkflowNode.TYPE_PROCESS, Map.of(
            "PROCESS", ApprovalDecisionRecorder.class.getName(),
            "PROCESS_ARGS", "DECISION:" + decision,
            "PROCESS_AUTO_ADVANCE", "true"));
    }

    /**
     * Builds a runtime model through the Workflow API. createNewModel wires
     * start → default "Step 1" → end, and the engine starts from that start node, so the
     * first added step reuses the default node and its outgoing transition.
     */
    private static final class Graph {
        private final WorkflowSession wf;
        private final WorkflowModel model;
        final WorkflowNode end;
        private WorkflowNode reusableNode;
        private WorkflowTransition reusableTransition;

        Graph(WorkflowSession wf, String name) throws WorkflowException {
            this.wf = wf;
            this.model = wf.createNewModel("Workflow skill verification: " + name, MODEL_ROOT + "/" + name);
            this.reusableNode = model.getRootNode().getTransitions().get(0).getTo();
            this.reusableTransition = reusableNode.getTransitions().get(0);
            this.end = reusableTransition.getTo();
        }

        WorkflowNode add(WorkflowSpec spec) {
            WorkflowNode node = reusableNode != null ? reusableNode
                : model.createNode(spec.title(), spec.type(), spec.title());
            reusableNode = null;
            apply(node, spec);
            return node;
        }

        void link(WorkflowNode from, WorkflowNode to) {
            if (reusableTransition != null && reusableTransition.getFrom().getId().equals(from.getId())) {
                reusableTransition.setTo(to);
                reusableTransition = null;
            } else {
                model.createTransition(from, to, null);
            }
        }

        /** from → OR Split → each branch's steps in order → OR Join → end. */
        void branches(WorkflowNode from, List<List<WorkflowSpec>> branches) {
            WorkflowNode split = add(new WorkflowSpec("Decision", WorkflowNode.TYPE_OR_SPLIT, Map.of()));
            WorkflowNode join = add(new WorkflowSpec("Join", WorkflowNode.TYPE_OR_JOIN, Map.of()));
            link(from, split);
            for (List<WorkflowSpec> steps : branches) {
                WorkflowNode previous = split;
                for (WorkflowSpec step : steps) {
                    WorkflowNode node = add(step);
                    link(previous, node);
                    previous = node;
                }
                link(previous, join);
            }
            link(join, end);
        }

        String deploy() throws WorkflowException {
            wf.deployModel(model);
            return model.getId();
        }
    }

    private static void apply(WorkflowNode node, WorkflowSpec step) {
        node.setType(step.type());
        node.setTitle(step.title());
        node.getMetaDataMap().clear();
        node.getMetaDataMap().putAll(step.metadata());
    }

    // ------------------------------------------------------------------ state

    private void state(WorkflowSession wf, Workflow workflow, Map<String, Object> out)
            throws WorkflowException {
        out.put("state", workflow.getState());
        WorkflowModel model = workflow.getWorkflowModel();
        out.put("model", model.getId() + "@" + model.getVersion());
        List<String> steps = new ArrayList<>();
        for (WorkflowNode node : model.getNodes()) {
            steps.add(node.getId() + ":" + node.getType() + ":" + node.getTitle());
        }
        out.put("modelNodes", steps);
        Map<String, Object> meta = new LinkedHashMap<>();
        workflow.getWorkflowData().getMetaDataMap().forEach((k, v) -> meta.put(k, String.valueOf(v)));
        out.put("metadata", meta);

        List<Map<String, Object>> items = new ArrayList<>();
        for (WorkItem item : workflow.getWorkItems()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", item.getId());
            entry.put("node", item.getNode().getTitle());
            entry.put("type", item.getNode().getType());
            // expand=true lists each OR Split branch as its own route, as the Inbox shows them
            List<Map<String, String>> routes = new ArrayList<>();
            for (Route route : wf.getRoutes(item, true)) {
                routes.add(Map.of("id", route.getId(), "name", String.valueOf(route.getName())));
            }
            entry.put("routes", routes);
            Map<String, Object> itemMeta = new LinkedHashMap<>();
            item.getMetaDataMap().forEach((k, v) -> itemMeta.put(k, String.valueOf(v)));
            entry.put("metadata", itemMeta);
            items.add(entry);
        }
        out.put("workItems", items);

        List<Map<String, Object>> history = new ArrayList<>();
        for (HistoryItem h : wf.getHistory(workflow)) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("node", h.getWorkItem() == null ? null : h.getWorkItem().getNode().getTitle());
            entry.put("user", h.getUserId());
            entry.put("action", h.getAction());
            entry.put("comment", h.getComment());
            history.add(entry);
        }
        out.put("history", history);
    }

    // -------------------------------------------- service user for the job consumer

    /**
     * What a project does with repoinit + a service user mapping config, done at run time so
     * the instance needs no restart: a system user that may write job results, read access
     * for the workflow service user, and a mapping for this bundle's "workflow-job-results"
     * subservice.
     */
    private void configure(ResourceResolver resolver, Map<String, Object> out) throws Exception {
        JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
        UserManager users = session.getUserManager();
        Authorizable jobsUser = users.getAuthorizable(JOBS_USER);
        if (jobsUser == null) {
            jobsUser = users.createSystemUser(JOBS_USER, "system/aem-workflow-skill-it");
        }
        Authorizable workflowUser = users.getAuthorizable("workflow-process-service");

        ResourceUtil.getOrCreateResource(resolver, RESULTS_ROOT, "sling:Folder", "sling:Folder", false);
        resolver.commit();
        AccessControlUtils.addAccessControlEntry(session, RESULTS_ROOT, jobsUser.getPrincipal(),
            new String[] {"jcr:all"}, true);
        if (workflowUser != null) {
            AccessControlUtils.addAccessControlEntry(session, RESULTS_ROOT, workflowUser.getPrincipal(),
                new String[] {"jcr:read"}, true);
        }
        session.save();

        Configuration mapping = configAdmin.getFactoryConfiguration(MAPPING_FACTORY_PID, CONFIG_NAME, null);
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("user.mapping", new String[] {
            "com.example.aemworkflowskill.verification:workflow-job-results=[" + JOBS_USER + "]"});
        mapping.update(props);

        out.put("serviceUser", JOBS_USER);
        out.put("workflowUserFound", workflowUser != null);
        out.put("mapping", mapping.getPid());
    }

    private void unconfigure(ResourceResolver resolver, Map<String, Object> out) throws Exception {
        Configuration[] configs = configAdmin.listConfigurations(
            "(service.factoryPid=" + MAPPING_FACTORY_PID + ")");
        if (configs != null) {
            for (Configuration config : configs) {
                if (config.getPid().endsWith("~" + CONFIG_NAME)) {
                    config.delete();
                }
            }
        }
        JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
        Authorizable jobsUser = session.getUserManager().getAuthorizable(JOBS_USER);
        if (jobsUser != null) {
            jobsUser.remove();
        }
        Resource example = resolver.getResource("/var/example");
        if (example != null && !example.hasChildren()) {
            resolver.delete(example);
        }
        resolver.commit();
        out.put("unconfigured", true);
    }

    // ---------------------------------------------------------------- cleanup

    private void cleanup(ResourceResolver resolver, WorkflowSession wf, Map<String, Object> out)
            throws Exception {
        int terminated = 0;
        for (Workflow workflow : wf.getWorkflows(new String[] {"RUNNING", "SUSPENDED", "STALE"})) {
            if (workflow.getWorkflowModel().getId().startsWith(MODEL_ROOT)) {
                wf.terminateWorkflow(workflow);
                terminated++;
            }
        }
        out.put("terminated", terminated);
        for (String path : List.of(MODEL_ROOT, CONTENT_ROOT, RESULTS_ROOT)) {
            Resource resource = resolver.getResource(path);
            if (resource != null) {
                resolver.delete(resource);
            }
        }
        resolver.commit();
        out.put("deleted", List.of(MODEL_ROOT, CONTENT_ROOT, RESULTS_ROOT));
    }
}
