package org.jbpm.test;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.jbpm.services.task.audit.impl.model.BAMTaskSummaryImpl;
import org.jbpm.services.task.audit.lifecycle.listeners.BAMTaskEventListener;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskLifeCycleEventListener;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.task.api.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a sample file to test a process.
 */
public class ProcessPersistenceHumanTaskOnLaneTest extends JbpmJUnitBaseTestCase {

    private static final Logger logger = LoggerFactory.getLogger(ProcessPersistenceHumanTaskOnLaneTest.class);

    public ProcessPersistenceHumanTaskOnLaneTest() {
        super(true, true);
        
    }

    @Test 
    public void testProcess() throws Exception {
        createRuntimeManager("HumanTaskOnLane.bpmn2");
        RuntimeEngine runtimeEngine = getRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();
        TaskService taskService = runtimeEngine.getTaskService();

        ProcessInstance processInstance = ksession.startProcess("UserTask");

        assertProcessInstanceActive(processInstance.getId(), ksession);
        

        // simulating a system restart
        logger.debug("Reloading the environemnt to simulate system restart");
        disposeRuntimeManager();
        createRuntimeManager("HumanTaskOnLane.bpmn2");
        runtimeEngine = getRuntimeEngine();
        ksession = runtimeEngine.getKieSession();
        taskService = runtimeEngine.getTaskService();

        // let john execute Task 1
        String taskUser = "john";
        String locale = "en-UK";
        List<TaskSummary> list = taskService.getTasksAssignedAsPotentialOwner(taskUser, locale);
        assertEquals(1, list.size());
        
        TaskSummary task = list.get(0);
        taskService.claim(task.getId(), taskUser);
        taskService.start(task.getId(), taskUser);
        taskService.complete(task.getId(), taskUser, null);

        // simulating a system restart
        logger.debug("Reloading the environemnt to simulate system restart once again");
        disposeRuntimeManager();
        createRuntimeManager("HumanTaskOnLane.bpmn2");
        runtimeEngine = getRuntimeEngine();
        ksession = runtimeEngine.getKieSession();
        taskService = runtimeEngine.getTaskService();
        
        
        List<Status> reservedOnly = new ArrayList<Status>();
        reservedOnly.add(Status.Reserved);
        
        list = taskService.getTasksAssignedAsPotentialOwnerByStatus(taskUser, reservedOnly, locale);
        assertEquals(1, list.size());
        
        task = list.get(0);
        taskService.start(task.getId(), taskUser);
        taskService.complete(task.getId(), taskUser, null);


        assertProcessInstanceCompleted(processInstance.getId(), ksession);
    }
    
    @Test 
    public void testProcessWIthDifferentGroups() throws Exception {
        createRuntimeManager("HumanTaskOnLaneDifferentGroups.bpmn2");
        RuntimeEngine runtimeEngine = getRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();
        TaskService taskService = runtimeEngine.getTaskService();

        ProcessInstance processInstance = ksession.startProcess("UserTask");

        assertProcessInstanceActive(processInstance.getId(), ksession);
        

        // simulating a system restart
        logger.debug("Reloading the environemnt to simulate system restart");
        disposeRuntimeManager();
        createRuntimeManager("HumanTaskOnLaneDifferentGroups.bpmn2");
        runtimeEngine = getRuntimeEngine();
        ksession = runtimeEngine.getKieSession();
        taskService = runtimeEngine.getTaskService();

        // let manager execute Task 1
        String taskUser = "manager";
        String locale = "en-UK";
        List<TaskSummary> list = taskService.getTasksAssignedAsPotentialOwner(taskUser, locale);
        assertEquals(1, list.size());
        
        TaskSummary task = list.get(0);
        taskService.claim(task.getId(), taskUser);
        taskService.start(task.getId(), taskUser);
        taskService.complete(task.getId(), taskUser, null);

        // simulating a system restart
        logger.debug("Reloading the environemnt to simulate system restart once again");
        disposeRuntimeManager();
        createRuntimeManager("HumanTaskOnLane.bpmn2");
        runtimeEngine = getRuntimeEngine();
        ksession = runtimeEngine.getKieSession();
        taskService = runtimeEngine.getTaskService();
        
        
        List<Status> reservedAndRegistered = new ArrayList<Status>();
        reservedAndRegistered.add(Status.Reserved);
        reservedAndRegistered.add(Status.Ready);
        // manager does not have access to the second task
        list = taskService.getTasksAssignedAsPotentialOwnerByStatus(taskUser, reservedAndRegistered, locale);
        assertEquals(0, list.size());
        
        // now try john 
        taskUser = "john";
        list = taskService.getTasksAssignedAsPotentialOwnerByStatus(taskUser, reservedAndRegistered, locale);
        assertEquals(1, list.size());
        
        task = list.get(0);
        // task is in ready state so claim is required
        assertEquals(Status.Ready, task.getStatus());
        taskService.claim(task.getId(), taskUser);
        taskService.start(task.getId(), taskUser);
        taskService.complete(task.getId(), taskUser, null);


        assertProcessInstanceCompleted(processInstance.getId(), ksession);
    }

