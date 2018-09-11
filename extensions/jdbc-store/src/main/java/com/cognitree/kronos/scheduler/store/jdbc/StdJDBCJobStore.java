/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cognitree.kronos.scheduler.store.jdbc;

import com.cognitree.kronos.scheduler.model.Job;
import com.cognitree.kronos.scheduler.model.JobId;
import com.cognitree.kronos.scheduler.store.JobStore;
import com.cognitree.kronos.scheduler.store.StoreException;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static com.cognitree.kronos.scheduler.store.jdbc.StdJDBCConstants.COL_COMPLETED_AT;
import static com.cognitree.kronos.scheduler.store.jdbc.StdJDBCConstants.COL_CREATED_AT;
import static com.cognitree.kronos.scheduler.store.jdbc.StdJDBCConstants.COL_ID;
import static com.cognitree.kronos.scheduler.store.jdbc.StdJDBCConstants.COL_NAMESPACE;
import static com.cognitree.kronos.scheduler.store.jdbc.StdJDBCConstants.COL_STATUS;
import static com.cognitree.kronos.scheduler.store.jdbc.StdJDBCConstants.COL_WORKFLOW_NAME;
import static com.cognitree.kronos.scheduler.store.jdbc.StdJDBCConstants.TABLE_JOBS;

/**
 * A standard JDBC based implementation of {@link JobStore}.
 */
public class StdJDBCJobStore implements JobStore {
    private static final Logger logger = LoggerFactory.getLogger(StdJDBCJobStore.class);

    private static final String INSERT_JOB = "INSERT INTO " + TABLE_JOBS + " VALUES (?,?,?,?,?,?,?)";
    private static final String LOAD_JOB_BY_NAMESPACE = "SELECT * FROM " + TABLE_JOBS + " WHERE "
            + COL_NAMESPACE + " = ?";
    private static final String LOAD_JOB_BY_ID = "SELECT * FROM " + TABLE_JOBS + " WHERE " + COL_ID + " = ? AND "
            + COL_NAMESPACE + " = ?";
    private static final String LOAD_ALL_JOB_CREATED_BETWEEN = "SELECT * FROM " + TABLE_JOBS + " WHERE "
            + COL_NAMESPACE + " = ? " + "AND " + COL_CREATED_AT + " > ? AND " + COL_CREATED_AT + " < ?";
    private static final String LOAD_JOB_BY_NAME_CREATED_BETWEEN = "SELECT * FROM " + TABLE_JOBS + " WHERE "
            + COL_WORKFLOW_NAME + " = ? " +
            "AND " + COL_NAMESPACE + " = ? AND " + COL_CREATED_AT + " > ? AND " + COL_CREATED_AT + " < ?";
    private static final String LOAD_JOB_BY_NAME_TRIGGER_CREATED_BETWEEN = "SELECT * FROM " + TABLE_JOBS + " WHERE "
            + COL_WORKFLOW_NAME + " = ? " + "AND trigger_name = ? AND " + COL_NAMESPACE + " = ? AND "
            + COL_CREATED_AT + " > ? AND " + COL_CREATED_AT + " < ?";
    private static final String UPDATE_JOB = "UPDATE " + TABLE_JOBS + " SET " + COL_STATUS + " = ?, " + COL_CREATED_AT
            + " = ?, " + COL_COMPLETED_AT + " = ? " + " WHERE " + COL_ID + " = ? AND " + COL_NAMESPACE + " = ?";
    private static final String DELETE_JOB = "DELETE FROM " + TABLE_JOBS + " WHERE " + COL_ID + " = ? AND "
            + COL_NAMESPACE + " = ?";

    private final BasicDataSource dataSource;

    public StdJDBCJobStore(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void store(Job job) throws StoreException {
        logger.debug("Received request to store job {}", job);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT_JOB)) {
            int paramIndex = 0;
            preparedStatement.setString(++paramIndex, job.getId());
            preparedStatement.setString(++paramIndex, job.getWorkflow());
            preparedStatement.setString(++paramIndex, job.getTrigger());
            preparedStatement.setString(++paramIndex, job.getNamespace());
            preparedStatement.setString(++paramIndex, job.getStatus().name());
            JDBCUtil.setLong(preparedStatement, ++paramIndex, job.getCreatedAt());
            JDBCUtil.setLong(preparedStatement, ++paramIndex, job.getCompletedAt());
            preparedStatement.execute();
        } catch (Exception e) {
            logger.error("Error storing job {} into database", job, e);
            throw new StoreException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public List<Job> load(String namespace) throws StoreException {
        logger.debug("Received request to get all jobs in namespace {}", namespace);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(LOAD_JOB_BY_NAMESPACE)) {
            int paramIndex = 0;
            preparedStatement.setString(++paramIndex, namespace);
            final ResultSet resultSet = preparedStatement.executeQuery();
            final ArrayList<Job> jobs = new ArrayList<>();
            while (resultSet.next()) {
                jobs.add(getJob(resultSet));
            }
            return jobs;
        } catch (Exception e) {
            logger.error("Error fetching all jobs from database in namespace {}", namespace, e);
            throw new StoreException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public Job load(JobId jobId) throws StoreException {
        logger.debug("Received request to get job with id {}", jobId);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(LOAD_JOB_BY_ID)) {
            int paramIndex = 0;
            preparedStatement.setString(++paramIndex, jobId.getId());
            preparedStatement.setString(++paramIndex, jobId.getNamespace());
            final ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return getJob(resultSet);
            }
        } catch (Exception e) {
            logger.error("Error fetching job from database with id {}", jobId, e);
            throw new StoreException(e.getMessage(), e.getCause());
        }
        return null;
    }

