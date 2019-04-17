package com.cognitree.kronos.scheduler;

import com.cognitree.kronos.queue.RAMQueueFactory;
import com.cognitree.kronos.scheduler.events.ConfigUpdate;
import com.cognitree.kronos.scheduler.model.Namespace;
import com.cognitree.kronos.scheduler.model.Workflow;
import com.cognitree.kronos.scheduler.model.WorkflowTrigger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import static com.cognitree.kronos.scheduler.TestHelper.*;

public class ConfigUpdateServiceTest {

    private static final SchedulerApp SCHEDULER_APP = new SchedulerApp();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeClass
    public static void start() throws Exception {
        SCHEDULER_APP.start();
    }

    @AfterClass
    public static void stop() {
        SCHEDULER_APP.stop();
    }

    @Test
    public void testNamespaceUpdates() throws Exception {
        ConfigUpdateService configUpdateService = ConfigUpdateService.getService();
        LinkedBlockingQueue<String> queue = RAMQueueFactory.getQueue(configUpdateService.configUpdatesQueue);

        // Create Namespace
        String testNsName = "testNamespaceUpdates";
        List<Namespace> namespacesBeforeCreate = getNamespacesWithName(testNsName);
        Assert.assertEquals(0, namespacesBeforeCreate.size());
        Namespace namespace = createNamespace(testNsName);
        ConfigUpdate createNamespaceConfigUpdate = createConfigUpdate(ConfigUpdate.Action.create, namespace);
        queue.offer(MAPPER.writerFor(ConfigUpdate.class).writeValueAsString(createNamespaceConfigUpdate));
        Thread.sleep(3000);
        List<Namespace> namespacesAfterCreate = getNamespacesWithName(testNsName);
        Assert.assertEquals(1, namespacesAfterCreate.size());
        Assert.assertEquals(namespace, namespacesAfterCreate.get(0));

        // Update Namespace
        namespace.setDescription("updated description");
        ConfigUpdate updateNamespaceConfigUpdate = createConfigUpdate(ConfigUpdate.Action.update, namespace);
        queue.offer(MAPPER.writerFor(ConfigUpdate.class).writeValueAsString(updateNamespaceConfigUpdate));
        Thread.sleep(3000);
        List<Namespace> namespacesAfterUpdate = getNamespacesWithName(testNsName);
        Assert.assertFalse(namespacesAfterUpdate.isEmpty());
        Assert.assertEquals(1, namespacesAfterUpdate.size());
        Assert.assertEquals(namespace, namespacesAfterUpdate.get(0));
    }

    private List<Namespace> getNamespacesWithName(String testNsName) throws ServiceException {
        return NamespaceService.getService()
                .get().stream()
                .filter(ns -> ns.getName().equals(testNsName))
                .collect(Collectors.toList());
    }

    @Test
    public void testWorkflowUpdates() throws Exception {
        ConfigUpdateService configUpdateService = ConfigUpdateService.getService();
        LinkedBlockingQueue<String> queue = RAMQueueFactory
                .getQueue(configUpdateService.configUpdatesQueue);

        String testNsName = "testWorkflowUpdatesNs";
        // Create Namespace
        Namespace namespace = createNamespace(testNsName);
        ConfigUpdate createNamespaceConfigUpdate = createConfigUpdate(ConfigUpdate.Action.create, namespace);
        queue.offer(MAPPER.writerFor(ConfigUpdate.class).writeValueAsString(createNamespaceConfigUpdate));
        Thread.sleep(3000);

        String testWorkflowName = "testWorkflowUpdatesWf";
        List<Workflow> workflowBeforeCreate = getWorkflowsWithName(testNsName, testWorkflowName);
        Assert.assertEquals(0, workflowBeforeCreate.size());
        // Create Workflow
        Workflow workflow = createWorkflow(testWorkflowName, testNsName);
        ConfigUpdate createWorkflowConfigUpdate = createConfigUpdate(ConfigUpdate.Action.create, workflow);
        queue.offer(MAPPER.writerFor(ConfigUpdate.class).writeValueAsString(createWorkflowConfigUpdate));
        Thread.sleep(3000);
        List<Workflow> workflowsAfterCreate = getWorkflowsWithName(testNsName, testWorkflowName);
        Assert.assertEquals(1, workflowsAfterCreate.size());
        Assert.assertEquals(workflow, workflowsAfterCreate.get(0));

        // Update Workflow
        workflow.setDescription("Updated Description");
        ConfigUpdate updateWorkflowConfigUpdate = createConfigUpdate(ConfigUpdate.Action.update, workflow);
        queue.offer(MAPPER.writerFor(ConfigUpdate.class).writeValueAsString(updateWorkflowConfigUpdate));
        Thread.sleep(3000);
        List<Workflow> workflowsAfterUpdate = getWorkflowsWithName(testNsName, testWorkflowName);
        Assert.assertEquals(1, workflowsAfterUpdate.size());
        Assert.assertEquals(workflow, workflowsAfterUpdate.get(0));

        // Delete Workflow
        ConfigUpdate deleteWorkflowConfigUpdate = createConfigUpdate(ConfigUpdate.Action.delete, workflow);
        queue.offer(MAPPER.writerFor(ConfigUpdate.class).writeValueAsString(deleteWorkflowConfigUpdate));
        Thread.sleep(3000);
        List<Workflow> workflowsAfterDelete = getWorkflowsWithName(testNsName, testWorkflowName);
        Assert.assertEquals(0, workflowsAfterDelete.size());
    }