	@SuppressWarnings("unchecked")
	@Test
	public void testProcessWithBAMListener() throws Exception {
		createRuntimeManager("HumanTaskOnLane.bpmn2");
		RuntimeEngine runtimeEngine = getRuntimeEngine();
		KieSession ksession = runtimeEngine.getKieSession();
		TaskService taskService = runtimeEngine.getTaskService();

		((EventService<TaskLifeCycleEventListener>) taskService)
				.registerTaskEventListener(new BAMTaskEventListener());

		ProcessInstance processInstance = ksession.startProcess("UserTask");

		assertProcessInstanceActive(processInstance.getId(), ksession);
		long task1 = -1;
		long task2 = -1;

		// simulating a system restart
		logger.debug("Reloading the environemnt to simulate system restart");
		disposeRuntimeManager();
		createRuntimeManager("HumanTaskOnLane.bpmn2");
		runtimeEngine = getRuntimeEngine();
		ksession = runtimeEngine.getKieSession();
		taskService = runtimeEngine.getTaskService();
		((EventService<TaskLifeCycleEventListener>) taskService)
				.registerTaskEventListener(new BAMTaskEventListener());

		// let john execute Task 1
		String taskUser = "john";
		String locale = "en-UK";
		List<TaskSummary> list = taskService.getTasksAssignedAsPotentialOwner(
				taskUser, locale);
		assertEquals(1, list.size());

		TaskSummary task = list.get(0);
		task1 = task.getId();
		taskService.claim(task.getId(), taskUser);
		taskService.start(task.getId(), taskUser);
		taskService.complete(task.getId(), taskUser, null);

		List<Status> reservedOnly = new ArrayList<Status>();
		reservedOnly.add(Status.Reserved);

		list = taskService.getTasksAssignedAsPotentialOwnerByStatus(taskUser,
				reservedOnly, locale);
		assertEquals(1, list.size());

		task = list.get(0);
		task2 = task.getId();
		taskService.start(task.getId(), taskUser);
		taskService.complete(task.getId(), taskUser, null);

		assertProcessInstanceCompleted(processInstance.getId(), ksession);

		EntityManager em = getEmf().createEntityManager();
		List<BAMTaskSummaryImpl> bamLogs = em.createQuery(
				"from BAMTaskSummaryImpl").getResultList();
		em.close();
		assertNotNull(bamLogs);
		assertEquals(2, bamLogs.size());

		List<Long> taskIdsFromBAM = new ArrayList<Long>();
		for (BAMTaskSummaryImpl bamEntry : bamLogs) {
			taskIdsFromBAM.add(bamEntry.getTaskId());
		}

		assertTrue(taskIdsFromBAM.contains(task1));
		assertTrue(taskIdsFromBAM.contains(task2));
	}
}
