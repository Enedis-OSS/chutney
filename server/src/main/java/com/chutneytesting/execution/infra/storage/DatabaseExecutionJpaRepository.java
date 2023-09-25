package com.chutneytesting.execution.infra.storage;

import com.chutneytesting.execution.infra.storage.jpa.ScenarioExecutionEntity;
import com.chutneytesting.server.core.domain.execution.report.ServerReportStatus;
import jakarta.persistence.Tuple;
import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface DatabaseExecutionJpaRepository extends CrudRepository<ScenarioExecutionEntity, Long>, JpaSpecificationExecutor<ScenarioExecutionEntity> {

    List<ScenarioExecutionEntity> findByStatus(ServerReportStatus status);

    List<ScenarioExecutionEntity> findFirst20ByScenarioIdOrderByIdDesc(String scenarioId);

    @Query("select max(se.id), se.scenarioId from SCENARIO_EXECUTIONS se where se.scenarioId in :scenarioIds group by se.scenarioId")
    List<Tuple> findLastExecutionsByScenarioId(@Param("scenarioIds") List<String> scenarioIds);

    List<ScenarioExecutionEntity> findAllByScenarioId(String scenarioId);
}