    @Override
    public List<Job> load(String namespace, long createdAfter, long createdBefore) throws StoreException {
        logger.debug("Received request to get all jobs under namespace {}, created after {}, created before {}",
                namespace, createdAfter, createdBefore);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(LOAD_ALL_JOB_CREATED_BETWEEN)) {
            int paramIndex = 0;
            preparedStatement.setString(++paramIndex, namespace);
            preparedStatement.setLong(++paramIndex, createdAfter);
            preparedStatement.setLong(++paramIndex, createdBefore);
            final ResultSet resultSet = preparedStatement.executeQuery();
            final ArrayList<Job> jobs = new ArrayList<>();
            while (resultSet.next()) {
                jobs.add(getJob(resultSet));
            }
            return jobs;
        } catch (Exception e) {
            logger.error("Error fetching all jobs from database under namespace {} created after {}, created before {}",
                    namespace, createdAfter, createdBefore, e);
            throw new StoreException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public List<Job> loadByWorkflowName(String workflowName, String namespace, long createdAfter, long createdBefore) throws StoreException {
        logger.debug("Received request to get jobs with workflow name {}, namespace {}, created after {}",
                workflowName, namespace, createdAfter);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(LOAD_JOB_BY_NAME_CREATED_BETWEEN)) {
            int paramIndex = 0;
            preparedStatement.setString(++paramIndex, workflowName);
            preparedStatement.setString(++paramIndex, namespace);
            preparedStatement.setLong(++paramIndex, createdAfter);
            preparedStatement.setLong(++paramIndex, createdBefore);
            final ResultSet resultSet = preparedStatement.executeQuery();
            final ArrayList<Job> jobs = new ArrayList<>();
            while (resultSet.next()) {
                jobs.add(getJob(resultSet));
            }
            return jobs;
        } catch (Exception e) {
            logger.error("Error fetching jobs from database with workflow name {}, namespace {}",
                    workflowName, namespace, e);
            throw new StoreException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public List<Job> loadByWorkflowNameAndTriggerName(String workflowName, String triggerName, String namespace,
                                                      long createdAfter, long createdBefore) throws StoreException {
        logger.debug("Received request to get all jobs with workflow name {} under namespace {}, triggerName {}," +
                " created after {}, created before {}", workflowName, namespace, triggerName, createdAfter, createdBefore);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(LOAD_JOB_BY_NAME_TRIGGER_CREATED_BETWEEN)) {
            int paramIndex = 0;
            preparedStatement.setString(++paramIndex, workflowName);
            preparedStatement.setString(++paramIndex, triggerName);
            preparedStatement.setString(++paramIndex, namespace);
            preparedStatement.setLong(++paramIndex, createdAfter);
            preparedStatement.setLong(++paramIndex, createdBefore);
            final ResultSet resultSet = preparedStatement.executeQuery();
            final ArrayList<Job> jobs = new ArrayList<>();
            while (resultSet.next()) {
                jobs.add(getJob(resultSet));
            }
            return jobs;
        } catch (Exception e) {
            logger.error("Error fetching all jobs from database with workflow name {} under namespace {} " +
                    "created after {}, created before {}", workflowName, namespace, createdAfter, createdBefore, e);
            throw new StoreException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void update(Job job) throws StoreException {
        final JobId jobId = job.getIdentity();
        logger.info("Received request to update job to {}", job);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_JOB)) {
            int paramIndex = 0;
            preparedStatement.setString(++paramIndex, job.getStatus().name());
            JDBCUtil.setLong(preparedStatement, ++paramIndex, job.getCreatedAt());
            JDBCUtil.setLong(preparedStatement, ++paramIndex, job.getCompletedAt());
            preparedStatement.setString(++paramIndex, jobId.getId());
            preparedStatement.setString(++paramIndex, jobId.getNamespace());
            preparedStatement.execute();
        } catch (Exception e) {
            logger.error("Error updating job to {}", job, e);
            throw new StoreException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void delete(JobId jobId) throws StoreException {
        logger.debug("Received request to delete job with id {}", jobId);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(DELETE_JOB)) {
            int paramIndex = 0;
            preparedStatement.setString(++paramIndex, jobId.getId());
            preparedStatement.setString(++paramIndex, jobId.getNamespace());
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            logger.error("Error deleting job with id {} from database", jobId, e);
            throw new StoreException(e.getMessage(), e.getCause());
        }
    }

    private Job getJob(ResultSet resultSet) throws Exception {
        int paramIndex = 0;
        Job job = new Job();
        job.setId(resultSet.getString(++paramIndex));
        job.setWorkflow(resultSet.getString(++paramIndex));
        job.setTrigger(resultSet.getString(++paramIndex));
        job.setNamespace(resultSet.getString(++paramIndex));
        job.setStatus(Job.Status.valueOf(resultSet.getString(++paramIndex)));
        job.setCreatedAt(JDBCUtil.getLong(resultSet, ++paramIndex));
        job.setCompletedAt(JDBCUtil.getLong(resultSet, ++paramIndex));
        return job;
    }
}
