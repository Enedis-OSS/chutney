/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.sql;

import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.notEmptyListValidation;
import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.targetValidation;
import static fr.enedis.chutney.action.spi.validation.Validator.getErrorsFrom;
import static fr.enedis.chutney.action.spi.validation.Validator.of;
import static java.util.Optional.ofNullable;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.ActionsConfiguration;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.action.spi.validation.Validator;
import fr.enedis.chutney.action.sql.core.DefaultSqlClientFactory;
import fr.enedis.chutney.action.sql.core.Records;
import fr.enedis.chutney.action.sql.core.SqlClient;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.StringUtils;

public class SqlAction implements Action {

    private static final String CONFIGURABLE_NB_LOGGED_ROW = "chutney.actions.sql.max-logged-rows";
    private static final Integer DEFAULT_NB_LOGGED_ROW = 30;

    private static final String CONFIGURABLE_MINIMUM_MEMORY_PERCENTAGE_REQUIRED = "chutney.actions.sql.minimum-memory-percentage-required";
    private static final Integer MINIMUM_MEMORY_PERCENTAGE_REQUIRED = 0;

    private final Target target;
    private final Logger logger;
    private final List<String> statements;
    private final Integer nbLoggedRow;
    private final Integer minimumMemoryPercentageRequired;

    private final DefaultSqlClientFactory clientFactory = new DefaultSqlClientFactory();

    public SqlAction(Target target, Logger logger, ActionsConfiguration configuration, @Input("statements") List<String> statements, @Input("nbLoggedRow") Integer nbLoggedRow, @Input("minimumMemoryPercentageRequired") Integer minimumMemoryPercentageRequired) {
        this.target = target;
        this.logger = logger;
        this.statements = statements;
        this.nbLoggedRow = ofNullable(nbLoggedRow)
            .orElse(configuration.getInteger(CONFIGURABLE_NB_LOGGED_ROW, DEFAULT_NB_LOGGED_ROW));
        this.minimumMemoryPercentageRequired = ofNullable(minimumMemoryPercentageRequired)
            .orElse(configuration.getInteger(CONFIGURABLE_MINIMUM_MEMORY_PERCENTAGE_REQUIRED, MINIMUM_MEMORY_PERCENTAGE_REQUIRED));
    }

    @Override
    public List<String> validateInputs() {
        Validator<Target> targetPropertiesValidation = of(target)
            .validate(t -> target.property("jdbcUrl").orElse(""), StringUtils::isNotBlank, "Missing Target property 'jdbcUrl'");
        return getErrorsFrom(
            targetPropertiesValidation,
            targetValidation(target),
            notEmptyListValidation(statements, "statements")
        );
    }

    @Override
    public ActionExecutionResult execute() {
        SqlClient sqlClient = clientFactory.create(target, minimumMemoryPercentageRequired);
        try {
            var records = new ArrayList<Records>();
            Map<String, Object> outputs = new HashMap<>();
            AtomicBoolean failure = new AtomicBoolean(false);
            statements.forEach(statement -> {
                try {
                    Records result = sqlClient.execute(statement);
                    records.add(result);
                    logger.info(result.printable(nbLoggedRow));
                } catch (SQLException e) {
                    logger.error(e.getMessage() + " for " + statement + "; Vendor error code: " + e.getErrorCode());
                    records.add(sqlClient.emptyRecords());
                    failure.set(true);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    records.add(sqlClient.emptyRecords());
                    failure.set(true);
                }
            });

            if (statements.size() == 1) {
                outputs.put("affectedRows", records.getFirst().affectedRows);
                outputs.put("rows", records.getFirst().rows()); // All rows result from the first statement only
                outputs.put("firstRow", records.getFirst().rows().get(0)); // First row of the first statement
                outputs.put("recordResult", records); // List of all results from each statement // TODO - remove after user migration
            } else {
                outputs.put("recordResult", records); // List of all results from each statement
            }

            return failure.get() ? ActionExecutionResult.ko(outputs) : ActionExecutionResult.ok(outputs);
        } finally {
            if (sqlClient != null) {
                sqlClient.closeDatasource();
            }
        }
    }
}