    private List<Workflow> getWorkflowsWithName(String testNsName, String workflowName) throws Exception {
        return WorkflowService.getService()
                .get(testNsName).stream()
                .filter(wf -> wf.getName().equals(workflowName))
                .collect(Collectors.toList());
    }

    @Test
    public void testWorkflowTriggerUpdates() throws Exception {
        ConfigUpdateService configUpdateService = ConfigUpdateService.getService();
        LinkedBlockingQueue<String> queue = RAMQueueFactory
                .getQueue(configUpdateService.configUpdatesQueue);

        // Create Namespace
        String testNsName = "testWorkflowTriggerUpdatesNs";
        Namespace namespace = createNamespace(testNsName);
        ConfigUpdate createNamespaceConfigUpdate = createConfigUpdate(ConfigUpdate.Action.create, namespace);
        queue.offer(MAPPER.writerFor(ConfigUpdate.class).writeValueAsString(createNamespaceConfigUpdate));
        // Create Workflow
        String testWorkflowName = "testWorkflowTriggerUpdatesWf";
        Workflow workflow = createWorkflow(testWorkflowName, testNsName);
        ConfigUpdate createWorkflowConfigUpdate = createConfigUpdate(ConfigUpdate.Action.create, workflow);
        queue.offer(MAPPER.writerFor(ConfigUpdate.class).writeValueAsString(createWorkflowConfigUpdate));
        Thread.sleep(3000);

        String testTriggerName = "testWorkflowTriggerUpdatesTgr";
        // Create Workflow trigger
        List<WorkflowTrigger> workflowTriggersBeforeCreate = getWorkflowTriggersWithName(testNsName, testWorkflowName, testTriggerName);
        Assert.assertEquals(0, workflowTriggersBeforeCreate.size());
        WorkflowTrigger trigger = createSimpleWorkflowTrigger(testTriggerName, testWorkflowName, testNsName);
        ConfigUpdate createWorkflowTriggerConfigUpdate = createConfigUpdate(ConfigUpdate.Action.create, trigger);
        queue.offer(MAPPER.writerFor(ConfigUpdate.class).writeValueAsString(createWorkflowTriggerConfigUpdate));
        Thread.sleep(3000);
        List<WorkflowTrigger> workflowTriggersAfterCreate = getWorkflowTriggersWithName(testNsName, testWorkflowName, testTriggerName);
        Assert.assertEquals(1, workflowTriggersAfterCreate.size());
        Assert.assertEquals(trigger, workflowTriggersAfterCreate.get(0));

        // Update Workflow trigger
        trigger.setEnabled(false);
        ConfigUpdate updateWorkflowTriggerConfigUpdate = createConfigUpdate(ConfigUpdate.Action.update, trigger);
        queue.offer(MAPPER.writerFor(ConfigUpdate.class).writeValueAsString(updateWorkflowTriggerConfigUpdate));
        Thread.sleep(3000);
        List<WorkflowTrigger> workflowTriggersAfterUpdate = getWorkflowTriggersWithName(testNsName, testWorkflowName, testTriggerName);
        Assert.assertEquals(1, workflowTriggersAfterUpdate.size());
        Assert.assertEquals(trigger, workflowTriggersAfterUpdate.get(0));

        // Update Workflow trigger
        ConfigUpdate deleteWorkflowTriggerConfigUpdate = createConfigUpdate(ConfigUpdate.Action.delete, trigger);
        queue.offer(MAPPER.writerFor(ConfigUpdate.class).writeValueAsString(deleteWorkflowTriggerConfigUpdate));
        Thread.sleep(3000);
        List<WorkflowTrigger> workflowTriggersAfterDelete = getWorkflowTriggersWithName(testNsName, testWorkflowName, testTriggerName);
        Assert.assertEquals(0, workflowTriggersAfterDelete.size());
    }

    private List<WorkflowTrigger> getWorkflowTriggersWithName(String testNsName, String workflowName,
                                                              String triggerName) throws Exception {
        return WorkflowTriggerService.getService()
                .get(testNsName, workflowName).stream()
                .filter(wt -> wt.getName().equals(triggerName))
                .collect(Collectors.toList());
    }
}